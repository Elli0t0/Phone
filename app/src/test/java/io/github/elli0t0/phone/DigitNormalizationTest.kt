package io.github.elli0t0.phone

import io.github.elli0t0.phone.core.common.PhoneNumberFormatter
import io.github.elli0t0.phone.core.common.PhoneNumberMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Persian (۰-۹) and Eastern Arabic (٠-٩) digits must become ASCII 0-9
 * before sanitizing, matching, formatting, or dialing: sanitize() keeps
 * them (Char.isDigit is Unicode-aware), so without normalization they
 * survive into matchers and tel: URIs that basebands reject.
 */
class DigitNormalizationTest {

    @Test
    fun persianDigitsBecomeAscii() {
        assertEquals("09123588236", PhoneNumberFormatter.normalizeDigits("۰۹۱۲۳۵۸۸۲۳۶"))
    }

    @Test
    fun easternArabicDigitsBecomeAscii() {
        assertEquals("0123456789", PhoneNumberFormatter.normalizeDigits("٠١٢٣٤٥٦٧٨٩"))
    }

    @Test
    fun mixedInputKeepsPlusSpacesAndUssdChars() {
        assertEquals("+98 912 358 8236", PhoneNumberFormatter.normalizeDigits("+۹۸ ۹۱۲ ۳۵۸ ۸۲۳۶"))
        assertEquals("*140#", PhoneNumberFormatter.normalizeDigits("*۱۴۰#"))
    }

    @Test
    fun asciiInputPassesThroughUnchanged() {
        assertEquals("0912 358 8236", PhoneNumberFormatter.normalizeDigits("0912 358 8236"))
        assertEquals("", PhoneNumberFormatter.normalizeDigits(""))
    }

    @Test
    fun sanitizeNormalizesPersianDigits() {
        assertEquals("0912358", PhoneNumberFormatter.sanitize("۰۹۱۲ ۳۵۸"))
        assertEquals("+989123588236", PhoneNumberFormatter.sanitize("+۹۸۹۱۲۳۵۸۸۲۳۶"))
    }

    @Test
    fun matcherTreatsPersianAndAsciiAsSameNumber() {
        assertTrue(PhoneNumberMatcher.matches("۰۹۱۲۳۵۸۸۲۳۶", "09123588236", "IR"))
        assertTrue(PhoneNumberMatcher.matchesQuery("09123588236", "۳۵۸", "IR"))
    }

    @Test
    fun formatNormalizesPersianInput() {
        assertEquals("0912 358 8236", PhoneNumberFormatter.format("۰۹۱۲۳۵۸۸۲۳۶", "IR"))
    }
}
