package io.github.elli0t0.phone

import io.github.elli0t0.phone.data.calllog.model.CallLogEntry
import io.github.elli0t0.phone.data.calllog.model.CallType
import io.github.elli0t0.phone.data.recordings.RecordingEntity
import io.github.elli0t0.phone.data.recordings.RecordingMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordingMatcherTest {

    private fun call(
        id: Long,
        number: String,
        date: Long,
        durationSeconds: Long
    ) = CallLogEntry(
        id = id,
        number = number,
        formattedNumber = number,
        cachedName = null,
        contactName = null,
        contactId = null,
        contactLookupKey = null,
        contactPhotoUri = null,
        type = CallType.Incoming,
        date = date,
        durationSeconds = durationSeconds,
        simInfo = null
    )

    private fun recording(
        id: Long,
        number: String,
        startedAt: Long,
        isTest: Boolean = false
    ) = RecordingEntity(
        id = id,
        uri = "content://media/recording_$id",
        number = number,
        displayName = null,
        startedAt = startedAt,
        durationMs = 60_000L,
        sizeBytes = 1000L,
        isTest = isTest,
        isSilent = false
    )

    @Test
    fun match_recordingStartedAfterCallWithDifferentNumberWriting() {
        val date = 1_700_000_000_000L
        val result = RecordingMatcher.match(
            listOf(call(1, "+989123456789", date, 60)),
            listOf(recording(10, "09123456789", date + 8_000))
        )
        assertEquals(recording(10, "09123456789", date + 8_000), result[1])
    }

    @Test
    fun match_rejectsOtherNumbersTestRecordingsAndZeroDurationCalls() {
        val date = 1_700_000_000_000L
        val result = RecordingMatcher.match(
            listOf(
                call(1, "+989123456789", date, 60),
                call(2, "+989123456789", date, 0)
            ),
            listOf(
                recording(10, "+989123456780", date + 8_000),
                recording(11, "09123456789", date + 8_000, isTest = true)
            )
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun match_neverReusesOneRecordingForTwoCalls() {
        val date = 1_700_000_000_000L
        val result = RecordingMatcher.match(
            listOf(
                call(1, "+989123456789", date, 120),
                call(2, "+989123456789", date + 10_000, 120)
            ),
            listOf(recording(10, "09123456789", date + 8_000))
        )
        assertEquals(10L, result[1]?.id)
        assertNull(result[2])
    }
}
