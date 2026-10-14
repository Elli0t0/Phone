package io.github.elli0t0.phone.feature.contacts

import io.github.elli0t0.phone.data.calllog.model.CallLogEntry

data class ContactHistoryUiState(
    val contactName: String = "",
    val entries: List<CallLogEntry> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false
)
