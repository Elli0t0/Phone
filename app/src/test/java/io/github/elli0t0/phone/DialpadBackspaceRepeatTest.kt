package io.github.elli0t0.phone

import androidx.lifecycle.SavedStateHandle
import io.github.elli0t0.phone.data.contacts.ContactsRepository
import io.github.elli0t0.phone.data.contacts.model.ContactDetail
import io.github.elli0t0.phone.data.contacts.model.ContactInput
import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import io.github.elli0t0.phone.feature.dialpad.DialpadViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DialpadBackspaceRepeatTest {

    private class EmptyFakeContactsRepo : ContactsRepository {
        override fun getContactsFlow(): Flow<List<ContactSummary>> = flowOf(emptyList())
        override fun getContactDetail(lookupKey: String, contactId: Long): Flow<ContactDetail?> = flowOf(null)
        override suspend fun createContact(input: ContactInput): Result<Long> = Result.success(1L)
        override suspend fun updateContact(contactId: Long, original: ContactDetail, updated: ContactInput): Result<Unit> = Result.success(Unit)
        override suspend fun deleteContact(contactId: Long, lookupKey: String): Result<Unit> = Result.success(Unit)
    }

    @Test
    fun backspace_onePressDeletesExactlyOneDigit() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val savedState = SavedStateHandle().apply { set("dialpad_raw_number", "12345") }
        val viewModel = DialpadViewModel(EmptyFakeContactsRepo(), savedState, testScope)

        viewModel.onBackspacePressed()
        assertEquals("1234", viewModel.uiState.value.rawNumber)
    }

    @Test
    fun backspace_holdingThenReleasingStopsFurtherDeletions() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val savedState = SavedStateHandle().apply { set("dialpad_raw_number", "1234567") }
        val viewModel = DialpadViewModel(EmptyFakeContactsRepo(), savedState, testScope)

        // Initial press
        viewModel.onBackspacePressed()
        assertEquals("123456", viewModel.uiState.value.rawNumber)

        // Hold starts
        viewModel.startBackspaceRepeat()

        // Before 400ms delay: no repeat deletion yet
        testScheduler.advanceTimeBy(350L)
        assertEquals("123456", viewModel.uiState.value.rawNumber)

        // At 400ms: first repeat deletion occurs
        testScheduler.advanceTimeBy(60L)
        assertEquals("12345", viewModel.uiState.value.rawNumber)

        // 100ms later: second repeat deletion
        testScheduler.advanceTimeBy(100L)
        assertEquals("1234", viewModel.uiState.value.rawNumber)

        // User releases backspace
        viewModel.stopBackspaceRepeat()

        // Further time passes: NO further deletions!
        testScheduler.advanceTimeBy(2000L)
        assertEquals("1234", viewModel.uiState.value.rawNumber)
    }

    @Test
    fun backspace_typingAfterReleaseNeverDeletes() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val savedState = SavedStateHandle().apply { set("dialpad_raw_number", "123") }
        val viewModel = DialpadViewModel(EmptyFakeContactsRepo(), savedState, testScope)

        viewModel.onBackspacePressed()
        viewModel.startBackspaceRepeat()
        testScheduler.advanceTimeBy(200L)
        viewModel.stopBackspaceRepeat()

        // User types new digit
        viewModel.onKeyPressed('9')
        testScheduler.advanceTimeBy(2000L)

        // Digits must remain intact!
        assertEquals("129", viewModel.uiState.value.rawNumber)
    }

    @Test
    fun backspace_repeatStopsAtEmptyNumber() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val savedState = SavedStateHandle().apply { set("dialpad_raw_number", "12") }
        val viewModel = DialpadViewModel(EmptyFakeContactsRepo(), savedState, testScope)

        viewModel.onBackspacePressed()
        viewModel.startBackspaceRepeat()

        // Advance plenty of virtual time
        testScheduler.advanceTimeBy(5000L)

        assertTrue(viewModel.uiState.value.rawNumber.isEmpty())
    }
}
