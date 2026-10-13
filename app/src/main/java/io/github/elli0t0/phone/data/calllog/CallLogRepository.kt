package io.github.elli0t0.phone.data.calllog

import io.github.elli0t0.phone.data.calllog.model.CallLogEntry
import kotlinx.coroutines.flow.Flow

interface CallLogRepository {
    fun getCallLogsFlow(): Flow<List<CallLogEntry>>
    suspend fun deleteCall(id: Long): Boolean
}
