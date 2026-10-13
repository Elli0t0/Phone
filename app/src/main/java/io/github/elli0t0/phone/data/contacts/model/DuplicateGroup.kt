package io.github.elli0t0.phone.data.contacts.model

enum class DuplicateReason {
    SAME_NUMBER,
    SAME_NAME
}

/**
 * A group of contacts that look like the same person: they share a normalized
 * phone number ([DuplicateReason.SAME_NUMBER]) or the same normalized name
 * ([DuplicateReason.SAME_NAME]). The first contact in [contacts] is the one kept
 * when the group is merged; the others are deleted after their numbers and
 * emails are moved into it.
 */
data class DuplicateGroup(
    val reason: DuplicateReason,
    val contacts: List<ContactSummary>
)
