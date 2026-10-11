package io.github.elli0t0.phone.feature.contacts

import io.github.elli0t0.phone.data.contacts.model.ContactDetail

data class ContactDetailUiState(
    val isLoading: Boolean = true,
    val contact: ContactDetail? = null,
    val showDeleteDialog: Boolean = false,
    val isDeleted: Boolean = false,
    val errorMessageRes: Int? = null
)
