package io.github.elli0t0.phone.feature.contacts

import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import io.github.elli0t0.phone.data.contacts.model.DuplicateGroup

data class ContactsUiState(
    val searchQuery: String = "",
    val allContacts: List<ContactSummary> = emptyList(),
    val favorites: List<ContactSummary> = emptyList(),
    val filteredContacts: List<ContactSummary> = emptyList(),
    val groupedContacts: Map<String, List<ContactSummary>> = emptyMap(),
    val isLoading: Boolean = true,
    val userMessageRes: Int? = null,
    val duplicateGroups: List<DuplicateGroup> = emptyList(),
    val showDuplicates: Boolean = false,
    val isFindingDuplicates: Boolean = false,
    val pendingMergeGroup: DuplicateGroup? = null,
    val isMerging: Boolean = false
)
