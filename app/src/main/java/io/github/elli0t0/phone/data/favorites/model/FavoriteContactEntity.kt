package io.github.elli0t0.phone.data.favorites.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteContactEntity(
    @PrimaryKey
    val contactLookupKey: String,
    val contactId: Long,
    val position: Int,
    val addedAt: Long = System.currentTimeMillis()
)
