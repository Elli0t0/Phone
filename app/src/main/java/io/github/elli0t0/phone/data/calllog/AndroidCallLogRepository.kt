package io.github.elli0t0.phone.data.calllog

import android.content.ContentResolver
import android.content.ContentUris
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.CallLog
import android.util.Log
import io.github.elli0t0.phone.core.telecom.SimAccountInfo
import io.github.elli0t0.phone.data.calllog.model.CallHistoryPage
import io.github.elli0t0.phone.data.calllog.model.CallLogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AndroidCallLogRepository(
    internal val contentResolver: ContentResolver,
    private val cacheDir: File,
    internal val simAccountsProvider: () -> List<SimAccountInfo> = { emptyList() },
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : CallLogRepository {

    internal val tag = "CallLogRepository"

    internal val cacheFile: File
        get() = File(cacheDir, "call_log_cache.json")

    internal val createdAt = SystemClock.uptimeMillis()
    internal var firstEmitLogged = false
    private var freshLoadStarted = false

    internal val _callLogsFlow = MutableStateFlow<List<CallLogEntry>>(emptyList())
    internal val _isLoadingFlow = MutableStateFlow(true)

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

    override suspend fun deleteCalls(ids: Collection<Long>): Int = withContext(Dispatchers.IO) {
        try {
            var deleted = 0
            val removedIds = mutableSetOf<Long>()
            for (chunk in deleteCallChunks(ids)) {
                val placeholders = chunk.joinToString(",") { "?" }
                val count = contentResolver.delete(
                    CallLog.Calls.CONTENT_URI,
                    "${CallLog.Calls._ID} IN ($placeholders)",
                    chunk.map { it.toString() }.toTypedArray()
                )
                deleted += count
                if (count > 0) removedIds += chunk
            }
            // One flow update and one cache write for the whole batch.
            if (removedIds.isNotEmpty()) {
                val updated = _callLogsFlow.value.filter { it.id !in removedIds }
                _callLogsFlow.value = updated
                saveCachedList(updated)
            }
            deleted
        } catch (_: Exception) {
            0
        }
    }


    // The paged query lives in AndroidCallLogHistory.kt; this override
    // only delegates so the class still implements the interface.
    override suspend fun getCallHistoryPage(
        numbers: List<String>,
        limit: Int,
        offset: Int
    ): CallHistoryPage = getCallHistoryPageInternal(numbers, limit, offset)

    companion object {
        internal const val HISTORY_CHUNK_SIZE = 100
        internal const val STREAMING_LIMIT = 500

        /** Pre-Android-11 sort order: no LIMIT clause (the provider rejects it). */
        internal fun legacyCallLogSortOrder(): String = "${CallLog.Calls.DATE} DESC"
    }
}
