package io.github.elli0t0.phone.data.contacts

import android.content.ContentResolver
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.RawContacts

internal fun findWritableRawContactId(contentResolver: ContentResolver, contactId: Long): Long? {
    val cursor = contentResolver.query(
        RawContacts.CONTENT_URI,
        arrayOf(RawContacts._ID, RawContacts.ACCOUNT_TYPE),
        "${RawContacts.CONTACT_ID} = ? AND ${RawContacts.DELETED} = 0",
        arrayOf(contactId.toString()),
        null
    )
    cursor?.use {
        if (it.moveToFirst()) {
            return it.getLong(it.getColumnIndexOrThrow(RawContacts._ID))
        }
    }
    return null
}

internal fun findDataRowId(contentResolver: ContentResolver, contactId: Long, mimeType: String): Long? {
    val cursor = contentResolver.query(
        Data.CONTENT_URI,
        arrayOf(Data._ID),
        "${Data.CONTACT_ID} = ? AND ${Data.MIMETYPE} = ?",
        arrayOf(contactId.toString(), mimeType),
        null
    )
    cursor?.use {
        if (it.moveToFirst()) {
            return it.getLong(it.getColumnIndexOrThrow(Data._ID))
        }
    }
    return null
}

internal fun queryContactIdFromRawContact(contentResolver: ContentResolver, rawContactId: Long): Long? {
    val cursor = contentResolver.query(
        RawContacts.CONTENT_URI,
        arrayOf(RawContacts.CONTACT_ID),
        "${RawContacts._ID} = ?",
        arrayOf(rawContactId.toString()),
        null
    )
    cursor?.use {
        if (it.moveToFirst()) {
            return it.getLong(it.getColumnIndexOrThrow(RawContacts.CONTACT_ID))
        }
    }
    return null
}
