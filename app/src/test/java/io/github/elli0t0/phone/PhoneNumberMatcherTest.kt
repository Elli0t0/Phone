package io.github.elli0t0.phone

import io.github.elli0t0.phone.core.common.PhoneNumberMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PhoneNumberMatcherTest {

    @Test
    fun normalize_replacesTrunkZeroWithRegionCountryCode() {
        assertEquals("989123456789", PhoneNumberMatcher.normalize("09123456789", "IR"))
        assertEquals("14155551234", PhoneNumberMatcher.normalize("04155551234", "US"))
        assertEquals("442071234567", PhoneNumberMatcher.normalize("02071234567", "GB"))
    }

    @Test
    fun normalize_dropsInternationalDoubleZeroPrefix() {
        assertEquals("989123456789", PhoneNumberMatcher.normalize("00989123456789", "IR"))
        assertEquals("989123456789", PhoneNumberMatcher.normalize("+989123456789", "IR"))
    }

    @Test
    fun matches_handlesCountryCodeAndTrunkDifferences() {
        assertTrue(PhoneNumberMatcher.matches("09123456789", "+989123456789", "IR"))
        assertTrue(PhoneNumberMatcher.matches("09123456789", "989123456789", "IR"))
        assertTrue(PhoneNumberMatcher.matches("9123456789", "+989123456789", "US"))
        assertFalse(PhoneNumberMatcher.matches("09123456789", "+989123456780", "IR"))
    }

    @Test
    fun matches_requiresExactMatchForShortCodes() {
        assertTrue(PhoneNumberMatcher.matches("110", "110", "IR"))
        assertFalse(PhoneNumberMatcher.matches("110", "1100", "IR"))
        assertFalse(PhoneNumberMatcher.matches("123456", "1234567", "IR"))
    }

    @Test
    fun matchesQuery_matchesPartialQueries() {
        assertTrue(PhoneNumberMatcher.matchesQuery("+989123456789", "0912", "IR"))
        assertTrue(PhoneNumberMatcher.matchesQuery("+989123456789", "912345", "IR"))
        assertTrue(PhoneNumberMatcher.matchesQuery("+989123456789", "3456789", "IR"))
        assertTrue(PhoneNumberMatcher.matchesQuery("+98 (912) 345-6789", "0912", "US"))
        assertFalse(PhoneNumberMatcher.matchesQuery("+989123456789", "999999", "IR"))
        assertFalse(PhoneNumberMatcher.matchesQuery("+989123456789", "", "IR"))
        assertFalse(PhoneNumberMatcher.matchesQuery("+989123456789", "abc", "IR"))
    }

    @Test
    fun groupingKey_reducesToLastTenDigits() {
        assertEquals("9123456789", PhoneNumberMatcher.groupingKey("09123456789", "IR"))
        assertEquals("9123456789", PhoneNumberMatcher.groupingKey("+989123456789", "IR"))
        assertEquals("9123456789", PhoneNumberMatcher.groupingKey("9123456789", "IR"))
        assertEquals("1234567", PhoneNumberMatcher.groupingKey("1234567", "IR"))
        assertNull(PhoneNumberMatcher.groupingKey("123456", "IR"))
        assertNull(PhoneNumberMatcher.groupingKey("", "IR"))
    }

    @Test
    fun normalizeName_collapsesWhitespaceAndLowercases() {
        assertEquals("john doe", PhoneNumberMatcher.normalizeName("  John   DOE "))
        assertEquals("ali", PhoneNumberMatcher.normalizeName("ALI"))
    }
}
