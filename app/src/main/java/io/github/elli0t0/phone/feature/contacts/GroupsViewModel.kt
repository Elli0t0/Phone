package io.github.elli0t0.phone.feature.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.elli0t0.phone.data.contacts.ContactsRepository
import io.github.elli0t0.phone.data.contacts.model.ContactGroup
import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GroupsViewModel(
    private val contactsRepository: ContactsRepository,
    coroutineScope: kotlinx.coroutines.CoroutineScope? = null
) : ViewModel() {

    private val _groups = MutableStateFlow<List<ContactGroup>>(emptyList())
    private val _selectedGroup = MutableStateFlow<ContactGroup?>(null)
    private val _members = MutableStateFlow<List<ContactSummary>>(emptyList())
    private val _isLoading = MutableStateFlow(true)
    private val _isLoadingMembers = MutableStateFlow(false)

    private val activeScope = coroutineScope ?: try {
        viewModelScope
    } catch (_: Exception) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
    }

    init {
        activeScope.launch {
            _groups.value = try {
                contactsRepository.getContactGroups()
            } catch (_: Exception) {
                emptyList()
            }
            _isLoading.value = false
        }
    }

    val uiState: StateFlow<GroupsUiState> = combine(
        _groups,
        _selectedGroup,
        _members,
        _isLoading,
        _isLoadingMembers
    ) { groups, selectedGroup, members, isLoading, isLoadingMembers ->
        GroupsUiState(
            groups = groups,
            selectedGroup = selectedGroup,
            members = members,
            isLoading = isLoading,
            isLoadingMembers = isLoadingMembers
        )
    }.stateIn(
        scope = activeScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GroupsUiState(isLoading = true)
    )

    fun selectGroup(group: ContactGroup) {
        _selectedGroup.value = group
        _members.value = emptyList()
        _isLoadingMembers.value = true
        activeScope.launch {
            _members.value = try {
                contactsRepository.getGroupMembers(group.id)
            } catch (_: Exception) {
                emptyList()
            }
            _isLoadingMembers.value = false
        }
    }

    fun clearSelection() {
        _selectedGroup.value = null
        _members.value = emptyList()
    }

    companion object {
        fun provideFactory(
            repository: ContactsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GroupsViewModel(repository) as T
            }
        }
    }
}
