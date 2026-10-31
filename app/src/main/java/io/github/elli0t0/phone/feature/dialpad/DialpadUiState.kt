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
    // Empty-keypad extras: favorite contacts (dialable ones only).
    val favorites: List<ContactSummary> = emptyList(),
    // Quick call strip (0.19.3): contacts to show above the keys,
    // built from the quick-call settings (source, count, hidden).
    val quickCall: List<QuickCallEntry> = emptyList(),
    // Speed-dial names on keys (0.19.3): digit to the first 6 letters
    // of the resolved contact name, only when the feature and the
    // show-on-keys toggle are on. Empty when either is off.
    val speedDialLabels: Map<Char, String> = emptyMap()
)

/** One recent number for the empty-keypad quick-dial chips. */
data class QuickRedialEntry(
    val number: String,
    val label: String
)

/** One Quick call strip card: the contact plus the number to call. */
data class QuickCallEntry(
    val contact: ContactSummary,
    val number: String
)
