package io.github.elli0t0.phone

import io.github.elli0t0.phone.core.common.PhoneNumberFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Number formatting on paths that do not depend on the Android
 * framework: Iranian national/international grouping, progressive
 * (partial) grouping, USSD passthrough, and [PhoneNumberFormatter.sanitize].
 *
 * Non-Iranian full formatting goes through PhoneNumberUtils, which is
 * stubbed on JVM tests, so it is covered only loosely by
 * DialpadLogicTest and not asserted exactly here.
 */
class PhoneNumberFormatterTest {

    @Test
    fun sanitizeKeepsOnlyKeypadCharacters() {
        assertEquals("+989121234567", PhoneNumberFormatter.sanitize("+98 912-123 4567"))
        assertEquals("09121234567", PhoneNumberFormatter.sanitize("0912 123 4567"))
        assertEquals("*140#", PhoneNumberFormatter.sanitize("*140#"))
        // '+' survives only in leading position.
        assertEquals("98912", PhoneNumberFormatter.sanitize("98+912"))
        assertEquals("", PhoneNumberFormatter.sanitize("abc"))
        assertEquals("", PhoneNumberFormatter.sanitize(""))
    }

    @Test
    fun formatIranianNationalMobileAndLandline() {
        assertEquals(
            "0912 358 8236",
            PhoneNumberFormatter.format("09123588236", defaultCountryIso = "IR")
        )
        assertEquals(
            "021 9120 0150",
            PhoneNumberFormatter.format("02191200150", defaultCountryIso = "IR")
        )
    }

    @Test
    fun formatIranianInternationalMobileAndLandline() {
        assertEquals(
            "+98 912 358 8236",
            PhoneNumberFormatter.format("+989123588236", defaultCountryIso = "IR")
        )
        assertEquals(
            "+98 21 9120 0150",
            PhoneNumberFormatter.format("+982191200150", defaultCountryIso = "IR")
        )
        // 00-prefixed input renders international too.
        assertEquals(
            "+98 912 358 8236",
            PhoneNumberFormatter.format("00989123588236", defaultCountryIso = "IR")
        )
    }

    @Test
    fun formatPartialGroupsIranianNumbersProgressively() {
        assertEquals(
            "0912 358 82",
            PhoneNumberFormatter.formatPartial("091235882", defaultCountryIso = "IR")
        )
        assertEquals(
            "021 9120 0",
            PhoneNumberFormatter.formatPartial("02191200", defaultCountryIso = "IR")
        )
        assertEquals(
            "+98 912 358 82",
            PhoneNumberFormatter.formatPartial("+9891235882", defaultCountryIso = "IR")
        )
    }

    @Test
    fun formatReturnsUssdAndBlankInputsUnchanged() {
        assertEquals("*140#", PhoneNumberFormatter.format("*140#", defaultCountryIso = "IR"))
        assertEquals("*#06#", PhoneNumberFormatter.format("*#06#", defaultCountryIso = "IR"))
        assertEquals("", PhoneNumberFormatter.format("", defaultCountryIso = "IR"))
        assertEquals("", PhoneNumberFormatter.format("   ", defaultCountryIso = "IR"))
    }
}
