package io.github.elli0t0.phone.data.spam

import android.telecom.TelecomManager
import io.github.elli0t0.phone.core.common.PhoneNumberFormatter
import io.github.elli0t0.phone.core.common.PhoneNumberMatcher

/**
 * Pure spam matching (offline, no network, no uploads). Storage
 * keys are [PhoneNumberMatcher.normalize] output, so the +98,
 * 0098 and 0-trunk spellings of one number are the same key.
 * Pure functions only, so [SpamMatcherTest] runs on the JVM.
 */
object SpamMatcher {
    /** Canonical key for storage and cache lookups. "" when unusable. */
    fun normalizeForStorage(number: String): String =
        PhoneNumberMatcher.normalize(number)

    /**
     * True when [number] is in [spamNormalized] (exact normalized
     * match) or matches an entry on trailing digits via
     * [PhoneNumberMatcher.matches] (covers numbers stored before a
     * region change, or with a different trunk spelling).
     */
    fun isSpam(spamNormalized: Set<String>, number: String): Boolean {
        if (spamNormalized.isEmpty()) return false
        val normalized = normalizeForStorage(number)
        if (normalized.isEmpty()) return false
        if (normalized in spamNormalized) return true
        return spamNormalized.any { PhoneNumberMatcher.matches(it, number) }
    }

    /** True when [number]'s digits start with any stored prefix. */
    fun matchesPrefix(prefixes: Set<String>, number: String): Boolean {
        if (prefixes.isEmpty()) return false
        val digits = PhoneNumberFormatter.normalizeDigits(number).filter { it.isDigit() }
        if (digits.isEmpty()) return false
        return prefixes.any { prefix ->
            val prefixDigits = PhoneNumberFormatter.normalizeDigits(prefix)
                .filter { it.isDigit() }
            prefixDigits.isNotEmpty() && digits.startsWith(prefixDigits)
        }
    }

    /**
     * True when the number is hidden/private: no handle at all, or a
     * presentation that says the network withheld it. The handle
     * presentation is only meaningful on API 29+.
     */
    fun isHiddenNumber(number: String, handlePresentation: Int): Boolean {
        if (number.isBlank()) return true
        // PRESENTATION_PAYPHONE still shows a number, so it is not
        // hidden; only RESTRICTED / UNKNOWN mean withheld.
        return handlePresentation == TelecomManager.PRESENTATION_RESTRICTED ||
            handlePresentation == TelecomManager.PRESENTATION_UNKNOWN
    }
}
