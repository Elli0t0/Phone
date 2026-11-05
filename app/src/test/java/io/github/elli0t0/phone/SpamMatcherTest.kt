package io.github.elli0t0.phone

import android.telecom.TelecomManager
import io.github.elli0t0.phone.data.spam.SpamDecider
import io.github.elli0t0.phone.data.spam.SpamDecision
import io.github.elli0t0.phone.data.spam.SpamMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpamMatcherTest {

    // Normalization variants: +98 / 0098 / 0 trunk all collapse to one key.
    @Test
    fun normalize_collapsesCountryAndTrunkVariants() {
        assertEquals("989123456789", SpamMatcher.normalizeForStorage("09123456789"))
        assertEquals("989123456789", SpamMatcher.normalizeForStorage("+989123456789"))
        assertEquals("989123456789", SpamMatcher.normalizeForStorage("00989123456789"))
        assertEquals("989123456789", SpamMatcher.normalizeForStorage("989123456789"))
    }

    @Test
    fun normalize_blankOrJunkIsEmpty() {
        assertEquals("", SpamMatcher.normalizeForStorage(""))
        assertEquals("", SpamMatcher.normalizeForStorage("   "))
        assertEquals("", SpamMatcher.normalizeForStorage("abc"))
    }

    @Test
    fun isSpam_matchesAnyStoredVariant() {
        val stored = setOf("989123456789")
        assertTrue(SpamMatcher.isSpam(stored, "09123456789"))
        assertTrue(SpamMatcher.isSpam(stored, "+989123456789"))
        assertTrue(SpamMatcher.isSpam(stored, "00989123456789"))
        assertFalse(SpamMatcher.isSpam(stored, "09123456780"))
        assertFalse(SpamMatcher.isSpam(stored, ""))
    }

    @Test
    fun isSpam_emptyListNeverMatches() {
        assertFalse(SpamMatcher.isSpam(emptySet(), "09123456789"))
    }

    // Prefix match: digits only, trunk-insensitive via the same digits.
    @Test
    fun matchesPrefix_matchesLeadingDigits() {
        val prefixes = setOf("021", "0901")
        assertTrue(SpamMatcher.matchesPrefix(prefixes, "02112345678"))
        assertTrue(SpamMatcher.matchesPrefix(prefixes, "09011234567"))
        // Prefix matching is spelling-sensitive, like the
        // InCallService fallback it shares: the international form
        // does not start with the trunk prefix.
        assertFalse(SpamMatcher.matchesPrefix(prefixes, "+982112345678"))
        assertFalse(SpamMatcher.matchesPrefix(prefixes, "09123456789"))
        assertFalse(SpamMatcher.matchesPrefix(emptySet(), "02112345678"))
        assertFalse(SpamMatcher.matchesPrefix(prefixes, ""))
    }

    // Unknown / hidden handling.
    @Test
    fun isHiddenNumber_blankOrWithheldPresentation() {
        assertTrue(
            SpamMatcher.isHiddenNumber("", TelecomManager.PRESENTATION_ALLOWED)
        )
        assertTrue(
            SpamMatcher.isHiddenNumber("   ", TelecomManager.PRESENTATION_ALLOWED)
        )
        assertTrue(
            SpamMatcher.isHiddenNumber(
                "09123456789",
                TelecomManager.PRESENTATION_RESTRICTED
            )
        )
        assertTrue(
            SpamMatcher.isHiddenNumber(
                "09123456789",
                TelecomManager.PRESENTATION_UNKNOWN
            )
        )
        assertFalse(
            SpamMatcher.isHiddenNumber(
                "09123456789",
                TelecomManager.PRESENTATION_ALLOWED
            )
        )
        assertFalse(
            SpamMatcher.isHiddenNumber(
                "09123456789",
                TelecomManager.PRESENTATION_PAYPHONE
            )
        )
    }

    // Contacts exemption: a contact is never screened, even when the
    // number is also on the spam list or matches a blocked prefix.
    @Test
    fun decider_contactsAreNeverScreened() {
        assertEquals(
            SpamDecision.ALLOW,
            SpamDecider.decide(
                blockKnownSpam = true,
                blockHiddenNumbers = true,
                silenceUnknownCallers = true,
                isSpam = true,
                prefixBlocked = true,
                isHiddenNumber = true,
                hasNumber = true,
                inContacts = true
            )
        )
    }

    @Test
    fun decider_spamListBlocksWhenEnabled() {
        assertEquals(
            SpamDecision.BLOCK,
            SpamDecider.decide(
                blockKnownSpam = true,
                blockHiddenNumbers = false,
                silenceUnknownCallers = false,
                isSpam = true,
                prefixBlocked = false,
                isHiddenNumber = false,
                hasNumber = true,
                inContacts = false
            )
        )
        assertEquals(
            SpamDecision.ALLOW,
            SpamDecider.decide(
                blockKnownSpam = false,
                blockHiddenNumbers = false,
                silenceUnknownCallers = false,
                isSpam = true,
                prefixBlocked = false,
                isHiddenNumber = false,
                hasNumber = true,
                inContacts = false
            )
        )
    }

    @Test
    fun decider_prefixAndHiddenBlock() {
        assertEquals(
            SpamDecision.BLOCK,
            SpamDecider.decide(
                blockKnownSpam = false,
                blockHiddenNumbers = false,
                silenceUnknownCallers = false,
                isSpam = false,
                prefixBlocked = true,
                isHiddenNumber = false,
                hasNumber = true,
                inContacts = false
            )
        )
        assertEquals(
            SpamDecision.BLOCK,
            SpamDecider.decide(
                blockKnownSpam = false,
                blockHiddenNumbers = true,
                silenceUnknownCallers = false,
                isSpam = false,
                prefixBlocked = false,
                isHiddenNumber = true,
                hasNumber = false,
                inContacts = false
            )
        )
        assertEquals(
            SpamDecision.ALLOW,
            SpamDecider.decide(
                blockKnownSpam = false,
                blockHiddenNumbers = false,
                silenceUnknownCallers = false,
                isSpam = false,
                prefixBlocked = false,
                isHiddenNumber = true,
                hasNumber = false,
                inContacts = false
            )
        )
    }

    @Test
    fun decider_silenceUnknownOnlyRingsSilently() {
        assertEquals(
            SpamDecision.SILENCE,
            SpamDecider.decide(
                blockKnownSpam = false,
                blockHiddenNumbers = false,
                silenceUnknownCallers = true,
                isSpam = false,
                prefixBlocked = false,
                isHiddenNumber = false,
                hasNumber = true,
                inContacts = false
            )
        )
        // No number at all: nothing to silence, allow (hidden-number
        // blocking is a separate toggle).
        assertEquals(
            SpamDecision.ALLOW,
            SpamDecider.decide(
                blockKnownSpam = false,
                blockHiddenNumbers = false,
                silenceUnknownCallers = true,
                isSpam = false,
                prefixBlocked = false,
                isHiddenNumber = false,
                hasNumber = false,
                inContacts = false
            )
        )
    }
}
