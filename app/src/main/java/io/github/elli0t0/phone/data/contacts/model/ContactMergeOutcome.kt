package io.github.elli0t0.phone.data.contacts.model

/**
 * What a merge produced, kept so Undo can reverse it. [linked] is true
 * when the contacts were linked (AggregationExceptions); the read-only
 * fallback copies fields and deletes the merged contacts instead, and
 * that path cannot be undone.
 */
data class ContactMergeOutcome(
    val keepRawId: Long,
    val targetRawIds: List<Long>,
    val linked: Boolean
)
