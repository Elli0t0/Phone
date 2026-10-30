package io.github.elli0t0.phone

import io.github.elli0t0.phone.core.recording.AutoRecordPolicy
import io.github.elli0t0.phone.data.settings.model.AutoRecordMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoRecordPolicyTest {

    @Test
    fun allMode_recordsEverything() {
        assertTrue(AutoRecordPolicy.shouldAutoRecord(AutoRecordMode.ALL, "09123456789", false, emptySet()))
        assertTrue(AutoRecordPolicy.shouldAutoRecord(AutoRecordMode.ALL, null, false, emptySet()))
        assertTrue(AutoRecordPolicy.shouldAutoRecord(AutoRecordMode.ALL, "", true, setOf("09123456789")))
    }

    @Test
    fun contactsOnly_recordsOnlyContacts() {
        assertTrue(AutoRecordPolicy.shouldAutoRecord(AutoRecordMode.CONTACTS_ONLY, "09123456789", true, emptySet()))
        assertFalse(AutoRecordPolicy.shouldAutoRecord(AutoRecordMode.CONTACTS_ONLY, "09123456789", false, emptySet()))
        assertFalse(AutoRecordPolicy.shouldAutoRecord(AutoRecordMode.CONTACTS_ONLY, null, false, emptySet()))
    }

    @Test
    fun unknownOnly_recordsOnlyUnknown() {
        assertTrue(AutoRecordPolicy.shouldAutoRecord(AutoRecordMode.UNKNOWN_ONLY, "09123456789", false, emptySet()))
        assertFalse(AutoRecordPolicy.shouldAutoRecord(AutoRecordMode.UNKNOWN_ONLY, "09123456789", true, emptySet()))
        // A blank number is not a contact, so it records.
        assertTrue(AutoRecordPolicy.shouldAutoRecord(AutoRecordMode.UNKNOWN_ONLY, null, false, emptySet()))
        assertTrue(AutoRecordPolicy.shouldAutoRecord(AutoRecordMode.UNKNOWN_ONLY, "", false, emptySet()))
    }

    @Test
    fun exceptList_skipsExcluded_matchesDifferentlyWrittenNumbers() {
        assertFalse(
            AutoRecordPolicy.shouldAutoRecord(
                AutoRecordMode.EXCEPT_LIST, "+989123456789", false, setOf("0912 345 6789")
            )
        )
        assertTrue(
            AutoRecordPolicy.shouldAutoRecord(
                AutoRecordMode.EXCEPT_LIST, "+989121111111", false, setOf("0912 345 6789")
            )
        )
        // Blank numbers are never excluded.
        assertTrue(AutoRecordPolicy.shouldAutoRecord(AutoRecordMode.EXCEPT_LIST, null, false, setOf("09123456789")))
        assertTrue(AutoRecordPolicy.shouldAutoRecord(AutoRecordMode.EXCEPT_LIST, "", false, setOf("09123456789")))
    }

    @Test
    fun exceptList_usesInjectedMatcher() {
        val neverMatches: (String, String) -> Boolean = { _, _ -> false }
        assertTrue(
            AutoRecordPolicy.shouldAutoRecord(
                AutoRecordMode.EXCEPT_LIST, "09123456789", false, setOf("09123456789"), neverMatches
            )
        )
    }
}
