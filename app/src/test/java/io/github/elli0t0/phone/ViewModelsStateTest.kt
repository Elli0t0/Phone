package io.github.elli0t0.phone

import androidx.lifecycle.SavedStateHandle
import io.github.elli0t0.phone.data.contacts.ContactsRepository
import io.github.elli0t0.phone.data.contacts.model.ContactDetail
import io.github.elli0t0.phone.data.contacts.model.ContactInput
import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import io.github.elli0t0.phone.feature.contacts.ContactsViewModel
import io.github.elli0t0.phone.feature.dialpad.DialpadViewModel
import io.github.elli0t0.phone.feature.recents.RecentsViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewModelsStateTest {

    private class FakeContactsRepository(
        private val initialList: List<ContactSummary> = emptyList()
    ) : ContactsRepository {
        override fun getContactsFlow(): Flow<List<ContactSummary>> = flowOf(initialList)
        override fun getContactDetail(lookupKey: String, contactId: Long): Flow<ContactDetail?> = flowOf(null)
        override suspend fun createContact(input: ContactInput): Result<Long> = Result.success(1L)
        override suspend fun updateContact(contactId: Long, original: ContactDetail, updated: ContactInput): Result<Unit> = Result.success(Unit)
        override suspend fun deleteContact(contactId: Long, lookupKey: String): Result<Unit> = Result.success(Unit)
    }

    @Test
    fun recentsViewModel_initialUiStateIsCorrect() {
        val viewModel = RecentsViewModel()
        val state = viewModel.uiState.value
        assertEquals(R.string.tab_recents, state.titleRes)
        assertEquals(R.string.placeholder_coming_soon, state.placeholderRes)
    }

    @Test
    fun contactsViewModel_initialUiStateIsCorrect() {
        val fakeRepo = FakeContactsRepository()
        val testScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        val viewModel = ContactsViewModel(fakeRepo, testScope)
        val state = viewModel.uiState.value
        assertTrue("Initial state should be loading", state.isLoading)
        assertTrue("Initial contacts should be empty", state.allContacts.isEmpty())
        assertEquals("", state.searchQuery)
    }

    @Test
    fun dialpadViewModel_entryAndDeletionFlow() {
        val fakeRepo = FakeContactsRepository()
        val savedStateHandle = SavedStateHandle()
        val testScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
        val viewModel = DialpadViewModel(fakeRepo, savedStateHandle, testScope)

        // Initial state
        assertEquals("", viewModel.uiState.value.rawNumber)
        assertFalse(viewModel.uiState.value.canCall)

        // Enter digits 0, 9, 1, 2
        viewModel.onKeyPressed('0')
        viewModel.onKeyPressed('9')
        viewModel.onKeyPressed('1')
        viewModel.onKeyPressed('2')
        assertEquals("0912", viewModel.uiState.value.rawNumber)
        assertTrue(viewModel.uiState.value.canCall)

        // Backspace deletes one digit
        viewModel.onBackspacePressed()
        assertEquals("091", viewModel.uiState.value.rawNumber)

        // Long press zero appends +
        viewModel.onLongPressZero()
        assertEquals("091+", viewModel.uiState.value.rawNumber)

        // Paste invalid and valid chars
        viewModel.onClearAll()
        assertEquals("", viewModel.uiState.value.rawNumber)
        assertFalse(viewModel.uiState.value.canCall)

        viewModel.onNumberPasted("+98 (912) 345-6789")
        assertEquals("+989123456789", viewModel.uiState.value.rawNumber)
    }
}
