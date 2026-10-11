package io.github.elli0t0.phone

import io.github.elli0t0.phone.feature.contacts.ContactsViewModel
import io.github.elli0t0.phone.feature.dialpad.DialpadViewModel
import io.github.elli0t0.phone.feature.recents.RecentsViewModel
import org.junit.Assert.assertEquals
import org.junit.Test

class ViewModelsStateTest {

    @Test
    fun recentsViewModel_initialUiStateIsCorrect() {
        val viewModel = RecentsViewModel()
        val state = viewModel.uiState.value
        assertEquals(R.string.tab_recents, state.titleRes)
        assertEquals(R.string.placeholder_coming_soon, state.placeholderRes)
    }

    @Test
    fun contactsViewModel_initialUiStateIsCorrect() {
        val viewModel = ContactsViewModel()
        val state = viewModel.uiState.value
        assertEquals(R.string.tab_contacts, state.titleRes)
        assertEquals(R.string.placeholder_coming_soon, state.placeholderRes)
    }

    @Test
    fun dialpadViewModel_initialUiStateIsCorrect() {
        val viewModel = DialpadViewModel()
        val state = viewModel.uiState.value
        assertEquals(R.string.tab_dialpad, state.titleRes)
        assertEquals(R.string.placeholder_coming_soon, state.placeholderRes)
    }
}
