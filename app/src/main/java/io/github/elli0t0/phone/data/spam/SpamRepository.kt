package io.github.elli0t0.phone.data.spam

import android.content.ContentResolver
import android.provider.ContactsContract
import io.github.elli0t0.phone.data.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

// TODO (later, optional): daily download of a community spam list
// (JSON from GitHub) into rows with source = "LIST". No network
// sync exists today: reporting stays on-device and nothing uploads
// a number anywhere. A download must stay opt-in, keep the local
// USER rows untouched, and never replace them.

/**
 * Spam list storage with an in-memory cache, so the call screening
 * service can answer within its ~3 s budget without touching disk
 * on the hot path. The cache is loaded once at app start
 * ([preload], called from PhoneApplication) and refreshed after
 * every report/remove. [isSpam] is a pure cache lookup and is safe
 * to call from composition (badges) and from the screening service.
 */
class SpamRepository(
    private val dao: SpamDao,
    private val settingsRepository: SettingsRepository
) {
    private val _spamNumbers = MutableStateFlow<Set<String>>(emptySet())
    val spamNumbers: StateFlow<Set<String>> = _spamNumbers.asStateFlow()

    /** All reported entries, newest first, for the settings list. */
    fun spamEntries(): Flow<List<SpamNumberEntity>> = dao.getAll()

    suspend fun preload() {
        runCatching {
            _spamNumbers.value = dao.getAllOnce().map { it.numberNormalized }.toSet()
        }
    }

    /** Synchronous cache lookup. False for blank numbers. */
    fun isSpam(number: String): Boolean =
        SpamMatcher.isSpam(_spamNumbers.value, number)

    suspend fun reportSpam(number: String, reason: String = "") {
        val normalized = SpamMatcher.normalizeForStorage(number)
        if (normalized.isEmpty()) return
        runCatching {
            val existing = dao.getByNumber(normalized)
            dao.insert(
                (existing ?: SpamNumberEntity(numberNormalized = normalized))
                    .copy(reason = reason.ifBlank { existing?.reason ?: "" }, reportCount = (existing?.reportCount ?: 0) + 1)
            )
            _spamNumbers.value = _spamNumbers.value + normalized
        }
    }

    suspend fun removeSpam(normalized: String) {
        if (normalized.isBlank()) return
        runCatching {
            dao.deleteByNumber(normalized)
            _spamNumbers.value = _spamNumbers.value - normalized
        }
    }

    /** Blocked-call counter (Settings > Spam protection). */
    suspend fun incrementBlockedCount() {
        runCatching {
            val current = settingsRepository.settingsFlow.first().spamBlockedCount
            settingsRepository.setSpamBlockedCount(current + 1)
        }
    }

    /**
     * True when [number] belongs to a saved contact (PhoneLookup).
     * False on any failure (including a missing READ_CONTACTS
     * grant) — callers must treat "unknown" as "not a contact" only
     * where the spec allows it; the spam list itself is explicit
     * user data and is checked regardless.
     */
    fun isInContacts(contentResolver: ContentResolver, number: String): Boolean {
        if (number.isBlank()) return false
        return runCatching {
            val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI.buildUpon()
                .appendPath(number).build()
            contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup._ID),
                null,
                null,
                null
            )?.use { it.count > 0 } ?: false
        }.getOrDefault(false)
    }
}
