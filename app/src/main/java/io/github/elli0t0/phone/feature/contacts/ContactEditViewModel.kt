package io.github.elli0t0.phone.feature.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.data.contacts.ContactsRepository
import io.github.elli0t0.phone.data.contacts.model.ContactDetail
import io.github.elli0t0.phone.data.contacts.model.ContactEmail
import io.github.elli0t0.phone.data.contacts.model.ContactInput
import io.github.elli0t0.phone.data.contacts.model.ContactPhone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContactEditViewModel(
    private val contactsRepository: ContactsRepository,
    initialContact: ContactDetail? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        if (initialContact != null) {
            ContactEditUiState(
                isNewContact = false,
                contactId = initialContact.id,
                lookupKey = initialContact.lookupKey,
                displayName = initialContact.displayName,
                organization = initialContact.organization.orEmpty(),
                phones = if (initialContact.phones.isNotEmpty()) initialContact.phones else listOf(ContactPhone(number = "")),
                emails = initialContact.emails,
                originalDetail = initialContact
            )
        } else {
            ContactEditUiState(
                isNewContact = true,
                phones = listOf(ContactPhone(number = "")),
                emails = emptyList()
            )
        }
    )
    val uiState: StateFlow<ContactEditUiState> = _uiState.asStateFlow()

    fun onNameChanged(name: String) {
        _uiState.update { it.copy(displayName = name, validationErrorRes = null) }
    }

    fun onOrganizationChanged(org: String) {
        _uiState.update { it.copy(organization = org) }
    }

    fun onPhoneNumberChanged(index: Int, number: String) {
        _uiState.update { state ->
            val updated = state.phones.toMutableList()
            if (index in updated.indices) {
                updated[index] = updated[index].copy(number = number)
            }
            state.copy(phones = updated, validationErrorRes = null)
        }
    }

    fun onPhoneTypeChanged(index: Int, type: Int) {
        _uiState.update { state ->
            val updated = state.phones.toMutableList()
            if (index in updated.indices) {
                updated[index] = updated[index].copy(type = type)
            }
            state.copy(phones = updated)
        }
    }

    fun addPhoneRow() {
        _uiState.update { state ->
            state.copy(phones = state.phones + ContactPhone(number = ""))
        }
    }

    fun removePhoneRow(index: Int) {
        _uiState.update { state ->
            val updated = state.phones.toMutableList()
            if (index in updated.indices) {
                updated.removeAt(index)
            }
            state.copy(phones = if (updated.isEmpty()) listOf(ContactPhone(number = "")) else updated)
        }
    }

    fun onEmailAddressChanged(index: Int, address: String) {
        _uiState.update { state ->
            val updated = state.emails.toMutableList()
            if (index in updated.indices) {
                updated[index] = updated[index].copy(address = address)
            }
            state.copy(emails = updated)
        }
    }

    fun onEmailTypeChanged(index: Int, type: Int) {
        _uiState.update { state ->
            val updated = state.emails.toMutableList()
            if (index in updated.indices) {
                updated[index] = updated[index].copy(type = type)
            }
            state.copy(emails = updated)
        }
    }

    fun addEmailRow() {
        _uiState.update { state ->
            state.copy(emails = state.emails + ContactEmail(address = ""))
        }
    }

    fun removeEmailRow(index: Int) {
        _uiState.update { state ->
            val updated = state.emails.toMutableList()
            if (index in updated.indices) {
                updated.removeAt(index)
            }
            state.copy(emails = updated)
        }
    }

    fun saveContact(onSuccess: (savedId: Long) -> Unit) {
        val state = _uiState.value
        val trimmedName = state.displayName.trim()
        val validPhones = state.phones.filter { it.number.trim().isNotBlank() }

        // Validation: At least a name or one phone number must be provided
        if (trimmedName.isBlank() && validPhones.isEmpty()) {
            _uiState.update { it.copy(validationErrorRes = R.string.contact_validation_error) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, validationErrorRes = null) }

            val input = ContactInput(
                displayName = trimmedName,
                phones = validPhones.map { it.copy(number = it.number.trim()) },
                emails = state.emails.filter { it.address.trim().isNotBlank() }.map { it.copy(address = it.address.trim()) },
                organization = state.organization.trim().takeIf { it.isNotBlank() }
            )

            if (state.isNewContact) {
                val result = contactsRepository.createContact(input)
                if (result.isSuccess) {
                    val newId = result.getOrNull() ?: 0L
                    _uiState.update { it.copy(isLoading = false, isSaved = true) }
                    onSuccess(newId)
                } else {
                    _uiState.update { it.copy(isLoading = false, validationErrorRes = R.string.snackbar_contact_error) }
                }
            } else {
                val original = state.originalDetail ?: ContactDetail(
                    id = state.contactId,
                    lookupKey = state.lookupKey,
                    displayName = state.displayName
                )
                val result = contactsRepository.updateContact(state.contactId, original, input)
                if (result.isSuccess) {
                    _uiState.update { it.copy(isLoading = false, isSaved = true) }
                    onSuccess(state.contactId)
                } else {
                    _uiState.update { it.copy(isLoading = false, validationErrorRes = R.string.snackbar_contact_error) }
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            repository: ContactsRepository,
            initialContact: ContactDetail? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ContactEditViewModel(repository, initialContact) as T
            }
        }
    }
}
