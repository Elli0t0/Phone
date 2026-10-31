package io.github.elli0t0.phone.data.contacts

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Data
import io.github.elli0t0.phone.data.contacts.model.ContactDetail
import io.github.elli0t0.phone.data.contacts.model.ContactEmail
import io.github.elli0t0.phone.data.contacts.model.ContactPhone
import io.github.elli0t0.phone.data.contacts.model.ContactSummary

internal fun queryContactsSummaries(contentResolver: ContentResolver): List<ContactSummary> {
    return try {
        val primaryPhones = queryPrimaryPhones(contentResolver)
        val list = mutableListOf<ContactSummary>()

        val projection = arrayOf(
            Contacts._ID,
            Contacts.LOOKUP_KEY,
            Contacts.DISPLAY_NAME_PRIMARY,
            Contacts.PHOTO_URI
        )

        val cursor: Cursor? = contentResolver.query(
            Contacts.CONTENT_URI,
            projection,
            null,
            null,
            "${Contacts.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC"
        )

        cursor?.use {
            val idIdx = it.getColumnIndex(Contacts._ID)
            val lookupIdx = it.getColumnIndex(Contacts.LOOKUP_KEY)
            val nameIdx = it.getColumnIndex(Contacts.DISPLAY_NAME_PRIMARY)
            val photoIdx = it.getColumnIndex(Contacts.PHOTO_URI)

            while (it.moveToNext()) {
                val id = it.getLong(idIdx)
                val lookupKey = it.getString(lookupIdx) ?: ""
                val rawName = it.getString(nameIdx)
                val photoUri = it.getString(photoIdx)
                val phonePreview = primaryPhones[id]

                val displayName = rawName?.takeIf { n -> n.isNotBlank() }
                    ?: phonePreview
                    ?: ""

                list.add(
                    ContactSummary(
                        id = id,
                        lookupKey = lookupKey,
                        displayName = displayName,
                        photoUri = photoUri,
                        primaryNumberPreview = phonePreview
                    )
                )
            }
        }
        list
    } catch (_: SecurityException) {
        emptyList()
    } catch (_: Exception) {
        emptyList()
    }
}

internal fun queryPrimaryPhones(contentResolver: ContentResolver): Map<Long, String> {
    val map = mutableMapOf<Long, String>()
    val projection = arrayOf(
        Phone.CONTACT_ID,
        Phone.NUMBER,
        Phone.IS_PRIMARY
    )

    val cursor = contentResolver.query(
        Phone.CONTENT_URI,
        projection,
        null,
        null,
        "${Phone.IS_PRIMARY} DESC"
    )

    cursor?.use {
        val contactIdIdx = it.getColumnIndex(Phone.CONTACT_ID)
        val numberIdx = it.getColumnIndex(Phone.NUMBER)

        while (it.moveToNext()) {
            val contactId = it.getLong(contactIdIdx)
            val number = it.getString(numberIdx) ?: ""
            if (!map.containsKey(contactId) && number.isNotBlank()) {
                map[contactId] = number
            }
        }
    }
    return map
}

internal fun queryContactDetail(contentResolver: ContentResolver, contactId: Long): ContactDetail? {
    return try {
        var displayName = ""
        var lookupKey = ""
        var photoUri: String? = null
        var organization: String? = null
        var customRingtone: String? = null
        var notes: String? = null
        var birthday: String? = null
        var address: String? = null
        val phones = mutableListOf<ContactPhone>()
        val emails = mutableListOf<ContactEmail>()

        val projection = arrayOf(
            Data._ID,
            Data.MIMETYPE,
            Data.LOOKUP_KEY,
            StructuredName.DISPLAY_NAME,
            Phone.NUMBER,
            Phone.TYPE,
            Phone.LABEL,
            Email.ADDRESS,
            Email.TYPE,
            Email.LABEL,
            Organization.COMPANY,
            Note.NOTE,
            Event.START_DATE,
            Event.TYPE,
            StructuredPostal.FORMATTED_ADDRESS
        )

        val cursor = contentResolver.query(
            Data.CONTENT_URI,
            projection,
            "${Data.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )

        cursor?.use {
            val dataIdIdx = it.getColumnIndex(Data._ID)
            val mimeIdx = it.getColumnIndex(Data.MIMETYPE)
            val lookupIdx = it.getColumnIndex(Data.LOOKUP_KEY)
            val nameIdx = it.getColumnIndex(StructuredName.DISPLAY_NAME)
            val phoneNumIdx = it.getColumnIndex(Phone.NUMBER)
            val phoneTypeIdx = it.getColumnIndex(Phone.TYPE)
            val phoneLabelIdx = it.getColumnIndex(Phone.LABEL)
            val emailAddrIdx = it.getColumnIndex(Email.ADDRESS)
            val emailTypeIdx = it.getColumnIndex(Email.TYPE)
            val emailLabelIdx = it.getColumnIndex(Email.LABEL)
            val companyIdx = it.getColumnIndex(Organization.COMPANY)
            val noteIdx = it.getColumnIndex(Note.NOTE)
            val eventDateIdx = it.getColumnIndex(Event.START_DATE)
            val eventTypeIdx = it.getColumnIndex(Event.TYPE)
            val postalAddrIdx = it.getColumnIndex(StructuredPostal.FORMATTED_ADDRESS)

            while (it.moveToNext()) {
                val dataId = it.getLong(dataIdIdx)
                val mime = it.getString(mimeIdx) ?: ""
                if (lookupKey.isEmpty()) {
                    lookupKey = it.getString(lookupIdx) ?: ""
                }

                when (mime) {
                    StructuredName.CONTENT_ITEM_TYPE -> {
                        val name = it.getString(nameIdx)
                        if (!name.isNullOrBlank()) displayName = name
                    }
                    Phone.CONTENT_ITEM_TYPE -> {
                        val number = it.getString(phoneNumIdx) ?: ""
                        val type = it.getInt(phoneTypeIdx)
                        val label = it.getString(phoneLabelIdx)
                        if (number.isNotBlank()) {
                            phones.add(ContactPhone(id = dataId, number = number, type = type, label = label))
                        }
                    }
                    Email.CONTENT_ITEM_TYPE -> {
                        val address = it.getString(emailAddrIdx) ?: ""
                        val type = it.getInt(emailTypeIdx)
                        val label = it.getString(emailLabelIdx)
                        if (address.isNotBlank()) {
                            emails.add(ContactEmail(id = dataId, address = address, type = type, label = label))
                        }
                    }
                    Organization.CONTENT_ITEM_TYPE -> {
                        val company = it.getString(companyIdx)
                        if (!company.isNullOrBlank()) organization = company
                    }
                    Note.CONTENT_ITEM_TYPE -> {
                        // First note wins; later note rows are left
                        // untouched (never dropped by an edit).
                        if (notes == null) {
                            it.getString(noteIdx)?.takeIf { it.isNotBlank() }?.let { notes = it }
                        }
                    }
                    Event.CONTENT_ITEM_TYPE -> {
                        // Only the birthday event; anniversaries and
                        // other events are not this field.
                        if (birthday == null && it.getInt(eventTypeIdx) == Event.TYPE_BIRTHDAY) {
                            it.getString(eventDateIdx)?.takeIf { it.isNotBlank() }?.let { birthday = it }
                        }
                    }
                    StructuredPostal.CONTENT_ITEM_TYPE -> {
                        // First postal row wins; other addresses are
                        // left untouched (never dropped by an edit).
                        if (address == null) {
                            it.getString(postalAddrIdx)?.takeIf { it.isNotBlank() }?.let { address = it }
                        }
                    }
                }
            }
        }

        // Also retrieve photo URI from Contacts table
        val contactCursor = contentResolver.query(
            ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId),
            arrayOf(Contacts.PHOTO_URI, Contacts.DISPLAY_NAME_PRIMARY, Contacts.CUSTOM_RINGTONE),
            null,
            null,
            null
        )
        contactCursor?.use {
            if (it.moveToFirst()) {
                photoUri = it.getString(it.getColumnIndexOrThrow(Contacts.PHOTO_URI))
                val primaryName = it.getString(it.getColumnIndexOrThrow(Contacts.DISPLAY_NAME_PRIMARY))
                if (displayName.isBlank() && !primaryName.isNullOrBlank()) {
                    displayName = primaryName
                }
                val ringtoneIdx = it.getColumnIndex(Contacts.CUSTOM_RINGTONE)
                if (ringtoneIdx >= 0) {
                    customRingtone = it.getString(ringtoneIdx)
                }
            }
        }

        ContactDetail(
            id = contactId,
            lookupKey = lookupKey,
            displayName = displayName.ifBlank { phones.firstOrNull()?.number ?: "" },
            photoUri = photoUri,
            phones = phones,
            emails = emails,
            organization = organization,
            customRingtone = customRingtone,
            notes = notes,
            birthday = birthday,
            address = address
        )
    } catch (_: Exception) {
        null
    }
}
