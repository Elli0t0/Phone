package io.github.elli0t0.phone

import androidx.lifecycle.SavedStateHandle
import io.github.elli0t0.phone.data.calllog.CallLogRepository
import io.github.elli0t0.phone.data.calllog.model.CallLogEntry
import io.github.elli0t0.phone.data.contacts.ContactsRepository
import io.github.elli0t0.phone.data.contacts.model.ContactDetail
import io.github.elli0t0.phone.data.contacts.model.ContactInput
import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import io.github.elli0t0.phone.data.favorites.FavoritesRepository
import io.github.elli0t0.phone.feature.contacts.ContactsViewModel
import io.github.elli0t0.phone.feature.dialpad.DialpadViewModel
import io.github.elli0t0.phone.feature.recents.CallFilter
import io.github.elli0t0.phone.feature.recents.RecentsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsStateTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeContactsRepository(
        private val initialList: List<ContactSummary> = emptyList()
    ) : ContactsRepository {
        override fun getContactsFlow(): Flow<List<ContactSummary>> = flowOf(initialList)
        override fun getContactDetail(lookupKey: String, contactId: Long): Flow<ContactDetail?> = flowOf(null)
        override suspend fun createContact(input: ContactInput): Result<Long> = Result.success(1L)
        override suspend fun updateContact(contactId: Long, original: ContactDetail, updated: ContactInput): Result<Unit> = Result.success(Unit)
        override suspend fun deleteContact(contactId: Long, lookupKey: String): Result<Unit> = Result.success(Unit)
    }

    private class FakeCallLogRepository : CallLogRepository {
        override fun getCallLogsFlow(): Flow<List<CallLogEntry>> = flowOf(emptyList())
        override suspend fun deleteCall(id: Long): Boolean = true
    }

    private class FakeFavoritesRepository : FavoritesRepository {
        override fun getFavoritesFlow(allContactsFlow: Flow<List<ContactSummary>>): Flow<List<ContactSummary>> = flowOf(emptyList())
        override fun isFavoriteFlow(lookupKey: String): Flow<Boolean> = flowOf(false)
        override suspend fun addFavorite(lookupKey: String, contactId: Long) {}
        override suspend fun removeFavorite(lookupKey: String) {}
        override suspend fun toggleFavorite(lookupKey: String, contactId: Long) {}
    }

    @Test
    fun recentsViewModel_initialUiStateIsCorrect() = runTest {
        val viewModel = RecentsViewModel(
            callLogRepository = FakeCallLogRepository(),
            favoritesRepository = FakeFavoritesRepository(),
            contactsRepository = FakeContactsRepository(),
            coroutineScope = backgroundScope
        )
        backgroundScope.launch { viewModel.uiState.collect() }

        assertEquals(CallFilter.ALL, viewModel.uiState.value.selectedFilter)
        assertEquals("", viewModel.uiState.value.searchQuery)
        assertTrue(viewModel.uiState.value.daySections.isEmpty())
        assertTrue(viewModel.uiState.value.favorites.isEmpty())

        // Test filter change
        viewModel.onFilterSelected(CallFilter.MISSED)
        runCurrent()
        assertEquals(CallFilter.MISSED, viewModel.uiState.value.selectedFilter)

        // Test search query change
        viewModel.onSearchQueryChanged("John")
        runCurrent()
        assertEquals("John", viewModel.uiState.value.searchQuery)
        assertTrue(viewModel.uiState.value.isSearching)
    }

    @Test
    fun contactsViewModel_initialUiStateIsCorrect() = runTest {
        val fakeRepo = FakeContactsRepository()
        val viewModel = ContactsViewModel(
            contactsRepository = fakeRepo,
            favoritesRepository = FakeFavoritesRepository(),
            coroutineScope = backgroundScope
        )
        backgroundScope.launch { viewModel.uiState.collect() }

        val state = viewModel.uiState.value
        assertTrue("Initial contacts should be empty", state.allContacts.isEmpty())
        assertEquals("", state.searchQuery)
    }

    @Test
    fun dialpadViewModel_entryAndDeletionFlow() = runTest {
        val fakeRepo = FakeContactsRepository()
        val savedStateHandle = SavedStateHandle()
        val viewModel = DialpadViewModel(
            contactsRepository = fakeRepo,
            savedStateHandle = savedStateHandle,
            coroutineScope = backgroundScope
        )
        backgroundScope.launch { viewModel.uiState.collect() }

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
