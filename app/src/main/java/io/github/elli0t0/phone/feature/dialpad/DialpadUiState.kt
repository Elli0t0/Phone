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
    val quickRedial: List<QuickRedialEntry> = emptyList(),
    // Empty-keypad extras: favorite contacts (dialable ones only) and
    // whether the clipboard holds plain text (checked via the clip
    // description only, so no clipboard toast fires). The text itself
    // is read and validated only when the Paste chip is tapped.
    val favorites: List<ContactSummary> = emptyList(),
    val showPasteChip: Boolean = false
)

/** One recent number for the empty-keypad quick-dial chips. */
data class QuickRedialEntry(
    val number: String,
    val label: String
)
