package io.github.elli0t0.phone.data.contacts

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.StructuredName
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

/**
 * The displayed row first: on a merged contact the first row
 * across all raw contacts is often a non-displayed one, so prefer
 * the super-primary row, then the primary row, then the lowest id.
 */
internal fun findDataRowId(contentResolver: ContentResolver, contactId: Long, mimeType: String): Long? =
    contentResolver.query(
        Data.CONTENT_URI,
        arrayOf(Data._ID),
        "${Data.CONTACT_ID} = ? AND ${Data.MIMETYPE} = ?",
        arrayOf(contactId.toString(), mimeType),
        "${Data.IS_SUPER_PRIMARY} DESC, ${Data.IS_PRIMARY} DESC, ${Data._ID} ASC"
    )?.use { if (it.moveToFirst()) it.getLong(0) else null }

/** Every StructuredName row of a contact as (row id, display name), super-primary first. */
internal fun findAllNameRows(contentResolver: ContentResolver, contactId: Long): List<Pair<Long, String?>> =
    contentResolver.query(
        Data.CONTENT_URI,
        arrayOf(Data._ID, StructuredName.DISPLAY_NAME),
        "${Data.CONTACT_ID} = ? AND ${Data.MIMETYPE} = ?",
        arrayOf(contactId.toString(), StructuredName.CONTENT_ITEM_TYPE),
        "${Data.IS_SUPER_PRIMARY} DESC, ${Data.IS_PRIMARY} DESC, ${Data._ID} ASC"
    )?.use { c ->
        buildList {
            while (c.moveToNext()) add(c.getLong(0) to c.getString(1))
        }
    } ?: emptyList()

/** Re-resolves the aggregated contact id by lookup key: the id can change when contacts aggregate. */
internal fun findContactIdByLookupKey(contentResolver: ContentResolver, lookupKey: String): Long? {
    if (lookupKey.isBlank()) return null
    val uri = Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, lookupKey)
    return contentResolver.query(uri, arrayOf(Contacts._ID), null, null, null)?.use {
        if (it.moveToFirst()) it.getLong(0) else null
    }
}

/**
 * The birthday event row specifically: other Event rows (e.g.
 * anniversaries) share the Event mime type, so a plain
 * [findDataRowId] could return the wrong row.
 */
internal fun findBirthdayEventRowId(contentResolver: ContentResolver, contactId: Long): Long? {
    val cursor = contentResolver.query(
        Data.CONTENT_URI,
        arrayOf(Data._ID),
        "${Data.CONTACT_ID} = ? AND ${Data.MIMETYPE} = ? AND ${Event.TYPE} = ?",
        arrayOf(
            contactId.toString(),
            Event.CONTENT_ITEM_TYPE,
            Event.TYPE_BIRTHDAY.toString()
        ),
        null
    )
    cursor?.use {
        if (it.moveToFirst()) {
            return it.getLong(it.getColumnIndexOrThrow(Data._ID))
        }
    }
    return null
}

internal fun findNameRowIdForRawContact(contentResolver: ContentResolver, rawContactId: Long): Long? {
    val cursor = contentResolver.query(
        Data.CONTENT_URI,
        arrayOf(Data._ID),
        "${Data.RAW_CONTACT_ID} = ? AND ${Data.MIMETYPE} = ?",
        arrayOf(rawContactId.toString(), StructuredName.CONTENT_ITEM_TYPE),
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
