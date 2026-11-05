package io.github.elli0t0.phone.data.spam

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SpamDao {
    @Query("SELECT * FROM spam_numbers ORDER BY addedAt DESC")
    fun getAll(): Flow<List<SpamNumberEntity>>

    @Query("SELECT * FROM spam_numbers ORDER BY addedAt DESC")
    suspend fun getAllOnce(): List<SpamNumberEntity>

    @Query("SELECT * FROM spam_numbers WHERE numberNormalized = :normalized LIMIT 1")
    suspend fun getByNumber(normalized: String): SpamNumberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SpamNumberEntity)

    @Query("DELETE FROM spam_numbers WHERE numberNormalized = :normalized")
    suspend fun deleteByNumber(normalized: String)

    @Query("SELECT COUNT(*) FROM spam_numbers")
    suspend fun getCount(): Int
}
