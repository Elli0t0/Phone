package io.github.elli0t0.phone

import io.github.elli0t0.phone.data.calllog.AndroidCallLogRepository
import io.github.elli0t0.phone.data.calllog.SimBadge
import io.github.elli0t0.phone.data.calllog.resolveSimBadge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * SIM badges resolve through the active accounts list, not by slicing
 * the account id: on devices where phoneAccountId is a 20-digit ICCID,
 * the old "SIM ${take(2)}" produced broken labels like "SIM 89".
 * The API 29 query contract is pinned too: no LIMIT in sortOrder
 * (the provider rejects it) and the 500-row cap enforced in the loop.
 */
class SimBadgeAndCapTest {

    private val sims = listOf(
        SimBadge("89010000000000000001", "Irancell"),
        SimBadge("89020000000000000002", "SIM 2")
    )

    @Test
    fun nullOrBlankAccountHasNoBadge() {
        assertNull(resolveSimBadge(null, sims))
        assertNull(resolveSimBadge("", sims))
        assertNull(resolveSimBadge("   ", sims))
    }

    @Test
    fun matchingIccidReturnsItsLabel() {
        assertEquals("Irancell", resolveSimBadge("89010000000000000001", sims))
        assertEquals("SIM 2", resolveSimBadge("89020000000000000002", sims))
    }

    @Test
    fun unknownAccountHasNoBadgeRatherThanAWrongOne() {
        assertNull(resolveSimBadge("89090000000000000009", sims))
        assertNull(resolveSimBadge("89010000000000000001", listOf(SimBadge(null, "SIM 1"))))
    }

    @Test
    fun legacySortOrderHasNoLimitClause() {
        val order = AndroidCallLogRepository.legacyCallLogSortOrder()
        assertFalse(order.contains("LIMIT"))
        assertEquals("date DESC", order)
    }

    @Test
    fun streamingCapIs500() {
        assertEquals(500, AndroidCallLogRepository.STREAMING_LIMIT)
    }
}
