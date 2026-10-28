package io.github.elli0t0.phone

import io.github.elli0t0.phone.core.recording.WavWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WavWriterTest {

    @Test
    fun headerHasRiffChunksAndPcmFormat() {
        val header = WavWriter.header(32000)
        assertEquals(44, header.size)
        assertEquals("RIFF", String(header, 0, 4, Charsets.US_ASCII))
        assertEquals("WAVE", String(header, 8, 4, Charsets.US_ASCII))
        assertEquals("fmt ", String(header, 12, 4, Charsets.US_ASCII))
        assertEquals("data", String(header, 36, 4, Charsets.US_ASCII))
        // RIFF chunk size = 36 + pcm bytes, little-endian.
        val riffSize = (header[4].toInt() and 0xFF) or
            ((header[5].toInt() and 0xFF) shl 8) or
            ((header[6].toInt() and 0xFF) shl 16) or
            ((header[7].toInt() and 0xFF) shl 24)
        assertEquals(36 + 32000, riffSize)
        // Data chunk size at offset 40.
        val dataSize = (header[40].toInt() and 0xFF) or
            ((header[41].toInt() and 0xFF) shl 8) or
            ((header[42].toInt() and 0xFF) shl 16) or
            ((header[43].toInt() and 0xFF) shl 24)
        assertEquals(32000, dataSize)
    }

    @Test
    fun byteRateAndDurationMatchMono16kHz() {
        assertEquals(32000, WavWriter.byteRate())
        assertEquals(1000L, WavWriter.durationMs(32000))
        assertEquals(500L, WavWriter.durationMs(16000))
    }

    @Test
    fun emptyHeaderIsValidPlaceholder() {
        val header = WavWriter.header(0)
        assertTrue(header.size == WavWriter.HEADER_SIZE)
    }
}
