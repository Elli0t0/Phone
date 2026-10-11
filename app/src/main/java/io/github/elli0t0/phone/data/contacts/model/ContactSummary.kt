package io.github.elli0t0.phone.data.contacts.model

data class ContactSummary(
    val id: Long,
    val lookupKey: String,
    val displayName: String,
    val photoUri: String? = null,
    val primaryNumberPreview: String? = null
)
