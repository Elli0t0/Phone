package io.github.elli0t0.phone.data.calllog.util

import io.github.elli0t0.phone.data.calllog.model.CallGroup
import io.github.elli0t0.phone.data.calllog.model.CallLogEntry
import io.github.elli0t0.phone.data.calllog.model.CallType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DaySection(
    val title: String,
    val calls: List<CallGroup>
)

object CallLogGroupingHelper {

    fun isSameDay(date1: Long, date2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = date1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * Groups consecutive same-number calls on the same day into one CallGroup.
     */
    fun groupConsecutiveCalls(calls: List<CallLogEntry>): List<CallGroup> {
        if (calls.isEmpty()) return emptyList()

        val result = mutableListOf<CallGroup>()
        var currentGroupCalls = mutableListOf<CallLogEntry>()

        for (call in calls) {
            if (currentGroupCalls.isEmpty()) {
                currentGroupCalls.add(call)
            } else {
                val previousCall = currentGroupCalls.last()
                val isSameNumber = normalizeForComparison(previousCall.number) ==
                    normalizeForComparison(call.number)
                val isSameCalendarDay = isSameDay(previousCall.date, call.date)

                if (isSameNumber && isSameCalendarDay) {
                    currentGroupCalls.add(call)
                } else {
                    result.add(
                        CallGroup(
                            primaryCall = currentGroupCalls.first(),
                            calls = currentGroupCalls.toList()
                        )
                    )
                    currentGroupCalls = mutableListOf(call)
                }
            }
        }

        if (currentGroupCalls.isNotEmpty()) {
            result.add(
                CallGroup(
                    primaryCall = currentGroupCalls.first(),
                    calls = currentGroupCalls.toList()
                )
            )
        }

        return result
    }

    fun getDayHeader(dateMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val dateCal = Calendar.getInstance().apply { timeInMillis = dateMillis }

        val isToday = nowCal.get(Calendar.YEAR) == dateCal.get(Calendar.YEAR) &&
            nowCal.get(Calendar.DAY_OF_YEAR) == dateCal.get(Calendar.DAY_OF_YEAR)

        if (isToday) return "Today"

        val yesterdayCal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val isYesterday = yesterdayCal.get(Calendar.YEAR) == dateCal.get(Calendar.YEAR) &&
            yesterdayCal.get(Calendar.DAY_OF_YEAR) == dateCal.get(Calendar.DAY_OF_YEAR)

        if (isYesterday) return "Yesterday"

        val format = if (nowCal.get(Calendar.YEAR) == dateCal.get(Calendar.YEAR)) {
            SimpleDateFormat("MMMM d", Locale.getDefault())
        } else {
            SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        }
        return format.format(Date(dateMillis))
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

    private fun normalizeForComparison(number: String): String {
        return number.filter { it.isDigit() || it == '+' }
    }
}
