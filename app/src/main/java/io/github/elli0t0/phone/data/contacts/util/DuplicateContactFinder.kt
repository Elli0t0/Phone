package io.github.elli0t0.phone.data.contacts.util

import io.github.elli0t0.phone.core.common.PhoneNumberMatcher
import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import io.github.elli0t0.phone.data.contacts.model.DuplicateGroup
import io.github.elli0t0.phone.data.contacts.model.DuplicateReason

/**
 * Finds groups of contacts that are probably the same person.
 *
 * - Same number: any of the contact's phone numbers shares a grouping key
 *   (normalized number reduced to its last 10 digits) with another contact.
 * - Same name: normalized display names are equal (case-insensitive, extra
 *   whitespace removed). Names shorter than 2 characters and names that are
 *   just a phone number are ignored.
 *
 * A group that matches on both number and name is reported once, as
 * [DuplicateReason.SAME_NUMBER].
 */
object DuplicateContactFinder {

    fun findDuplicates(
        summaries: List<ContactSummary>,
        numbersByContactId: Map<Long, List<String>>,
        regionIso: String = PhoneNumberMatcher.deviceRegionIso()
    ): List<DuplicateGroup> {
        val byId = summaries.associateBy { it.id }
        val groups = mutableListOf<DuplicateGroup>()
        val seenIdSets = mutableSetOf<Set<Long>>()

        val byNumberKey = mutableMapOf<String, MutableSet<Long>>()
        for (summary in summaries) {
            val numbers = numbersByContactId[summary.id].orEmpty()
            val candidates = if (numbers.isNotEmpty()) {
                numbers
            } else {
                listOfNotNull(summary.primaryNumberPreview)
            }
            for (number in candidates) {
                val key = PhoneNumberMatcher.groupingKey(number, regionIso) ?: continue
                byNumberKey.getOrPut(key) { mutableSetOf() }.add(summary.id)
            }
        }
        for ((_, ids) in byNumberKey) {
            if (ids.size > 1) {
                addGroup(groups, seenIdSets, byId, ids, DuplicateReason.SAME_NUMBER)
            }
        }

        val byName = mutableMapOf<String, MutableSet<Long>>()
        for (summary in summaries) {
            val normalizedName = PhoneNumberMatcher.normalizeName(summary.displayName)
            if (normalizedName.length < 2) continue
            if (normalizedName.all { it.isDigit() }) continue
            byName.getOrPut(normalizedName) { mutableSetOf() }.add(summary.id)
        }
        for ((_, ids) in byName) {
            if (ids.size > 1) {
                addGroup(groups, seenIdSets, byId, ids, DuplicateReason.SAME_NAME)
            }
        }

        return groups.sortedWith(
            compareBy(
                { it.reason },
                { it.contacts.firstOrNull()?.displayName.orEmpty() }
            )
        )
    }

    private fun addGroup(
        groups: MutableList<DuplicateGroup>,
        seenIdSets: MutableSet<Set<Long>>,
        byId: Map<Long, ContactSummary>,
        ids: Set<Long>,
        reason: DuplicateReason
    ) {
        if (!seenIdSets.add(ids)) return
        val contacts = ids.sorted().mapNotNull { byId[it] }
        if (contacts.size > 1) {
            groups.add(DuplicateGroup(reason, contacts))
        }
    }
}
