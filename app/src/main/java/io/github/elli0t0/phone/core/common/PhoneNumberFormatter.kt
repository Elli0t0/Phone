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
        // Normalize first: Persian/Arabic digits become ASCII before any
        // check below, so every return path yields the dialable form.
        val rawNumber = normalizeDigits(rawNumber)
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
            PhoneNumberUtils.formatNumber(display, ownCountry)
                ?.takeIf { it.isNotBlank() && it != display }
        } catch (_: Exception) {
            null
        } ?: formatFallback(display, ownCountry)
    }

    /**
     * Live-typing display: same grouping as [format], applied progressively
     * while the number is still incomplete. USSD/short codes stay as typed.
     * The raw digits stay the source of truth; this only affects display.
     */
    fun formatPartial(rawNumber: String, defaultCountryIso: String = PhoneNumberMatcher.deviceRegionIso()): String {
        // Normalize first: see format().
        val rawNumber = normalizeDigits(rawNumber)
        if (rawNumber.isBlank()) return ""
        if (rawNumber.contains('*') || rawNumber.contains('#')) return rawNumber
        val trimmed = rawNumber.trim()
        val digits = trimmed.filter { it.isDigit() }
        if (digits.isEmpty()) return rawNumber
        val region = defaultCountryIso.uppercase()
        if (trimmed.startsWith("+") || trimmed.startsWith("00")) {
            val intlDigits = digits.removePrefix("00")
            val intl = "+$intlDigits"
            val ownCountry = PhoneNumberMatcher.countryIsoForE164(intl) ?: region
            if (ownCountry == "IR" && intlDigits.startsWith("98")) {
                val rest = intlDigits.removePrefix("98")
                val chunks = if (rest.startsWith("9")) intArrayOf(3, 3, 4) else intArrayOf(2, 4, 4)
                return "+98" + if (rest.isEmpty()) "" else " " + groupProgressive(rest, chunks)
            }
            return try {
                PhoneNumberUtils.formatNumber(intl, ownCountry)?.takeIf { it.isNotBlank() && it != intl }
            } catch (_: Exception) {
                null
            } ?: intl
        }
        if (region == "IR") {
            if (digits.startsWith("0")) {
                val chunks = if (digits.startsWith("09")) intArrayOf(4, 3, 4) else intArrayOf(3, 4, 4)
                return groupProgressive(digits, chunks)
            }
            if (digits.length < 5) return rawNumber
            val chunks = if (digits.startsWith("9")) intArrayOf(3, 3, 4) else intArrayOf(2, 4, 4)
            return groupProgressive(digits, chunks)
        }
        return try {
            PhoneNumberUtils.formatNumber(digits, region)?.takeIf { it.isNotBlank() && it != digits }
        } catch (_: Exception) {
            null
        } ?: rawNumber
    }

    /** Groups [digits] into [chunks]-sized groups separated by single spaces. */
    private fun groupProgressive(digits: String, chunks: IntArray): String {
        val sb = StringBuilder()
        var index = 0
        for ((chunkIndex, size) in chunks.withIndex()) {
            if (index >= digits.length) break
            if (chunkIndex > 0) sb.append(' ')
            val end = minOf(index + size, digits.length)
            sb.append(digits.substring(index, end))
            index = end
        }
        if (index < digits.length) {
            sb.append(' ').append(digits.substring(index))
        }
        return sb.toString()
    }

    /**
     * Unit tests run against stubbed android.jar, so formatNumber returns
     * null or throws there. A US number gets its own grouping in that case;
     * every other country stays as typed (never US grouping as a default).
     */
    private fun formatFallback(display: String, country: String): String {
        if (country == "US" && display.length == 10 && display.all { it.isDigit() }) {
            return "${display.substring(0, 3)}-${display.substring(3, 6)}-${display.substring(6)}"
        }
        return display
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
     * Map Persian (۰-۹, U+06F0–U+06F9) and Eastern Arabic (٠-٩, U+0660–U+0669)
     * digits to ASCII 0-9. sanitize() keeps these (Char.isDigit is
     * Unicode-aware), so without this they survive into matchers and
     * tel: URIs that cellular basebands reject. Every other character
     * passes through untouched.
     */
    fun normalizeDigits(input: String): String {
        if (input.none { it in '۰'..'۹' || it in '٠'..'٩' }) return input
        return buildString(input.length) {
            for (c in input) {
                when (c) {
                    in '۰'..'۹' -> append('0' + (c - '۰'))
                    in '٠'..'٩' -> append('0' + (c - '٠'))
                    else -> append(c)
                }
            }
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
        for (char in normalizeDigits(input)) {
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
