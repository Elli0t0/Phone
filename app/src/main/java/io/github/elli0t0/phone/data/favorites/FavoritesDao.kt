package io.github.elli0t0.phone.data.favorites

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.elli0t0.phone.data.favorites.model.FavoriteContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritesDao {
    @Query("SELECT * FROM favorites ORDER BY position ASC, addedAt ASC")
    fun getAllFavorites(): Flow<List<FavoriteContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteContactEntity)

    @Query("DELETE FROM favorites WHERE contactLookupKey = :lookupKey")
    suspend fun deleteFavoriteByLookupKey(lookupKey: String)

    @Query("UPDATE favorites SET contactLookupKey = :newKey WHERE contactLookupKey = :oldKey")
    suspend fun rekeyLookupKey(oldKey: String, newKey: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE contactLookupKey = :lookupKey)")
    fun isFavoriteFlow(lookupKey: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE contactLookupKey = :lookupKey)")
    suspend fun isFavorite(lookupKey: String): Boolean

    @Query("SELECT COUNT(*) FROM favorites")
    suspend fun getCount(): Int

    @Update
    suspend fun updateFavorites(favorites: List<FavoriteContactEntity>)
}
