package io.github.elli0t0.phone.feature.contacts

import io.github.elli0t0.phone.data.contacts.model.ContactGroup
import io.github.elli0t0.phone.data.contacts.model.ContactSummary

data class GroupsUiState(
    val groups: List<ContactGroup> = emptyList(),
    val selectedGroup: ContactGroup? = null,
    val members: List<ContactSummary> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMembers: Boolean = false
)
