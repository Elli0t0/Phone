package io.github.elli0t0.phone.data.recordings

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface RecordingsRepository {
    val recordings: Flow<List<RecordingEntity>>
    suspend fun add(entity: RecordingEntity): Long
    suspend fun remove(entity: RecordingEntity)
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
}
