package io.github.elli0t0.phone.feature.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.elli0t0.phone.core.common.PhoneNumberMatcher
import io.github.elli0t0.phone.data.calllog.CallLogRepository
import io.github.elli0t0.phone.data.calllog.model.CallLogEntry
import io.github.elli0t0.phone.data.contacts.ContactsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Full call history for one contact, rendered in a LazyColumn. This
// version filters the shared call-log flow (same matching as details);
// provider-level paging lands in the next version.
class ContactHistoryViewModel(
    private val contactsRepository: ContactsRepository,
    private val callLogRepository: CallLogRepository,
    private val contactId: Long,
    private val lookupKey: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactHistoryUiState(isLoading = true))
    val uiState: StateFlow<ContactHistoryUiState> = _uiState.asStateFlow()
    private var latestCallLogs: List<CallLogEntry> = emptyList()
    private var effectiveLookupKey: String = lookupKey

    init {
        viewModelScope.launch {
            contactsRepository.getContactDetail(lookupKey, contactId).collect { detail ->
                if (detail == null) {
                    _uiState.update { it.copy(isLoading = false) }
                    return@collect
                }
                val loadedKey = detail.lookupKey.takeIf { it.isNotBlank() }
                if (loadedKey != null) effectiveLookupKey = loadedKey
                _uiState.update { it.copy(contactName = detail.displayName) }
                refreshHistory(detail.phones.map { it.number })
            }
        }
        viewModelScope.launch {
            callLogRepository.getCallLogsFlow().collect { logs ->
                latestCallLogs = logs
                val contact = _uiState.value.contactName
                if (contact.isNotBlank()) {
                    // Numbers come from the contact flow; re-filter on new logs.
                    refreshHistory(currentNumbers)
                }
            }
        }
    }

    private var currentNumbers: List<String> = emptyList()

    private fun refreshHistory(numbers: List<String>) {
        currentNumbers = numbers
        val key = effectiveLookupKey
        val history = latestCallLogs.filter { entry ->
            (key.isNotBlank() && entry.contactLookupKey == key) ||
                (contactId > 0 && entry.contactId == contactId) ||
                numbers.any { PhoneNumberMatcher.matches(it, entry.number) }
        }.sortedByDescending { it.date }
        _uiState.update { it.copy(entries = history, isLoading = false) }
    }

    // No-op until provider paging arrives: the whole history is loaded.
    fun loadMoreIfNeeded(@Suppress("UNUSED_PARAMETER") lastVisibleIndex: Int) = Unit

    companion object {
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
