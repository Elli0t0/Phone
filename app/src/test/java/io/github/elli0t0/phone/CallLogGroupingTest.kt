package io.github.elli0t0.phone

import io.github.elli0t0.phone.data.calllog.model.CallLogEntry
import io.github.elli0t0.phone.data.calllog.model.CallType
import io.github.elli0t0.phone.data.calllog.util.CallLogGroupingHelper
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class CallLogGroupingTest {

    private fun createCall(
        id: Long,
        number: String,
        date: Long,
        type: CallType = CallType.Incoming
    ) = CallLogEntry(
        id = id,
        number = number,
        formattedNumber = number,
        cachedName = null,
        contactName = null,
        contactId = null,
        contactLookupKey = null,
        contactPhotoUri = null,
        type = type,
        date = date,
        durationSeconds = 60L,
        simInfo = null
    )

    @Test
    fun grouping_consecutiveSameNumberCallsOnSameDay_groupedWithCount() {
        val now = System.currentTimeMillis()
        val call1 = createCall(1L, "1234567", now)
        val call2 = createCall(2L, "1234567", now - 1000L)
        val call3 = createCall(3L, "9999999", now - 2000L)
        val call4 = createCall(4L, "1234567", now - 3000L)

        val groups = CallLogGroupingHelper.groupConsecutiveCalls(listOf(call1, call2, call3, call4))

        assertEquals(3, groups.size)
        // Group 1: call1 & call2
        assertEquals(1L, groups[0].id)
        assertEquals(2, groups[0].count)
        assertEquals("1234567", groups[0].number)

        // Group 2: call3
        assertEquals(3L, groups[1].id)
        assertEquals(1, groups[1].count)
        assertEquals("9999999", groups[1].number)

        // Group 3: call4 (not consecutive with call1/call2 because call3 was in between)
        assertEquals(4L, groups[2].id)
        assertEquals(1, groups[2].count)
        assertEquals("1234567", groups[2].number)
    }

    @Test
    fun grouping_sameNumberDifferentDays_notGroupedTogether() {
        val today = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 12, 14, 0, 0)
        }.timeInMillis

        val yesterday = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 11, 14, 0, 0)
        }.timeInMillis

        val callToday = createCall(1L, "1234567", today)
        val callYesterday = createCall(2L, "1234567", yesterday)

        val groups = CallLogGroupingHelper.groupConsecutiveCalls(listOf(callToday, callYesterday))

        assertEquals(2, groups.size)
        assertEquals(1, groups[0].count)
        assertEquals(1, groups[1].count)
    }

    @Test
    fun dayHeaders_useFullDateForAllDays() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 12, 14, 0, 0)
        }
        val now = nowCal.timeInMillis
        val format = java.text.SimpleDateFormat("EEEE, MMMM d", java.util.Locale.getDefault())

        assertEquals(format.format(java.util.Date(now)), CallLogGroupingHelper.getDayHeader(now, now))

        val yesterdayCal = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -1)
        }
        assertEquals(
            format.format(java.util.Date(yesterdayCal.timeInMillis)),
            CallLogGroupingHelper.getDayHeader(yesterdayCal.timeInMillis, now)
        )

        val olderCal = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -5)
        }
        assertEquals(
            format.format(java.util.Date(olderCal.timeInMillis)),
            CallLogGroupingHelper.getDayHeader(olderCal.timeInMillis, now)
        )
    }

    @Test
    fun daySections_organizesCallGroupsIntoSections() {
        val now = System.currentTimeMillis()
        val yesterday = now - 86400000L

        val group1 = CallLogGroupingHelper.groupConsecutiveCalls(listOf(createCall(1L, "111", now)))
        val group2 = CallLogGroupingHelper.groupConsecutiveCalls(listOf(createCall(2L, "222", yesterday)))

        val sections = CallLogGroupingHelper.organizeIntoDaySections(group1 + group2, now)

        val format = java.text.SimpleDateFormat("EEEE, MMMM d", java.util.Locale.getDefault())
        assertEquals(2, sections.size)
        assertEquals(format.format(java.util.Date(now)), sections[0].title)
        assertEquals(1, sections[0].calls.size)
        assertEquals(format.format(java.util.Date(yesterday)), sections[1].title)
        assertEquals(1, sections[1].calls.size)
    }
}
