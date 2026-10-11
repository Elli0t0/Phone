package io.github.elli0t0.phone

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
import org.junit.Assert.assertNotNull
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
        val viewModel = ContactsViewModel(fakeRepo)
        val state = viewModel.uiState.value
        assertTrue("Initial state should be loading", state.isLoading)
        assertTrue("Initial contacts should be empty", state.allContacts.isEmpty())
        assertEquals("", state.searchQuery)
    }

    @Test
    fun dialpadViewModel_initialUiStateIsCorrect() {
        val viewModel = DialpadViewModel()
        val state = viewModel.uiState.value
        assertEquals(R.string.tab_dialpad, state.titleRes)
        assertEquals(R.string.placeholder_coming_soon, state.placeholderRes)
    }
}
