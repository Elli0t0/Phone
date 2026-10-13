package io.github.elli0t0.phone.feature.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.data.contacts.ContactsRepository
import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import io.github.elli0t0.phone.data.contacts.model.DuplicateGroup
import io.github.elli0t0.phone.data.contacts.util.ContactGroupingUtil
import io.github.elli0t0.phone.data.contacts.util.ContactSearchMatcher
import io.github.elli0t0.phone.data.contacts.util.DuplicateContactFinder
import io.github.elli0t0.phone.data.favorites.FavoritesRepository
import io.github.elli0t0.phone.data.settings.SettingsRepository
import io.github.elli0t0.phone.data.settings.model.AppSettings
import io.github.elli0t0.phone.data.settings.model.ContactNameFormat
import io.github.elli0t0.phone.data.settings.model.ContactSortOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactsViewModel(
    private val contactsRepository: ContactsRepository,
    private val favoritesRepository: FavoritesRepository? = null,
    private val settingsRepository: SettingsRepository? = null,
    coroutineScope: kotlinx.coroutines.CoroutineScope? = null
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _userMessageRes = MutableStateFlow<Int?>(null)
    private val _duplicateGroups = MutableStateFlow<List<DuplicateGroup>>(emptyList())
    private val _showDuplicates = MutableStateFlow(false)
    private val _isFindingDuplicates = MutableStateFlow(false)
    private val _pendingMergeGroup = MutableStateFlow<DuplicateGroup?>(null)
    private val _isMerging = MutableStateFlow(false)

    // Raw contacts (before name-format display changes) for duplicate detection.
    private var latestContacts: List<ContactSummary> = emptyList()

    private val activeScope = coroutineScope ?: try {
        viewModelScope
    } catch (_: Exception) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
    }

    private val favoritesFlow = favoritesRepository?.getFavoritesFlow(
        contactsRepository.getContactsFlow()
    ) ?: flowOf(emptyList())

    private val settingsFlow = settingsRepository?.settingsFlow ?: flowOf(AppSettings())

    val uiState: StateFlow<ContactsUiState> = combine(
        contactsRepository.getContactsFlow(),
        favoritesFlow,
        settingsFlow,
        _searchQuery,
        _userMessageRes,
        _duplicateGroups,
        _showDuplicates,
        _isFindingDuplicates,
        _pendingMergeGroup,
        _isMerging
    ) { contacts, favorites, settings, query, messageRes, duplicateGroups, showDuplicates, isFindingDuplicates, pendingMergeGroup, isMerging ->
        latestContacts = contacts
        val formattedContacts = contacts.map { contact ->
            val parts = contact.displayName.trim().split("\\s+".toRegex())
            val formattedName = if (settings.contactNameFormat == ContactNameFormat.LAST_NAME_FIRST && parts.size > 1) {
                "${parts.last()}, ${parts.dropLast(1).joinToString(" ")}"
            } else {
                contact.displayName
            }
            contact.copy(displayName = formattedName)
        }.sortedWith { a, b ->
            if (settings.contactSortOrder == ContactSortOrder.LAST_NAME) {
                val lastA = a.displayName.trim().split("\\s+".toRegex()).lastOrNull().orEmpty()
                val lastB = b.displayName.trim().split("\\s+".toRegex()).lastOrNull().orEmpty()
                lastA.compareTo(lastB, ignoreCase = true)
            } else {
                a.displayName.compareTo(b.displayName, ignoreCase = true)
            }
        }

        val filtered = ContactSearchMatcher.filter(formattedContacts, query)
        val grouped = ContactGroupingUtil.groupContacts(filtered)

        ContactsUiState(
            searchQuery = query,
            allContacts = formattedContacts,
            favorites = favorites,
            filteredContacts = filtered,
            groupedContacts = grouped,
            isLoading = false,
            userMessageRes = messageRes,
            duplicateGroups = duplicateGroups,
            showDuplicates = showDuplicates,
            isFindingDuplicates = isFindingDuplicates,
            pendingMergeGroup = pendingMergeGroup,
            isMerging = isMerging
        )
    }.stateIn(
        scope = activeScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ContactsUiState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
    }

    fun showMessage(messageRes: Int) {
        _userMessageRes.value = messageRes
    }

    fun clearMessage() {
        _userMessageRes.value = null
    }

    fun findDuplicates() {
        activeScope.launch {
            _isFindingDuplicates.value = true
            _showDuplicates.value = true
            try {
                val numbers = contactsRepository.getAllContactNumbers()
                _duplicateGroups.value = DuplicateContactFinder.findDuplicates(latestContacts, numbers)
            } catch (_: Exception) {
                _showDuplicates.value = false
                _userMessageRes.value = R.string.snackbar_contact_error
            } finally {
                _isFindingDuplicates.value = false
            }
        }
    }

    fun hideDuplicates() {
        _showDuplicates.value = false
        _pendingMergeGroup.value = null
    }

    fun requestMerge(group: DuplicateGroup) {
        _pendingMergeGroup.value = group
    }

    fun dismissMergeDialog() {
        _pendingMergeGroup.value = null
    }

    /**
     * Merges the pending group only after the user confirmed it in the dialog.
     * Keeps the first contact, moves the others' numbers and emails into it,
     * then deletes the merged contacts.
     */
    fun confirmMerge() {
        val group = _pendingMergeGroup.value ?: return
        if (group.contacts.size < 2) {
            _pendingMergeGroup.value = null
            return
        }
        activeScope.launch {
            _isMerging.value = true
            try {
                val keep = group.contacts.first()
                val mergeIds = group.contacts.drop(1).map { it.id }
                val result = contactsRepository.mergeContacts(keep.id, mergeIds)
                if (result.isSuccess) {
                    _pendingMergeGroup.value = null
                    _showDuplicates.value = false
                    _duplicateGroups.value = emptyList()
                    _userMessageRes.value = R.string.snackbar_contacts_merged
                } else {
                    _pendingMergeGroup.value = null
                    _userMessageRes.value = R.string.snackbar_contact_error
                }
            } catch (_: Exception) {
                _pendingMergeGroup.value = null
                _userMessageRes.value = R.string.snackbar_contact_error
            } finally {
                _isMerging.value = false
            }
        }
    }

    companion object {
        fun provideFactory(
            repository: ContactsRepository,
            favoritesRepository: FavoritesRepository? = null,
            settingsRepository: SettingsRepository? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ContactsViewModel(repository, favoritesRepository, settingsRepository) as T
            }
        }
    }
}
