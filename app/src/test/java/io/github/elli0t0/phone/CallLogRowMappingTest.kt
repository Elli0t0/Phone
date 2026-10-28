package io.github.elli0t0.phone

import android.provider.CallLog
import io.github.elli0t0.phone.data.calllog.mapCallType
import io.github.elli0t0.phone.data.calllog.model.CallType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The row-mapping file was extracted from AndroidCallLogRepository
 * in 0.11.49 without behavior change: every provider type code must
 * still map to the same CallType. (CallLog type codes are compile-time
 * int constants, so this runs on plain JVM tests.)
 */
class CallLogRowMappingTest {

    @Test
    fun allTypeCodesMap() {
        assertEquals(CallType.Incoming, mapCallType(CallLog.Calls.INCOMING_TYPE))
        assertEquals(CallType.Outgoing, mapCallType(CallLog.Calls.OUTGOING_TYPE))
        assertEquals(CallType.Missed, mapCallType(CallLog.Calls.MISSED_TYPE))
        assertEquals(CallType.Voicemail, mapCallType(CallLog.Calls.VOICEMAIL_TYPE))
        assertEquals(CallType.Rejected, mapCallType(CallLog.Calls.REJECTED_TYPE))
        assertEquals(CallType.Blocked, mapCallType(CallLog.Calls.BLOCKED_TYPE))
    }

    @Test
    fun unknownTypeCodeMapsToUnknown() {
        assertEquals(CallType.Unknown, mapCallType(0))
        assertEquals(CallType.Unknown, mapCallType(99))
    }
}
