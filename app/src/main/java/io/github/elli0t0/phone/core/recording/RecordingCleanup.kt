package io.github.elli0t0.phone.core.recording

import io.github.elli0t0.phone.data.recordings.RecordingsRepository
import io.github.elli0t0.phone.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first

/**
 * Deletes old recordings when a retention period is set. No
 * scheduler: the call sites (app start, after a recording stops,
 * recordings list open, and the retention confirm) cover every
 * path that creates or lists recordings, and the 6-hour gate
 * keeps it from running more than a few times a day.
 */
class RecordingCleanup(
    private val settingsRepository: SettingsRepository,
    private val recordingsRepository: RecordingsRepository
) {
    suspend fun runIfDue(force: Boolean = false) {
        val days = settingsRepository.settingsFlow.first().recordingRetentionDays
        if (days == 0) return
        val now = System.currentTimeMillis()
        if (!force) {
            val last = settingsRepository.getLastCleanupAt()
            if (now - last < 6L * 3_600_000L) return
        }
        recordingsRepository.deleteOlderThan(now - days * 86_400_000L, keepStarred = true)
        settingsRepository.setLastCleanupAt(now)
    }
}
