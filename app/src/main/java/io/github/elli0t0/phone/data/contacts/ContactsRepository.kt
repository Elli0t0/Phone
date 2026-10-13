package io.github.elli0t0.phone.data.contacts

import io.github.elli0t0.phone.data.contacts.model.ContactDetail
import io.github.elli0t0.phone.data.contacts.model.ContactInput
import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import kotlinx.coroutines.flow.Flow

interface ContactsRepository {
    /**
     * Flow emitting the full list of contacts, automatically refreshing on ContentObserver change.
     */
    fun getContactsFlow(): Flow<List<ContactSummary>>

    /**
     * Flow emitting detailed data for a specific contact.
     */
    fun getContactDetail(lookupKey: String, contactId: Long): Flow<ContactDetail?>

    /**
     * Creates a new contact with the provided information.
     * Returns the created contact ID or a failure result.
     */
    suspend fun createContact(input: ContactInput): Result<Long>

    /**
     * Updates an existing contact, updating changed data rows in-place by _ID,
     * inserting newly added rows, and deleting removed rows.
     */
    suspend fun updateContact(
        contactId: Long,
        original: ContactDetail,
        updated: ContactInput
    ): Result<Unit>

    /**
     * Deletes a contact from the device.
     */
    suspend fun deleteContact(contactId: Long, lookupKey: String): Result<Unit>

    /**
     * All phone numbers per contact, keyed by contact ID. Used for duplicate
     * detection, which needs every number, not just the primary preview.
     */
    suspend fun getAllContactNumbers(): Map<Long, List<String>> = emptyMap()

    /**
     * Merges [mergeContactIds] into [keepContactId]: phone numbers and emails
     * missing from the kept contact are moved into it, then the merged contacts
     * are deleted. The kept contact's name, photo, ringtone, and organization
     * are unchanged. Call only after the user confirmed the merge.
     */
    suspend fun mergeContacts(keepContactId: Long, mergeContactIds: List<Long>): Result<Unit> =
        Result.failure(UnsupportedOperationException("Merging contacts is not supported"))

    /**
     * Updates or inserts only the photo row for a contact in ContactsContract.Data.
     */
    suspend fun saveContactPhoto(contactId: Long, photoBytes: ByteArray): Result<Unit> = Result.success(Unit)

    /**
     * Removes the photo row for a contact from ContactsContract.Data.
     */
    suspend fun removeContactPhoto(contactId: Long): Result<Unit> = Result.success(Unit)

    /**
     * Updates the custom ringtone for a contact in ContactsContract.Contacts.
     */
    suspend fun updateContactRingtone(contactId: Long, ringtoneUri: String?): Result<Unit> = Result.success(Unit)
}

