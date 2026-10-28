package io.github.elli0t0.phone.data.recordings

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uri: String,
    val number: String,
    val displayName: String?,
    val startedAt: Long,
    val durationMs: Long,
    val sizeBytes: Long,
    val isTest: Boolean
)
