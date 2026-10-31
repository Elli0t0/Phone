package io.github.elli0t0.phone.data.contacts

import io.github.elli0t0.phone.data.contacts.model.ContactDetail
import io.github.elli0t0.phone.data.contacts.model.ContactGroup
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
     * The device owner's own profile contact from ContactsContract.Profile,
     * or null when the device has no profile contact. Used for the
     * "My profile" row in the contacts list.
     */
    suspend fun getProfileContact(): ContactSummary? = null

    /**
     * All non-deleted contact groups from ContactsContract.Groups, each with
     * its member count. Used by the Groups screen.
     */
    suspend fun getContactGroups(): List<ContactGroup> = emptyList()

    /**
     * Members of one group, as contact summaries sorted by display name.
     */
    suspend fun getGroupMembers(groupId: Long): List<ContactSummary> = emptyList()

    /**
     * All phone numbers per contact, keyed by contact ID. Used for duplicate
     * detection, which needs every number, not just the primary preview.
     */
    suspend fun getAllContactNumbers(): Map<Long, List<String>> = emptyMap()

    /**
     * Merges [mergeContactIds] into [keepContactId] by linking their raw
     * contacts (AggregationExceptions TYPE_KEEP_TOGETHER in one batch):
     * nothing is deleted, so notes, birthday, address, photo, and
     * ringtone all survive, and the kept contact's name is marked
     * primary. Only when linking is rejected (read-only accounts) does
     * it fall back to copying every field into the kept contact and
     * deleting the others; that path reports linked = false because
     * it cannot be undone. Call only after the user confirmed the merge.
     */
    suspend fun mergeContacts(keepContactId: Long, mergeContactIds: List<Long>): Result<ContactMergeOutcome> =
        Result.failure(UnsupportedOperationException("Merging contacts is not supported"))

    /**
     * Reverses a linked merge: the same AggregationExceptions batch
     * with TYPE_KEEP_SEPARATE. Only meaningful for outcomes with
     * linked = true.
     */
    suspend fun unlinkContacts(outcome: ContactMergeOutcome): Result<Unit> =
        Result.failure(UnsupportedOperationException("Unmerging contacts is not supported"))

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

