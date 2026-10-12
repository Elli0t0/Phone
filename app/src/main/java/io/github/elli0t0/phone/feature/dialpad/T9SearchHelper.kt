package io.github.elli0t0.phone.feature.dialpad

import io.github.elli0t0.phone.data.contacts.model.ContactSummary

/**
 * Utility for mapping letters and names to dialpad T9 numeric digits (2-9).
 * Standard ITU E.164 / Telephone keypad mapping:
 * 2 -> A, B, C
 * 3 -> D, E, F
 * 4 -> G, H, I
 * 5 -> J, K, L
 * 6 -> M, N, O
 * 7 -> P, Q, R, S
 * 8 -> T, U, V
 * 9 -> W, X, Y, Z
 */
object T9SearchHelper {

    fun charToDigit(c: Char): Char? {
        return when (c.uppercaseChar()) {
            'A', 'B', 'C' -> '2'
            'D', 'E', 'F' -> '3'
            'G', 'H', 'I' -> '4'
            'J', 'K', 'L' -> '5'
            'M', 'N', 'O' -> '6'
            'P', 'Q', 'R', 'S' -> '7'
            'T', 'U', 'V' -> '8'
            'W', 'X', 'Y', 'Z' -> '9'
            else -> null
        }
    }

    /**
     * Converts a string (e.g. contact name) into a sequence of T9 digits.
     * Characters without digit mappings (spaces, symbols, or unmapped Unicode) are preserved or ignored based on matching.
     */
    fun nameToT9Digits(name: String): String {
        val sb = StringBuilder()
        for (ch in name) {
            val digit = charToDigit(ch)
            if (digit != null) {
                sb.append(digit)
            } else if (ch.isDigit()) {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Determines whether [contact] matches the entered dial string [query].
     * Matches if:
     * 1. The primary phone number contains the query digits (digits-insensitive).
     * 2. The contact name contains a word or sequence whose T9 representation matches the query digits.
     */
    fun matches(contact: ContactSummary, query: String): Boolean {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) return false

        val queryDigits = trimmedQuery.filter { it.isDigit() }
        if (queryDigits.isEmpty()) return false

        // 1. Phone number match
        val phoneDigits = (contact.primaryNumberPreview ?: "").filter { it.isDigit() }
        if (phoneDigits.contains(queryDigits)) {
            return true
        }
        if (queryDigits.startsWith("0")) {
            val withoutZero = queryDigits.dropWhile { it == '0' }
            if (withoutZero.isNotEmpty() && phoneDigits.contains(withoutZero)) {
                return true
            }
        }

        // 2. Name T9 match across whole name
        val wholeNameT9 = nameToT9Digits(contact.displayName)
        if (wholeNameT9.contains(queryDigits)) {
            return true
        }

        // 3. Name T9 match by individual word prefixes (e.g. "John Doe": "5646" or "363")
        val words = contact.displayName.split(Regex("[\\s,.-]+")).filter { it.isNotBlank() }
        for (word in words) {
            val wordT9 = nameToT9Digits(word)
            if (wordT9.startsWith(queryDigits)) {
                return true
            }
        }

        return false
    }

    /**
     * Filters and ranks contacts for T9 and number search.
     */
    fun filter(contacts: List<ContactSummary>, query: String): List<ContactSummary> {
        if (query.isBlank()) return emptyList()
        return contacts.filter { matches(it, query) }
    }
}
