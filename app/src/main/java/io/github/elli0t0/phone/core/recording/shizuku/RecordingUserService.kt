package io.github.elli0t0.phone.core.recording.shizuku

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.ParcelFileDescriptor
import android.util.Log
import io.github.elli0t0.phone.core.recording.WavWriter
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * Runs in Shizuku's privileged (shell UID 2000) process, NOT in the app
 * process: no ContentResolver, no receivers here — just AudioRecord and
 * a raw file descriptor handed over by the app. Shizuku instantiates
 * this class by reflection, so it needs a public no-arg constructor and
 * a ProGuard keep rule. The user-service process is not killed on
 * unbind, so it idles after a recording until the next bind reuses it.
 */
class RecordingUserService : IRecordingService.Stub() {

    private val lock = Any()
    private var captureThread: Thread? = null
    private var stopRequested = false
    private var recorder: AudioRecord? = null
    private var output: FileOutputStream? = null
    private var pcmBytes = 0L

    override fun startRecording(fd: ParcelFileDescriptor?): Int {
        if (fd == null) return 1
        synchronized(lock) {
            if (captureThread != null) return 2
            val minBuffer = AudioRecord.getMinBufferSize(
                WavWriter.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuffer <= 0) return 1
            val record = AudioRecord(
                MediaRecorder.AudioSource.VOICE_CALL,
                WavWriter.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuffer, 8192)
            )
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                return 1
            }
            val out = FileOutputStream(fd.fileDescriptor)
            try {
                out.write(WavWriter.header(0))
                out.flush()
            } catch (e: Exception) {
                Log.e(TAG, "Could not write WAV header", e)
                runCatching { out.close() }
                record.release()
                return 1
            }
            recorder = record
            output = out
            pcmBytes = 0L
            stopRequested = false
            val thread = Thread(::captureLoop, "phone-recording")
            captureThread = thread
            thread.start()
            return 0
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
                // read == 0: no data yet, loop again.
            }
        } catch (e: Exception) {
            Log.e(TAG, "Capture loop failed", e)
        } finally {
            runCatching { record.stop() }
            runCatching { record.release() }
        }
    }

    override fun stopRecording(): Long {
        val thread = synchronized(lock) { captureThread } ?: return -1
        stopRequested = true
        // Unblock a pending read so the loop can exit promptly.
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

    override fun isRecording(): Boolean = synchronized(lock) { captureThread != null }

    companion object {
        private const val TAG = "PhoneRecording"
    }
}
