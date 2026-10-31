package io.github.elli0t0.phone.data.calllog

import io.github.elli0t0.phone.data.calllog.model.CallHistoryPage
import io.github.elli0t0.phone.data.calllog.model.CallLogEntry
import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.flow.StateFlow

interface CallLogRepository {
    fun getCallLogsFlow(): Flow<List<CallLogEntry>>
    val isLoadingFlow: StateFlow<Boolean> get() = kotlinx.coroutines.flow.MutableStateFlow(false)
    suspend fun deleteCall(id: Long): Boolean
    /**
     * Deletes every call in [ids] (chunked provider deletes), updates
     * the in-memory list once, and returns how many rows were removed.
     */
    suspend fun deleteCalls(ids: Collection<Long>): Int = 0
    // One page of one contact's calls, newest first. `offset` counts
    // matches already returned, not raw call-log rows: the query pages
    // the provider in chunks and keeps only normalized matches, so a
    // page is always full unless the history is exhausted.
    suspend fun getCallHistoryPage(
        numbers: List<String>,
        limit: Int,
        offset: Int
    ): CallHistoryPage = CallHistoryPage(emptyList(), false)
    fun reload() {}
}

