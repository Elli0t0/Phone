package io.github.elli0t0.phone.data.contacts

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.ContactsContract
import android.util.Log
import io.github.elli0t0.phone.core.common.PhoneNumberFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Single in-memory index of all contacts' phone numbers, built from ONE query on
 * [ContactsContract.CommonDataKinds.Phone.CONTENT_URI]. Recents, the incoming-call
 * lookup and the call screen resolve names from this map with no per-row queries.
 * Rebuilt only when the contacts ContentObserver fires (debounced).
 */
object ContactNameIndex {

    data class Entry(
        val displayName: String?,
        val contactId: Long?,
        val lookupKey: String?,
        val photoUri: String?
    )

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _buildFailed = MutableStateFlow(false)

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    private var index: Map<String, Entry> = emptyMap()

    @Volatile
    private var started = false

    @Volatile
    private var building = false

    @Volatile
    private var contentResolver: ContentResolver? = null

    private var rebuildJob: Job? = null

    /** Normalized key: ASCII digits only, last 10 digits. */
    fun key(number: String): String =
        PhoneNumberFormatter.normalizeDigits(number).filter { it.isDigit() }.takeLast(10)

    /** Resolve a number to its contact entry, or null if unknown / index not ready. */
    fun resolve(number: String): Entry? {
        if (number.isBlank()) return null
        return index[key(number)]
    }

    /**
     * The whole observer-registration step, extracted so unit tests can
     * simulate a SecurityException: ContentResolver.registerContentObserver
     * is final and cannot be overridden by a fake resolver, and
     * Handler/ContentObserver construction itself throws on JVM stubs —
     * so the seam must cover observer creation too, not just the
     * register call. Production behavior is unchanged.
     */
    internal fun defaultRegisterContactsObserver(
        resolver: ContentResolver,
        onChange: () -> Unit
    ) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                onChange()
            }
        }
        resolver.registerContentObserver(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            true,
            observer
        )
    }

    internal var registerContactsObserver: (ContentResolver, () -> Unit) -> Unit =
        ::defaultRegisterContactsObserver

    /** Start the index (register observer + first build). Idempotent. */
    @Synchronized
    fun ensureStarted(resolver: ContentResolver) {
        if (started) return
        contentResolver = resolver
        try {
            registerContactsObserver(resolver) { rebuildDebounced() }
            // Only mark started once registration actually succeeded: the
            // flag used to be set first, so a call before READ_CONTACTS
            // was granted left the cache permanently stale — later calls
            // returned early without ever registering the observer.
            started = true
        } catch (e: SecurityException) {
            Log.w("PhoneBoot", "Cannot observe contacts without permission")
        } catch (e: Exception) {
            Log.e("PhoneBoot", "Failed to register contacts observer", e)
        }
        ensureBuilt()
    }

    /** Trigger a build if the index is not ready yet. Idempotent while a build runs. */
    fun ensureBuilt() {
        if (_ready.value || building) return
        _buildFailed.value = false
        scope.launch { build() }
    }

    /**
     * Suspend until the index is ready, the last build failed (e.g. permission
     * denied), or [timeoutMs] elapses. Returns true when the index is ready.
     */
    suspend fun awaitReady(timeoutMs: Long = 10_000): Boolean {
        if (_ready.value) return true
        if (_buildFailed.value) return false
        return withTimeoutOrNull(timeoutMs) {
            combine(_ready, _buildFailed) { ready, failed -> ready || failed }.first { it }
            _ready.value
        } ?: false
    }

    private fun rebuildDebounced() {
        rebuildJob?.cancel()
        rebuildJob = scope.launch {
            delay(500)
            build()
        }
    }

    private suspend fun build() {
        val resolver = contentResolver ?: return
        if (building) return
        building = true
        try {
            val start = SystemClock.uptimeMillis()
            val built = HashMap<String, Entry>()
            try {
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
                    ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY
                )
                resolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
                    val lookupIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
                    while (cursor.moveToNext()) {
                        val number = if (numberIdx >= 0) cursor.getString(numberIdx) ?: "" else ""
                        val mapKey = key(number)
                        if (mapKey.isBlank() || built.containsKey(mapKey)) continue
                        built[mapKey] = Entry(
                            displayName = if (nameIdx >= 0) cursor.getString(nameIdx) else null,
                            contactId = if (idIdx >= 0) cursor.getLong(idIdx) else null,
                            lookupKey = if (lookupIdx >= 0) cursor.getString(lookupIdx) else null,
                            photoUri = if (photoIdx >= 0) cursor.getString(photoIdx) else null
                        )
                    }
                }
            } catch (e: SecurityException) {
                Log.w("PhoneBoot", "Cannot build contact index without permission")
                _buildFailed.value = true
                return
            } catch (e: Exception) {
                Log.e("PhoneBoot", "Failed to build contact index", e)
                _buildFailed.value = true
                return
            }
            index = built
            _ready.value = true
            Log.d(
                "PhoneBoot",
                "contact index built in ${SystemClock.uptimeMillis() - start}ms, ${built.size} entries"
            )
        } finally {
            building = false
        }
    }
}
