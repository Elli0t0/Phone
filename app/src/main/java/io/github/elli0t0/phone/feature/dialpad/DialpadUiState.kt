package io.github.elli0t0.phone.feature.dialpad

import androidx.annotation.StringRes
import io.github.elli0t0.phone.R

data class DialpadUiState(
    @StringRes val titleRes: Int = R.string.tab_dialpad,
    @StringRes val placeholderRes: Int = R.string.placeholder_coming_soon
)
