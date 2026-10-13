package io.github.elli0t0.phone.feature.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.data.contacts.ContactsRepository
import io.github.elli0t0.phone.data.favorites.FavoritesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ContactDetailViewModel(
    private val contactsRepository: ContactsRepository,
    private val favoritesRepository: FavoritesRepository,
    private val contactId: Long,
    private val lookupKey: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactDetailUiState(isLoading = true))
    val uiState: StateFlow<ContactDetailUiState> = _uiState.asStateFlow()

    init {
        loadContact()
        observeFavorite()
    }

    private fun loadContact() {
        viewModelScope.launch {
            contactsRepository.getContactDetail(lookupKey, contactId).collect { detail ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        contact = detail
                    )
                }
            }
        }
    }

    private fun observeFavorite() {
        if (lookupKey.isNotBlank()) {
            viewModelScope.launch {
                favoritesRepository.isFavoriteFlow(lookupKey).collect { isFav ->
                    _uiState.update { it.copy(isFavorite = isFav) }
                }
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val currentKey = _uiState.value.contact?.lookupKey ?: lookupKey
            if (currentKey.isNotBlank()) {
                favoritesRepository.toggleFavorite(currentKey, contactId)
            }
        }
    }

    fun setDeleteDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showDeleteDialog = visible) }
    }

    fun deleteContact(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showDeleteDialog = false) }
            val result = contactsRepository.deleteContact(contactId, lookupKey)
            if (result.isSuccess) {
                // Also clean up favorite if any
                favoritesRepository.removeFavorite(lookupKey)
                _uiState.update { it.copy(isDeleted = true, isLoading = false) }
                onDeleted()
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessageRes = R.string.snackbar_contact_error
                    )
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            repository: ContactsRepository,
            favoritesRepository: FavoritesRepository,
            contactId: Long,
            lookupKey: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ContactDetailViewModel(
                    repository,
                    favoritesRepository,
                    contactId,
                    lookupKey
                ) as T
            }
        }
    }
}
