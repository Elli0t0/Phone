package io.github.elli0t0.phone.feature.recents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.data.calllog.CallLogRepository
import io.github.elli0t0.phone.data.calllog.model.CallLogEntry
import io.github.elli0t0.phone.data.calllog.model.CallType
import io.github.elli0t0.phone.data.calllog.util.CallLogGroupingHelper
import io.github.elli0t0.phone.data.contacts.ContactsRepository
import io.github.elli0t0.phone.data.favorites.FavoritesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecentsViewModel(
    private val callLogRepository: CallLogRepository,
    private val favoritesRepository: FavoritesRepository,
    private val contactsRepository: ContactsRepository,
    coroutineScope: CoroutineScope? = null
) : ViewModel() {

    private val activeScope = coroutineScope ?: try {
        viewModelScope
    } catch (_: Exception) {
        null
    }

    private val _selectedFilter = MutableStateFlow(CallFilter.ALL)
    private val _searchQuery = MutableStateFlow("")
    private val _expandedCallId = MutableStateFlow<Long?>(null)
    private val _userMessageRes = MutableStateFlow<Int?>(null)

    private val favoritesFlow = favoritesRepository.getFavoritesFlow(
        contactsRepository.getContactsFlow()
    )

    val uiState: StateFlow<RecentsUiState> = combine(
        combine(callLogRepository.getCallLogsFlow(), favoritesFlow, ::Pair),
        _selectedFilter,
        _searchQuery,
        _expandedCallId,
        _userMessageRes
    ) { (calls, favorites), filter, query, expandedId, messageRes ->
        val filteredCalls = filterCalls(calls, filter, query)
        val grouped = CallLogGroupingHelper.groupConsecutiveCalls(filteredCalls)
        val daySections = CallLogGroupingHelper.organizeIntoDaySections(grouped)

        RecentsUiState(
            daySections = daySections,
            favorites = favorites,
            selectedFilter = filter,
            searchQuery = query,
            expandedCallId = expandedId,
            isSearching = query.isNotBlank(),
            userMessageRes = messageRes
        )
    }.stateIn(
        scope = activeScope ?: CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate),
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecentsUiState()
    )

    private fun filterCalls(
        calls: List<CallLogEntry>,
        filter: CallFilter,
        query: String
    ): List<CallLogEntry> {
        val trimmedQuery = query.trim().lowercase()

        return calls.filter { call ->
            val matchesFilter = when (filter) {
                CallFilter.ALL -> true
                CallFilter.MISSED -> call.type == CallType.Missed || call.type == CallType.Rejected
                CallFilter.INCOMING -> call.type == CallType.Incoming
                CallFilter.OUTGOING -> call.type == CallType.Outgoing
            }

            val matchesQuery = if (trimmedQuery.isEmpty()) {
                true
            } else {
                call.displayTitle.lowercase().contains(trimmedQuery) ||
                    call.number.contains(trimmedQuery)
            }

            matchesFilter && matchesQuery
        }
    }

    fun onFilterSelected(filter: CallFilter) {
        _selectedFilter.value = filter
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onToggleExpandCall(callId: Long) {
        _expandedCallId.value = if (_expandedCallId.value == callId) null else callId
    }

    fun deleteCall(callId: Long) {
        activeScope?.launch {
            val success = callLogRepository.deleteCall(callId)
            if (success) {
                if (_expandedCallId.value == callId) {
                    _expandedCallId.value = null
                }
                _userMessageRes.value = R.string.snackbar_call_deleted
            }
        }
    }

    fun clearUserMessage() {
        _userMessageRes.value = null
    }

    override fun onCleared() {
        super.onCleared()
        // If activeScope is a custom Job or scope, or tied to viewModelScope, ensure clean up
    }

    companion object {
        fun provideFactory(
            callLogRepository: CallLogRepository,
            favoritesRepository: FavoritesRepository,
            contactsRepository: ContactsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return RecentsViewModel(
                    callLogRepository,
                    favoritesRepository,
                    contactsRepository
                ) as T
            }
        }
    }
}
