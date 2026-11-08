package io.github.elli0t0.phone.core.recording

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

/**
 * WAV (16-bit PCM) to M4A (AAC-LC) encoder. Pure file/stream work,
 * no Android UI: callers run it on Dispatchers.IO. The WAV header
 * is parsed by scanning chunk IDs, so the "data" chunk is found
 * wherever it sits — a fixed 44-byte header is never assumed.
 * Returns the encoded duration in ms, or null on any failure
 * (a partial output file is deleted by [convertFile]).
 */
object WavToM4a {

    private data class WavInfo(
        val channels: Int,
        val sampleRate: Int,
        val dataSize: Long
    )

    private fun readExact(input: InputStream, buf: ByteArray, len: Int): Boolean {
        var read = 0
        while (read < len) {
            val n = input.read(buf, read, len - read)
            if (n < 0) return false
            read += n
        }
        return true
    }

    private fun skipExact(input: InputStream, count: Long): Boolean {
        var remaining = count
        while (remaining > 0) {
            val skipped = input.skip(remaining)
            if (skipped <= 0) {
                // skip() may return 0: consume one byte instead.
                if (input.read() < 0) return false
                remaining--
            } else {
                remaining -= skipped
            }
        }
        return true
    }

    private fun u16le(buf: ByteArray, off: Int): Int =
        (buf[off].toInt() and 0xFF) or ((buf[off + 1].toInt() and 0xFF) shl 8)

    private fun u32le(buf: ByteArray, off: Int): Long =
        (buf[off].toLong() and 0xFF) or
            ((buf[off + 1].toLong() and 0xFF) shl 8) or
            ((buf[off + 2].toLong() and 0xFF) shl 16) or
            ((buf[off + 3].toLong() and 0xFF) shl 24)

    /**
     * Reads the RIFF header and scans chunks until "data". Consumes
     * the header bytes; on success the stream sits at the first PCM
     * byte. Returns null for a non-RIFF file, non-PCM audio, or
     * anything other than 16-bit samples.
     */
    private fun parseHeader(input: InputStream): WavInfo? {
        val head = ByteArray(12)
        if (!readExact(input, head, 12)) return null
        if (String(head, 0, 4, Charsets.US_ASCII) != "RIFF" ||
            String(head, 8, 4, Charsets.US_ASCII) != "WAVE"
        ) {
            return null
        }
        var channels = 0
        var sampleRate = 0
        var bitsPerSample = 0
        val chunkHead = ByteArray(8)
        while (true) {
            if (!readExact(input, chunkHead, 8)) return null
            val id = String(chunkHead, 0, 4, Charsets.US_ASCII)
            val size = u32le(chunkHead, 4)
            when (id) {
                "fmt " -> {
                    if (size < 16) return null
                    val fmt = ByteArray(16)
                    if (!readExact(input, fmt, 16)) return null
                    if (size > 16 && !skipExact(input, size - 16)) return null
                    val audioFormat = u16le(fmt, 0)
                    channels = u16le(fmt, 2)
                    sampleRate = u32le(fmt, 4).toInt()
                    bitsPerSample = u16le(fmt, 14)
                    if (audioFormat != 1 || channels <= 0 || sampleRate <= 0 ||
                        bitsPerSample != 16
                    ) {
                        return null
                    }
                }
                "data" -> {
                    if (channels == 0 || sampleRate == 0) return null
                    return WavInfo(channels, sampleRate, size)
                }
                else -> {
                    if (!skipExact(input, size + (size and 1L))) return null
                }
            }
        }
    }

    /**
     * Encodes the WAV PCM from [input] into [muxer]. Takes ownership
     * of the muxer: starts it on format change and stops/releases it
     * (and the codec) in all cases. Returns the encoded duration in
     * ms, or null when nothing was encoded or any step failed.
     */
    fun encode(input: InputStream, muxer: MediaMuxer): Long? {
        val info = parseHeader(input) ?: return null
        val channels = info.channels
        val sampleRate = info.sampleRate
        val frameBytes = 2 * channels
        val bitrate = if (channels == 1) 64_000 else 96_000
        val format = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels
        ).apply {
            setInteger(
                MediaFormat.KEY_AAC_PROFILE,
                MediaCodecInfo.CodecProfileLevel.AACObjectLC
            )
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }
        var codec: MediaCodec? = null
        var muxerStarted = false
        var trackIndex = -1
        var framesTotal = 0L
        try {
            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            val bufferInfo = MediaCodec.BufferInfo()
            fun drain(endOfStream: Boolean): Boolean {
                var eosSeen = false
                var guard = 0
                while (guard++ < 1000 && !eosSeen) {
                    val outIndex = codec!!.dequeueOutputBuffer(bufferInfo, 10_000)
                    when {
                        outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            if (!muxerStarted) {
                                trackIndex = muxer.addTrack(codec.outputFormat)
                                muxer.start()
                                muxerStarted = true
                            }
                        }
                        outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            if (!endOfStream) return false
                        }
                        outIndex >= 0 -> {
                            val outBuffer = codec.getOutputBuffer(outIndex)
                            val isConfig =
                                (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
                            if (!isConfig && bufferInfo.size > 0 && muxerStarted && outBuffer != null) {
                                muxer.writeSampleData(trackIndex, outBuffer, bufferInfo)
                            }
                            codec.releaseOutputBuffer(outIndex, false)
                            if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                eosSeen = true
                            }
                        }
                    }
                    if (!endOfStream) return eosSeen
                }
                return eosSeen
            }

            // Feed frame-aligned chunks; every queued chunk carries
            // a presentation time from the frames fed so far.
            val readSize = 16384 - (16384 % frameBytes)
            val pcm = ByteArray(readSize)
            var bytesLeft = info.dataSize
            while (bytesLeft > 0) {
                val want = minOf(readSize.toLong(), bytesLeft).toInt()
                if (!readExact(input, pcm, want)) return null
                bytesLeft -= want
                var offset = 0
                while (offset < want) {
                    val inIndex = codec.dequeueInputBuffer(10_000)
                    if (inIndex < 0) {
                        // No input buffer yet: drain output so the
                        // codec can consume what it already holds.
                        drain(endOfStream = false)
                        continue
                    }
                    val inBuffer = codec.getInputBuffer(inIndex) ?: continue
                    val len = minOf(want - offset, inBuffer.remaining())
                    inBuffer.put(pcm, offset, len)
                    val frames = framesTotal + len / frameBytes
                    val ptsUs = framesTotal * 1_000_000L / sampleRate
                    framesTotal = frames
                    codec.queueInputBuffer(inIndex, 0, len, ptsUs, 0)
                    offset += len
                }
                drain(endOfStream = false)
            }
            // End of stream, then drain until the codec says so.
            val eosIndex = codec.dequeueInputBuffer(10_000)
            if (eosIndex >= 0) {
                val ptsUs = framesTotal * 1_000_000L / sampleRate
                codec.queueInputBuffer(
                    eosIndex, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM
                )
            }
            if (!drain(endOfStream = true)) return null
            if (!muxerStarted || framesTotal == 0L) return null
            return framesTotal * 1000L / sampleRate
        } catch (_: Exception) {
            return null
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            runCatching { if (muxerStarted) muxer.stop() }
            runCatching { muxer.release() }
        }
    }

    /**
     * File-to-file conversion. Returns the duration in ms, or null
     * on failure, in which case the partial output is deleted.
     */
    fun convertFile(wav: File, out: File): Long? {
        if (!wav.exists()) return null
        val duration = try {
            FileInputStream(wav).use { input ->
                val muxer = MediaMuxer(out.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                encode(input, muxer)
            }
        } catch (_: Exception) {
            null
        }
        if (duration == null || duration <= 0 || !out.exists() || out.length() <= 0) {
            runCatching { out.delete() }
            return null
        }
        return duration
    }
}
