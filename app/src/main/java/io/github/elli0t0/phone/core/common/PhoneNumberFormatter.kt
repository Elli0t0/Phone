package io.github.elli0t0.phone.core.common

import android.telephony.PhoneNumberUtils
import java.util.Locale

/**
 * Formats telephone numbers for display purposes without mutating raw input.
 * Uses Android platform's PhoneNumberUtils.
 *
 * For international numbers starting with '+' or country codes, or local numbers
 * like Iranian '0912...', it formats them into standard readable phone chunks.
 * USSD codes, special character sequences ('*#...'), and short service codes
 * are kept unformatted to avoid breaking dial string presentation.
 */
object PhoneNumberFormatter {

    /**
     * One display format for every number: normalize to E.164 first, then
     * format the device-country numbers in national form (trunk 0, except NANP
     * which has no trunk) and all
     * others in international form. The same number therefore renders
     * identically no matter how it was stored ("0901 766 4791" and
     * "+98 901 766 4791" both become "0901 766 4791" on a device set to IR).
     * USSD codes, short codes, emergency numbers and partial input are
     * returned as typed.
     */
    fun format(rawNumber: String, defaultCountryIso: String = Locale.getDefault().country.ifBlank { "US" }): String {
        if (rawNumber.isBlank()) return ""

        // Preserve USSD codes, short codes, or strings starting with * or # without spacing/dashes
        if (rawNumber.startsWith("*") || rawNumber.startsWith("#") || rawNumber.endsWith("#") || rawNumber.contains("*") || rawNumber.contains("#")) {
            return rawNumber
        }

        val e164 = PhoneNumberMatcher.toE164(rawNumber, defaultCountryIso)
        if (e164.isBlank()) return rawNumber

        val code = PhoneNumberMatcher.countryCallingCode(defaultCountryIso)
        val digits = e164.removePrefix("+")
        val display = if (code != null && digits.startsWith(code)) {
            // Device-country number: national form. Most regions use trunk 0;
            // NANP (country code 1) has no trunk prefix.
            val trunk = if (code == "1") "" else "0"
            trunk + digits.removePrefix(code)
        } else {
            // Foreign number: international form.
            e164
        }

        try {
            val formatted = PhoneNumberUtils.formatNumber(display, defaultCountryIso)
            // formatNumber returns the input unchanged when it cannot parse
            // it for the device region. In that case fall through to the
            // chunking fallback so all rows format consistently.
            if (!formatted.isNullOrBlank() && formatted != display) {
                return formatted
            }
        } catch (_: Exception) {
            // JVM unit test fallback when android.jar is mocked or returns default/null
        }

        // Lightweight fallback formatting if PhoneNumberUtils returns null (e.g. unit tests or rapid typing)
        return formatFallback(display)
    }

    private fun formatFallback(number: String): String {
        // National 11-digit numbers (mobiles and landlines): 4-3-4 chunking,
        // e.g. 0912 345 6789, 0133 322 3782.
        if (number.length == 11 && number.startsWith("0") && number.all { it.isDigit() }) {
            return "${number.substring(0, 4)} ${number.substring(4, 7)} ${number.substring(7)}"
        }
        // International formatting fallback: +98 912 345 6789
        if (number.startsWith("+98") && number.length == 13 && number.drop(1).all { it.isDigit() }) {
            return "${number.substring(0, 3)} ${number.substring(3, 6)} ${number.substring(6, 9)} ${number.substring(9)}"
        }
        // US 10-digit: 212-555-1212
        if (number.length == 10 && number.all { it.isDigit() }) {
            return "${number.substring(0, 3)}-${number.substring(3, 6)}-${number.substring(6)}"
        }
        return number
    }

    /**
     * Sanitizes pasted text or imported input by retaining only characters valid on a telephone keypad:
     * digits 0-9, '*', '#', and leading '+'.
     */
    fun sanitize(input: String): String {
        val sb = StringBuilder()
        var hasSeenLeadingPlus = false

        // Strip non-dialpad characters, preserving '+' only if it appears before any digits/dial chars
        for (char in input) {
            if (char == '+' && sb.isEmpty() && !hasSeenLeadingPlus) {
                sb.append(char)
                hasSeenLeadingPlus = true
            } else if (char.isDigit() || char == '*' || char == '#') {
                sb.append(char)
            }
        }
        return sb.toString()
    }
}
