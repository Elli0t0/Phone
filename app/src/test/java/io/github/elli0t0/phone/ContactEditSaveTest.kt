package io.github.elli0t0.phone

import android.provider.ContactsContract.CommonDataKinds.Phone
import io.github.elli0t0.phone.data.contacts.ContactsRepository
import io.github.elli0t0.phone.data.contacts.model.ContactDetail
import io.github.elli0t0.phone.data.contacts.model.ContactInput
import io.github.elli0t0.phone.data.contacts.model.ContactPhone
import io.github.elli0t0.phone.data.contacts.model.ContactSummary
import io.github.elli0t0.phone.feature.contacts.ContactEditViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Edit Contact must never alter a stored number: the edit field
 * loads the stored value verbatim (no formatter, no length cap),
 * and saving writes it back unchanged. Regression test for the
 * 11-digit number whose last digit was clipped in the field.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ContactEditSaveTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeContactsRepository : ContactsRepository {
        var savedUpdate: ContactInput? = null

        override fun getContactsFlow(): Flow<List<ContactSummary>> = flowOf(emptyList())

        override fun getContactDetail(lookupKey: String, contactId: Long): Flow<ContactDetail?> =
            flowOf(null)

        override suspend fun createContact(input: ContactInput): Result<Long> =
            Result.success(1L)

        override suspend fun updateContact(
            contactId: Long,
            original: ContactDetail,
            updated: ContactInput
        ): Result<Unit> {
            savedUpdate = updated
            return Result.success(Unit)
        }

        override suspend fun deleteContact(contactId: Long, lookupKey: String): Result<Unit> =
            Result.success(Unit)
    }

    @Test
    fun editSaveKeepsNumbersUnchanged() = runTest {
        val numbers = listOf("09359521521", "+989123456789")
        val original = ContactDetail(
            id = 7L,
            lookupKey = "key",
            displayName = "Test",
            phones = numbers.map { ContactPhone(number = it) }
        )
        val repo = FakeContactsRepository()
        val viewModel = ContactEditViewModel(repo, initialContact = original)

        // The edit field loads the stored numbers verbatim.
        assertEquals(numbers, viewModel.uiState.value.phones.map { it.number })

        var savedId = -1L
        viewModel.saveContact { savedId = it }

        assertEquals(7L, savedId)
        assertEquals(numbers, repo.savedUpdate?.phones?.map { it.number })
    }

    @Test
    fun editSaveKeepsExtraFieldsAndCustomLabel() = runTest {
        val original = ContactDetail(
            id = 9L,
            lookupKey = "key2",
            displayName = "Extra",
            phones = listOf(
                ContactPhone(number = "09123456789", type = Phone.TYPE_CUSTOM, label = "Direct")
            ),
            notes = "Met at conf",
            birthday = "1990-05-04",
            address = "Tehran, Valiasr St"
        )
        val repo = FakeContactsRepository()
        val viewModel = ContactEditViewModel(repo, initialContact = original)

        // The extra fields load verbatim into the edit state.
        assertEquals("Met at conf", viewModel.uiState.value.notes)
        assertEquals("1990-05-04", viewModel.uiState.value.birthday)
        assertEquals("Tehran, Valiasr St", viewModel.uiState.value.address)
        assertEquals("Direct", viewModel.uiState.value.phones.first().label)

        var savedId = -1L
        viewModel.saveContact { savedId = it }

        assertEquals(9L, savedId)
        val saved = repo.savedUpdate
        assertEquals("Met at conf", saved?.notes)
        assertEquals("1990-05-04", saved?.birthday)
        assertEquals("Tehran, Valiasr St", saved?.address)
        assertEquals("Direct", saved?.phones?.first()?.label)

        // Switching away from Custom drops the label so a stale
        // label is never saved under a stock type.
        viewModel.onPhoneTypeChanged(0, Phone.TYPE_MOBILE)
        assertEquals(null, viewModel.uiState.value.phones.first().label)
    }
}
