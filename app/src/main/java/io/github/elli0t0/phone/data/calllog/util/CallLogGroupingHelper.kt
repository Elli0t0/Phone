package io.github.elli0t0.phone.data.calllog.util

import io.github.elli0t0.phone.core.common.PhoneNumberMatcher
import io.github.elli0t0.phone.data.calllog.model.CallGroup
import io.github.elli0t0.phone.data.calllog.model.CallLogEntry
import io.github.elli0t0.phone.data.calllog.model.CallType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class DaySection(
    val title: String,
    val calls: List<CallGroup>
)

object CallLogGroupingHelper {

    // Shared, thread-safe formatters: building a SimpleDateFormat per group
    // was the hottest allocation in recents grouping.
    private val sameYearFormatter =
        DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
    private val otherYearFormatter =
        DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.getDefault())

    private fun toLocalDate(millis: Long) =
        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

    fun isSameDay(date1: Long, date2: Long): Boolean =
        toLocalDate(date1) == toLocalDate(date2)

    /**
     * Groups consecutive same-number calls on the same day into one CallGroup.
     */
    fun groupConsecutiveCalls(calls: List<CallLogEntry>): List<CallGroup> {
        if (calls.isEmpty()) return emptyList()

        val result = mutableListOf<CallGroup>()
        var currentGroupCalls = mutableListOf<CallLogEntry>()
        // The group number is normalized once: toE164 is a full
        // libphonenumber parse, too expensive to redo per call.
        var currentGroupNormalized = ""

        fun flush() {
            result.add(
                CallGroup(
                    primaryCall = currentGroupCalls.first(),
                    calls = currentGroupCalls.toList()
                )
            )
            currentGroupCalls = mutableListOf()
        }

        for (call in calls) {
            if (currentGroupCalls.isEmpty()) {
                currentGroupCalls.add(call)
                currentGroupNormalized = normalizeForComparison(call.number)
            } else {
                val previousCall = currentGroupCalls.last()
                val isSameNumber =
                    currentGroupNormalized == normalizeForComparison(call.number)
                val isSameCalendarDay = isSameDay(previousCall.date, call.date)

                if (isSameNumber && isSameCalendarDay) {
                    currentGroupCalls.add(call)
                } else {
                    flush()
                    currentGroupCalls.add(call)
                    currentGroupNormalized = normalizeForComparison(call.number)
                }
            }
        }

        if (currentGroupCalls.isNotEmpty()) {
            flush()
        }

        return result
    }

    /**
     * Full-date day header, e.g. "Monday, October 12". The reference UI
     * always shows the full date, never "Today"/"Yesterday".
     */
    fun getDayHeader(dateMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val date = toLocalDate(dateMillis)
        val now = toLocalDate(nowMillis)
        val formatter = if (now.year == date.year) sameYearFormatter else otherYearFormatter
        return date.format(formatter)
    }

    fun organizeIntoDaySections(
        groups: List<CallGroup>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<DaySection> {
        if (groups.isEmpty()) return emptyList()

        val sections = mutableListOf<DaySection>()
        var currentSectionTitle = ""
        var currentSectionGroups = mutableListOf<CallGroup>()

        for (group in groups) {
            val header = getDayHeader(group.date, nowMillis)
            if (header != currentSectionTitle) {
                if (currentSectionGroups.isNotEmpty()) {
                    sections.add(DaySection(title = currentSectionTitle, calls = currentSectionGroups.toList()))
                    currentSectionGroups = mutableListOf()
                }
                currentSectionTitle = header
            }
            currentSectionGroups.add(group)
        }

        if (currentSectionGroups.isNotEmpty()) {
            sections.add(DaySection(title = currentSectionTitle, calls = currentSectionGroups.toList()))
        }

        return sections
    }

    /**
     * Grouping key: the E.164 form, so the same number stored as "0901..."
     * and "+98 901..." groups together. Falls back to raw digits for short
     * numbers that cannot be normalized.
     */
    private fun normalizeForComparison(number: String): String {
        return PhoneNumberMatcher.toE164(number).ifBlank {
            number.filter { it.isDigit() || it == '+' }
        }
    }
}
