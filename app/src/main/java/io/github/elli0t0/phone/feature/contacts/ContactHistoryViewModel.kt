package io.github.elli0t0.phone.feature.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.elli0t0.phone.data.calllog.CallLogRepository
import io.github.elli0t0.phone.data.contacts.ContactsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Paged call history for one contact: the contact's numbers are loaded
// once, then pages of 50 matches are fetched on demand. `offset` counts
// matches already shown, so pages never repeat or skip a call.
class ContactHistoryViewModel(
    private val contactsRepository: ContactsRepository,
    private val callLogRepository: CallLogRepository,
    private val contactId: Long,
    private val lookupKey: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactHistoryUiState(isLoading = true))
    val uiState: StateFlow<ContactHistoryUiState> = _uiState.asStateFlow()
    private var numbers: List<String> = emptyList()
    private var firstPageLoaded = false

    init {
        viewModelScope.launch {
            contactsRepository.getContactDetail(lookupKey, contactId).collect { detail ->
                if (detail == null) {
                    _uiState.update { it.copy(isLoading = false) }
                    return@collect
                }
                _uiState.update { it.copy(contactName = detail.displayName) }
                if (!firstPageLoaded) {
                    firstPageLoaded = true
                    numbers = detail.phones.map { it.number }
                    loadPage()
                }
            }
        }
    }

    fun loadMoreIfNeeded(lastVisibleIndex: Int) {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore || state.entries.isEmpty()) return
        if (lastVisibleIndex < state.entries.size - LOAD_AHEAD) return
        loadPage()
    }

    private fun loadPage() {
        val state = _uiState.value
        val isFirstPage = state.entries.isEmpty()
        _uiState.update { it.copy(isLoading = isFirstPage, isLoadingMore = !isFirstPage) }
        viewModelScope.launch {
            val page = callLogRepository.getCallHistoryPage(numbers, PAGE_SIZE, state.entries.size)
            _uiState.update {
                it.copy(
                    entries = it.entries + page.entries,
                    isLoading = false,
                    isLoadingMore = false,
                    hasMore = page.hasMore
                )
            }
        }
    }

    companion object {
        const val PAGE_SIZE = 50
        private const val LOAD_AHEAD = 5

        fun provideFactory(
            contactsRepository: ContactsRepository,
            callLogRepository: CallLogRepository,
            contactId: Long,
            lookupKey: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ContactHistoryViewModel(
                    contactsRepository,
                    callLogRepository,
                    contactId,
                    lookupKey
                ) as T
            }
        }
    }
}
