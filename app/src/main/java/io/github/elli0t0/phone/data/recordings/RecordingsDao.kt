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
}
