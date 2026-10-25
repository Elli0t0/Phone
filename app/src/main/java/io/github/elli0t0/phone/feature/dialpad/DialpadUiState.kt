package io.github.elli0t0.phone.feature.dialpad

import io.github.elli0t0.phone.data.contacts.model.ContactSummary

data class DialpadUiState(
    val rawNumber: String = "",
    val formattedNumber: String = "",
    val matchingContacts: List<ContactSummary> = emptyList(),
    val matchingContactCounts: Map<Long, Int> = emptyMap(),
    val nameSearchQuery: String = "",
    val nameSearchResults: List<ContactSummary> = emptyList(),
    val isSearching: Boolean = false,
    val canCall: Boolean = false,
    val quickRedial: List<QuickRedialEntry> = emptyList()
)

/** One recent number for the empty-keypad quick-dial chips. */
data class QuickRedialEntry(
    val number: String,
    val label: String
)
