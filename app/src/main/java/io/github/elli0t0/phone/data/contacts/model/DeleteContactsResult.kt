package io.github.elli0t0.phone.data.contacts.model

/**
 * Outcome of a bulk contact delete: read-only contacts land in
 * [failed] instead of aborting the batch.
 */
data class DeleteContactsResult(val deleted: Int, val failed: Int)
