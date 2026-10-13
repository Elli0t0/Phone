package io.github.elli0t0.phone.feature.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.elli0t0.phone.data.contacts.ContactsRepository
import io.github.elli0t0.phone.data.contacts.util.ContactGroupingUtil
import io.github.elli0t0.phone.data.contacts.util.ContactSearchMatcher
import io.github.elli0t0.phone.data.favorites.FavoritesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

class ContactsViewModel(
    private val contactsRepository: ContactsRepository,
    private val favoritesRepository: FavoritesRepository? = null,
    coroutineScope: kotlinx.coroutines.CoroutineScope? = null
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _userMessageRes = MutableStateFlow<Int?>(null)

    private val activeScope = coroutineScope ?: try {
        viewModelScope
    } catch (_: Exception) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
    }

    private val favoritesFlow = favoritesRepository?.getFavoritesFlow(
        contactsRepository.getContactsFlow()
    ) ?: flowOf(emptyList())

    val uiState: StateFlow<ContactsUiState> = combine(
        contactsRepository.getContactsFlow(),
        favoritesFlow,
        _searchQuery,
        _userMessageRes
    ) { contacts, favorites, query, messageRes ->
        val filtered = ContactSearchMatcher.filter(contacts, query)
        val grouped = ContactGroupingUtil.groupContacts(filtered)

        ContactsUiState(
            searchQuery = query,
            allContacts = contacts,
            favorites = favorites,
            filteredContacts = filtered,
            groupedContacts = grouped,
            isLoading = false,
            userMessageRes = messageRes
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

    companion object {
        fun provideFactory(
            repository: ContactsRepository,
            favoritesRepository: FavoritesRepository? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ContactsViewModel(repository, favoritesRepository) as T
            }
        }
    }
}
