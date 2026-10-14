package io.github.elli0t0.phone.feature.recents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.common.PhoneNumberMatcher
import io.github.elli0t0.phone.data.calllog.CallLogRepository
import io.github.elli0t0.phone.data.calllog.model.CallLogEntry
import io.github.elli0t0.phone.data.calllog.model.CallType
import io.github.elli0t0.phone.data.calllog.util.CallLogGroupingHelper
import io.github.elli0t0.phone.data.contacts.ContactsRepository
import io.github.elli0t0.phone.data.favorites.FavoritesRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)

class RecentsViewModel(
    private val callLogRepository: CallLogRepository,
    private val favoritesRepository: FavoritesRepository,
    private val contactsRepository: ContactsRepository,
    coroutineScope: CoroutineScope? = null,
    // Injectable so tests can run the transform on the test scheduler;
    // production uses Default.
    private val transformDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val activeScope = coroutineScope ?: try {
        viewModelScope
    } catch (_: Exception) {
        null
    }

    private val _selectedFilter = MutableStateFlow(CallFilter.ALL)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    private val _expandedCallId = MutableStateFlow<Long?>(null)
    private val _userMessageRes = MutableStateFlow<Int?>(null)
    private val _reloadTrigger = MutableStateFlow(0L)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val favoritesFlow = favoritesRepository.getFavoritesFlow(
        contactsRepository.getContactsFlow()
    )

    private val callLogsFlow = _reloadTrigger.flatMapLatest {
        callLogRepository.getCallLogsFlow()
            .catch { e ->
                android.util.Log.e("RecentsViewModel", "Error in callLogsFlow", e)
                _errorMessage.value = e.localizedMessage ?: "Unknown error"
                emit(emptyList())
            }
    }

    private data class LogData(
        val calls: List<CallLogEntry>,
        val favorites: List<io.github.elli0t0.phone.data.contacts.model.ContactSummary>,
        val isLoading: Boolean,
        val errorMsg: String?
    )

    private val logDataFlow = combine(
        callLogsFlow,
        favoritesFlow,
        callLogRepository.isLoadingFlow,
        _errorMessage
    ) { calls, favorites, isLoading, errorMsg ->
        LogData(calls, favorites, isLoading, errorMsg)
    }

    private data class FilterState(
        val filter: CallFilter,
        val query: String,
        val date: LocalDate?,
        val expandedId: Long?,
        val messageRes: Int?
    )

    private val filterStateFlow = combine(
        _selectedFilter,
        _searchQuery,
        _selectedDate,
        _expandedCallId,
        _userMessageRes
    ) { filter, query, date, expandedId, messageRes ->
        FilterState(filter, query, date, expandedId, messageRes)
    }

    val uiState: StateFlow<RecentsUiState> = combine(
        logDataFlow,
        filterStateFlow
    ) { logData, filterState ->
        val filteredCalls = filterCalls(logData.calls, filterState.filter, filterState.query, filterState.date)
        val grouped = CallLogGroupingHelper.groupConsecutiveCalls(filteredCalls)
        val daySections = CallLogGroupingHelper.organizeIntoDaySections(grouped)

        RecentsUiState(
            daySections = daySections,
            favorites = logData.favorites,
            selectedFilter = filterState.filter,
            searchQuery = filterState.query,
            selectedDate = filterState.date,
            expandedCallId = filterState.expandedId,
            isSearching = filterState.query.isNotBlank(),
            isLoading = logData.isLoading && logData.calls.isEmpty(),
            userMessageRes = filterState.messageRes,
            errorMessage = logData.errorMsg
        )
    }
    // Filter + group + sort run on transformDispatcher: the transform
    // touches every call-log row and must never block the main thread.
    .flowOn(transformDispatcher)
    .stateIn(
        scope = activeScope ?: CoroutineScope(kotlinx.coroutines.Dispatchers.Main.immediate),
        started = SharingStarted.WhileSubscribed(5000),
        // Loading until the first emission: a fresh ViewModel must show the
        // skeleton, never the empty state, while the first load runs.
        initialValue = RecentsUiState(isLoading = true)
    )

    private fun filterCalls(
        calls: List<CallLogEntry>,
        filter: CallFilter,
        query: String,
        date: LocalDate?
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
                    PhoneNumberMatcher.matchesQuery(call.number, trimmedQuery)
            }

            val matchesDate = date == null ||
                LocalDate.ofInstant(Instant.ofEpochMilli(call.date), ZoneId.systemDefault()) == date

            matchesFilter && matchesQuery && matchesDate
        }
    }

    fun onFilterSelected(filter: CallFilter) {
        _selectedFilter.value = filter
    }

    fun onDateSelected(date: LocalDate?) {
        _selectedDate.value = date
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

    fun reload() {
        _errorMessage.value = null
        _reloadTrigger.value = System.currentTimeMillis()
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
