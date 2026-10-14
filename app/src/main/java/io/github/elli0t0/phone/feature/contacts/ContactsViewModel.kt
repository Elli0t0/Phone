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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ContactsViewModel(
    private val contactsRepository: ContactsRepository,
    private val favoritesRepository: FavoritesRepository? = null,
    private val settingsRepository: SettingsRepository? = null,
    coroutineScope: kotlinx.coroutines.CoroutineScope? = null,
    // Injectable so tests can run the transform on the test scheduler;
    // production uses Default.
    private val transformDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _userMessageRes = MutableStateFlow<Int?>(null)
    private val _duplicateGroups = MutableStateFlow<List<DuplicateGroup>>(emptyList())
    private val _showDuplicates = MutableStateFlow(false)
    private val _isFindingDuplicates = MutableStateFlow(false)
    private val _pendingMergeGroup = MutableStateFlow<DuplicateGroup?>(null)
    private val _isMerging = MutableStateFlow(false)
    private val _profileContact = MutableStateFlow<ContactSummary?>(null)
    private val _duplicatesCardDismissed = MutableStateFlow(false)

    // Raw contacts (before name-format display changes) for duplicate detection.
    // Written by the uiState transform (now on Default) and read by
    // findDuplicates: volatile for safe cross-thread visibility.
    @Volatile
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

    init {
        // Load the device owner's profile contact once for the "My profile" row.
        activeScope.launch {
            _profileContact.value = try {
                contactsRepository.getProfileContact()
            } catch (_: Exception) {
                null
            }
        }
        // Silent duplicate scan so the duplicates card can appear on top of
        // the list without opening the duplicates screen.
        activeScope.launch {
            try {
                val contacts = contactsRepository.getContactsFlow().first()
                val numbers = contactsRepository.getAllContactNumbers()
                _duplicateGroups.value = DuplicateContactFinder.findDuplicates(contacts, numbers)
            } catch (_: Exception) {
            }
        }
    }

    // kotlinx-coroutines 1.8.1 only provides combine() overloads for up to 5 flows,
    // so the 10 inputs are grouped into two 5-flow combines first.
    private data class ContactsData(
        val contacts: List<ContactSummary>,
        val favorites: List<ContactSummary>,
        val settings: AppSettings,
        val query: String,
        val messageRes: Int?
    )

    private data class ContactsUiExtras(
        val duplicateGroups: List<DuplicateGroup>,
        val showDuplicates: Boolean,
        val isFindingDuplicates: Boolean,
        val pendingMergeGroup: DuplicateGroup?,
        val isMerging: Boolean
    )

    private val contactsDataFlow = combine(
        contactsRepository.getContactsFlow(),
        favoritesFlow,
        settingsFlow,
        _searchQuery,
        _userMessageRes
    ) { contacts, favorites, settings, query, messageRes ->
        ContactsData(contacts, favorites, settings, query, messageRes)
    }

    private val contactsUiExtrasFlow = combine(
        _duplicateGroups,
        _showDuplicates,
        _isFindingDuplicates,
        _pendingMergeGroup,
        _isMerging
    ) { duplicateGroups, showDuplicates, isFindingDuplicates, pendingMergeGroup, isMerging ->
        ContactsUiExtras(duplicateGroups, showDuplicates, isFindingDuplicates, pendingMergeGroup, isMerging)
    }

    val uiState: StateFlow<ContactsUiState> = combine(
        contactsDataFlow,
        contactsUiExtrasFlow,
        _profileContact,
        _duplicatesCardDismissed
    ) { data, extras, profileContact, duplicatesCardDismissed ->
        latestContacts = data.contacts
        val formattedContacts = data.contacts.map { contact ->
            val parts = contact.displayName.trim().split("\\s+".toRegex())
            val formattedName = if (data.settings.contactNameFormat == ContactNameFormat.LAST_NAME_FIRST && parts.size > 1) {
                "${parts.last()}, ${parts.dropLast(1).joinToString(" ")}"
            } else {
                contact.displayName
            }
            contact.copy(displayName = formattedName)
        }.sortedWith { a, b ->
            if (data.settings.contactSortOrder == ContactSortOrder.LAST_NAME) {
                val lastA = a.displayName.trim().split("\\s+".toRegex()).lastOrNull().orEmpty()
                val lastB = b.displayName.trim().split("\\s+".toRegex()).lastOrNull().orEmpty()
                lastA.compareTo(lastB, ignoreCase = true)
            } else {
                a.displayName.compareTo(b.displayName, ignoreCase = true)
            }
        }

        val filtered = ContactSearchMatcher.filter(formattedContacts, data.query)
        val grouped = ContactGroupingUtil.groupContacts(filtered)

        ContactsUiState(
            searchQuery = data.query,
            allContacts = formattedContacts,
            favorites = data.favorites,
            filteredContacts = filtered,
            groupedContacts = grouped,
            isLoading = false,
            userMessageRes = data.messageRes,
            duplicateGroups = extras.duplicateGroups,
            showDuplicates = extras.showDuplicates,
            profileContact = profileContact,
            showDuplicatesCard = extras.duplicateGroups.isNotEmpty() && !duplicatesCardDismissed,
            isFindingDuplicates = extras.isFindingDuplicates,
            pendingMergeGroup = extras.pendingMergeGroup,
            isMerging = extras.isMerging
        )
    }
    // Name formatting + search filter + grouping run on
    // transformDispatcher: the transform touches every contact and must
    // never block the main thread.
    .flowOn(transformDispatcher)
    .stateIn(
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

    fun showDuplicates() {
        _showDuplicates.value = true
    }

    fun dismissDuplicatesCard() {
        _duplicatesCardDismissed.value = true
    }

    fun requestMerge(group: DuplicateGroup) {
        _pendingMergeGroup.value = group
    }

    fun dismissMergeDialog() {
        _pendingMergeGroup.value = null
    }

    /**
     * Merges the pending group only after the user confirmed it in the dialog.
     * Keeps the contact the user chose, moves the others' numbers and emails
     * into it, then deletes the merged contacts.
     */
    fun confirmMerge(keepContactId: Long) {
        val group = _pendingMergeGroup.value ?: return
        if (group.contacts.size < 2) {
            _pendingMergeGroup.value = null
            return
        }
        activeScope.launch {
            _isMerging.value = true
            try {
                val keep = group.contacts.firstOrNull { it.id == keepContactId }
                    ?: group.contacts.first()
                val mergeIds = group.contacts.filter { it.id != keep.id }.map { it.id }
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
