package io.github.elli0t0.phone.data.spam

/**
 * Pure screening decision, extracted so the contacts exemption and
 * the check order are unit-testable without Android ([SpamDeciderTest]).
 * The service only gathers the inputs (settings, cache lookups, one
 * PhoneLookup query) and applies the outcome.
 */
enum class SpamDecision { ALLOW, BLOCK, SILENCE }

object SpamDecider {
    fun decide(
        blockKnownSpam: Boolean,
        blockHiddenNumbers: Boolean,
        silenceUnknownCallers: Boolean,
        isSpam: Boolean,
        prefixBlocked: Boolean,
        isHiddenNumber: Boolean,
        hasNumber: Boolean,
        inContacts: Boolean
    ): SpamDecision {
        // Contacts are NEVER screened, whatever else matches.
        if (inContacts) return SpamDecision.ALLOW
        if (blockKnownSpam && isSpam) return SpamDecision.BLOCK
        if (prefixBlocked) return SpamDecision.BLOCK
        if (blockHiddenNumbers && isHiddenNumber) return SpamDecision.BLOCK
        if (silenceUnknownCallers && hasNumber) return SpamDecision.SILENCE
        return SpamDecision.ALLOW
    }
}
