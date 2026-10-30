package io.github.elli0t0.phone.data.recordings

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf

interface RecordingsRepository {
    val recordings: Flow<List<RecordingEntity>>
    suspend fun add(entity: RecordingEntity): Long
    suspend fun remove(entity: RecordingEntity)

    /** Deletes rows whose file no longer exists; returns how many were removed. */
    suspend fun removeMissingFiles(): Int = 0

    val stats: Flow<RecordingStats> get() = flowOf(RecordingStats(0, 0))
    suspend fun setStarred(id: Long, starred: Boolean) {}
    suspend fun setTitleNote(id: Long, title: String?, note: String?) {}
    suspend fun removeAll(entities: List<RecordingEntity>): Int = 0
    suspend fun countOlderThanUnstarred(cutoff: Long): Int = 0
    suspend fun deleteOlderThan(cutoff: Long, keepStarred: Boolean = true): Int = 0
}

class DefaultRecordingsRepository(
    private val dao: RecordingsDao,
    private val contentResolver: ContentResolver,
    private val fileDeleter: (ContentResolver, Uri) -> Unit =
        { resolver, uri -> io.github.elli0t0.phone.core.recording.deleteFile(resolver, uri) }
) : RecordingsRepository {
    override val recordings: Flow<List<RecordingEntity>> = dao.getAll()

    override val stats: Flow<RecordingStats> = dao.stats()

    override suspend fun add(entity: RecordingEntity): Long = dao.insert(entity)

    override suspend fun remove(entity: RecordingEntity) {
        fileDeleter(contentResolver, Uri.parse(entity.uri))
        dao.deleteById(entity.id)
    }

    override suspend fun setStarred(id: Long, starred: Boolean) {
        dao.setStarred(id, starred)
    }

    override suspend fun setTitleNote(id: Long, title: String?, note: String?) {
        dao.setTitleNote(id, title, note)
    }

    override suspend fun removeAll(entities: List<RecordingEntity>): Int {
        entities.forEach { fileDeleter(contentResolver, Uri.parse(it.uri)) }
        entities.forEach { dao.deleteById(it.id) }
        return entities.size
    }

    override suspend fun countOlderThanUnstarred(cutoff: Long): Int {
        return dao.olderThanUnstarred(cutoff).size
    }

    override suspend fun deleteOlderThan(cutoff: Long, keepStarred: Boolean): Int {
        val victims = if (keepStarred) {
            dao.olderThanUnstarred(cutoff)
        } else {
            recordings.first().filter { !it.isTest && it.startedAt < cutoff }
        }
        // Test rows go when older than a day, whatever the cutoff is.
        val oneDayAgo = System.currentTimeMillis() - 86_400_000L
        val all = (victims + dao.oldTests(oneDayAgo)).distinctBy { it.id }
        all.forEach { fileDeleter(contentResolver, Uri.parse(it.uri)) }
        all.forEach { dao.deleteById(it.id) }
        return all.size
    }

    override suspend fun removeMissingFiles(): Int {
        var removed = 0
        recordings.first().forEach { e ->
            val exists = runCatching {
                contentResolver.openFileDescriptor(Uri.parse(e.uri), "r")?.use { true } ?: false
            }.getOrDefault(false)
            if (!exists) {
                dao.deleteById(e.id)
                removed++
            }
        }
        return removed
    }
}
