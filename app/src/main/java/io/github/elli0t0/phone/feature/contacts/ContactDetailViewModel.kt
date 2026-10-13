package io.github.elli0t0.phone.feature.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.data.contacts.ContactsRepository
import io.github.elli0t0.phone.data.favorites.ContactSimRepository
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
    private val lookupKey: String,
    private val contactSimRepository: ContactSimRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactDetailUiState(isLoading = true))
    val uiState: StateFlow<ContactDetailUiState> = _uiState.asStateFlow()
    private var effectiveLookupKey: String = lookupKey
    private var callingSimJob: kotlinx.coroutines.Job? = null

    init {
        loadContact()
        observeFavorite()
        observeCallingSim()
    }

    private fun observeCallingSim() {
        val repo = contactSimRepository ?: return
        val key = effectiveLookupKey
        if (key.isBlank()) return
        callingSimJob?.cancel()
        callingSimJob = viewModelScope.launch {
            repo.getSimIdFlow(key).collect { simId ->
                _uiState.update { it.copy(callingSimId = simId) }
            }
        }
    }

    fun setCallingSim(simId: String?) {
        val repo = contactSimRepository ?: return
        val key = _uiState.value.contact?.lookupKey?.takeIf { it.isNotBlank() } ?: effectiveLookupKey
        if (key.isBlank()) return
        viewModelScope.launch {
            repo.setSimId(key, simId)
        }
    }

    private fun loadContact() {
        viewModelScope.launch {
            contactsRepository.getContactDetail(lookupKey, contactId).collect { detail ->
                val loadedKey = detail?.lookupKey?.takeIf { it.isNotBlank() }
                if (loadedKey != null && loadedKey != effectiveLookupKey) {
                    effectiveLookupKey = loadedKey
                    observeCallingSim()
                }
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

    fun setPhotoPreviewVisible(visible: Boolean) {
        _uiState.update { it.copy(showPhotoPreview = visible) }
    }

    fun setPhotoOptionsDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showPhotoOptionsDialog = visible) }
    }

    fun onPhotoPickedForPreview(bytes: ByteArray) {
        _uiState.update { it.copy(pendingPhotoBytes = bytes, showPhotoOptionsDialog = false) }
    }

    fun onDiscardPendingPhoto() {
        _uiState.update { it.copy(pendingPhotoBytes = null) }
    }

    fun onConfirmSavePhoto() {
        val bytes = _uiState.value.pendingPhotoBytes ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(pendingPhotoBytes = null, isLoading = true) }
            val result = contactsRepository.saveContactPhoto(contactId, bytes)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    infoMessageRes = if (result.isSuccess) R.string.snackbar_photo_updated else null,
                    errorMessageRes = if (result.isFailure) R.string.snackbar_contact_error else null
                )
            }
        }
    }

    fun onRemovePhoto() {
        viewModelScope.launch {
            _uiState.update { it.copy(showPhotoOptionsDialog = false, isLoading = true) }
            val result = contactsRepository.removeContactPhoto(contactId)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    infoMessageRes = if (result.isSuccess) R.string.snackbar_photo_removed else null,
                    errorMessageRes = if (result.isFailure) R.string.snackbar_contact_error else null
                )
            }
        }
    }

    fun onRingtoneSelected(ringtoneUri: android.net.Uri?) {
        viewModelScope.launch {
            val uriString = ringtoneUri?.toString()
            val result = contactsRepository.updateContactRingtone(contactId, uriString)
            _uiState.update {
                it.copy(
                    infoMessageRes = if (result.isSuccess) R.string.snackbar_ringtone_updated else null,
                    errorMessageRes = if (result.isFailure) R.string.snackbar_contact_error else null
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(infoMessageRes = null, errorMessageRes = null) }
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
            lookupKey: String,
            contactSimRepository: ContactSimRepository? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ContactDetailViewModel(
                    repository,
                    favoritesRepository,
                    contactId,
                    lookupKey,
                    contactSimRepository
                ) as T
            }
        }
    }
}
