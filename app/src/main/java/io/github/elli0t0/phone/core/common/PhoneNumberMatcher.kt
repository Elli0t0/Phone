package io.github.elli0t0.phone.core.common

import android.content.Context
import android.telephony.TelephonyManager
import java.util.Locale
import kotlin.jvm.Volatile

/**
 * Shared phone number matching used consistently by keypad search, contacts search,
 * recents search and lookup, and the call screen.
 *
 * Normalization rules:
 * - Only digits are kept.
 * - An international "00" prefix is dropped ("0044..." becomes "44...").
 * - A trunk "0" is replaced with the device region's country calling code
 *   ("0912..." in IR becomes "98912...").
 * - Numbers that differ only by country code / trunk prefix still match through
 *   trailing-digit comparison: when both numbers have at least 7 digits, the last
 *   up to 10 digits must be equal ("9123456789" matches "+989123456789").
 */
object PhoneNumberMatcher {

    private const val MIN_SIGNIFICANT_DIGITS = 7
    private const val MAX_SIGNIFICANT_DIGITS = 10

    private val COUNTRY_CALLING_CODES = mapOf(
        "US" to "1",
        "CA" to "1",
        "IR" to "98",
        "GB" to "44",
        "DE" to "49",
        "FR" to "33",
        "IT" to "39",
        "ES" to "34",
        "NL" to "31",
        "BE" to "32",
        "CH" to "41",
        "AT" to "43",
        "SE" to "46",
        "NO" to "47",
        "DK" to "45",
        "FI" to "358",
        "PL" to "48",
        "TR" to "90",
        "RU" to "7",
        "KZ" to "7",
        "UA" to "380",
        "IN" to "91",
        "PK" to "92",
        "BD" to "880",
        "AF" to "93",
        "IQ" to "964",
        "SA" to "966",
        "AE" to "971",
        "QA" to "974",
        "KW" to "965",
        "BH" to "973",
        "OM" to "968",
        "JO" to "962",
        "LB" to "961",
        "SY" to "963",
        "YE" to "967",
        "EG" to "20",
        "MA" to "212",
        "DZ" to "213",
        "TN" to "216",
        "LY" to "218",
        "SD" to "249",
        "IL" to "972",
        "PS" to "970",
        "JP" to "81",
        "KR" to "82",
        "CN" to "86",
        "TW" to "886",
        "HK" to "852",
        "SG" to "65",
        "MY" to "60",
        "TH" to "66",
        "VN" to "84",
        "ID" to "62",
        "PH" to "63",
        "AU" to "61",
        "NZ" to "64",
        "ZA" to "27",
        "NG" to "234",
        "KE" to "254",
        "BR" to "55",
        "MX" to "52",
        "AR" to "54",
        "CL" to "56",
        "CO" to "57",
        "PE" to "51",
        "VE" to "58"
    )

    /**
     * Device region (ISO country code): SIM country, else network country,
     * else "IR". Never the device locale. Cached once per process from
     * [initRegion]; tests and processes without init fall back to "IR".
     */
    @Volatile
    private var regionOverride: String? = null

    fun initRegion(context: Context) {
        if (regionOverride != null) return
        regionOverride = resolveRegionIso(context)
    }

    @Suppress("DEPRECATION")
    fun resolveRegionIso(context: Context): String {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val sim = tm?.simCountryIso?.takeIf { it.length == 2 }?.uppercase()
            val network = tm?.networkCountryIso?.takeIf { it.length == 2 }?.uppercase()
            sim ?: network ?: "IR"
        } catch (_: Exception) {
            "IR"
        }
    }

    fun deviceRegionIso(): String = regionOverride ?: "IR"

    fun countryCallingCode(regionIso: String): String? =
        COUNTRY_CALLING_CODES[regionIso.uppercase(Locale.ROOT)]

    private val CALLING_CODE_TO_ISO: Map<String, String> by lazy {
        val map = linkedMapOf<String, String>()
        for ((iso, code) in COUNTRY_CALLING_CODES) {
            map.putIfAbsent(code, iso)
        }
        map
    }

    /** ISO country for an E.164 number, from its calling code. Null if unknown. */
    fun countryIsoForE164(e164: String): String? {
        val digits = e164.removePrefix("+").filter { it.isDigit() }
        for (len in 3 downTo 1) {
            if (digits.length >= len) {
                CALLING_CODE_TO_ISO[digits.substring(0, len)]?.let { return it }
            }
        }
        return null
    }

    /**
     * Normalizes a phone number to digits with the country code applied when it
     * can be derived from the trunk prefix and the device region.
     */
    fun normalize(number: String, regionIso: String = deviceRegionIso()): String {
        val trimmed = PhoneNumberFormatter.normalizeDigits(number.trim())
        if (trimmed.isEmpty()) return ""
        val hadPlus = trimmed.startsWith("+")
        val hadDoubleZero = trimmed.startsWith("00")
        var digits = trimmed.filter { it.isDigit() }
        if (hadDoubleZero && digits.length > 2) {
            digits = digits.substring(2)
        } else if (!hadPlus && digits.startsWith("0")) {
            val code = countryCallingCode(regionIso)
            if (code != null) {
                digits = code + digits.drop(1).trimStart('0')
            }
        }
        return digits
    }

    /**
     * True when two numbers refer to the same number: equal after normalization,
     * or equal on their trailing digits (at least 7, up to 10).
     * Short codes (fewer than 7 digits) must match exactly.
     */
    fun matches(a: String, b: String, regionIso: String = deviceRegionIso()): Boolean {
        val first = normalize(a, regionIso)
        val second = normalize(b, regionIso)
        if (first.isEmpty() || second.isEmpty()) return false
        if (first == second) return true
        if (first.length >= MIN_SIGNIFICANT_DIGITS && second.length >= MIN_SIGNIFICANT_DIGITS) {
            val significant = minOf(MAX_SIGNIFICANT_DIGITS, first.length, second.length)
            return first.takeLast(significant) == second.takeLast(significant)
        }
        return false
    }

    /**
     * True when a stored phone number matches a (possibly partial) search query:
     * full/trailing equivalence, substring of the normalized number, or the same
     * ignoring leading zeros (covers trunk-prefix input when the region is unknown).
     */
    fun matchesQuery(phone: String, query: String, regionIso: String = deviceRegionIso()): Boolean {
        val queryDigits = PhoneNumberFormatter.normalizeDigits(query).filter { it.isDigit() }
        if (queryDigits.isEmpty()) return false
        if (matches(phone, query, regionIso)) return true
        val phoneNormalized = normalize(phone, regionIso)
        if (phoneNormalized.isEmpty()) return false
        val queryNormalized = normalize(query, regionIso)
        if (queryNormalized.isNotEmpty() && phoneNormalized.contains(queryNormalized)) return true
        val phoneStripped = phoneNormalized.trimStart('0')
        val queryStripped = queryNormalized.trimStart('0')
        if (queryStripped.isNotEmpty() && phoneStripped.contains(queryStripped)) return true
        val queryRawStripped = queryDigits.trimStart('0')
        if (queryRawStripped.isNotEmpty() && phoneStripped.contains(queryRawStripped)) return true
        return false
    }

    /**
     * Normalizes a phone number to E.164 ("+<country code><national number>")
     * using the device region. Returns "" when the input cannot be identified
     * reliably: too short (short codes, emergency numbers, partial input) or
     * no digits. Explicitly international forms ("+..." / "00...") keep their
     * country code; a trunk "0" is replaced with the device region's code;
     * bare plausibly-complete numbers (10+ digits) get the device code, unless
     * they already start with it.
     */
    fun toE164(number: String, regionIso: String = deviceRegionIso()): String {
        val trimmed = PhoneNumberFormatter.normalizeDigits(number.trim())
        if (trimmed.isEmpty()) return ""
        val digits = trimmed.filter { it.isDigit() }
        if (digits.isEmpty()) return ""
        if (trimmed.startsWith("+")) return "+$digits"
        if (trimmed.startsWith("00") && digits.length > 2) return "+" + digits.substring(2)
        val code = countryCallingCode(regionIso)
        if (digits.startsWith("0")) {
            val national = digits.drop(1).trimStart('0')
            if (national.length < MIN_SIGNIFICANT_DIGITS) return ""
            if (code == null) return "+$digits"
            return "+$code$national"
        }
        // Bare digits: only canonicalize plausibly complete numbers.
        if (digits.length < 10) return ""
        if (code != null && digits.startsWith(code) &&
            digits.length - code.length >= MIN_SIGNIFICANT_DIGITS
        ) {
            return "+$digits"
        }
        if (code != null) return "+$code$digits"
        return "+$digits"
    }

    /**
     * Grouping key for duplicate detection: normalized number reduced to its last
     * 10 digits. Null when the number is too short to identify a contact safely.
     */
    fun groupingKey(number: String, regionIso: String = deviceRegionIso()): String? {
        val normalized = normalize(number, regionIso)
        if (normalized.length < MIN_SIGNIFICANT_DIGITS) return null
        return if (normalized.length > MAX_SIGNIFICANT_DIGITS) {
            normalized.takeLast(MAX_SIGNIFICANT_DIGITS)
        } else {
            normalized
        }
    }

    /**
     * Normalizes a display name for duplicate detection: lowercased, trimmed,
     * internal whitespace collapsed to single spaces.
     */
    fun normalizeName(name: String): String {
        return name.trim().lowercase(Locale.ROOT)
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .joinToString(" ")
    }
}
