package io.github.elli0t0.phone

import io.github.elli0t0.phone.data.calllog.model.CallLogEntry
import io.github.elli0t0.phone.data.calllog.model.CallType
import io.github.elli0t0.phone.data.calllog.reResolveEntries
import io.github.elli0t0.phone.data.contacts.ContactNameIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * A rename or merge must refresh cached and live call log entries:
 * name, id, lookup key and photo come from the contact index, and an
 * unmatched number falls back to its cached name with no contact link.
 */
class ReResolveEntriesTest {

    private fun entry(
        id: Long = 1L,
        number: String = "09123456789",
        cachedName: String? = "Old Name",
        contactName: String? = "Old Name",
        contactId: Long? = 7L,
        contactLookupKey: String? = "old-key",
        contactPhotoUri: String? = "content://old/photo"
    ) = CallLogEntry(
        id = id,
        number = number,
        formattedNumber = number,
        cachedName = cachedName,
        contactName = contactName,
        contactId = contactId,
        contactLookupKey = contactLookupKey,
        contactPhotoUri = contactPhotoUri,
        type = CallType.Incoming,
        date = 1_000L,
        durationSeconds = 60L,
        simInfo = null
    )

    @Test
    fun renamedContact_updatesNameIdLookupKeyAndPhoto() {
        val resolver: (String) -> ContactNameIndex.Entry? = {
            ContactNameIndex.Entry(
                displayName = "New Name",
                contactId = 42L,
                lookupKey = "new-key",
                photoUri = "content://new/photo"
            )
        }
        val updated = reResolveEntries(listOf(entry()), resolver)

        assertEquals("New Name", updated[0].contactName)
        assertEquals(42L, updated[0].contactId)
        assertEquals("new-key", updated[0].contactLookupKey)
        assertEquals("content://new/photo", updated[0].contactPhotoUri)
        // The entry itself is unchanged apart from the contact fields.
        assertEquals(1L, updated[0].id)
        assertEquals("09123456789", updated[0].number)
    }

    @Test
    fun unmatchedNumber_fallsBackToCachedNameWithoutContactLink() {
        val updated = reResolveEntries(listOf(entry())) { null }

        assertEquals("Old Name", updated[0].contactName)
        assertNull(updated[0].contactId)
        assertNull(updated[0].contactLookupKey)
        assertNull(updated[0].contactPhotoUri)
    }

    @Test
    fun unmatchedNumberWithoutCachedName_hasNoName() {
        val updated = reResolveEntries(
            listOf(entry(cachedName = null, contactName = "Stale Name"))
        ) { null }

        assertNull(updated[0].contactName)
        assertNull(updated[0].contactId)
    }

    @Test
    fun blankNumber_isNotResolved() {
        var resolved = false
        val updated = reResolveEntries(listOf(entry(number = ""))) {
            resolved = true
            null
        }

        assertEquals(false, resolved)
        assertEquals("Old Name", updated[0].contactName)
    }

    @Test
    fun mixedList_resolvesEachEntryIndependently() {
        val resolver: (String) -> ContactNameIndex.Entry? = { number ->
            if (number == "09123456789") {
                ContactNameIndex.Entry("Renamed", 9L, "k9", null)
            } else {
                null
            }
        }
        val updated = reResolveEntries(
            listOf(
                entry(id = 1L, number = "09123456789"),
                entry(id = 2L, number = "09998887766", cachedName = "Kept", contactName = "Kept")
            ),
            resolver
        )

        assertEquals("Renamed", updated[0].contactName)
        assertEquals(9L, updated[0].contactId)
        assertEquals("Kept", updated[1].contactName)
        assertNull(updated[1].contactId)
    }
}
