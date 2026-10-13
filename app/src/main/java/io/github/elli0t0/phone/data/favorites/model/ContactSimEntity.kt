package io.github.elli0t0.phone.data.favorites.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Per-contact calling SIM. simId is the PhoneAccountHandle id (SimAccountInfo.iccId).
 * Null simId means "Ask every time" (no fixed SIM).
 */
@Entity(tableName = "contact_sims")
data class ContactSimEntity(
    @PrimaryKey
    val contactLookupKey: String,
    val simId: String?,
    val updatedAt: Long = System.currentTimeMillis()
)
