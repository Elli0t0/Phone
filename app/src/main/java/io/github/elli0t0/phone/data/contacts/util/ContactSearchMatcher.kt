package io.github.elli0t0.phone.data.contacts.util

import io.github.elli0t0.phone.data.contacts.model.ContactSummary

object ContactSearchMatcher {

    /**
     * Matches contact by display name (case-insensitive) or primary phone number (digits-insensitive).
     * For example, "0912" matches "0912 345 6789", "+98 912 ...", or "0912-345-6789".
     *
     * Handles local prefix normalization: in many regions (such as Iran or UK), "0912" matches "+98912..."
     * where the local leading '0' corresponds to country-code prefix "+98" or international format.
     */
    fun matches(contact: ContactSummary, query: String): Boolean {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return true

        // 1. Name match (case-insensitive)
        if (contact.displayName.contains(trimmedQuery, ignoreCase = true)) {
            return true
        }

        // 2. Digits-insensitive phone match
        val queryDigits = trimmedQuery.filter { it.isDigit() }
        if (queryDigits.isNotEmpty()) {
            val phoneDigits = (contact.primaryNumberPreview ?: "").filter { it.isDigit() }
            if (phoneDigits.contains(queryDigits)) {
                return true
            }

            // If query starts with leading 0 (e.g. 0912) and phone is international (e.g. 98912...),
            // match query without leading 0
            if (queryDigits.startsWith("0")) {
                val queryWithoutLeadingZero = queryDigits.dropWhile { it == '0' }
                if (queryWithoutLeadingZero.isNotEmpty() && phoneDigits.contains(queryWithoutLeadingZero)) {
                    return true
                }
            }

            // If phone starts with leading 0 and query is without leading 0 or vice versa
            if (phoneDigits.startsWith("0")) {
                val phoneWithoutLeadingZero = phoneDigits.dropWhile { it == '0' }
                if (phoneWithoutLeadingZero.isNotEmpty() && phoneWithoutLeadingZero.contains(queryDigits)) {
                    return true
                }
            }
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
