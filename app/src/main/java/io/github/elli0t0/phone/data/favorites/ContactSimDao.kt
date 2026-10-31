package io.github.elli0t0.phone.data.favorites

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.elli0t0.phone.data.favorites.model.ContactSimEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactSimDao {
    @Query("SELECT simId FROM contact_sims WHERE contactLookupKey = :lookupKey")
    suspend fun getSimId(lookupKey: String): String?

    @Query("SELECT simId FROM contact_sims WHERE contactLookupKey = :lookupKey")
    fun getSimIdFlow(lookupKey: String): Flow<String?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ContactSimEntity)

    @Query("DELETE FROM contact_sims WHERE contactLookupKey = :lookupKey")
    suspend fun deleteByLookupKey(lookupKey: String)

    @Query("UPDATE contact_sims SET contactLookupKey = :newKey WHERE contactLookupKey = :oldKey")
    suspend fun rekeyLookupKey(oldKey: String, newKey: String)
}
