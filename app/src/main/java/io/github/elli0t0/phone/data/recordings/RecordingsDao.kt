package io.github.elli0t0.phone.data.recordings

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingsDao {
    @Query("SELECT * FROM recordings ORDER BY startedAt DESC")
    fun getAll(): Flow<List<RecordingEntity>>

    @Insert
    suspend fun insert(entity: RecordingEntity): Long

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE recordings SET starred = :starred WHERE id = :id")
    suspend fun setStarred(id: Long, starred: Boolean)

    @Query("UPDATE recordings SET title = :title, note = :note WHERE id = :id")
    suspend fun setTitleNote(id: Long, title: String?, note: String?)

    @Query("SELECT * FROM recordings WHERE startedAt < :cutoff AND starred = 0 AND isTest = 0")
    suspend fun olderThanUnstarred(cutoff: Long): List<RecordingEntity>

    @Query("SELECT * FROM recordings WHERE isTest = 1 AND startedAt < :cutoff")
    suspend fun oldTests(cutoff: Long): List<RecordingEntity>

    @Query("SELECT COUNT(*) AS count, COALESCE(SUM(sizeBytes), 0) AS totalBytes FROM recordings WHERE isTest = 0")
    fun stats(): Flow<RecordingStats>
}

data class RecordingStats(val count: Int, val totalBytes: Long)
