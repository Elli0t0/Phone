package io.github.elli0t0.phone.data.contacts.model

data class ContactInput(
    val displayName: String,
    val phones: List<ContactPhone> = emptyList(),
    val emails: List<ContactEmail> = emptyList(),
    val organization: String? = null,
    val notes: String? = null,
    val birthday: String? = null,
    val address: String? = null
)
