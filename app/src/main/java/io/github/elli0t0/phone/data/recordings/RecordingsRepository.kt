package io.github.elli0t0.phone.data.recordings

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface RecordingsRepository {
    val recordings: Flow<List<RecordingEntity>>
    suspend fun add(entity: RecordingEntity): Long
    suspend fun remove(entity: RecordingEntity)

    /** Deletes rows whose file no longer exists; returns how many were removed. */
    suspend fun removeMissingFiles(): Int = 0
}

class DefaultRecordingsRepository(
    private val dao: RecordingsDao,
    private val contentResolver: ContentResolver
) : RecordingsRepository {
    override val recordings: Flow<List<RecordingEntity>> = dao.getAll()

    override suspend fun add(entity: RecordingEntity): Long = dao.insert(entity)

    override suspend fun remove(entity: RecordingEntity) {
        runCatching { contentResolver.delete(Uri.parse(entity.uri), null, null) }
        dao.deleteById(entity.id)
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
