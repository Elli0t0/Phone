package io.github.elli0t0.phone.feature.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.common.Selection
import io.github.elli0t0.phone.data.contacts.ContactsRepository
import io.github.elli0t0.phone.data.contacts.model.ContactMergeOutcome
import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import io.github.elli0t0.phone.data.contacts.model.DeleteContactsResult
import io.github.elli0t0.phone.data.contacts.model.DuplicateGroup
import io.github.elli0t0.phone.data.contacts.util.ContactGroupingUtil
import io.github.elli0t0.phone.data.contacts.util.ContactMergeUtil
import io.github.elli0t0.phone.data.contacts.util.ContactSearchMatcher
import io.github.elli0t0.phone.data.contacts.util.DuplicateContactFinder
import io.github.elli0t0.phone.data.favorites.FavoritesRepository
import io.github.elli0t0.phone.data.settings.SettingsRepository
import io.github.elli0t0.phone.data.settings.model.AppSettings
import io.github.elli0t0.phone.data.settings.model.ContactNameFormat
import io.github.elli0t0.phone.data.settings.model.ContactSortOrder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Compiled once: the old code recompiled this regex twice per contact
// on every list transform (plus twice per sort comparison).
private val whitespaceRegex = "\\s+".toRegex()

class ContactsViewModel(
    private val contactsRepository: ContactsRepository,
    private val favoritesRepository: FavoritesRepository? = null,
    private val settingsRepository: SettingsRepository? = null,
    coroutineScope: kotlinx.coroutines.CoroutineScope? = null,
    // Application scope for merges: leaving the duplicates screen
    // must not cancel a merge (the list VM is activity-scoped, but
    // its viewModelScope still ends with the activity).
    applicationScope: kotlinx.coroutines.CoroutineScope? = null,
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
    private val _allDuplicatesMerged = MutableStateFlow(false)
    private val _lastMergeBatch = MutableStateFlow<MergeBatch?>(null)

    /** One finished merge batch: what can be undone, how many groups merged, of how many. */
    data class MergeBatch(
        val linkedOutcomes: List<ContactMergeOutcome>,
        val mergedCount: Int,
        val totalCount: Int
    )

    /** Multi-select state, kept in one flow so the uiState combine stays at 5 inputs. */
    private data class SelectionUi(
        val selection: Selection = Selection(),
        val isDeleting: Boolean = false,
        val deleteReport: DeleteContactsResult? = null
    )

    private val _selectionUi = MutableStateFlow(SelectionUi())

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

    private val mergeScope = applicationScope ?: activeScope

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
        // the list without opening the duplicates screen. A fresh
        // snapshot, not the cached flow: the flow may still predate
        // a merge the provider has already applied.
        activeScope.launch {
            try {
                refreshDuplicates()
                migrateDismissedHash(_duplicateGroups.value)
            } catch (_: Exception) {
            }
        }
    }

    // kotlinx-coroutines 1.8.1 only provides combine() overloads for up to 5 flows,
    // so the inputs are grouped into combines of at most 5 flows.
    private data class ContactsListData(
        val formattedContacts: List<ContactSummary>,
        val filteredContacts: List<ContactSummary>,
        val groupedContacts: Map<String, List<ContactSummary>>,
        val favorites: List<ContactSummary>,
        val query: String,
        val duplicatesDismissedHash: String?
    )

    private data class ContactsUiExtras(
        val duplicateGroups: List<DuplicateGroup>,
        val showDuplicates: Boolean,
        val isFindingDuplicates: Boolean,
        val pendingMergeGroup: DuplicateGroup?,
        val isMerging: Boolean
    )

    // List transform (format + sort + filter + group): runs only when the
    // contacts, favorites, settings, or query change — never when a
    // snackbar, the duplicates card, or a merge dialog changes.
    private val contactsListFlow = combine(
        contactsRepository.getContactsFlow(),
        favoritesFlow,
        settingsFlow,
        _searchQuery
    ) { contacts, favorites, settings, query ->
        latestContacts = contacts
        val formattedContacts = contacts.map { contact ->
            val parts = contact.displayName.trim().split(whitespaceRegex)
            val formattedName = if (settings.contactNameFormat == ContactNameFormat.LAST_NAME_FIRST && parts.size > 1) {
                "${parts.last()}, ${parts.dropLast(1).joinToString(" ")}"
            } else {
                contact.displayName
            }
            contact.copy(displayName = formattedName)
        }.sortedWith { a, b ->
            if (settings.contactSortOrder == ContactSortOrder.LAST_NAME) {
                val lastA = a.displayName.trim().split(whitespaceRegex).lastOrNull().orEmpty()
                val lastB = b.displayName.trim().split(whitespaceRegex).lastOrNull().orEmpty()
                lastA.compareTo(lastB, ignoreCase = true)
            } else {
                a.displayName.compareTo(b.displayName, ignoreCase = true)
            }
        }

        val filtered = ContactSearchMatcher.filter(formattedContacts, query)
        ContactsListData(
            formattedContacts = formattedContacts,
            filteredContacts = filtered,
            groupedContacts = ContactGroupingUtil.groupContacts(filtered),
            favorites = favorites,
            query = query,
            duplicatesDismissedHash = settings.duplicatesDismissedHash
        )
    }
    // Name formatting + search filter + grouping run on
    // transformDispatcher: the transform touches every contact and must
    // never block the main thread.
    .flowOn(transformDispatcher)

    private val contactsUiExtrasFlow = combine(
        _duplicateGroups,
        _showDuplicates,
        _isFindingDuplicates,
        _pendingMergeGroup,
        _isMerging
    ) { duplicateGroups, showDuplicates, isFindingDuplicates, pendingMergeGroup, isMerging ->
        ContactsUiExtras(duplicateGroups, showDuplicates, isFindingDuplicates, pendingMergeGroup, isMerging)
    }

    private data class MergeUiState(
        val allDuplicatesMerged: Boolean,
        val mergeBatch: MergeBatch?
    )

    private val mergeUiFlow = combine(
        _allDuplicatesMerged,
        _lastMergeBatch
    ) { allDuplicatesMerged, mergeBatch ->
        MergeUiState(allDuplicatesMerged, mergeBatch)
    }

    private val baseUiState: StateFlow<ContactsUiState> = combine(
        contactsListFlow,
        contactsUiExtrasFlow,
        mergeUiFlow,
        _profileContact,
        _userMessageRes
    ) { list, extras, mergeUi, profileContact, messageRes ->
        ContactsUiState(
            searchQuery = list.query,
            allContacts = list.formattedContacts,
            favorites = list.favorites,
            filteredContacts = list.filteredContacts,
            groupedContacts = list.groupedContacts,
            isLoading = false,
            userMessageRes = messageRes,
            duplicateGroups = extras.duplicateGroups,
            showDuplicates = extras.showDuplicates,
            profileContact = profileContact,
            // The banner shows only while the current duplicate set
            // differs from the set the user dismissed.
            showDuplicatesCard = extras.duplicateGroups.isNotEmpty() &&
                ContactMergeUtil.duplicateSetHash(extras.duplicateGroups) != list.duplicatesDismissedHash,
            isFindingDuplicates = extras.isFindingDuplicates,
            pendingMergeGroup = extras.pendingMergeGroup,
            isMerging = extras.isMerging,
            allDuplicatesMerged = mergeUi.allDuplicatesMerged,
            showMergeUndo = mergeUi.mergeBatch != null && mergeUi.mergeBatch.linkedOutcomes.isNotEmpty(),
            mergedGroupCount = mergeUi.mergeBatch?.mergedCount ?: 0,
            mergedGroupTotal = mergeUi.mergeBatch?.totalCount ?: 0,
            mergedSnackbarVisible = mergeUi.mergeBatch != null
        )
    }
    .stateIn(
        scope = activeScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ContactsUiState(isLoading = true)
    )

    // Selection folds in second so the base combine stays at 5 flows.
    val uiState: StateFlow<ContactsUiState> = combine(
        baseUiState,
        _selectionUi
    ) { base, sel ->
        base.copy(
            selection = sel.selection,
            isDeleting = sel.isDeleting,
            deleteReport = sel.deleteReport
        )
    }
    .stateIn(
        scope = activeScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ContactsUiState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        clearSelection()
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
        clearSelection()
    }

    fun showMessage(messageRes: Int) {
        _userMessageRes.value = messageRes
    }

    fun clearMessage() {
        _userMessageRes.value = null
    }

    // Multi-select: long-press enters (first toggle from empty),
    // tap toggles while active, deselecting the last row exits.
    fun toggleSelection(id: Long) {
        _selectionUi.update { it.copy(selection = it.selection.toggle(id)) }
    }

    fun selectAllSelection(ids: Collection<Long>) {
        _selectionUi.update { it.copy(selection = it.selection.selectAll(ids)) }
    }

    fun clearSelection() {
        if (_selectionUi.value.selection.isActive) {
            _selectionUi.update { it.copy(selection = Selection()) }
        }
    }

    private fun selectedSummaries(): List<ContactSummary> {
        val ids = _selectionUi.value.selection.ids
        return latestContacts.filter { it.id in ids }
    }

    /** Bulk delete: one confirmation dialog, one applyBatch, progress, never on the UI thread. */
    fun deleteSelectedContacts() {
        val selected = selectedSummaries()
        if (selected.isEmpty()) return
        _selectionUi.update { it.copy(isDeleting = true) }
        mergeScope.launch {
            try {
                val result = contactsRepository.deleteContacts(selected.map { it.lookupKey })
                _selectionUi.update {
                    it.copy(
                        isDeleting = false,
                        selection = Selection(),
                        deleteReport = result
                    )
                }
            } catch (_: Exception) {
                _selectionUi.update {
                    it.copy(
                        isDeleting = false,
                        deleteReport = DeleteContactsResult(0, selected.size)
                    )
                }
            }
        }
    }

    fun clearDeleteReport() {
        _selectionUi.update { it.copy(deleteReport = null) }
    }

    /** Merge the selection into its first contact, using the link logic from Part 2. */
    fun mergeSelectedContacts() {
        val selected = selectedSummaries()
        if (selected.size < 2) return
        val keep = selected.first()
        val targets = selected.drop(1).map { it.id }
        mergeScope.launch {
            _isMerging.value = true
            try {
                val outcome = contactsRepository.mergeContacts(keep.id, targets).getOrNull()
                if (outcome != null) {
                    clearSelection()
                    onMergeSuccess(listOf(outcome), mergedCount = 1, totalCount = 1)
                    delay(600)
                    refreshDuplicates()
                } else {
                    _userMessageRes.value = R.string.snackbar_contacts_merge_failed
                }
            } catch (_: Exception) {
                _userMessageRes.value = R.string.snackbar_contacts_merge_failed
            } finally {
                _isMerging.value = false
            }
        }
    }

    /** Add every selected contact to favorites; the grid itself is the feedback. */
    fun addSelectedToFavorites() {
        val selected = selectedSummaries()
        val repo = favoritesRepository ?: return
        if (selected.isEmpty()) return
        activeScope.launch {
            for (contact in selected) {
                runCatching { repo.addFavorite(contact.lookupKey, contact.id) }
            }
            clearSelection()
        }
    }

    fun findDuplicates() {
        activeScope.launch {
            _isFindingDuplicates.value = true
            _showDuplicates.value = true
            try {
                refreshDuplicates()
                migrateDismissedHash(_duplicateGroups.value)
            } catch (_: Exception) {
                _showDuplicates.value = false
                _userMessageRes.value = R.string.snackbar_contact_error
            } finally {
                _isFindingDuplicates.value = false
            }
        }
    }

    /**
     * Migration for the old boolean banner dismissal: a true value
     * stores the current set's hash on the first scan, so the banner
     * stays gone for that set and nothing resets.
     */
    private suspend fun migrateDismissedHash(groups: List<DuplicateGroup>) {
        val repo = settingsRepository ?: return
        try {
            val settings = settingsFlow.first()
            if (settings.duplicatesDismissed && settings.duplicatesDismissedHash == null) {
                repo.setDuplicatesDismissedHash(ContactMergeUtil.duplicateSetHash(groups))
            }
        } catch (_: Exception) {
        }
    }

    fun hideDuplicates() {
        _showDuplicates.value = false
        _pendingMergeGroup.value = null
        _allDuplicatesMerged.value = false
    }

    fun showDuplicates() {
        _showDuplicates.value = true
    }

    fun dismissDuplicatesCard() {
        // Persisted hash of the current set: the banner stays gone
        // for this set across restarts and reappears only when the
        // set itself changes.
        settingsRepository?.let { repo ->
            mergeScope.launch {
                repo.setDuplicatesDismissedHash(
                    ContactMergeUtil.duplicateSetHash(_duplicateGroups.value)
                )
            }
        }
    }

    fun dismissFavoritesHint() {
        activeScope.launch {
            settingsRepository?.setFavoritesHintDismissed(true)
        }
    }

    fun requestMerge(group: DuplicateGroup) {
        _pendingMergeGroup.value = group
    }

    fun dismissMergeDialog() {
        _pendingMergeGroup.value = null
    }

    /**
     * Merges the pending group only after the user confirmed it in the dialog.
     * Linking keeps every field; the screen stays open on the remaining
     * groups, and closes only after the last group shows the
     * "all duplicates merged" state briefly.
     */
    fun confirmMerge(keepContactId: Long) {
        val group = _pendingMergeGroup.value ?: return
        if (group.contacts.size < 2) {
            _pendingMergeGroup.value = null
            return
        }
        mergeScope.launch {
            _isMerging.value = true
            try {
                val keep = group.contacts.firstOrNull { it.id == keepContactId }
                    ?: group.contacts.first()
                val mergeIds = group.contacts.filter { it.id != keep.id }.map { it.id }
                val outcome = contactsRepository.mergeContacts(keep.id, mergeIds).getOrNull()
                if (outcome != null) {
                    _pendingMergeGroup.value = null
                    removeMergedGroup(group)
                    onMergeSuccess(listOf(outcome), mergedCount = 1, totalCount = 1)
                    delay(600)
                    refreshDuplicates()
                } else {
                    _pendingMergeGroup.value = null
                    _userMessageRes.value = R.string.snackbar_contacts_merge_failed
                }
            } catch (_: Exception) {
                _pendingMergeGroup.value = null
                _userMessageRes.value = R.string.snackbar_contacts_merge_failed
            } finally {
                _isMerging.value = false
            }
        }
    }

    /**
     * Merges every group, keeping each group's first contact. No
     * per-group keep choice: the dialog confirms the group count only.
     * Only verified merges count: a failed group stays listed, and
     * the message says how many of the total merged.
     */
    fun confirmMergeAll() {
        val groups = _duplicateGroups.value
        if (groups.isEmpty()) return
        mergeScope.launch {
            _isMerging.value = true
            try {
                val linkedOutcomes = mutableListOf<ContactMergeOutcome>()
                var mergedCount = 0
                for (group in groups) {
                    val keep = group.contacts.first()
                    val mergeIds = group.contacts.drop(1).map { it.id }
                    val outcome = contactsRepository.mergeContacts(keep.id, mergeIds).getOrNull()
                    if (outcome != null) {
                        mergedCount++
                        removeMergedGroup(group)
                        if (outcome.linked) linkedOutcomes.add(outcome)
                    }
                }
                if (mergedCount == 0) {
                    _userMessageRes.value = R.string.snackbar_contacts_merge_failed
                } else {
                    onMergeSuccess(linkedOutcomes, mergedCount, totalCount = groups.size)
                    delay(600)
                    refreshDuplicates()
                }
            } catch (_: Exception) {
                _userMessageRes.value = R.string.snackbar_contact_error
            } finally {
                _isMerging.value = false
            }
        }
    }

    fun undoMerge() {
        val batch = _lastMergeBatch.value ?: return
        _lastMergeBatch.value = null
        mergeScope.launch {
            for (outcome in batch.linkedOutcomes) {
                contactsRepository.unlinkContacts(outcome)
            }
            delay(600)
            refreshDuplicates()
        }
    }

    fun clearMergeUndo() {
        _lastMergeBatch.value = null
    }

    /**
     * Every finished batch surfaces as the merged snackbar (with Undo
     * only when at least one merge was linked); a copy-only batch
     * shows the same message without Undo. The batch carries the
     * total so a partial batch reads "Merged X of Y".
     */
    private fun onMergeSuccess(
        linkedOutcomes: List<ContactMergeOutcome>,
        mergedCount: Int,
        totalCount: Int
    ) {
        _lastMergeBatch.value = MergeBatch(linkedOutcomes, mergedCount, totalCount)
    }

    /** Removes only the merged group (matched by its contact id set). */
    private fun removeMergedGroup(group: DuplicateGroup) {
        val ids = group.contacts.map { it.id }.toSet()
        val remaining = _duplicateGroups.value.filter {
            it.contacts.map { contact -> contact.id }.toSet() != ids
        }
        _duplicateGroups.value = remaining
        if (remaining.isEmpty()) {
            _allDuplicatesMerged.value = true
            mergeScope.launch {
                delay(2000)
                _allDuplicatesMerged.value = false
                if (_duplicateGroups.value.isEmpty()) {
                    _showDuplicates.value = false
                }
            }
        }
    }

    /**
     * Fresh duplicate scan from a one-shot snapshot, replacing the
     * list outright: groups that really merged vanish, groups that
     * failed stay. The cached flow is never used here — its last
     * emission may still predate a merge the provider applied.
     */
    private suspend fun refreshDuplicates() {
        val contacts = contactsRepository.getContactsSnapshot()
        val numbers = contactsRepository.getAllContactNumbers()
        _duplicateGroups.value = DuplicateContactFinder.findDuplicates(contacts, numbers)
    }

    companion object {
        fun provideFactory(
            repository: ContactsRepository,
            favoritesRepository: FavoritesRepository? = null,
            settingsRepository: SettingsRepository? = null,
            applicationScope: kotlinx.coroutines.CoroutineScope? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ContactsViewModel(
                    repository,
                    favoritesRepository,
                    settingsRepository,
                    applicationScope = applicationScope
                ) as T
            }
        }
    }
}
