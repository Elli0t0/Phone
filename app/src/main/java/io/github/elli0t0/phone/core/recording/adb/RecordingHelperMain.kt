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
 * START [CALL|MIC] -> OK followed by raw 16-bit mono PCM chunks on
 * the same connection until the app closes it (or ERR <reason>
 * instead of OK). CALL (MediaRecorder.AudioSource.VOICE_CALL) is the
 * default; MIC is the settings-page test only, because VOICE_CALL
 * returns audio solely during an active call.
 *
 * VOICE_CALL fallback (own function, easy to remove): if the first 2 s
 * of a CALL capture are pure silence, the helper retries once with
 * VOICE_DOWNLINK + VOICE_UPLINK mixed (summed with clipping) into one
 * mono stream. When mixing is not possible on this device, the exact
 * exception is logged as FALLBACK_FAILED and the VOICE_CALL stream
 * resumes, so the recording still exists and the failure is visible.
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
    /** Second mixed-capture record (VOICE_UPLINK); null outside the fallback. */
    private var secondRecord: AudioRecord? = null
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
            val command = readLine(input)
            when {
                command == "STATUS" -> {
                    reply(socket, (if (isRecording()) "RECORDING" else "IDLE") + " v$HELPER_VERSION")
                    runCatching { socket.close() }
                }
                command == "QUIT" -> {
                    reply(socket, "OK")
                    stopCaptureInternal()
                    quitRequested = true
                    runCatching { socket.close() }
                    runCatching { serverSocket?.close() }
                }
                command == "START" || command.startsWith("START ") -> {
                    // CALL is the default; the app passes MIC only for
                    // the settings-page test. Anything else after START
                    // is treated as CALL, never rejected: a test must
                    // not fail on a protocol spelling detail.
                    val source = if (command == "START MIC") {
                        MediaRecorder.AudioSource.MIC
                    } else {
                        MediaRecorder.AudioSource.VOICE_CALL
                    }
                    val result = startCapture(socket, source)
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
     * SESSION_START marks the log so the app can attribute a later
     * FALLBACK_FAILED line to this capture, not an earlier one.
     */
    private fun startCapture(socket: Socket, source: Int): String {
        if (isRecording()) stopCaptureInternal()
        val record = try {
            createRecord(source)
        } catch (e: Exception) {
            return "ERR ${e.message ?: e.toString()}"
        }
        synchronized(lock) {
            recorder = record
            stopRequested = false
            val thread = thread(isDaemon = true, name = "phone-recording") {
                captureLoop(socket, source)
            }
            captureThread = thread
        }
        System.err.println("SESSION_START")
        return "OK"
    }

    /**
     * Creates and starts an AudioRecord for [source], or throws with
     * the exact failure (creation/start exceptions are printed, the
     * "audio" sentinel covers an unusable configuration). Shared by
     * the main capture and the mixed fallback so both fail identically.
     */
    private fun createRecord(source: Int): AudioRecord {
        val minBuffer = AudioRecord.getMinBufferSize(
            WavWriter.SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) throw IllegalStateException("audio")
        val record = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AudioRecord.Builder()
                    .setAudioSource(source)
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
                    source,
                    WavWriter.SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    maxOf(minBuffer, 8192)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw IllegalStateException("audio")
        }
        try {
            record.startRecording()
        } catch (e: Exception) {
            e.printStackTrace()
            record.release()
            throw e
        }
        return record
    }

    /** Writer thread: raw PCM to the START connection until the app
     * closes it (write fails), the capture errors, or a stop arrives.
     * For CALL captures the first 2 s are watched: pure silence there
     * triggers the mixed downlink/uplink retry once (see below). The
     * app only starts a CALL capture once the call is ACTIVE, so the
     * 2 s window runs during a connected call, not dialing. */
    private fun captureLoop(socket: Socket, source: Int) {
        var record = synchronized(lock) { recorder } ?: return
        val buffer = ByteArray(8192)
        var samplesSeen = 0L
        var allZero = true
        var fallbackAttempted = false
        try {
            while (!stopRequested) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    if (source == MediaRecorder.AudioSource.VOICE_CALL && !fallbackAttempted) {
                        var i = 0
                        while (i + 1 < read) {
                            val sample = (buffer[i].toInt() and 0xFF) or
                                (buffer[i + 1].toInt() shl 8)
                            if (sample != 0) {
                                allZero = false
                                break
                            }
                            i += 2
                        }
                        samplesSeen += read / 2
                        if (samplesSeen >= 2L * WavWriter.SAMPLE_RATE && allZero) {
                            fallbackAttempted = true
                            // The mixed loop owns the socket to the end
                            // when it runs; otherwise resume VOICE_CALL
                            // with a fresh record and keep the stream.
                            if (runMixedCapture(socket)) return
                            runCatching { record.stop() }
                            runCatching { record.release() }
                            record = try {
                                createRecord(MediaRecorder.AudioSource.VOICE_CALL)
                            } catch (_: Exception) {
                                break
                            }
                            synchronized(lock) { recorder = record }
                            continue
                        }
                    }
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
                // loop's recorder is still the current one. After a
                // mixed capture the fields already moved on (or were
                // nulled), so this check skips them safely.
                if (recorder === record) {
                    recorder = null
                    runCatching { record.release() }
                }
            }
            runCatching { socket.close() }
        }
    }

    /**
     * VOICE_CALL fallback, kept as its own function so it is easy to
     * remove: VOICE_DOWNLINK + VOICE_UPLINK as two AudioRecord
     * instances, summed sample-wise with clipping into one mono
     * stream on the same socket. Returns true when the mixed loop ran
     * to its end (stop or socket error); false when mixing is not
     * possible on this device, with the exact exception logged as
     * FALLBACK_FAILED for the app to surface under Recorder status.
     * The two streams are read in turn, so they can drift by up to one
     * buffer; that is accepted for a fallback path.
     */
    private fun runMixedCapture(socket: Socket): Boolean {
        val old = synchronized(lock) { recorder }
        runCatching { old?.stop() }
        runCatching { old?.release() }
        val down: AudioRecord
        val up: AudioRecord
        try {
            down = createRecord(MediaRecorder.AudioSource.VOICE_DOWNLINK)
            try {
                up = createRecord(MediaRecorder.AudioSource.VOICE_UPLINK)
            } catch (e: Exception) {
                runCatching { down.stop() }
                runCatching { down.release() }
                throw e
            }
        } catch (e: Exception) {
            System.err.println("FALLBACK_FAILED: ${e.message ?: e.toString()}")
            e.printStackTrace()
            return false
        }
        synchronized(lock) {
            recorder = down
            secondRecord = up
        }
        val bufferDown = ByteArray(8192)
        val bufferUp = ByteArray(8192)
        try {
            while (!stopRequested) {
                val readDown = down.read(bufferDown, 0, bufferDown.size)
                val readUp = up.read(bufferUp, 0, bufferUp.size)
                if (readDown <= 0 || readUp <= 0) break
                val n = minOf(readDown, readUp) / 2 * 2
                var i = 0
                while (i < n) {
                    val a = (bufferDown[i].toInt() and 0xFF) or
                        (bufferDown[i + 1].toInt() shl 8)
                    val b = (bufferUp[i].toInt() and 0xFF) or
                        (bufferUp[i + 1].toInt() shl 8)
                    var mixed = a + b
                    if (mixed > 32767) mixed = 32767
                    if (mixed < -32768) mixed = -32768
                    bufferDown[i] = (mixed and 0xFF).toByte()
                    bufferDown[i + 1] = (mixed shr 8).toByte()
                    i += 2
                }
                try {
                    socket.outputStream.write(bufferDown, 0, n)
                } catch (_: Exception) {
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            synchronized(lock) {
                if (recorder === down) recorder = null
                if (secondRecord === up) secondRecord = null
                runCatching { down.release() }
                runCatching { up.release() }
            }
            runCatching { socket.close() }
        }
        return true
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
            // The mixed fallback holds a second record; stopping only
            // the first would leave its read blocked and the thread alive.
            runCatching { secondRecord?.stop() }
            captureThread
        }
        thread?.join(5000)
        synchronized(lock) {
            val stale = recorder
            val staleSecond = secondRecord
            recorder = null
            secondRecord = null
            runCatching { stale?.release() }
            runCatching { staleSecond?.release() }
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
