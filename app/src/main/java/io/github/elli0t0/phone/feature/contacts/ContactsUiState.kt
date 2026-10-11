package io.github.elli0t0.phone.feature.contacts

import androidx.annotation.StringRes
import io.github.elli0t0.phone.R

data class ContactsUiState(
    @StringRes val titleRes: Int = R.string.tab_contacts,
    @StringRes val placeholderRes: Int = R.string.placeholder_coming_soon
)
