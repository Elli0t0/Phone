package io.github.elli0t0.phone.data.contacts

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.GroupMembership
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Photo
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.Groups
import android.provider.ContactsContract.RawContacts
import io.github.elli0t0.phone.core.common.PhoneNumberMatcher
import io.github.elli0t0.phone.data.contacts.model.ContactDetail
import io.github.elli0t0.phone.data.contacts.model.ContactEmail
import io.github.elli0t0.phone.data.contacts.model.ContactGroup
import io.github.elli0t0.phone.data.contacts.model.ContactInput
import io.github.elli0t0.phone.data.contacts.model.ContactPhone
import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class AndroidContactsRepository(
    private val contentResolver: ContentResolver
) : ContactsRepository {

    override fun getContactsFlow(): Flow<List<ContactSummary>> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                trySend(queryContactsSummaries())
            }
        }

        try {
            contentResolver.registerContentObserver(
                Contacts.CONTENT_URI,
                true,
                observer
            )
        } catch (_: SecurityException) {
            // Permission denied or revoked while registering
        }

        // Initial emission
        trySend(queryContactsSummaries())

        awaitClose {
            try {
                contentResolver.unregisterContentObserver(observer)
            } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)

    override fun getContactDetail(lookupKey: String, contactId: Long): Flow<ContactDetail?> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                trySend(queryContactDetail(contactId))
            }
        }

        val contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId)
        try {
            contentResolver.registerContentObserver(contactUri, true, observer)
        } catch (_: SecurityException) {}

        trySend(queryContactDetail(contactId))

        awaitClose {
            try {
                contentResolver.unregisterContentObserver(observer)
            } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)

    private fun queryContactsSummaries(): List<ContactSummary> {
        return try {
            val primaryPhones = queryPrimaryPhones()
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

    private fun queryPrimaryPhones(): Map<Long, String> {
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

    private fun queryContactDetail(contactId: Long): ContactDetail? {
        return try {
            var displayName = ""
            var lookupKey = ""
            var photoUri: String? = null
            var organization: String? = null
            var customRingtone: String? = null
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
                Organization.COMPANY
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
                customRingtone = customRingtone
            )
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun createContact(input: ContactInput): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val ops = ArrayList<ContentProviderOperation>()

            // 1. RawContact entry
            ops.add(
                ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
                    .withValue(RawContacts.ACCOUNT_TYPE, null)
                    .withValue(RawContacts.ACCOUNT_NAME, null)
                    .build()
            )

            // 2. StructuredName
            if (input.displayName.isNotBlank()) {
                ops.add(
                    ContentProviderOperation.newInsert(Data.CONTENT_URI)
                        .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                        .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(StructuredName.DISPLAY_NAME, input.displayName.trim())
                        .build()
                )
            }

            // 3. Phones
            input.phones.forEach { phone ->
                val cleanNum = phone.number.trim()
                if (cleanNum.isNotBlank()) {
                    ops.add(
                        ContentProviderOperation.newInsert(Data.CONTENT_URI)
                            .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                            .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                            .withValue(Phone.NUMBER, cleanNum)
                            .withValue(Phone.TYPE, phone.type)
                            .build()
                    )
                }
            }

            // 4. Emails
            input.emails.forEach { email ->
                val cleanAddr = email.address.trim()
                if (cleanAddr.isNotBlank()) {
                    ops.add(
                        ContentProviderOperation.newInsert(Data.CONTENT_URI)
                            .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                            .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
                            .withValue(Email.ADDRESS, cleanAddr)
                            .withValue(Email.TYPE, email.type)
                            .build()
                    )
                }
            }

            // 5. Organization
            input.organization?.trim()?.takeIf { it.isNotBlank() }?.let { org ->
                ops.add(
                    ContentProviderOperation.newInsert(Data.CONTENT_URI)
                        .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                        .withValue(Data.MIMETYPE, Organization.CONTENT_ITEM_TYPE)
                        .withValue(Organization.COMPANY, org)
                        .build()
                )
            }

            val results = contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            val rawContactUri = results[0].uri ?: error("Failed to create raw contact")
            val rawContactId = ContentUris.parseId(rawContactUri)

            // Find aggregated Contact ID from raw contact ID
            queryContactIdFromRawContact(rawContactId) ?: rawContactId
        }
    }

    override suspend fun updateContact(
        contactId: Long,
        original: ContactDetail,
        updated: ContactInput
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val rawContactId = findWritableRawContactId(contactId)
                ?: error("No writable raw contact found for contact $contactId")

            val ops = ArrayList<ContentProviderOperation>()

            // 1. StructuredName update/insert
            val existingNameRowId = findDataRowId(contactId, StructuredName.CONTENT_ITEM_TYPE)
            val cleanNewName = updated.displayName.trim()
            if (existingNameRowId != null) {
                if (cleanNewName != original.displayName.trim()) {
                    ops.add(
                        ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                            .withSelection("${Data._ID} = ?", arrayOf(existingNameRowId.toString()))
                            .withValue(StructuredName.DISPLAY_NAME, cleanNewName)
                            .build()
                    )
                }
            } else if (cleanNewName.isNotBlank()) {
                ops.add(
                    ContentProviderOperation.newInsert(Data.CONTENT_URI)
                        .withValue(Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
                        .withValue(StructuredName.DISPLAY_NAME, cleanNewName)
                        .build()
                )
            }

            // 2. Phones differential update
            val updatedPhonesWithIds = updated.phones.filter { it.number.isNotBlank() }
            val retainedPhoneIds = updatedPhonesWithIds.map { it.id }.filter { it > 0 }.toSet()

            // Delete removed phones
            original.phones.filter { it.id > 0 && it.id !in retainedPhoneIds }.forEach { removed ->
                ops.add(
                    ContentProviderOperation.newDelete(Data.CONTENT_URI)
                        .withSelection("${Data._ID} = ?", arrayOf(removed.id.toString()))
                        .build()
                )
            }

            // Update modified phones and insert new phones
            updatedPhonesWithIds.forEach { phone ->
                val cleanNumber = phone.number.trim()
                if (phone.id > 0) {
                    val orig = original.phones.find { it.id == phone.id }
                    if (orig == null || orig.number.trim() != cleanNumber || orig.type != phone.type) {
                        ops.add(
                            ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                                .withSelection("${Data._ID} = ?", arrayOf(phone.id.toString()))
                                .withValue(Phone.NUMBER, cleanNumber)
                                .withValue(Phone.TYPE, phone.type)
                                .build()
                        )
                    }
                } else {
                    ops.add(
                        ContentProviderOperation.newInsert(Data.CONTENT_URI)
                            .withValue(Data.RAW_CONTACT_ID, rawContactId)
                            .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
                            .withValue(Phone.NUMBER, cleanNumber)
                            .withValue(Phone.TYPE, phone.type)
                            .build()
                    )
                }
            }

            // 3. Emails differential update
            val updatedEmailsWithIds = updated.emails.filter { it.address.isNotBlank() }
            val retainedEmailIds = updatedEmailsWithIds.map { it.id }.filter { it > 0 }.toSet()

            // Delete removed emails
            original.emails.filter { it.id > 0 && it.id !in retainedEmailIds }.forEach { removed ->
                ops.add(
                    ContentProviderOperation.newDelete(Data.CONTENT_URI)
                        .withSelection("${Data._ID} = ?", arrayOf(removed.id.toString()))
                        .build()
                )
            }

            // Update modified emails and insert new emails
            updatedEmailsWithIds.forEach { email ->
                val cleanAddress = email.address.trim()
                if (email.id > 0) {
                    val orig = original.emails.find { it.id == email.id }
                    if (orig == null || orig.address.trim() != cleanAddress || orig.type != email.type) {
                        ops.add(
                            ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                                .withSelection("${Data._ID} = ?", arrayOf(email.id.toString()))
                                .withValue(Email.ADDRESS, cleanAddress)
                                .withValue(Email.TYPE, email.type)
                                .build()
                        )
                    }
                } else {
                    ops.add(
                        ContentProviderOperation.newInsert(Data.CONTENT_URI)
                            .withValue(Data.RAW_CONTACT_ID, rawContactId)
                            .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
                            .withValue(Email.ADDRESS, cleanAddress)
                            .withValue(Email.TYPE, email.type)
                            .build()
                    )
                }
            }

            // 4. Organization update
            val existingOrgRowId = findDataRowId(contactId, Organization.CONTENT_ITEM_TYPE)
            val cleanOrg = updated.organization?.trim().orEmpty()
            val originalOrg = original.organization?.trim().orEmpty()

            if (existingOrgRowId != null) {
                if (cleanOrg.isBlank()) {
                    ops.add(
                        ContentProviderOperation.newDelete(Data.CONTENT_URI)
                            .withSelection("${Data._ID} = ?", arrayOf(existingOrgRowId.toString()))
                            .build()
                    )
                } else if (cleanOrg != originalOrg) {
                    ops.add(
                        ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                            .withSelection("${Data._ID} = ?", arrayOf(existingOrgRowId.toString()))
                            .withValue(Organization.COMPANY, cleanOrg)
                            .build()
                    )
                }
            } else if (cleanOrg.isNotBlank()) {
                ops.add(
                    ContentProviderOperation.newInsert(Data.CONTENT_URI)
                        .withValue(Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(Data.MIMETYPE, Organization.CONTENT_ITEM_TYPE)
                        .withValue(Organization.COMPANY, cleanOrg)
                        .build()
                )
            }

            if (ops.isNotEmpty()) {
                contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            }
        }
    }

    override suspend fun deleteContact(contactId: Long, lookupKey: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val contactUri = if (lookupKey.isNotBlank()) {
                Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, lookupKey)
            } else {
                ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId)
            }
            val count = contentResolver.delete(contactUri, null, null)
            if (count == 0) {
                // Fallback delete by ID
                contentResolver.delete(ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId), null, null)
            }
            Unit
        }
    }

    override suspend fun getProfileContact(): ContactSummary? = withContext(Dispatchers.IO) {
        try {
            val projection = arrayOf(
                ContactsContract.Profile._ID,
                ContactsContract.Profile.LOOKUP_KEY,
                ContactsContract.Profile.DISPLAY_NAME_PRIMARY,
                ContactsContract.Profile.PHOTO_URI
            )
            var profile: ContactSummary? = null
            contentResolver.query(
                ContactsContract.Profile.CONTENT_URI,
                projection,
                null,
                null,
                null
            )?.use {
                if (it.moveToFirst()) {
                    val id = it.getLong(it.getColumnIndexOrThrow(ContactsContract.Profile._ID))
                    val lookupKey = it.getString(it.getColumnIndexOrThrow(ContactsContract.Profile.LOOKUP_KEY)) ?: ""
                    val name = it.getString(it.getColumnIndexOrThrow(ContactsContract.Profile.DISPLAY_NAME_PRIMARY))
                    val photoUri = it.getString(it.getColumnIndexOrThrow(ContactsContract.Profile.PHOTO_URI))
                    var phone: String? = null
                    contentResolver.query(
                        Phone.CONTENT_URI,
                        arrayOf(Phone.NUMBER),
                        "${Phone.CONTACT_ID} = ?",
                        arrayOf(id.toString()),
                        "${Phone.IS_PRIMARY} DESC"
                    )?.use { phoneCursor ->
                        if (phoneCursor.moveToFirst()) {
                            phone = phoneCursor.getString(0)?.takeIf { number -> number.isNotBlank() }
                        }
                    }
                    profile = ContactSummary(
                        id = id,
                        lookupKey = lookupKey,
                        displayName = name?.takeIf { n -> n.isNotBlank() } ?: phone ?: "",
                        photoUri = photoUri,
                        primaryNumberPreview = phone
                    )
                }
            }
            profile
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun getContactGroups(): List<ContactGroup> = withContext(Dispatchers.IO) {
        try {
            // Member counts per group: distinct contact IDs per GROUP_ROW_ID.
            val counts = mutableMapOf<Long, MutableSet<Long>>()
            contentResolver.query(
                Data.CONTENT_URI,
                arrayOf(Data.CONTACT_ID, GroupMembership.GROUP_ROW_ID),
                "${Data.MIMETYPE} = ?",
                arrayOf(GroupMembership.CONTENT_ITEM_TYPE),
                null
            )?.use {
                val contactIdx = it.getColumnIndex(Data.CONTACT_ID)
                val groupIdx = it.getColumnIndex(GroupMembership.GROUP_ROW_ID)
                while (it.moveToNext()) {
                    val members = counts.getOrPut(it.getLong(groupIdx)) { mutableSetOf() }
                    members.add(it.getLong(contactIdx))
                }
            }

            val groups = mutableListOf<ContactGroup>()
            contentResolver.query(
                Groups.CONTENT_URI,
                arrayOf(Groups._ID, Groups.TITLE),
                "${Groups.DELETED} = 0",
                null,
                "${Groups.TITLE} COLLATE LOCALIZED ASC"
            )?.use {
                val idIdx = it.getColumnIndex(Groups._ID)
                val titleIdx = it.getColumnIndex(Groups.TITLE)
                while (it.moveToNext()) {
                    val id = it.getLong(idIdx)
                    val title = it.getString(titleIdx)?.takeIf { t -> t.isNotBlank() } ?: continue
                    groups.add(ContactGroup(id = id, title = title, memberCount = counts[id]?.size ?: 0))
                }
            }
            groups
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun getGroupMembers(groupId: Long): List<ContactSummary> = withContext(Dispatchers.IO) {
        try {
            val memberIds = mutableListOf<Long>()
            contentResolver.query(
                Data.CONTENT_URI,
                arrayOf(Data.CONTACT_ID),
                "${Data.MIMETYPE} = ? AND ${GroupMembership.GROUP_ROW_ID} = ?",
                arrayOf(GroupMembership.CONTENT_ITEM_TYPE, groupId.toString()),
                null
            )?.use {
                val idx = it.getColumnIndex(Data.CONTACT_ID)
                while (it.moveToNext()) {
                    val id = it.getLong(idx)
                    if (!memberIds.contains(id)) {
                        memberIds.add(id)
                    }
                }
            }
            if (memberIds.isEmpty()) {
                return@withContext emptyList()
            }
            val primaryPhones = queryPrimaryPhones()
            val summaries = mutableListOf<ContactSummary>()
            val selection = "${Contacts._ID} IN (${memberIds.joinToString(",") { "?" }})"
            contentResolver.query(
                Contacts.CONTENT_URI,
                arrayOf(
                    Contacts._ID,
                    Contacts.LOOKUP_KEY,
                    Contacts.DISPLAY_NAME_PRIMARY,
                    Contacts.PHOTO_URI
                ),
                selection,
                memberIds.map { it.toString() }.toTypedArray(),
                "${Contacts.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC"
            )?.use {
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
                    summaries.add(
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
            summaries
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun getAllContactNumbers(): Map<Long, List<String>> = withContext(Dispatchers.IO) {
        val map = mutableMapOf<Long, MutableList<String>>()
        try {
            val cursor = contentResolver.query(
                Phone.CONTENT_URI,
                arrayOf(Phone.CONTACT_ID, Phone.NUMBER),
                null,
                null,
                null
            )
            cursor?.use {
                val contactIdIdx = it.getColumnIndex(Phone.CONTACT_ID)
                val numberIdx = it.getColumnIndex(Phone.NUMBER)
                while (it.moveToNext()) {
                    val contactId = it.getLong(contactIdIdx)
                    val number = it.getString(numberIdx)?.trim().orEmpty()
                    if (number.isNotBlank()) {
                        val numbers = map.getOrPut(contactId) { mutableListOf() }
                        if (!numbers.contains(number)) {
                            numbers.add(number)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Return what was collected (possibly empty) instead of failing the scan.
        }
        map
    }

    override suspend fun mergeContacts(keepContactId: Long, mergeContactIds: List<Long>): Result<Unit> =
        withContext(Dispatchers.IO) {
            // try/catch, not runCatching: updateContact/deleteContact are suspend
            // functions and cannot be called inside runCatching's non-suspend block.
            try {
                val keptDetail = queryContactDetail(keepContactId)
                    ?: error("Kept contact not found")
                val mergedPhones = keptDetail.phones.toMutableList()
                val mergedEmails = keptDetail.emails.toMutableList()
                val targets = mergeContactIds.filter { it != keepContactId }.distinct()

                for (targetId in targets) {
                    val detail = queryContactDetail(targetId) ?: continue
                    for (phone in detail.phones) {
                        if (mergedPhones.none { PhoneNumberMatcher.matches(it.number, phone.number) }) {
                            mergedPhones.add(
                                ContactPhone(id = 0L, number = phone.number, type = phone.type, label = phone.label)
                            )
                        }
                    }
                    for (email in detail.emails) {
                        if (mergedEmails.none { it.address.equals(email.address, ignoreCase = true) }) {
                            mergedEmails.add(
                                ContactEmail(id = 0L, address = email.address, type = email.type, label = email.label)
                            )
                        }
                    }
                }

                // Update first (no data loss), then delete the merged contacts.
                updateContact(
                    keepContactId,
                    keptDetail,
                    ContactInput(
                        displayName = keptDetail.displayName,
                        phones = mergedPhones,
                        emails = mergedEmails,
                        organization = keptDetail.organization
                    )
                ).getOrThrow()

                for (targetId in targets) {
                    val detail = queryContactDetail(targetId) ?: continue
                    deleteContact(targetId, detail.lookupKey).getOrThrow()
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun saveContactPhoto(contactId: Long, photoBytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val existingPhotoRowId = findDataRowId(contactId, Photo.CONTENT_ITEM_TYPE)
            if (existingPhotoRowId != null) {
                val values = ContentValues().apply {
                    put(Photo.PHOTO, photoBytes)
                }
                contentResolver.update(
                    Data.CONTENT_URI,
                    values,
                    "${Data._ID} = ?",
                    arrayOf(existingPhotoRowId.toString())
                )
            } else {
                val rawContactId = findWritableRawContactId(contactId)
                    ?: error("No writable raw contact found for contact $contactId")
                val values = ContentValues().apply {
                    put(Data.RAW_CONTACT_ID, rawContactId)
                    put(Data.MIMETYPE, Photo.CONTENT_ITEM_TYPE)
                    put(Photo.PHOTO, photoBytes)
                }
                contentResolver.insert(Data.CONTENT_URI, values)
            }
            Unit
        }
    }

    override suspend fun removeContactPhoto(contactId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            contentResolver.delete(
                Data.CONTENT_URI,
                "${Data.CONTACT_ID} = ? AND ${Data.MIMETYPE} = ?",
                arrayOf(contactId.toString(), Photo.CONTENT_ITEM_TYPE)
            )
            Unit
        }
    }

    override suspend fun updateContactRingtone(contactId: Long, ringtoneUri: String?): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId)
            val values = ContentValues().apply {
                if (ringtoneUri == null) {
                    putNull(Contacts.CUSTOM_RINGTONE)
                } else {
                    put(Contacts.CUSTOM_RINGTONE, ringtoneUri)
                }
            }
            contentResolver.update(contactUri, values, null, null)
            Unit
        }
    }


    private fun findWritableRawContactId(contactId: Long): Long? {
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

    private fun findDataRowId(contactId: Long, mimeType: String): Long? {
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

    private fun queryContactIdFromRawContact(rawContactId: Long): Long? {
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
}
