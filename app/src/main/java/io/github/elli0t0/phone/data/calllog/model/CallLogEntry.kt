package io.github.elli0t0.phone.data.calllog.model

data class CallLogEntry(
    val id: Long,
    val number: String,
    val formattedNumber: String,
    val cachedName: String?,
    val contactName: String?,
    val contactId: Long?,
    val contactLookupKey: String?,
    val contactPhotoUri: String?,
    val type: CallType,
    val date: Long,
    val durationSeconds: Long,
    val simInfo: String?
) {
    val displayTitle: String
        get() = contactName?.takeIf { it.isNotBlank() }
            ?: cachedName?.takeIf { it.isNotBlank() }
            ?: formattedNumber.takeIf { it.isNotBlank() }
            ?: number

    val isMissedOrRejected: Boolean
        get() = type == CallType.Missed || type == CallType.Rejected || type == CallType.Blocked
}
