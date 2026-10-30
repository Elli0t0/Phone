package io.github.elli0t0.phone.data.recordings

import io.github.elli0t0.phone.core.common.PhoneNumberMatcher
import io.github.elli0t0.phone.data.calllog.model.CallLogEntry

/**
 * Matches recordings to Recents rows so a row can offer playback.
 * A recording matches a call when its start falls in the call's
 * window: from a few seconds before the log time (clock skew, the
 * tap landing just before the log row) to the call's end plus
 * ringing slack (a call can ring long before it connects, and
 * recording only starts at connect). Each recording is used at
 * most once; among candidates the closest start wins.
 */
object RecordingMatcher {
    private const val EARLY_SLACK_MS = 5_000L
    private const val RINGING_SLACK_MS = 120_000L

    fun match(calls: List<CallLogEntry>, recordings: List<RecordingEntity>): Map<Long, RecordingEntity> {
        val candidates = recordings.filter { !it.isTest }
        if (candidates.isEmpty() || calls.isEmpty()) return emptyMap()
        val used = HashSet<Long>()
        val result = HashMap<Long, RecordingEntity>()
        for (call in calls.sortedBy { it.date }) {
            if (call.durationSeconds <= 0L) continue
            val windowEnd = call.date + call.durationSeconds * 1000L + RINGING_SLACK_MS
            val best = candidates.asSequence()
                .filter { it.id !in used }
                .filter { it.startedAt >= call.date - EARLY_SLACK_MS && it.startedAt <= windowEnd }
                .filter { sameNumber(it.number, call.number) }
                .minByOrNull { kotlin.math.abs(it.startedAt - call.date) } ?: continue
            used.add(best.id)
            result[call.id] = best
        }
        return result
    }

    private fun sameNumber(a: String, b: String): Boolean {
        if (a.isBlank() || b.isBlank()) return a.isBlank() && b.isBlank()
        return PhoneNumberMatcher.matches(a, b)
    }
}
