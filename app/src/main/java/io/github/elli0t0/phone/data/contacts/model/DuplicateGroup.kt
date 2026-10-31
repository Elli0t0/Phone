package io.github.elli0t0.phone.data.contacts.model

enum class DuplicateReason {
    SAME_NUMBER,
    SAME_NAME
}

/**
 * A group of contacts that look like the same person: they share a normalized
 * phone number ([DuplicateReason.SAME_NUMBER]) or the same normalized name
 * ([DuplicateReason.SAME_NAME]). Merging links the contacts (nothing is
 * deleted); the first contact in [contacts] is the one kept as primary.
 */
data class DuplicateGroup(
    val reason: DuplicateReason,
    val contacts: List<ContactSummary>
)
