package io.github.elli0t0.phone.core.common

/**
 * Multi-select state for a list, shared by Recents and Contacts.
 * Keys are row ids (call-group ids in Recents, contact ids in
 * Contacts); the empty set means selection mode is off.
 */
data class Selection(val ids: Set<Long> = emptySet()) {
    val isActive get() = ids.isNotEmpty()
    fun toggle(id: Long) = copy(ids = if (id in ids) ids - id else ids + id)
    fun selectAll(all: Collection<Long>) = copy(ids = all.toSet())
    fun clear() = copy(ids = emptySet())
}
