package io.github.elli0t0.phone.data.contacts.model

import android.provider.ContactsContract

data class ContactEmail(
    val id: Long = 0L,
    val address: String,
    val type: Int = ContactsContract.CommonDataKinds.Email.TYPE_HOME,
    val label: String? = null
)
