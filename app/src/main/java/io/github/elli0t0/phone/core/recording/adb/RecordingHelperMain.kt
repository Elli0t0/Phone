package io.github.elli0t0.phone.core.recording.adb

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import io.github.elli0t0.phone.core.recording.WavWriter
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread

/**
 * Shell-side recorder helper. Started as shell via app_process (see
 * AdbRecordingBackend) and detached from the ADB session, so it keeps
 * running after wireless debugging is switched off, until reboot.
 *
 * Speaks a tiny line protocol over loopback TCP (an abstract local
 * socket owned by the shell SELinux domain turned out to be denied to
 * the app): every connection sends the launch token as its first line
 * and is closed immediately otherwise, then one command:
 * STATUS -> IDLE|RECORDING, QUIT -> OK (and the helper exits),
 * START -> OK followed by raw 16-bit mono PCM chunks on the same
 * connection until the app closes it (or ERR <reason> instead of OK).
 *
 * Uses only framework classes plus WavWriter's constants, all present
 * in the APK's classpath under app_process. Never touches the app
 * process, storage permissions, or ContentResolver: the app writes the
 * PCM it reads here into its own MediaStore file.
 *
 * Everything is wrapped so failures land in stderr, which the backend
 * redirects to [HELPER_LOG_PATH] and reads back for diagnostics.
 */
fun main(args: Array<String>) {
    try {
        val port = args.getOrNull(0)?.toIntOrNull()
        val token = args.getOrNull(1)?.takeIf { it.isNotBlank() }
        if (port == null || token == null) {
            System.err.println("usage: RecordingHelperMainKt <port> <token>")
            return
        }
        RecordingHelperServer(port, token).run()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private class RecordingHelperServer(
    private val port: Int,
    private val token: String
) {
    private val lock = Any()
    private var recorder: AudioRecord? = null
    private var captureThread: Thread? = null
    @Volatile
    private var stopRequested = false
    private var serverSocket: ServerSocket? = null
    private var quitRequested = false

    fun run() {
        val server = try {
            ServerSocket(port, 4, InetAddress.getByName("127.0.0.1"))
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }
        serverSocket = server
        System.err.println("Helper listening on 127.0.0.1:$port")
        try {
            while (true) {
                val socket = try {
                    server.accept()
                } catch (e: Exception) {
                    if (!quitRequested) e.printStackTrace()
                    break
                }
                // Daemon threads: a blocked watcher must not keep the
                // process alive after QUIT closes the server socket.
                thread(isDaemon = true) { handle(socket) }
            }
        } finally {
            runCatching { server.close() }
        }
    }

    private fun handle(socket: Socket) {
        try {
            val input = socket.inputStream
            // A bare TCP probe (connect + close, no data) reads EOF
            // here and is dropped, same as a wrong token.
            val clientToken = readLine(input)
            if (clientToken != token) {
                runCatching { socket.close() }
                return
            }
            when (val command = readLine(input)) {
                "STATUS" -> {
                    reply(socket, if (isRecording()) "RECORDING" else "IDLE")
                    runCatching { socket.close() }
                }
                "QUIT" -> {
                    reply(socket, "OK")
                    stopCaptureInternal()
                    quitRequested = true
                    runCatching { socket.close() }
                    runCatching { serverSocket?.close() }
                }
                "START" -> {
                    val result = startCapture(socket)
                    reply(socket, result)
                    if (result == "OK") {
                        // This thread watches the socket: EOF (the app
                        // closing it) ends the capture promptly, even
                        // while the writer thread is blocked in a read.
                        try {
                            input.read()
                        } catch (_: Exception) {
                        }
                        stopCaptureInternal()
                        runCatching { socket.close() }
                    } else {
                        runCatching { socket.close() }
                    }
                }
                else -> {
                    System.err.println("Unknown helper command: $command")
                    reply(socket, "ERR unknown")
                    runCatching { socket.close() }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            runCatching { socket.close() }
        }
    }

    /**
     * Starts capture on [socket]'s output. A capture orphaned by an
     * app restart is preempted: the new session owns the recorder.
     * Returns "OK" or "ERR <reason>" (the exact AudioRecord failure
     * text when creation or start throws, so it shows in the UI).
     */
    private fun startCapture(socket: Socket): String {
        if (isRecording()) stopCaptureInternal()
        synchronized(lock) {
            val minBuffer = AudioRecord.getMinBufferSize(
                WavWriter.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuffer <= 0) return "ERR audio"
            val record = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    AudioRecord.Builder()
                        .setAudioSource(MediaRecorder.AudioSource.VOICE_CALL)
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setSampleRate(WavWriter.SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .build()
                        )
                        .setBufferSizeInBytes(maxOf(minBuffer, 8192))
                        .build()
                } else {
                    AudioRecord(
                        MediaRecorder.AudioSource.VOICE_CALL,
                        WavWriter.SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        maxOf(minBuffer, 8192)
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return "ERR ${e.message ?: e.toString()}"
            }
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                return "ERR audio"
            }
            try {
                record.startRecording()
            } catch (e: Exception) {
                e.printStackTrace()
                record.release()
                return "ERR ${e.message ?: e.toString()}"
            }
            recorder = record
            stopRequested = false
            val thread = thread(isDaemon = true, name = "phone-recording") {
                captureLoop(socket)
            }
            captureThread = thread
            return "OK"
        }
    }

    /** Writer thread: raw PCM to the START connection until the app
     * closes it (write fails), the capture errors, or a stop arrives. */
    private fun captureLoop(socket: Socket) {
        val record = synchronized(lock) { recorder } ?: return
        val buffer = ByteArray(8192)
        try {
            while (!stopRequested) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    try {
                        socket.outputStream.write(buffer, 0, read)
                    } catch (_: Exception) {
                        break
                    }
                } else if (read < 0) {
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            synchronized(lock) {
                if (captureThread == Thread.currentThread()) {
                    captureThread = null
                }
                // Exactly-once release: a preempt owns the release of
                // the recorder it replaced, so only release when this
                // loop's recorder is still the current one.
                if (recorder === record) {
                    recorder = null
                    runCatching { record.release() }
                }
            }
            runCatching { socket.close() }
        }
    }

    /**
     * Stops the capture thread and releases the recorder. The join
     * runs outside the lock so the writer's finally (which takes the
     * lock) can complete; stop() unblocks its read first.
     */
    private fun stopCaptureInternal() {
        val thread = synchronized(lock) {
            stopRequested = true
            runCatching { recorder?.stop() }
            captureThread
        }
        thread?.join(5000)
        synchronized(lock) {
            val stale = recorder
            recorder = null
            runCatching { stale?.release() }
            captureThread = null
            stopRequested = false
        }
    }

    private fun isRecording(): Boolean = synchronized(lock) { captureThread != null }

    private fun reply(socket: Socket, text: String) {
        socket.outputStream.write("$text\n".toByteArray(Charsets.US_ASCII))
        socket.outputStream.flush()
    }

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
