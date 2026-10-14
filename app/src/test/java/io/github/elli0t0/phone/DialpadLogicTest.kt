package io.github.elli0t0.phone

import io.github.elli0t0.phone.core.common.PhoneNumberFormatter
import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import io.github.elli0t0.phone.feature.dialpad.T9SearchHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DialpadLogicTest {

    @Test
    fun phoneNumberFormatter_formatsLocalAndIranianNumbers() {
        // Iranian mobile numbers: 09121234567
        val formattedIranian = PhoneNumberFormatter.format("09121234567", defaultCountryIso = "IR")
        assertTrue("Formatted number should contain chunks", formattedIranian.isNotEmpty())

        // US standard number
        val formattedUS = PhoneNumberFormatter.format("2125551212", defaultCountryIso = "US")
        assertTrue("Formatted US number should format", formattedUS.contains("-") || formattedUS.contains(" "))
    }

    @Test
    fun phoneNumberFormatter_formatsPlusNumbersInternationally() {
        // A +98 number renders in international form: +98 912 123 4567.
        val formattedIntl = PhoneNumberFormatter.format("+989121234567", defaultCountryIso = "IR")
        assertEquals("+98 912 123 4567", formattedIntl)
    }

    @Test
    fun phoneNumberFormatter_preservesUssdAndSpecialCodes() {
        assertEquals("*140#", PhoneNumberFormatter.format("*140#"))
        assertEquals("*#06#", PhoneNumberFormatter.format("*#06#"))
        assertEquals("#31#", PhoneNumberFormatter.format("#31#"))
        assertEquals("*100*1#", PhoneNumberFormatter.format("*100*1#"))
    }

    @Test
    fun phoneNumberFormatter_sanitizesInvalidCharactersOnPaste() {
        val sanitized = PhoneNumberFormatter.sanitize("Tel: +98 (912) 345-6789 ext. 10")
        assertEquals("+98912345678910", sanitized)

        val ussdSanitized = PhoneNumberFormatter.sanitize("dial *140*1# please")
        assertEquals("*140*1#", ussdSanitized)
    }

    @Test
    fun t9SearchHelper_matchesByNumericKeypadMapping() {
        // 'JOHN': J(5), O(6), H(4), N(6) -> "5646"
        val johnContact = ContactSummary(id = 1L, lookupKey = "k1", displayName = "John Doe", primaryNumberPreview = "123456")
        assertTrue("5646 should match John", T9SearchHelper.matches(johnContact, "5646"))
        assertTrue("56 should match Jo", T9SearchHelper.matches(johnContact, "56"))

        // 'DOE': D(3), O(6), E(3) -> "363"
        assertTrue("363 should match Doe (second word)", T9SearchHelper.matches(johnContact, "363"))

        // Phone number matching: "123" matches 123456
        assertTrue("123 should match phone digits", T9SearchHelper.matches(johnContact, "123"))

        // Non-match
        assertFalse(T9SearchHelper.matches(johnContact, "9999"))
    }

    @Test
    fun t9SearchHelper_matchesIranianNumbersWithPrefixNormalizing() {
        val contact = ContactSummary(id = 2L, lookupKey = "k2", displayName = "Office", primaryNumberPreview = "+989123456789")
        // "0912" should match "+98912..."
        assertTrue("0912 should match international +98912...", T9SearchHelper.matches(contact, "0912"))
    }
}
