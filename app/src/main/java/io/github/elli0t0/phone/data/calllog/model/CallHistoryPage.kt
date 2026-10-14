package io.github.elli0t0.phone.data.calllog.model

// One page of a contact's call history. `hasMore` is exact: the query
// scans one extra match past the page to decide it.
data class CallHistoryPage(
    val entries: List<CallLogEntry>,
    val hasMore: Boolean
)
