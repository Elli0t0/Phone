package io.github.elli0t0.phone.core.recording.adb

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.util.Log
import io.github.elli0t0.phone.core.recording.WavWriter
import java.io.ByteArrayOutputStream
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer

/**
 * Shell-side recorder helper. Started as shell via app_process (see
 * AdbRecordingBackend) and detached from the ADB session, so it keeps
 * running after wireless debugging is switched off, until reboot.
 * Speaks a tiny line protocol over an abstract-namespace local socket:
 * START (with a file descriptor attached) / STOP / STATUS / QUIT.
 * Uses only framework classes plus WavWriter, all present in the APK's
 * classpath under app_process. Never touches the app process, storage
 * permissions, or ContentResolver: the app hands it a descriptor for a
 * MediaStore file it created, exactly like the Shizuku path.
 */
private const val TAG = "PhoneAdbRecorder"

fun main(args: Array<String>) {
    RecordingHelperServer().run()
}

private class RecordingHelperServer {
    private val lock = Any()
    private var recorder: AudioRecord? = null
    private var output: FileOutputStream? = null
    private var captureThread: Thread? = null
    private var pcmBytes = 0L
    private var stopRequested = false

    fun run() {
        val server = try {
            LocalServerSocket(ADB_RECORDING_SOCKET)
        } catch (e: Exception) {
            Log.e(TAG, "Could not listen", e)
            return
        }
        Log.i(TAG, "Helper listening")
        try {
            while (true) {
                val socket = try {
                    server.accept()
                } catch (e: Exception) {
                    break
                }
                try {
                    if (handle(socket)) break
                } catch (e: Exception) {
                    Log.e(TAG, "Command failed", e)
                } finally {
                    runCatching { socket.close() }
                }
            }
        } finally {
            runCatching { server.close() }
        }
    }

    /** Returns true when the server loop should exit (QUIT). */
    private fun handle(socket: LocalSocket): Boolean {
        val input = socket.inputStream
        val command = readLine(input)
        val reply = when (command) {
            "START" -> startRecording(socket.ancillaryFileDescriptors?.firstOrNull())
            "STOP" -> stopRecording()?.let { "OK $it" } ?: "ERR not_recording"
            "STATUS" -> if (isRecording()) "RECORDING" else "IDLE"
            "QUIT" -> {
                stopRecording()
                socket.outputStream.write("OK\n".toByteArray(Charsets.US_ASCII))
                socket.outputStream.flush()
                return true
            }
            else -> "ERR unknown"
        }
        socket.outputStream.write("$reply\n".toByteArray(Charsets.US_ASCII))
        socket.outputStream.flush()
        return false
    }

    private fun startRecording(fd: FileDescriptor?): String {
        if (fd == null) return "ERR no_fd"
        synchronized(lock) {
            if (captureThread != null) return "ERR already_recording"
            val minBuffer = AudioRecord.getMinBufferSize(
                WavWriter.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuffer <= 0) return "ERR audio"
            val record = AudioRecord(
                MediaRecorder.AudioSource.VOICE_CALL,
                WavWriter.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuffer, 8192)
            )
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                return "ERR audio"
            }
            val out = FileOutputStream(fd)
            try {
                out.write(WavWriter.header(0))
                out.flush()
            } catch (e: Exception) {
                Log.e(TAG, "Could not write WAV header", e)
                runCatching { out.close() }
                record.release()
                return "ERR io"
            }
            recorder = record
            output = out
            pcmBytes = 0L
            stopRequested = false
            val thread = Thread(::captureLoop, "phone-recording")
            captureThread = thread
            thread.start()
            return "OK"
        }
    }

    private fun captureLoop() {
        val record = synchronized(lock) { recorder } ?: return
        val buffer = ByteArray(8192)
        try {
            record.startRecording()
            while (!stopRequested) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    synchronized(lock) {
                        output?.write(buffer, 0, read)
                        pcmBytes += read
                    }
                } else if (read < 0) {
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Capture loop failed", e)
        } finally {
            runCatching { record.stop() }
            runCatching { record.release() }
        }
    }

    private fun stopRecording(): Long? {
        val thread = synchronized(lock) { captureThread } ?: return null
        stopRequested = true
        runCatching { synchronized(lock) { recorder }?.stop() }
        thread.join(5000)
        synchronized(lock) {
            captureThread = null
            recorder = null
            try {
                output?.flush()
                val channel = output?.channel
                if (channel != null) {
                    channel.position(0)
                    val header = ByteBuffer.wrap(WavWriter.header(pcmBytes.toInt()))
                    while (header.hasRemaining()) {
                        channel.write(header)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Could not patch WAV header", e)
            } finally {
                runCatching { output?.close() }
                output = null
            }
            val written = pcmBytes
            pcmBytes = 0L
            return written
        }
    }

    private fun isRecording(): Boolean = synchronized(lock) { captureThread != null }

    private fun readLine(input: InputStream): String {
        val bytes = ByteArrayOutputStream()
        while (true) {
            val b = input.read()
            if (b == -1 || b == '\n'.code) break
            if (b != '\r'.code) bytes.write(b)
        }
        return String(bytes.toByteArray(), Charsets.US_ASCII)
    }
}
