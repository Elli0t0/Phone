package io.github.elli0t0.phone.data.contacts.util

import io.github.elli0t0.phone.core.common.PhoneNumberMatcher
import io.github.elli0t0.phone.data.contacts.model.ContactDetail
import io.github.elli0t0.phone.data.contacts.model.ContactInput
import io.github.elli0t0.phone.data.contacts.model.DuplicateGroup
import java.util.zip.CRC32

/**
 * Pure helpers for duplicate merging. Kept free of Android framework
 * types so the hash, pairing, and fallback-copy rules are unit-testable.
 *
 * The primary merge path links raw contacts with AggregationExceptions
 * (nothing is deleted, so nothing is lost); the fallback path below is
 * only for read-only accounts, where linking is rejected.
 */
object ContactMergeUtil {

    /**
     * Stable hash of the current duplicate set: sorted contact ids per
     * group, groups sorted, joined, CRC32 hex. Group order and contact
     * order inside a group do not change the hash; the banner shows
     * again only when the set itself changes.
     */
    fun duplicateSetHash(groups: List<DuplicateGroup>): String {
        val canonical = groups
            .map { group -> group.contacts.map { it.id }.sorted().joinToString(",") }
            .sorted()
            .joinToString(";")
        val crc = CRC32()
        crc.update(canonical.toByteArray(Charsets.UTF_8))
        return crc.value.toString(radix = 16)
    }

    /**
     * (keepRawId, targetRawId) pairs for one AggregationExceptions
     * batch, in either direction: the same pairs link
     * (TYPE_KEEP_TOGETHER) and unlink (TYPE_KEEP_SEPARATE).
     */
    fun aggregationPairs(keepRawId: Long, targetRawIds: List<Long>): List<Pair<Long, Long>> =
        targetRawIds.distinct().filter { it != keepRawId }.map { keepRawId to it }

    /**
     * Fallback merge input: the kept contact's fields always win, and
     * a target's field fills a blank only when the kept one has none.
     * Passing the kept contact's own notes/birthday/address through is
     * what keeps the differential update from wiping them.
     */
    fun mergedFallbackInput(kept: ContactDetail, targets: List<ContactDetail>): ContactInput {
        val phones = kept.phones.toMutableList()
        for (target in targets) {
            for (phone in target.phones) {
                if (phones.none { PhoneNumberMatcher.matches(it.number, phone.number) }) {
                    phones.add(phone.copy(id = 0L))
                }
            }
        }
        val emails = kept.emails.toMutableList()
        for (target in targets) {
            for (email in target.emails) {
                if (emails.none { it.address.equals(email.address, ignoreCase = true) }) {
                    emails.add(email.copy(id = 0L))
                }
            }
        }
        return ContactInput(
            displayName = kept.displayName,
            phones = phones,
            emails = emails,
            organization = kept.organization ?: targets.firstNotNullOfOrNull { it.organization },
            notes = kept.notes ?: targets.firstNotNullOfOrNull { it.notes },
            birthday = kept.birthday ?: targets.firstNotNullOfOrNull { it.birthday },
            address = kept.address ?: targets.firstNotNullOfOrNull { it.address }
        )
    }
}
