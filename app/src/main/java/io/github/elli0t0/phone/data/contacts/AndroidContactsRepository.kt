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
import android.provider.ContactsContract.CommonDataKinds.Event
import android.provider.ContactsContract.CommonDataKinds.GroupMembership
import android.provider.ContactsContract.CommonDataKinds.Note
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.Photo
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import android.provider.ContactsContract.Contacts
import android.provider.ContactsContract.Data
import android.provider.ContactsContract.Groups
import android.provider.ContactsContract.RawContacts
import io.github.elli0t0.phone.data.contacts.model.ContactDetail
import io.github.elli0t0.phone.data.contacts.model.ContactGroup
import io.github.elli0t0.phone.data.contacts.model.ContactInput
import io.github.elli0t0.phone.data.contacts.model.ContactMergeOutcome
import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import io.github.elli0t0.phone.data.contacts.util.ContactMergeUtil
import io.github.elli0t0.phone.data.favorites.ContactSimDao
import io.github.elli0t0.phone.data.favorites.FavoritesDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

class AndroidContactsRepository(
    private val contentResolver: ContentResolver,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
    // Optional: when the container wires them, a merge re-keys the
    // lookupKey-keyed Room rows (favorites, per-contact SIM) to the
    // merged aggregate's new key. Tests construct without them.
    private val favoritesDao: FavoritesDao? = null,
    private val contactSimDao: ContactSimDao? = null
) : ContactsRepository {

    // One shared, cached contacts flow: the list is kept in memory in this
    // singleton repository and every collector (Recents, Contacts, Dialpad)
    // gets the last value instantly. The ContentObserver stays registered
    // for the app's lifetime, so the cache is refreshed on any contacts
    // change and the provider is never re-queried on a tab switch.
    private val contactsFlow: StateFlow<List<ContactSummary>> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                trySend(queryContactsSummaries(contentResolver))
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
        trySend(queryContactsSummaries(contentResolver))

        awaitClose {
            try {
                contentResolver.unregisterContentObserver(observer)
            } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)
        .stateIn(scope, SharingStarted.Lazily, emptyList())

    override fun getContactsFlow(): Flow<List<ContactSummary>> = contactsFlow

    override fun getContactDetail(lookupKey: String, contactId: Long): Flow<ContactDetail?> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                trySend(queryContactDetail(contentResolver, contactId))
            }
        }

        val contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId)
        try {
            contentResolver.registerContentObserver(contactUri, true, observer)
        } catch (_: SecurityException) {}

        trySend(queryContactDetail(contentResolver, contactId))

        awaitClose {
            try {
                contentResolver.unregisterContentObserver(observer)
            } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)

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
                            .withValue(
                                Phone.LABEL,
                                phone.label?.trim()?.takeIf {
                                    phone.type == Phone.TYPE_CUSTOM && it.isNotBlank()
                                }
                            )
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

            // 6. Notes
            input.notes?.trim()?.takeIf { it.isNotBlank() }?.let { note ->
                ops.add(
                    ContentProviderOperation.newInsert(Data.CONTENT_URI)
                        .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                        .withValue(Data.MIMETYPE, Note.CONTENT_ITEM_TYPE)
                        .withValue(Note.NOTE, note)
                        .build()
                )
            }

            // 7. Birthday
            input.birthday?.trim()?.takeIf { it.isNotBlank() }?.let { date ->
                ops.add(
                    ContentProviderOperation.newInsert(Data.CONTENT_URI)
                        .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                        .withValue(Data.MIMETYPE, Event.CONTENT_ITEM_TYPE)
                        .withValue(Event.TYPE, Event.TYPE_BIRTHDAY)
                        .withValue(Event.START_DATE, date)
                        .build()
                )
            }

            // 8. Address (formatted only; no type picker, so Other)
            input.address?.trim()?.takeIf { it.isNotBlank() }?.let { addr ->
                ops.add(
                    ContentProviderOperation.newInsert(Data.CONTENT_URI)
                        .withValueBackReference(Data.RAW_CONTACT_ID, 0)
                        .withValue(Data.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE)
                        .withValue(StructuredPostal.TYPE, StructuredPostal.TYPE_OTHER)
                        .withValue(StructuredPostal.FORMATTED_ADDRESS, addr)
                        .build()
                )
            }

            val results = contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
            val rawContactUri = results[0].uri ?: error("Failed to create raw contact")
            val rawContactId = ContentUris.parseId(rawContactUri)

            // Find aggregated Contact ID from raw contact ID
            queryContactIdFromRawContact(contentResolver, rawContactId) ?: rawContactId
        }
    }

    override suspend fun updateContact(
        contactId: Long,
        original: ContactDetail,
        updated: ContactInput
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val rawContactId = findWritableRawContactId(contentResolver, contactId)
                ?: error("No writable raw contact found for contact $contactId")

            val ops = ArrayList<ContentProviderOperation>()

            // 1. StructuredName update/insert
            val existingNameRowId = findDataRowId(contentResolver, contactId, StructuredName.CONTENT_ITEM_TYPE)
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
                val cleanLabel = phone.label?.trim()?.takeIf {
                    phone.type == Phone.TYPE_CUSTOM && it.isNotBlank()
                }
                if (phone.id > 0) {
                    val orig = original.phones.find { it.id == phone.id }
                    if (orig == null || orig.number.trim() != cleanNumber || orig.type != phone.type ||
                        orig.label?.trim().orEmpty() != cleanLabel.orEmpty()
                    ) {
                        ops.add(
                            ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                                .withSelection("${Data._ID} = ?", arrayOf(phone.id.toString()))
                                .withValue(Phone.NUMBER, cleanNumber)
                                .withValue(Phone.TYPE, phone.type)
                                .withValue(Phone.LABEL, cleanLabel)
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
                            .withValue(Phone.LABEL, cleanLabel)
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
            val existingOrgRowId = findDataRowId(contentResolver, contactId, Organization.CONTENT_ITEM_TYPE)
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

            // 5. Notes: first note row only; further note rows are
            // never touched (unedited data is never dropped).
            val existingNoteRowId = findDataRowId(contentResolver, contactId, Note.CONTENT_ITEM_TYPE)
            val cleanNotes = updated.notes?.trim().orEmpty()
            val originalNotes = original.notes?.trim().orEmpty()
            if (existingNoteRowId != null) {
                if (cleanNotes.isBlank()) {
                    ops.add(
                        ContentProviderOperation.newDelete(Data.CONTENT_URI)
                            .withSelection("${Data._ID} = ?", arrayOf(existingNoteRowId.toString()))
                            .build()
                    )
                } else if (cleanNotes != originalNotes) {
                    ops.add(
                        ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                            .withSelection("${Data._ID} = ?", arrayOf(existingNoteRowId.toString()))
                            .withValue(Note.NOTE, cleanNotes)
                            .build()
                    )
                }
            } else if (cleanNotes.isNotBlank()) {
                ops.add(
                    ContentProviderOperation.newInsert(Data.CONTENT_URI)
                        .withValue(Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(Data.MIMETYPE, Note.CONTENT_ITEM_TYPE)
                        .withValue(Note.NOTE, cleanNotes)
                        .build()
                )
            }

            // 6. Birthday: the birthday event row only; other event
            // rows (anniversaries) are never touched.
            val existingBirthdayRowId = findBirthdayEventRowId(contentResolver, contactId)
            val cleanBirthday = updated.birthday?.trim().orEmpty()
            val originalBirthday = original.birthday?.trim().orEmpty()
            if (existingBirthdayRowId != null) {
                if (cleanBirthday.isBlank()) {
                    ops.add(
                        ContentProviderOperation.newDelete(Data.CONTENT_URI)
                            .withSelection("${Data._ID} = ?", arrayOf(existingBirthdayRowId.toString()))
                            .build()
                    )
                } else if (cleanBirthday != originalBirthday) {
                    ops.add(
                        ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                            .withSelection("${Data._ID} = ?", arrayOf(existingBirthdayRowId.toString()))
                            .withValue(Event.START_DATE, cleanBirthday)
                            .build()
                    )
                }
            } else if (cleanBirthday.isNotBlank()) {
                ops.add(
                    ContentProviderOperation.newInsert(Data.CONTENT_URI)
                        .withValue(Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(Data.MIMETYPE, Event.CONTENT_ITEM_TYPE)
                        .withValue(Event.TYPE, Event.TYPE_BIRTHDAY)
                        .withValue(Event.START_DATE, cleanBirthday)
                        .build()
                )
            }

            // 7. Address: first postal row only; further postal rows
            // are never touched.
            val existingPostalRowId = findDataRowId(contentResolver, contactId, StructuredPostal.CONTENT_ITEM_TYPE)
            val cleanAddress = updated.address?.trim().orEmpty()
            val originalAddress = original.address?.trim().orEmpty()
            if (existingPostalRowId != null) {
                if (cleanAddress.isBlank()) {
                    ops.add(
                        ContentProviderOperation.newDelete(Data.CONTENT_URI)
                            .withSelection("${Data._ID} = ?", arrayOf(existingPostalRowId.toString()))
                            .build()
                    )
                } else if (cleanAddress != originalAddress) {
                    ops.add(
                        ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                            .withSelection("${Data._ID} = ?", arrayOf(existingPostalRowId.toString()))
                            .withValue(StructuredPostal.FORMATTED_ADDRESS, cleanAddress)
                            .build()
                    )
                }
            } else if (cleanAddress.isNotBlank()) {
                ops.add(
                    ContentProviderOperation.newInsert(Data.CONTENT_URI)
                        .withValue(Data.RAW_CONTACT_ID, rawContactId)
                        .withValue(Data.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE)
                        .withValue(StructuredPostal.TYPE, StructuredPostal.TYPE_OTHER)
                        .withValue(StructuredPostal.FORMATTED_ADDRESS, cleanAddress)
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
            val primaryPhones = queryPrimaryPhones(contentResolver)
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

    override suspend fun mergeContacts(keepContactId: Long, mergeContactIds: List<Long>): Result<ContactMergeOutcome> =
        withContext(Dispatchers.IO) {
            // try/catch, not runCatching: updateContact/deleteContact are suspend
            // functions and cannot be called inside runCatching's non-suspend block.
            try {
                val targets = mergeContactIds.filter { it != keepContactId }.distinct()
                if (targets.isEmpty()) {
                    return@withContext Result.success(
                        ContactMergeOutcome(keepRawId = -1L, targetRawIds = emptyList(), linked = true)
                    )
                }
                val keepRawId = queryRawContactIds(keepContactId).firstOrNull()
                    ?: error("Kept contact has no raw contact")
                val targetRawIds = targets.flatMap { queryRawContactIds(it) }
                if (targetRawIds.isEmpty()) error("Merged contacts have no raw contacts")
                // Old keys, captured before linking changes the aggregate key.
                val oldKeys = buildList {
                    queryContactLookupKey(keepContactId)?.let { add(it) }
                    targets.forEach { queryContactLookupKey(it)?.let { key -> add(key) } }
                }.distinct()
                try {
                    applyAggregationExceptions(
                        keepRawId,
                        targetRawIds,
                        ContactsContract.AggregationExceptions.TYPE_KEEP_TOGETHER
                    )
                } catch (e: Exception) {
                    // Read-only accounts reject aggregation exceptions:
                    // fall back to copying every field into the kept
                    // contact, then deleting the others. Not undoable.
                    mergeByCopy(keepContactId, targets)
                    return@withContext Result.success(
                        ContactMergeOutcome(keepRawId, targetRawIds, linked = false)
                    )
                }
                // The kept contact's name wins: mark its own name row
                // primary (best effort — the merged aggregate keeps its
                // own display name otherwise).
                try {
                    val nameRowId = findNameRowIdForRawContact(contentResolver, keepRawId)
                    if (nameRowId != null) {
                        val values = ContentValues().apply {
                            put(StructuredName.IS_PRIMARY, 1)
                            put(StructuredName.IS_SUPER_PRIMARY, 1)
                        }
                        contentResolver.update(
                            Data.CONTENT_URI,
                            values,
                            "${Data._ID} = ?",
                            arrayOf(nameRowId.toString())
                        )
                    }
                } catch (_: Exception) {
                }
                // Linking changes the aggregate lookupKey: re-key the
                // Room rows that point at the old keys (best effort).
                try {
                    val newKey = queryContactIdFromRawContact(contentResolver, keepRawId)
                        ?.let { queryContactLookupKey(it) }
                    if (!newKey.isNullOrBlank()) {
                        for (oldKey in oldKeys) {
                            if (oldKey != newKey) rekeyLookupKey(oldKey, newKey)
                        }
                    }
                } catch (_: Exception) {
                }
                Result.success(ContactMergeOutcome(keepRawId, targetRawIds, linked = true))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun unlinkContacts(outcome: ContactMergeOutcome): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                if (!outcome.linked || outcome.targetRawIds.isEmpty()) {
                    return@withContext Result.success(Unit)
                }
                val mergedKey = queryContactIdFromRawContact(contentResolver, outcome.keepRawId)
                    ?.let { queryContactLookupKey(it) }
                applyAggregationExceptions(
                    outcome.keepRawId,
                    outcome.targetRawIds,
                    ContactsContract.AggregationExceptions.TYPE_KEEP_SEPARATE
                )
                // Best effort: the kept contact's key may change again
                // once the aggregates split.
                try {
                    val currentKey = queryContactIdFromRawContact(contentResolver, outcome.keepRawId)
                        ?.let { queryContactLookupKey(it) }
                    if (!mergedKey.isNullOrBlank() && !currentKey.isNullOrBlank() && mergedKey != currentKey) {
                        rekeyLookupKey(mergedKey, currentKey)
                    }
                } catch (_: Exception) {
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun queryRawContactIds(contactId: Long): List<Long> {
        val ids = mutableListOf<Long>()
        contentResolver.query(
            RawContacts.CONTENT_URI,
            arrayOf(RawContacts._ID),
            "${RawContacts.CONTACT_ID} = ? AND ${RawContacts.DELETED} = 0",
            arrayOf(contactId.toString()),
            "${RawContacts._ID} ASC"
        )?.use { cursor ->
            while (cursor.moveToNext()) ids.add(cursor.getLong(0))
        }
        return ids
    }

    private fun queryContactLookupKey(contactId: Long): String? {
        contentResolver.query(
            ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId),
            arrayOf(Contacts.LOOKUP_KEY),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) return cursor.getString(0)
        }
        return null
    }

    private fun applyAggregationExceptions(keepRawId: Long, targetRawIds: List<Long>, type: Int) {
        val pairs = ContactMergeUtil.aggregationPairs(keepRawId, targetRawIds)
        if (pairs.isEmpty()) return
        val ops = ArrayList<ContentProviderOperation>()
        for ((raw1, raw2) in pairs) {
            ops.add(
                ContentProviderOperation.newUpdate(ContactsContract.AggregationExceptions.CONTENT_URI)
                    .withValue(ContactsContract.AggregationExceptions.TYPE, type)
                    .withValue(ContactsContract.AggregationExceptions.RAW_CONTACT_ID1, raw1)
                    .withValue(ContactsContract.AggregationExceptions.RAW_CONTACT_ID2, raw2)
                    .build()
            )
        }
        contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
    }

    private suspend fun rekeyLookupKey(oldKey: String, newKey: String) {
        favoritesDao?.let { dao ->
            if (dao.isFavorite(newKey)) {
                dao.deleteFavoriteByLookupKey(oldKey)
            } else {
                dao.rekeyLookupKey(oldKey, newKey)
            }
        }
        contactSimDao?.let { dao ->
            if (dao.getSimId(newKey) != null) {
                dao.deleteByLookupKey(oldKey)
            } else {
                dao.rekeyLookupKey(oldKey, newKey)
            }
        }
    }

    /**
     * Read-only fallback: copy every field (phones, emails, organization,
     * notes, birthday, address, photo, ringtone — kept contact wins on
     * each) into the kept contact, then delete the others.
     */
    private suspend fun mergeByCopy(keepContactId: Long, targets: List<Long>) {
        val keptDetail = queryContactDetail(contentResolver, keepContactId)
            ?: error("Kept contact not found")
        val targetDetails = targets.mapNotNull { queryContactDetail(contentResolver, it) }
        updateContact(
            keepContactId,
            keptDetail,
            ContactMergeUtil.mergedFallbackInput(keptDetail, targetDetails)
        ).getOrThrow()
        if (keptDetail.photoUri.isNullOrBlank()) {
            for (detail in targetDetails) {
                val bytes = queryPhotoBytes(detail.id) ?: continue
                saveContactPhoto(keepContactId, bytes).getOrThrow()
                break
            }
        }
        if (keptDetail.customRingtone.isNullOrBlank()) {
            val ringtone = targetDetails.firstNotNullOfOrNull { it.customRingtone }
            if (ringtone != null) updateContactRingtone(keepContactId, ringtone).getOrThrow()
        }
        for (targetId in targets) {
            val detail = queryContactDetail(contentResolver, targetId) ?: continue
            deleteContact(targetId, detail.lookupKey).getOrThrow()
        }
    }

    private fun queryPhotoBytes(contactId: Long): ByteArray? {
        contentResolver.query(
            Data.CONTENT_URI,
            arrayOf(Photo.PHOTO),
            "${Data.CONTACT_ID} = ? AND ${Data.MIMETYPE} = ?",
            arrayOf(contactId.toString(), Photo.CONTENT_ITEM_TYPE),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst() && !cursor.isNull(0)) return cursor.getBlob(0)
        }
        return null
    }

    override suspend fun saveContactPhoto(contactId: Long, photoBytes: ByteArray): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val existingPhotoRowId = findDataRowId(contentResolver, contactId, Photo.CONTENT_ITEM_TYPE)
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
                val rawContactId = findWritableRawContactId(contentResolver, contactId)
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

}
