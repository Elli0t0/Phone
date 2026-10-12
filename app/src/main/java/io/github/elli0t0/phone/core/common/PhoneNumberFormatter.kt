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

    fun format(rawNumber: String, defaultCountryIso: String = Locale.getDefault().country.ifBlank { "US" }): String {
        if (rawNumber.isBlank()) return ""

        // Preserve USSD codes, short codes, or strings starting with * or # without spacing/dashes
        if (rawNumber.startsWith("*") || rawNumber.startsWith("#") || rawNumber.endsWith("#") || rawNumber.contains("*") || rawNumber.contains("#")) {
            return rawNumber
        }

        try {
            val formatted = PhoneNumberUtils.formatNumber(rawNumber, defaultCountryIso)
            if (!formatted.isNullOrBlank()) {
                return formatted
            }
        } catch (_: Exception) {
            // JVM unit test fallback when android.jar is mocked or returns default/null
        }

        // Lightweight fallback formatting if PhoneNumberUtils returns null (e.g. unit tests or rapid typing)
        return formatFallback(rawNumber)
    }

    private fun formatFallback(number: String): String {
        // Iranian mobile formatting: 0912 345 6789 (11 digits starting with 09)
        if (number.startsWith("09") && number.length == 11 && number.all { it.isDigit() }) {
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
