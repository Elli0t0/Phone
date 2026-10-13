package io.github.elli0t0.phone.data.calllog

import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.provider.ContactsContract
import android.util.Log
import io.github.elli0t0.phone.core.common.PhoneNumberFormatter
import io.github.elli0t0.phone.core.common.PhoneNumberMatcher
import io.github.elli0t0.phone.data.calllog.model.CallLogEntry
import io.github.elli0t0.phone.data.calllog.model.CallType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class AndroidCallLogRepository(
    private val contentResolver: ContentResolver,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : CallLogRepository {

    private val tag = "CallLogRepository"

    private data class ResolvedContact(
        val displayName: String?,
        val contactId: Long?,
        val lookupKey: String?,
        val photoUri: String?
    )

    private val resolvedContactsCache = ConcurrentHashMap<String, ResolvedContact>()
    private val _callLogsFlow = MutableStateFlow<List<CallLogEntry>>(emptyList())
    private val _isLoadingFlow = MutableStateFlow(true)

    override val isLoadingFlow: StateFlow<Boolean> = _isLoadingFlow.asStateFlow()

    private var contentObserver: ContentObserver? = null
    private var isObserverRegistered = false
    private var queryJob: Job? = null

    init {
        registerObserverIfNeeded()
    }

    override fun reload() {
        loadCallLogs()
    }

    fun preload() {
        loadCallLogs()
    }

    private fun registerObserverIfNeeded() {
        if (isObserverRegistered) return
        try {
            val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean, uri: Uri?) {
                    loadCallLogs()
                }
            }
            contentResolver.registerContentObserver(
                CallLog.Calls.CONTENT_URI,
                true,
                observer
            )
            contentObserver = observer
            isObserverRegistered = true
        } catch (e: SecurityException) {
            Log.w(tag, "Permission not granted to register CallLog ContentObserver")
        } catch (e: Exception) {
            Log.e(tag, "Failed to register CallLog ContentObserver", e)
        }
    }

    override fun getCallLogsFlow(): Flow<List<CallLogEntry>> {
        registerObserverIfNeeded()
        if (_callLogsFlow.value.isEmpty()) {
            loadCallLogs()
        }
        return _callLogsFlow
    }

    @Synchronized
    private fun loadCallLogs() {
        queryJob?.cancel()
        queryJob = scope.launch(Dispatchers.IO) {
            try {
                if (_callLogsFlow.value.isEmpty()) {
                    _isLoadingFlow.value = true
                }
                executeStreamingQuery()
            } catch (e: SecurityException) {
                Log.e(tag, "SecurityException loading call logs", e)
                _callLogsFlow.value = emptyList()
                _isLoadingFlow.value = false
            } catch (e: Exception) {
                Log.e(tag, "Exception loading call logs", e)
                _isLoadingFlow.value = false
            }
        }
    }

    private suspend fun executeStreamingQuery() = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.PHONE_ACCOUNT_ID
        )

        val cursor: Cursor? = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val queryArgs = Bundle().apply {
                    putInt(ContentResolver.QUERY_ARG_LIMIT, 500)
                    putStringArray(
                        ContentResolver.QUERY_ARG_SORT_COLUMNS,
                        arrayOf(CallLog.Calls.DATE)
                    )
                    putInt(
                        ContentResolver.QUERY_ARG_SORT_DIRECTION,
                        ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                    )
                }
                contentResolver.query(CallLog.Calls.CONTENT_URI, projection, queryArgs, null)
            } else {
                contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${CallLog.Calls.DATE} DESC LIMIT 500"
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Error querying CallLog cursor", e)
            throw e
        }

        if (cursor == null) {
            _callLogsFlow.value = emptyList()
            _isLoadingFlow.value = false
            return@withContext
        }

        val entries = mutableListOf<CallLogEntry>()
        val unlookupedNumbers = mutableSetOf<String>()
        var emittedFirst50 = false

        cursor.use { c ->
            val idIdx = c.getColumnIndex(CallLog.Calls._ID)
            val numberIdx = c.getColumnIndex(CallLog.Calls.NUMBER)
            val cachedNameIdx = c.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val typeIdx = c.getColumnIndex(CallLog.Calls.TYPE)
            val dateIdx = c.getColumnIndex(CallLog.Calls.DATE)
            val durationIdx = c.getColumnIndex(CallLog.Calls.DURATION)
            val accountIdIdx = c.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)

            var count = 0
            while (c.moveToNext()) {
                val id = if (idIdx >= 0) c.getLong(idIdx) else 0L
                val rawNumber = if (numberIdx >= 0) c.getString(numberIdx) ?: "" else ""
                val cachedName = if (cachedNameIdx >= 0) c.getString(cachedNameIdx) else null
                val typeCode = if (typeIdx >= 0) c.getInt(typeIdx) else 0
                val date = if (dateIdx >= 0) c.getLong(dateIdx) else 0L
                val duration = if (durationIdx >= 0) c.getLong(durationIdx) else 0L
                val phoneAccountId = if (accountIdIdx >= 0) c.getString(accountIdIdx) else null

                val callType = mapCallType(typeCode)
                val simInfo = phoneAccountId?.let { "SIM ${it.take(2)}" }

                // Check in-memory cache without blocking
                val cachedContact = if (rawNumber.isNotBlank()) resolvedContactsCache[rawNumber] else null
                if (cachedContact == null && rawNumber.isNotBlank()) {
                    unlookupedNumbers.add(rawNumber)
                }

                val displayName = cachedContact?.displayName ?: cachedName

                entries.add(
                    CallLogEntry(
                        id = id,
                        number = rawNumber,
                        formattedNumber = PhoneNumberFormatter.format(rawNumber),
                        cachedName = cachedName,
                        contactName = displayName,
                        contactId = cachedContact?.contactId,
                        contactLookupKey = cachedContact?.lookupKey,
                        contactPhotoUri = cachedContact?.photoUri,
                        type = callType,
                        date = date,
                        durationSeconds = duration,
                        simInfo = simInfo
                    )
                )

                count++
                // Fast path: emit first 50 rows immediately so screen renders instantly
                if (count == 50 && !emittedFirst50) {
                    emittedFirst50 = true
                    _callLogsFlow.value = entries.toList()
                    _isLoadingFlow.value = false
                }
            }
        }

        // Full emit of all rows
        _callLogsFlow.value = entries.toList()
        _isLoadingFlow.value = false

        // Background pass: resolve names in batch for un-looked-up numbers
        if (unlookupedNumbers.isNotEmpty()) {
            resolveContactsInBatch(unlookupedNumbers, entries)
        }
    }

    private suspend fun resolveContactsInBatch(
        numbers: Set<String>,
        currentEntries: List<CallLogEntry>
    ) = withContext(Dispatchers.IO) {
        var didResolveAny = false
        for (num in numbers) {
            if (resolvedContactsCache.containsKey(num)) continue
            val resolved = querySinglePhoneLookup(num)
            resolvedContactsCache[num] = resolved
            if (!resolved.displayName.isNullOrBlank()) {
                didResolveAny = true
            }
        }

        if (didResolveAny) {
            val updated = currentEntries.map { entry ->
                val cached = resolvedContactsCache[entry.number]
                if (cached != null && (!cached.displayName.isNullOrBlank() || cached.photoUri != null)) {
                    entry.copy(
                        contactName = cached.displayName ?: entry.cachedName,
                        contactId = cached.contactId ?: entry.contactId,
                        contactLookupKey = cached.lookupKey ?: entry.contactLookupKey,
                        contactPhotoUri = cached.photoUri ?: entry.contactPhotoUri
                    )
                } else {
                    entry
                }
            }
            _callLogsFlow.value = updated
        }
    }

    private fun querySinglePhoneLookup(number: String): ResolvedContact {
        if (number.isBlank()) return ResolvedContact(null, null, null, null)

        // Try the number as-is, then the shared normalized form (device region +
        // trunk prefix), then the normalized form with an explicit "+" prefix.
        // PhoneLookup already normalizes, but the stored number may use a different
        // format than the call log number, so the extra attempts close that gap.
        val normalized = PhoneNumberMatcher.normalize(number)
        val candidates = listOfNotNull(
            number,
            normalized.ifBlank { null },
            "+$normalized".takeIf { normalized.isNotBlank() }
        ).distinct()
        for (candidate in candidates) {
            val resolved = queryPhoneLookupRow(candidate)
            if (resolved != null) return resolved
        }
        return ResolvedContact(null, null, null, null)
    }

    private fun queryPhoneLookupRow(number: String): ResolvedContact? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )
        val projection = arrayOf(
            ContactsContract.PhoneLookup._ID,
            ContactsContract.PhoneLookup.LOOKUP_KEY,
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI
        )

        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val idIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup._ID)
                val lookupKeyIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.LOOKUP_KEY)
                val nameIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                val photoIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_THUMBNAIL_URI)

                return ResolvedContact(
                    contactId = if (idIdx >= 0) cursor.getLong(idIdx) else null,
                    lookupKey = if (lookupKeyIdx >= 0) cursor.getString(lookupKeyIdx) else null,
                    displayName = if (nameIdx >= 0) cursor.getString(nameIdx) else null,
                    photoUri = if (photoIdx >= 0) cursor.getString(photoIdx) else null
                )
            }
        } catch (_: Exception) {
            // Permission revoked or not granted
        } finally {
            cursor?.close()
        }

        return null
    }

    private fun mapCallType(typeCode: Int): CallType {
        return when (typeCode) {
            CallLog.Calls.INCOMING_TYPE -> CallType.Incoming
            CallLog.Calls.OUTGOING_TYPE -> CallType.Outgoing
            CallLog.Calls.MISSED_TYPE -> CallType.Missed
            CallLog.Calls.VOICEMAIL_TYPE -> CallType.Voicemail
            CallLog.Calls.REJECTED_TYPE -> CallType.Rejected
            CallLog.Calls.BLOCKED_TYPE -> CallType.Blocked
            else -> CallType.Unknown
        }
    }

    override suspend fun deleteCall(id: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val uri = ContentUris.withAppendedId(CallLog.Calls.CONTENT_URI, id)
            val deletedRows = contentResolver.delete(uri, null, null)
            if (deletedRows > 0) {
                // Update in-memory state immediately
                _callLogsFlow.value = _callLogsFlow.value.filter { it.id != id }
                true
            } else false
        } catch (_: Exception) {
            false
        }
    }
}
