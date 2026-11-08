package io.github.elli0t0.phone.core.recording

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.media.MediaMuxer
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import io.github.elli0t0.phone.data.recordings.RecordingEntity
import io.github.elli0t0.phone.data.recordings.RecordingsRepository
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred

/**
 * Converts one stored WAV recording to M4A (AAC) and swaps the
 * database row to the new file. The WAV is deleted only after the
 * .m4a exists with a duration > 0 and the row points at it; on any
 * failure the WAV stays and the recording is not lost.
 *
 * Two homes: private recordings are plain files (same directory,
 * same base name, .m4a extension); MediaStore recordings get a new
 * audio/mp4 entry (same display name, .m4a extension) written
 * through the muxer's FileDescriptor. Old WAV rows keep working
 * everywhere: playback, search and cleanup are URI-based.
 *
 * Callers run this on Dispatchers.IO; encoding never runs on the
 * main thread.
 */
object RecordingConverter {

    // The stored uri is NOT the file name: a MediaStore recording
    // is content://media/external/audio/media/<id>, with no
    // extension at all. The format lives in the display name, so
    // every check below resolves it (file name for file://, the
    // MediaStore DISPLAY_NAME otherwise). Callers run these on
    // Dispatchers.IO; a resolver query must never run on the main
    // thread.
    fun fileName(resolver: ContentResolver, uri: Uri): String? =
        if (uri.scheme == "file") {
            uri.path?.substringAfterLast('/')
        } else {
            displayName(resolver, uri)
        }

    fun isWav(context: Context, entity: RecordingEntity): Boolean {
        val uri = Uri.parse(entity.uri)
        return fileName(context.contentResolver, uri)
            ?.endsWith(".wav", ignoreCase = true) == true
    }

    fun isM4a(context: Context, entity: RecordingEntity): Boolean {
        val uri = Uri.parse(entity.uri)
        return fileName(context.contentResolver, uri)
            ?.endsWith(".m4a", ignoreCase = true) == true
    }

    fun mimeTypeFor(context: Context, entity: RecordingEntity): String =
        if (isM4a(context, entity)) "audio/mp4" else "audio/x-wav"

    // One conversion per recording at a time: auto-convert after a
    // call, the bulk "convert old" run, and share can all ask for
    // the same WAV at once. The second caller waits for the first
    // instead of encoding the same file twice (twice the wait, and
    // the loser can delete the WAV under the winner). The deferred
    // is completed even on failure, so waiters never hang.
    private val inFlight = ConcurrentHashMap<Long, CompletableDeferred<Boolean>>()

    /** Converts [entity]'s WAV to M4A, updates the row, deletes the WAV. True on success. */
    suspend fun convertEntityToM4a(
        context: Context,
        repo: RecordingsRepository,
        entity: RecordingEntity
    ): Boolean {
        if (!isWav(context, entity)) return false
        inFlight[entity.id]?.let { return it.await() }
        val deferred = CompletableDeferred<Boolean>()
        val existing = inFlight.putIfAbsent(entity.id, deferred)
        if (existing != null) return existing.await()
        var result = false
        try {
            result = try {
                doConvert(context, repo, entity)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                false
            }
            return result
        } finally {
            // Always complete, even when the owner is cancelled
            // (screen gone mid-share): a waiter must never hang on
            // a deferred whose owner is gone.
            if (!deferred.isCompleted) deferred.complete(result)
            inFlight.remove(entity.id, deferred)
        }
    }

    private suspend fun doConvert(
        context: Context,
        repo: RecordingsRepository,
        entity: RecordingEntity
    ): Boolean {
        val resolver = context.contentResolver
        val wavUri = Uri.parse(entity.uri)
        return try {
            if (wavUri.scheme == "file") {
                convertPrivateFile(context, repo, entity, wavUri)
            } else {
                convertMediaStoreEntry(resolver, repo, entity, wavUri)
            }
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun convertPrivateFile(
        context: Context,
        repo: RecordingsRepository,
        entity: RecordingEntity,
        wavUri: Uri
    ): Boolean {
        val wavFile = wavUri.path?.let { File(it) } ?: return false
        if (!wavFile.exists()) return false
        val outFile = File(wavFile.parentFile, wavFile.nameWithoutExtension + ".m4a")
        val durationMs = WavToM4a.convertFile(wavFile, outFile) ?: return false
        if (durationMs <= 0) {
            runCatching { outFile.delete() }
            return false
        }
        repo.updateFile(entity.id, Uri.fromFile(outFile).toString(), outFile.length(), durationMs)
        deleteFile(context.contentResolver, wavUri)
        return true
    }

    private suspend fun convertMediaStoreEntry(
        resolver: ContentResolver,
        repo: RecordingsRepository,
        entity: RecordingEntity,
        wavUri: Uri
    ): Boolean {
        val input = resolver.openInputStream(wavUri) ?: return false
        val baseName = displayName(resolver, wavUri) ?: "Phone_${entity.startedAt}.wav"
        val outName = baseName.removeSuffix(".wav").removeSuffix(".WAV") + ".m4a"
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, outName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
            put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/Phone")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val outUri = resolver.insert(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values
        )
        if (outUri == null) {
            runCatching { input.close() }
            return false
        }
        var durationMs: Long? = null
        try {
            val pfd = resolver.openFileDescriptor(outUri, "w")
            if (pfd == null) {
                runCatching { resolver.delete(outUri, null, null) }
                return false
            }
            pfd.use {
                val muxer = MediaMuxer(
                    it.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
                )
                durationMs = WavToM4a.encode(input, muxer)
            }
        } catch (_: Exception) {
            runCatching { resolver.delete(outUri, null, null) }
            return false
        } finally {
            runCatching { input.close() }
        }
        if (durationMs == null || durationMs <= 0) {
            runCatching { resolver.delete(outUri, null, null) }
            return false
        }
        runCatching {
            val pendingOff = ContentValues().apply {
                put(MediaStore.Audio.Media.IS_PENDING, 0)
            }
            resolver.update(outUri, pendingOff, null, null)
        }
        val sizeBytes = fileSize(resolver, outUri) ?: entity.sizeBytes
        repo.updateFile(entity.id, outUri.toString(), sizeBytes, durationMs)
        deleteFile(resolver, wavUri)
        return true
    }

    private fun displayName(resolver: ContentResolver, uri: Uri): String? {
        return runCatching {
            resolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(0)?.takeIf { it.isNotBlank() }
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    private fun fileSize(resolver: ContentResolver, uri: Uri): Long? {
        return runCatching {
            resolver.query(
                uri, arrayOf(OpenableColumns.SIZE), null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getLong(0).takeIf { it > 0 }
                } else {
                    null
                }
            }
        }.getOrNull()
    }
}
