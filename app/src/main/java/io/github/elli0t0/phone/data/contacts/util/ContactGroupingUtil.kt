package io.github.elli0t0.phone.data.contacts.util

import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import java.text.Collator
import java.util.Locale

object ContactGroupingUtil {

    private val collator: Collator by lazy {
        Collator.getInstance(Locale.getDefault()).apply {
            strength = Collator.PRIMARY
        }
    }

    /**
     * Determines section header for a contact.
     * Letters (Latin, Persian, Arabic, etc.) return their uppercase character.
     * Non-letters (digits, symbols, punctuation, or empty) return "#".
     */
    fun getSectionHeader(name: String): String {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return "#"

        val firstChar = trimmed.first()
        return if (Character.isLetter(firstChar)) {
            firstChar.uppercaseChar().toString()
        } else {
            "#"
        }
    }

    /**
     * Sorts and groups contacts into an ordered map of section header to contacts.
     * Section headers are sorted alphabetically, with "#" placed at the end.
     */
    fun groupContacts(contacts: List<ContactSummary>): Map<String, List<ContactSummary>> {
        val sortedContacts = contacts.sortedWith { a, b ->
            val nameA = a.displayName.ifBlank { a.primaryNumberPreview ?: "" }
            val nameB = b.displayName.ifBlank { b.primaryNumberPreview ?: "" }
            collator.compare(nameA, nameB)
        }

        val grouped = sortedContacts.groupBy { contact ->
            getSectionHeader(contact.displayName.ifBlank { contact.primaryNumberPreview ?: "" })
        }

        // Return sorted keys with '#' at the end
        val letterKeys = grouped.keys.filter { it != "#" }.sortedWith(collator)
        val sortedMap = LinkedHashMap<String, List<ContactSummary>>()

        for (key in letterKeys) {
            grouped[key]?.let { sortedMap[key] = it }
        }
        grouped["#"]?.let { sortedMap["#"] = it }

        return sortedMap
    }
}
