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
import android.os.SystemClock
import android.provider.CallLog
import android.util.Log
import io.github.elli0t0.phone.core.common.PhoneNumberFormatter
import io.github.elli0t0.phone.data.calllog.model.CallLogEntry
import io.github.elli0t0.phone.data.calllog.model.CallType
import io.github.elli0t0.phone.data.contacts.ContactNameIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class AndroidCallLogRepository(
    private val contentResolver: ContentResolver,
    private val cacheDir: File,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : CallLogRepository {

    private val tag = "CallLogRepository"

    private val cacheFile: File
        get() = File(cacheDir, "call_log_cache.json")

    private val createdAt = SystemClock.uptimeMillis()
    private var firstEmitLogged = false
    private var freshLoadStarted = false

    private val _callLogsFlow = MutableStateFlow<List<CallLogEntry>>(emptyList())
    private val _isLoadingFlow = MutableStateFlow(true)

    override val isLoadingFlow: StateFlow<Boolean> = _isLoadingFlow.asStateFlow()

    private var contentObserver: ContentObserver? = null
    private var isObserverRegistered = false
    private var queryJob: Job? = null

    init {
        registerObserverIfNeeded()
        // Show the last resolved list instantly on cold start; the fresh
        // query refreshes it silently right after.
        scope.launch { loadCachedList() }
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
        if (!freshLoadStarted) {
            loadCallLogs()
        }
        return _callLogsFlow
    }

    @Synchronized
    private fun loadCallLogs() {
        freshLoadStarted = true
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
        // Wait for the contact index so the first render already has names:
        // never emit rows with raw numbers while the index is still loading.
        // Falls through on failure/timeout (cached names only).
        ContactNameIndex.ensureStarted(contentResolver)
        ContactNameIndex.ensureBuilt()
        ContactNameIndex.awaitReady()

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

                // Resolve from the in-memory index: one map lookup, no queries.
                val indexed = if (rawNumber.isNotBlank()) ContactNameIndex.resolve(rawNumber) else null
                val displayName = indexed?.displayName ?: cachedName

                entries.add(
                    CallLogEntry(
                        id = id,
                        number = rawNumber,
                        formattedNumber = PhoneNumberFormatter.format(rawNumber),
                        cachedName = cachedName,
                        contactName = displayName,
                        contactId = indexed?.contactId,
                        contactLookupKey = indexed?.lookupKey,
                        contactPhotoUri = indexed?.photoUri,
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
                    logFirstEmit()
                }
            }
        }

        // Full emit of all rows
        _callLogsFlow.value = entries.toList()
        _isLoadingFlow.value = false
        logFirstEmit()
        saveCachedList(entries)
    }

    private fun logFirstEmit() {
        if (!firstEmitLogged) {
            firstEmitLogged = true
            Log.d(
                "PhoneBoot",
                "first call-log emit in ${SystemClock.uptimeMillis() - createdAt}ms"
            )
        }
    }

    private fun saveCachedList(entries: List<CallLogEntry>) {
        try {
            val array = JSONArray()
            for (entry in entries) {
                val obj = JSONObject()
                obj.put("id", entry.id)
                obj.put("number", entry.number)
                obj.put("formattedNumber", entry.formattedNumber)
                obj.put("cachedName", entry.cachedName)
                obj.put("contactName", entry.contactName)
                obj.put("contactId", entry.contactId)
                obj.put("contactLookupKey", entry.contactLookupKey)
                obj.put("contactPhotoUri", entry.contactPhotoUri)
                obj.put("type", entry.type.name)
                obj.put("date", entry.date)
                obj.put("durationSeconds", entry.durationSeconds)
                obj.put("simInfo", entry.simInfo)
                array.put(obj)
            }
            cacheFile.writeText(array.toString())
        } catch (e: Exception) {
            Log.w(tag, "Failed to save cached call log", e)
        }
    }

    private fun loadCachedList() {
        try {
            if (!cacheFile.exists()) return
            val text = cacheFile.readText()
            if (text.isBlank()) return
            val array = JSONArray(text)
            val entries = mutableListOf<CallLogEntry>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val type = runCatching { CallType.valueOf(obj.optString("type")) }.getOrNull()
                    ?: CallType.Unknown
                entries.add(
                    CallLogEntry(
                        id = obj.optLong("id"),
                        number = obj.optString("number"),
                        formattedNumber = obj.optString("formattedNumber"),
                        cachedName = obj.optString("cachedName").takeIf { it.isNotEmpty() },
                        contactName = obj.optString("contactName").takeIf { it.isNotEmpty() },
                        contactId = obj.optLong("contactId").takeIf { it != 0L },
                        contactLookupKey = obj.optString("contactLookupKey").takeIf { it.isNotEmpty() },
                        contactPhotoUri = obj.optString("contactPhotoUri").takeIf { it.isNotEmpty() },
                        type = type,
                        date = obj.optLong("date"),
                        durationSeconds = obj.optLong("durationSeconds"),
                        simInfo = obj.optString("simInfo").takeIf { it.isNotEmpty() }
                    )
                )
            }
            // Only fill when nothing fresher arrived meanwhile.
            if (_callLogsFlow.value.isEmpty() && entries.isNotEmpty()) {
                _callLogsFlow.value = entries
                _isLoadingFlow.value = false
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to load cached call log", e)
        }
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
                val updated = _callLogsFlow.value.filter { it.id != id }
                _callLogsFlow.value = updated
                saveCachedList(updated)
                true
            } else false
        } catch (_: Exception) {
            false
        }
    }
}
