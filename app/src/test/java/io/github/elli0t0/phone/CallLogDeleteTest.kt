package io.github.elli0t0.phone

import io.github.elli0t0.phone.data.calllog.deleteCallChunks
import io.github.elli0t0.phone.data.calllog.model.CallLogEntry
import io.github.elli0t0.phone.data.calllog.model.CallType
import io.github.elli0t0.phone.data.calllog.util.CallLogGroupingHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CallLogDeleteTest {

    @Test
    fun deleteChunks_smallList_singleChunk() {
        assertEquals(listOf(listOf(1L, 2L, 3L)), deleteCallChunks(listOf(1L, 2L, 3L)))
    }

    @Test
    fun deleteChunks_overLimit_splitsAt400() {
        val ids = (1L..401L).toList()
        val chunks = deleteCallChunks(ids)
        assertEquals(2, chunks.size)
        assertEquals(400, chunks[0].size)
        assertEquals(listOf(401L), chunks[1])
    }

    @Test
    fun deleteChunks_duplicates_deduplicated() {
        assertEquals(listOf(listOf(1L, 2L)), deleteCallChunks(listOf(1L, 1L, 2L)))
    }

    @Test
    fun pendingDelete_beforeGrouping_groupOfThreeDisappearsFully() {
        val now = System.currentTimeMillis()
        fun call(id: Long) = CallLogEntry(
            id = id,
            number = "1234567",
            formattedNumber = "1234567",
            cachedName = null,
            contactName = null,
            contactId = null,
            contactLookupKey = null,
            contactPhotoUri = null,
            type = CallType.Incoming,
            date = now - id * 1000L,
            durationSeconds = 60L,
            simInfo = null
        )
        val calls = listOf(call(1L), call(2L), call(3L))
        // Sanity: the three consecutive calls form one group.
        val grouped = CallLogGroupingHelper.groupConsecutiveCalls(calls)
        assertEquals(1, grouped.size)
        assertEquals(3, grouped[0].calls.size)

        // Pending deletes apply to the flat list BEFORE grouping, so
        // deleting every call id leaves no group behind (the old
        // after-grouping filter only removed the primary row).
        val pendingDeleteIds = setOf(1L, 2L, 3L)
        val remaining = calls.filter { it.id !in pendingDeleteIds }
        assertTrue(CallLogGroupingHelper.groupConsecutiveCalls(remaining).isEmpty())
    }
}
