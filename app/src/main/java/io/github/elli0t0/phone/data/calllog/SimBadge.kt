package io.github.elli0t0.phone.data.calllog

/**
 * Minimal SIM identity for badge mapping: the account's ICCID (handle id)
 * plus its display label. Kept free of Android telecom types so the
 * mapping stays JVM-testable.
 */
internal data class SimBadge(
    val iccId: String?,
    val label: String
)

/**
 * Map a call-log phoneAccountId to its SIM badge: the matching account's
 * label (carrier name when known, "SIM N" otherwise). Returns null when
 * the account is unknown or unresolvable — no badge beats a wrong badge
 * like the old "SIM ${take(2)}" on 20-digit ICCIDs.
 */
internal fun resolveSimBadge(phoneAccountId: String?, sims: List<SimBadge>): String? {
    if (phoneAccountId.isNullOrBlank()) return null
    return sims.firstOrNull { it.iccId == phoneAccountId }?.label
}
