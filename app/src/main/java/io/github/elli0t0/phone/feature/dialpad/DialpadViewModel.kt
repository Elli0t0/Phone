package io.github.elli0t0.phone.feature.dialpad

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.elli0t0.phone.core.common.PhoneNumberFormatter
import io.github.elli0t0.phone.data.contacts.ContactsRepository
import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class DialpadViewModel(
    private val contactsRepository: ContactsRepository,
    private val savedStateHandle: SavedStateHandle,
    coroutineScope: kotlinx.coroutines.CoroutineScope? = null
) : ViewModel() {

    private val KEY_RAW_NUMBER = "dialpad_raw_number"

    private val _rawNumber = MutableStateFlow(savedStateHandle.get<String>(KEY_RAW_NUMBER) ?: "")
    val rawNumber: StateFlow<String> = _rawNumber.asStateFlow()

    private val _uiState = MutableStateFlow(
        DialpadUiState(
            rawNumber = _rawNumber.value,
            formattedNumber = PhoneNumberFormatter.format(_rawNumber.value),
            canCall = _rawNumber.value.isNotBlank()
        )
    )
    val uiState: StateFlow<DialpadUiState> = _uiState.asStateFlow()

    private var allContactsCache: List<ContactSummary> = emptyList()

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

    private fun updateMatchingContacts(query: String) {
        val matches = if (query.isNotBlank()) {
            T9SearchHelper.filter(allContactsCache, query).take(10)
        } else {
            emptyList()
        }
        _uiState.value = _uiState.value.copy(matchingContacts = matches)
    }

    fun onKeyPressed(digit: Char) {
        val updated = _rawNumber.value + digit
        setRawNumber(updated)
    }

    fun onLongPressZero() {
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

    fun onClearAll() {
        setRawNumber("")
    }

    fun onNumberPasted(pastedText: String) {
        val sanitized = PhoneNumberFormatter.sanitize(pastedText)
        if (sanitized.isNotEmpty()) {
            val updated = _rawNumber.value + sanitized
            setRawNumber(updated)
        }
    }

    fun onContactSelected(contact: ContactSummary) {
        val numberToSet = contact.primaryNumberPreview ?: ""
        if (numberToSet.isNotEmpty()) {
            val sanitized = PhoneNumberFormatter.sanitize(numberToSet)
            setRawNumber(sanitized)
        }
    }

    private fun setRawNumber(number: String) {
        _rawNumber.value = number
        savedStateHandle[KEY_RAW_NUMBER] = number
        _uiState.value = _uiState.value.copy(
            rawNumber = number,
            formattedNumber = PhoneNumberFormatter.format(number),
            isSearching = number.isNotBlank(),
            canCall = number.isNotBlank()
        )
    }

    companion object {
        fun provideFactory(
            repository: ContactsRepository,
            savedStateHandle: SavedStateHandle
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DialpadViewModel(repository, savedStateHandle) as T
            }
        }
    }
}
