package io.github.elli0t0.phone.core.recording

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Minimal WAV (RIFF, PCM 16-bit mono) header writer. Pure Kotlin so it is
 * unit-testable on the JVM. The privileged recorder writes a placeholder
 * header first, then patches it with the final sizes on stop.
 */
object WavWriter {
    const val SAMPLE_RATE = 16000
    const val CHANNELS = 1
    const val BITS_PER_SAMPLE = 16
    const val HEADER_SIZE = 44

    fun byteRate(): Int = SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8

    fun durationMs(pcmBytes: Long): Long = pcmBytes * 1000L / byteRate()

    fun header(pcmBytes: Int): ByteArray {
        val buffer = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(36 + pcmBytes)
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16)
        buffer.putShort(1) // PCM
        buffer.putShort(CHANNELS.toShort())
        buffer.putInt(SAMPLE_RATE)
        buffer.putInt(byteRate())
        buffer.putShort((CHANNELS * BITS_PER_SAMPLE / 8).toShort()) // block align
        buffer.putShort(BITS_PER_SAMPLE.toShort())
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(pcmBytes)
        return buffer.array()
    }
}
