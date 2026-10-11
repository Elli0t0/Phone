package io.github.elli0t0.phone.data.contacts.model

import android.provider.ContactsContract

data class ContactPhone(
    val id: Long = 0L,
    val number: String,
    val type: Int = ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
    val label: String? = null
)
