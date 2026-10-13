package io.github.elli0t0.phone.data.contacts.model

data class ContactDetail(
    val id: Long,
    val lookupKey: String,
    val displayName: String,
    val photoUri: String? = null,
    val phones: List<ContactPhone> = emptyList(),
    val emails: List<ContactEmail> = emptyList(),
    val organization: String? = null,
    val customRingtone: String? = null
)

