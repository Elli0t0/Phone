package io.github.elli0t0.phone.data.calllog

import io.github.elli0t0.phone.data.calllog.model.CallLogEntry
import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.flow.StateFlow

interface CallLogRepository {
    fun getCallLogsFlow(): Flow<List<CallLogEntry>>
    val isLoadingFlow: StateFlow<Boolean> get() = kotlinx.coroutines.flow.MutableStateFlow(false)
    suspend fun deleteCall(id: Long): Boolean
    fun reload() {}
}

