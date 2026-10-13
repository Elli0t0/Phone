package io.github.elli0t0.phone.data.calllog.model

data class CallGroup(
    val primaryCall: CallLogEntry,
    val calls: List<CallLogEntry>
) {
    val count: Int get() = calls.size
    val id: Long get() = primaryCall.id
    val number: String get() = primaryCall.number
    val displayTitle: String get() = primaryCall.displayTitle
    val type: CallType get() = primaryCall.type
    val date: Long get() = primaryCall.date
    val durationSeconds: Long get() = primaryCall.durationSeconds
}
