package io.github.elli0t0.phone.data.contacts.util

import io.github.elli0t0.phone.core.common.PhoneNumberMatcher
import io.github.elli0t0.phone.data.contacts.model.ContactSummary

/**
 * Resolve a dialed number to a contact using only the summaries'
 * primary numbers. First match wins; summaries list the primary
 * number, so a contact whose other numbers match is not found here.
 * Used by the keypad Quick call strip, the keypad speed-dial labels,
 * and the Speed dial manager — all three resolve live, so a renamed
 * or re-numbered contact updates without touching stored settings.
 */
fun List<ContactSummary>.findByNumber(number: String): ContactSummary? {
    if (number.isBlank()) return null
    return firstOrNull { contact ->
        val preview = contact.primaryNumberPreview
        !preview.isNullOrBlank() && PhoneNumberMatcher.matches(preview, number)
    }
}
