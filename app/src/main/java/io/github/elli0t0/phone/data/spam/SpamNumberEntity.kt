package io.github.elli0t0.phone.data.spam

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One user-reported spam number. The primary key is the normalized
 * number (see [SpamMatcher.normalizeForStorage]): +98 / 0 trunk
 * variants of the same number collapse to one row. `source` is
 * "USER" for a number reported in the app; "LIST" is reserved for a
 * future optional community-list download (see the TODO in
 * [SpamRepository]) — no list is downloaded today.
 */
@Entity(tableName = "spam_numbers")
data class SpamNumberEntity(
    @PrimaryKey
    val numberNormalized: String,
    val reason: String = "",
    val source: String = SOURCE_USER,
    val reportCount: Int = 1,
    val addedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val SOURCE_USER = "USER"
        const val SOURCE_LIST = "LIST"
    }
}
