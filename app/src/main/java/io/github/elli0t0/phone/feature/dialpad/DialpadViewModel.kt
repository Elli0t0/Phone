package io.github.elli0t0.phone.feature.dialpad

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.elli0t0.phone.core.common.PhoneNumberFormatter
import io.github.elli0t0.phone.core.common.PhoneNumberMatcher
import io.github.elli0t0.phone.data.calllog.CallLogRepository
import io.github.elli0t0.phone.data.contacts.ContactsRepository
import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import io.github.elli0t0.phone.data.contacts.util.findByNumber
import io.github.elli0t0.phone.data.favorites.FavoritesRepository
import io.github.elli0t0.phone.data.settings.SettingsRepository
import io.github.elli0t0.phone.data.settings.model.AppSettings
import io.github.elli0t0.phone.data.settings.model.QuickCallSource
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class DialpadViewModel(
    private val contactsRepository: ContactsRepository,
    private val savedStateHandle: SavedStateHandle,
    coroutineScope: kotlinx.coroutines.CoroutineScope? = null,
    private val callLogRepository: CallLogRepository? = null,
    private val favoritesRepository: FavoritesRepository? = null,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    private val KEY_RAW_NUMBER = "dialpad_raw_number"

    private val _rawNumber = MutableStateFlow(savedStateHandle.get<String>(KEY_RAW_NUMBER) ?: "")
    val rawNumber: StateFlow<String> = _rawNumber.asStateFlow()

    private val _uiState = MutableStateFlow(
        DialpadUiState(
            rawNumber = _rawNumber.value,
            formattedNumber = PhoneNumberFormatter.formatPartial(_rawNumber.value),
            canCall = _rawNumber.value.isNotBlank()
        )
    )
    val uiState: StateFlow<DialpadUiState> = _uiState.asStateFlow()

    private var allContactsCache: List<ContactSummary> = emptyList()

    // Quick call strip inputs (0.19.3): the strip recomputes whenever
    // any of them changes.
    private var recentEntriesCache: List<QuickRedialEntry> = emptyList()
    private var favoritesCache: List<ContactSummary> = emptyList()
    private var latestSettings: AppSettings? = null

    private val activeScope = coroutineScope ?: try {
        viewModelScope
    } catch (_: Exception) {
        null
    }

    init {
        // Collect contacts cache
        activeScope?.launch {
            contactsRepository.getContactsFlow().collectLatest { contacts ->
                allContactsCache = contacts
                updateMatchingContacts(_rawNumber.value)
                updateQuickCall()
                updateSpeedDialLabels()
            }
        }

        // Quick-call settings (0.19.3): source, count, hidden, and the
        // speed-dial toggles feed the strip and the key labels.
        if (settingsRepository != null) {
            activeScope?.launch {
                settingsRepository.settingsFlow.collectLatest { settings ->
                    latestSettings = settings
                    updateQuickCall()
                    updateSpeedDialLabels()
                }
            }
        }

        // Quick redial: the last 3 distinct recent numbers, newest
        // first, for the empty-keypad chips. Empty when the call-log
        // permission is missing (the repository emits an empty list).
        // The full deduped list feeds the Quick call strip (0.19.3);
        // the strip resolves numbers to contacts and drops numbers
        // with no contact.
        if (callLogRepository != null) {
            activeScope?.launch {
                callLogRepository.getCallLogsFlow().collectLatest { entries ->
                    val seen = mutableSetOf<String>()
                    val deduped = entries
                        .filter { it.number.isNotBlank() }
                        .filter { seen.add(PhoneNumberMatcher.normalize(it.number)) }
                        .map { entry ->
                            QuickRedialEntry(
                                number = entry.number,
                                label = entry.contactName?.takeIf { it.isNotBlank() }
                                    ?: entry.cachedName?.takeIf { it.isNotBlank() }
                                    ?: entry.formattedNumber
                            )
                        }
                    recentEntriesCache = deduped
                    _uiState.value = _uiState.value.copy(quickRedial = deduped.take(3))
                    updateQuickCall()
                }
            }
        }

        // Favorites strip: favorite contacts with a number to dial.
        // Undialable favorites (no number) are hidden, not shown as
        // dead entries.
        if (favoritesRepository != null) {
            activeScope?.launch {
                favoritesRepository.getFavoritesFlow(contactsRepository.getContactsFlow())
                    .collectLatest { favorites ->
                        val dialable = favorites.filter { !it.primaryNumberPreview.isNullOrBlank() }
                        favoritesCache = dialable
                        _uiState.value = _uiState.value.copy(
                            favorites = dialable
                        )
                        updateQuickCall()
                    }
            }
        }

        // Debounce query changes by 150 ms for contact search
        activeScope?.launch {
            _rawNumber
                .debounce(150L)
                .distinctUntilChanged()
                .collectLatest { query ->
                    updateMatchingContacts(query)
                }
        }
    }

    /**
     * Quick call strip (0.19.3): favorites first, then recents, per
     * the source setting. Recents resolve to contacts by number and
     * drop numbers with no contact (every card needs a lookup key
     * for the hide action and the open-contact action). Hidden
     * lookup keys are excluded, duplicates collapse by lookup key,
     * and the list caps at the count setting.
     */
    private fun updateQuickCall() {
        val settings = latestSettings
        if (settings == null || !settings.quickCallEnabled) {
            _uiState.value = _uiState.value.copy(quickCall = emptyList())
            return
        }
        val hidden = settings.quickCallHidden
        val seenKeys = mutableSetOf<String>()
        val strip = mutableListOf<QuickCallEntry>()
        fun add(contact: ContactSummary, number: String) {
            if (contact.lookupKey in hidden) return
            if (!seenKeys.add(contact.lookupKey)) return
            if (number.isBlank()) return
            strip.add(QuickCallEntry(contact = contact, number = number))
        }
        if (settings.quickCallSource != QuickCallSource.RECENTS) {
            favoritesCache.forEach { contact ->
                add(contact, contact.primaryNumberPreview ?: "")
            }
        }
        if (settings.quickCallSource != QuickCallSource.FAVORITES) {
            recentEntriesCache.forEach { entry ->
                val contact = allContactsCache.findByNumber(entry.number) ?: return@forEach
                add(contact, entry.number)
            }
        }
        _uiState.value = _uiState.value.copy(
            quickCall = strip.take(settings.quickCallCount)
        )
    }

    /**
     * Speed-dial key labels (0.19.3): digit to the first 6 letters of
     * the resolved contact name. Empty when the feature or the toggle
     * is off, and when a number resolves to no contact.
     */
    private fun updateSpeedDialLabels() {
        val settings = latestSettings
        if (settings == null || !settings.speedDialEnabled || !settings.showSpeedDialOnKeys) {
            _uiState.value = _uiState.value.copy(speedDialLabels = emptyMap())
            return
        }
        val labels = settings.speedDial.entries.mapNotNull { (digit, number) ->
            val name = allContactsCache.findByNumber(number)
                ?.displayName?.trim()?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            digit to name.take(6)
        }.toMap()
        _uiState.value = _uiState.value.copy(speedDialLabels = labels)
    }

    /** Resolved contact name for a speed-dial number, else the number itself. */
    fun speedDialDisplayName(number: String): String =
        allContactsCache.findByNumber(number)
            ?.displayName?.trim()?.takeIf { it.isNotBlank() }
            ?: number

    private fun updateMatchingContacts(query: String) {
        val matches = if (query.isNotBlank()) {
            T9SearchHelper.filter(allContactsCache, query).take(10)
        } else {
            emptyList()
        }
        // De-duplicate before showing: the same contact, or two entries
        // with the same name and normalized number, collapse into one
        // row; the group size rides along as a "N contacts" hint.
        val groups = matches.groupBy { contact ->
            val number = contact.primaryNumberPreview
            if (number.isNullOrBlank()) {
                "id:${contact.id}"
            } else {
                "num:${PhoneNumberMatcher.normalize(number)}|" +
                    PhoneNumberMatcher.normalizeName(contact.displayName)
            }
        }
        _uiState.value = _uiState.value.copy(
            matchingContacts = groups.values.map { it.first() },
            matchingContactCounts = groups.values.associate { group -> group.first().id to group.size }
        )
    }

    private var backspaceRepeatJob: kotlinx.coroutines.Job? = null

    fun onKeyPressed(digit: Char) {
        stopBackspaceRepeat()
        val updated = _rawNumber.value + digit
        setRawNumber(updated)
    }

    fun onLongPressZero() {
        stopBackspaceRepeat()
        val updated = _rawNumber.value + "+"
        setRawNumber(updated)
    }

    fun onBackspacePressed() {
        val current = _rawNumber.value
        if (current.isNotEmpty()) {
            val updated = current.substring(0, current.length - 1)
            setRawNumber(updated)
        }
    }

    fun startBackspaceRepeat(onTickHaptic: (() -> Unit)? = null) {
        stopBackspaceRepeat()
        val scope = activeScope ?: return
        backspaceRepeatJob = scope.launch {
            kotlinx.coroutines.delay(400L)
            if (!isActive || _rawNumber.value.isEmpty()) return@launch
            onTickHaptic?.invoke()
            var interval = 100L
            while (isActive && _rawNumber.value.isNotEmpty()) {
                onBackspacePressed()
                if (_rawNumber.value.isEmpty()) break
                kotlinx.coroutines.delay(interval)
                if (interval > 40L) {
                    interval = (interval - 10L).coerceAtLeast(40L)
                }
            }
        }
    }

    fun stopBackspaceRepeat() {
        backspaceRepeatJob?.cancel()
        backspaceRepeatJob = null
    }

    override fun onCleared() {
        stopBackspaceRepeat()
        super.onCleared()
    }

    fun onClearAll() {
        stopBackspaceRepeat()
        setRawNumber("")
    }

    /** Long-press paste on the number display: sanitize and append. */
    fun onNumberPasted(pastedText: String) {
        val sanitized = PhoneNumberFormatter.sanitize(pastedText)
        if (sanitized.isNotEmpty()) {
            val updated = _rawNumber.value + sanitized
            setRawNumber(updated)
        }
    }

    /** External dial request: replace the input with the number
     * (sanitized, so +, * and # survive). */
    fun onExternalDialNumber(number: String) {
        val sanitized = PhoneNumberFormatter.sanitize(number)
        if (sanitized.isNotEmpty()) setRawNumber(sanitized)
    }

    fun onContactSelected(contact: ContactSummary) {
        val numberToSet = contact.primaryNumberPreview ?: ""
        if (numberToSet.isNotEmpty()) {
            val sanitized = PhoneNumberFormatter.sanitize(numberToSet)
            setRawNumber(sanitized)
        }
    }

    /** Filters contacts by name for the keypad search bar. */
    fun onNameSearchQueryChanged(query: String) {
        val results = if (query.isNotBlank()) {
            allContactsCache.filter { it.displayName.contains(query, ignoreCase = true) }.take(10)
        } else {
            emptyList()
        }
        _uiState.value = _uiState.value.copy(
            nameSearchQuery = query,
            nameSearchResults = results
        )
    }

    private fun setRawNumber(number: String) {
        _rawNumber.value = number
        savedStateHandle[KEY_RAW_NUMBER] = number
        _uiState.value = _uiState.value.copy(
            rawNumber = number,
            formattedNumber = PhoneNumberFormatter.formatPartial(number),
            isSearching = number.isNotBlank(),
            canCall = number.isNotBlank()
        )
    }

    companion object {
        fun provideFactory(
            repository: ContactsRepository,
            savedStateHandle: SavedStateHandle,
            callLogRepository: CallLogRepository? = null,
            favoritesRepository: FavoritesRepository? = null,
            settingsRepository: SettingsRepository? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DialpadViewModel(
                    repository, savedStateHandle, null, callLogRepository, favoritesRepository,
                    settingsRepository
                ) as T
            }
        }
    }
}
