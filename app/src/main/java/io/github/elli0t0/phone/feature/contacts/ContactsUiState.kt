package io.github.elli0t0.phone.feature.contacts

import io.github.elli0t0.phone.data.contacts.model.ContactSummary

data class ContactsUiState(
    val searchQuery: String = "",
    val allContacts: List<ContactSummary> = emptyList(),
    val filteredContacts: List<ContactSummary> = emptyList(),
    val groupedContacts: Map<String, List<ContactSummary>> = emptyMap(),
    val isLoading: Boolean = true,
    val userMessageRes: Int? = null
)
