package io.github.elli0t0.phone.data.contacts.util

import io.github.elli0t0.phone.core.common.PhoneNumberMatcher
import io.github.elli0t0.phone.data.contacts.model.ContactSummary

object ContactSearchMatcher {

    /**
     * Matches contact by display name (case-insensitive) or phone number using the
     * shared [PhoneNumberMatcher] (device region + trailing-digit match).
     * For example, "0912" matches "0912 345 6789", "+98 912 ...", or "0912-345-6789".
     */
    fun matches(contact: ContactSummary, query: String): Boolean {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return true

        // 1. Name match (case-insensitive)
        if (contact.displayName.contains(trimmedQuery, ignoreCase = true)) {
            return true
        }

        // 2. Phone match (shared normalization: device region + trailing digits)
        if (PhoneNumberMatcher.matchesQuery(contact.primaryNumberPreview ?: "", trimmedQuery)) {
            return true
        }

        return false
    }

    /**
     * Filters list of contacts using [matches].
     */
    fun filter(contacts: List<ContactSummary>, query: String): List<ContactSummary> {
        if (query.isBlank()) return contacts
        return contacts.filter { matches(it, query) }
    }
}
