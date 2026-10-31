package io.github.elli0t0.phone.feature.contacts

import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import io.github.elli0t0.phone.data.contacts.model.DeleteContactsResult
import io.github.elli0t0.phone.data.contacts.model.DuplicateGroup
import io.github.elli0t0.phone.core.common.Selection

data class ContactsUiState(
    val searchQuery: String = "",
    val allContacts: List<ContactSummary> = emptyList(),
    val favorites: List<ContactSummary> = emptyList(),
    val filteredContacts: List<ContactSummary> = emptyList(),
    val groupedContacts: Map<String, List<ContactSummary>> = emptyMap(),
    val profileContact: ContactSummary? = null,
    val showDuplicatesCard: Boolean = false,
    val isLoading: Boolean = true,
    val userMessageRes: Int? = null,
    val duplicateGroups: List<DuplicateGroup> = emptyList(),
    val showDuplicates: Boolean = false,
    val isFindingDuplicates: Boolean = false,
    val pendingMergeGroup: DuplicateGroup? = null,
    val isMerging: Boolean = false,
    val allDuplicatesMerged: Boolean = false,
    val showMergeUndo: Boolean = false,
    val mergedGroupCount: Int = 0,
    val selection: Selection = Selection(),
    val isDeleting: Boolean = false,
    val deleteReport: DeleteContactsResult? = null
)
