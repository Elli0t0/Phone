package io.github.elli0t0.phone.feature.recents

import androidx.annotation.StringRes
import io.github.elli0t0.phone.R

data class RecentsUiState(
    @StringRes val titleRes: Int = R.string.tab_recents,
    @StringRes val placeholderRes: Int = R.string.placeholder_coming_soon
)
