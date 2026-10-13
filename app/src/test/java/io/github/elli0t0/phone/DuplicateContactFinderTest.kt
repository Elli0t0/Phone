package io.github.elli0t0.phone

import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import io.github.elli0t0.phone.data.contacts.model.DuplicateReason
import io.github.elli0t0.phone.data.contacts.util.DuplicateContactFinder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateContactFinderTest {

    private fun summary(id: Long, name: String, number: String? = null) =
        ContactSummary(id = id, lookupKey = "k$id", displayName = name, primaryNumberPreview = number)

    @Test
    fun findDuplicates_groupsBySameNormalizedNumber() {
        val contacts = listOf(
            summary(1, "Alice", "09123456789"),
            summary(2, "Alice Work", "+989123456789"),
            summary(3, "Bob", "09111111111")
        )
        val groups = DuplicateContactFinder.findDuplicates(
            contacts,
            mapOf(1L to listOf("09123456789"), 2L to listOf("+989123456789"), 3L to listOf("09111111111")),
            "IR"
        )
        assertEquals(1, groups.size)
        assertEquals(DuplicateReason.SAME_NUMBER, groups[0].reason)
        assertEquals(listOf(1L, 2L), groups[0].contacts.map { it.id })
    }

    @Test
    fun findDuplicates_groupsBySameNormalizedName() {
        val contacts = listOf(
            summary(1, "Alice Smith", "09123456789"),
            summary(2, "alice  smith", "09222222222"),
            summary(3, "Bob", "09333333333")
        )
        val groups = DuplicateContactFinder.findDuplicates(
            contacts,
            mapOf(1L to listOf("09123456789"), 2L to listOf("09222222222"), 3L to listOf("09333333333")),
            "IR"
        )
        assertEquals(1, groups.size)
        assertEquals(DuplicateReason.SAME_NAME, groups[0].reason)
        assertEquals(listOf(1L, 2L), groups[0].contacts.map { it.id })
    }

    @Test
    fun findDuplicates_reportsNumberAndNameOverlapOnce() {
        val contacts = listOf(
            summary(1, "Alice", "09123456789"),
            summary(2, "alice", "+989123456789")
        )
        val groups = DuplicateContactFinder.findDuplicates(
            contacts,
            mapOf(1L to listOf("09123456789"), 2L to listOf("+989123456789")),
            "IR"
        )
        assertEquals(1, groups.size)
        assertEquals(DuplicateReason.SAME_NUMBER, groups[0].reason)
    }

    @Test
    fun findDuplicates_ignoresNumberOnlyNamesAndShortNumbers() {
        val contacts = listOf(
            summary(1, "09123456789", "09123456789"),
            summary(2, "09123456789", "09123456789"),
            summary(3, "A", "123456"),
            summary(4, "A", "123456")
        )
        val groups = DuplicateContactFinder.findDuplicates(
            contacts,
            mapOf(
                1L to listOf("09123456789"),
                2L to listOf("09123456789"),
                3L to listOf("123456"),
                4L to listOf("123456")
            ),
            "IR"
        )
        // Only the shared real number groups; the number-as-name and the
        // too-short numbers do not create name groups.
        assertEquals(1, groups.size)
        assertEquals(DuplicateReason.SAME_NUMBER, groups[0].reason)
        assertEquals(listOf(1L, 2L), groups[0].contacts.map { it.id })
    }

    @Test
    fun findDuplicates_returnsEmptyWhenNoDuplicates() {
        val contacts = listOf(
            summary(1, "Alice", "09123456789"),
            summary(2, "Bob", "09111111111")
        )
        val groups = DuplicateContactFinder.findDuplicates(
            contacts,
            mapOf(1L to listOf("09123456789"), 2L to listOf("09111111111")),
            "IR"
        )
        assertTrue(groups.isEmpty())
    }
}
