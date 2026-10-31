package io.github.elli0t0.phone

import io.github.elli0t0.phone.data.contacts.model.ContactDetail
import io.github.elli0t0.phone.data.contacts.model.ContactEmail
import io.github.elli0t0.phone.data.contacts.model.ContactPhone
import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import io.github.elli0t0.phone.data.contacts.model.DuplicateGroup
import io.github.elli0t0.phone.data.contacts.model.DuplicateReason
import io.github.elli0t0.phone.data.contacts.util.ContactMergeUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactMergeUtilTest {

    private fun summary(id: Long, name: String = "Name$id") =
        ContactSummary(id = id, lookupKey = "k$id", displayName = name)

    @Test
    fun duplicateSetHash_ignoresGroupAndContactOrder() {
        val a = listOf(
            DuplicateGroup(DuplicateReason.SAME_NUMBER, listOf(summary(2), summary(1))),
            DuplicateGroup(DuplicateReason.SAME_NAME, listOf(summary(4), summary(3)))
        )
        val b = listOf(
            DuplicateGroup(DuplicateReason.SAME_NAME, listOf(summary(3), summary(4))),
            DuplicateGroup(DuplicateReason.SAME_NUMBER, listOf(summary(1), summary(2)))
        )
        assertEquals(ContactMergeUtil.duplicateSetHash(a), ContactMergeUtil.duplicateSetHash(b))
    }

    @Test
    fun duplicateSetHash_differsWhenSetChanges() {
        val base = listOf(DuplicateGroup(DuplicateReason.SAME_NUMBER, listOf(summary(1), summary(2))))
        val changed = listOf(DuplicateGroup(DuplicateReason.SAME_NUMBER, listOf(summary(1), summary(3))))
        assertNotEquals(ContactMergeUtil.duplicateSetHash(base), ContactMergeUtil.duplicateSetHash(changed))
        assertNotEquals(ContactMergeUtil.duplicateSetHash(base), ContactMergeUtil.duplicateSetHash(emptyList()))
        assertEquals(ContactMergeUtil.duplicateSetHash(emptyList()), ContactMergeUtil.duplicateSetHash(emptyList()))
    }

    @Test
    fun aggregationPairs_dedupesAndDropsKeep() {
        assertEquals(
            listOf(10L to 20L, 10L to 30L),
            ContactMergeUtil.aggregationPairs(10L, listOf(20L, 30L, 20L, 10L))
        )
        assertEquals(emptyList<Pair<Long, Long>>(), ContactMergeUtil.aggregationPairs(10L, listOf(10L)))
    }

    @Test
    fun mergedFallbackInput_keepsOwnFieldsAndFillsBlanks() {
        val kept = ContactDetail(
            id = 1L,
            lookupKey = "k1",
            displayName = "Alice",
            phones = listOf(ContactPhone(id = 5L, number = "09123456789")),
            emails = listOf(ContactEmail(id = 7L, address = "Alice@Example.com")),
            organization = null,
            notes = "kept note",
            birthday = null,
            address = null
        )
        val target = ContactDetail(
            id = 2L,
            lookupKey = "k2",
            displayName = "Alice Work",
            phones = listOf(
                ContactPhone(id = 9L, number = "+989123456789"),
                ContactPhone(id = 10L, number = "09111111111")
            ),
            emails = listOf(ContactEmail(id = 11L, address = "alice@example.com")),
            organization = "Acme",
            notes = "target note",
            birthday = "1990-01-01",
            address = "Tehran"
        )
        val input = ContactMergeUtil.mergedFallbackInput(kept, listOf(target))
        // Same number in another format and same email in another case: not duplicated.
        assertEquals(listOf("09123456789", "09111111111"), input.phones.map { it.number })
        assertEquals(listOf("Alice@Example.com"), input.emails.map { it.address })
        // The kept contact's own note survives (the old bug wiped it);
        // blank fields fill from the target.
        assertEquals("kept note", input.notes)
        assertEquals("Acme", input.organization)
        assertEquals("1990-01-01", input.birthday)
        assertEquals("Tehran", input.address)
        // Newly added rows carry id 0 so the differential update inserts them.
        assertTrue(input.phones.last().id == 0L)
    }

    @Test
    fun mergedFallbackInput_keptFieldsWinOverTargets() {
        val kept = ContactDetail(
            id = 1L, lookupKey = "k1", displayName = "Alice",
            notes = "kept", birthday = "1980-05-05", address = "Kept St"
        )
        val target = ContactDetail(
            id = 2L, lookupKey = "k2", displayName = "Alice Work",
            notes = "target", birthday = "1990-01-01", address = "Target St"
        )
        val input = ContactMergeUtil.mergedFallbackInput(kept, listOf(target))
        assertEquals("kept", input.notes)
        assertEquals("1980-05-05", input.birthday)
        assertEquals("Kept St", input.address)
    }
}
