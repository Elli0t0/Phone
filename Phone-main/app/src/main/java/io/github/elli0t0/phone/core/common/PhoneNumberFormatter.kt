package io.github.elli0t0.phone.core.common

import android.telephony.PhoneNumberUtils

/**
 * One display format for every number. Region = SIM/network country
 * (fallback "IR"), never the device locale. The input's form decides the
 * output form: "+..."/"00..." renders international, trunk-0/bare renders
 * national. USSD codes and short codes are returned as typed.
 */
object PhoneNumberFormatter {

    fun format(rawNumber: String, defaultCountryIso: String = PhoneNumberMatcher.deviceRegionIso()): String {
        if (rawNumber.isBlank()) return ""
        if (rawNumber.contains('*') || rawNumber.contains('#')) return rawNumber
        val trimmed = rawNumber.trim()
        val digits = trimmed.filter { it.isDigit() }
        if (digits.isEmpty()) return rawNumber
        val region = defaultCountryIso.uppercase()
        val international = trimmed.startsWith("+") || trimmed.startsWith("00")
        val e164 = when {
            trimmed.startsWith("+") -> "+$digits"
            trimmed.startsWith("00") && digits.length > 2 -> "+" + digits.substring(2)
            else -> PhoneNumberMatcher.toE164(trimmed, region)
        }
        if (e164.isBlank()) return rawNumber
        val ownCountry = PhoneNumberMatcher.countryIsoForE164(e164) ?: region
        if (ownCountry == "IR") {
            return if (international) formatIranianInternational(e164) else formatIranianNational(e164)
        }
        val code = PhoneNumberMatcher.countryCallingCode(ownCountry)
        val display = if (!international && ownCountry == region && code != null &&
            e164.removePrefix("+").startsWith(code)
        ) {
            val trunk = if (code == "1") "" else "0"
            trunk + e164.removePrefix("+").removePrefix(code)
        } else {
            e164
        }
        return try {
            PhoneNumberUtils.formatNumber(display, ownCountry)?.takeIf { it.isNotBlank() } ?: display
        } catch (_: Exception) {
            display
        }
    }

    /** National IR form: mobile 0912 358 8236 (4-3-4), landline 021 9120 0150 (3-4-4). */
    private fun formatIranianNational(e164: String): String {
        val national = "0" + e164.removePrefix("+").removePrefix("98")
        if (national.length != 11 || !national.all { it.isDigit() }) return national
        return if (national.startsWith("09")) {
            "${national.substring(0, 4)} ${national.substring(4, 7)} ${national.substring(7)}"
        } else {
            "${national.substring(0, 3)} ${national.substring(3, 7)} ${national.substring(7)}"
        }
    }

    /** International IR form: +98 912 358 8236 (mobile), +98 21 9120 0150 (landline, trunk dropped). */
    private fun formatIranianInternational(e164: String): String {
        val rest = e164.removePrefix("+").removePrefix("98")
        if (rest.length != 10 || !rest.all { it.isDigit() }) return e164
        return if (rest.startsWith("9")) {
            "+98 ${rest.substring(0, 3)} ${rest.substring(3, 6)} ${rest.substring(6)}"
        } else {
            "+98 ${rest.substring(0, 2)} ${rest.substring(2, 6)} ${rest.substring(6)}"
        }
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
