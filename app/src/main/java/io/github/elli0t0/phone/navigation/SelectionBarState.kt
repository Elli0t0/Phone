package io.github.elli0t0.phone.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * What the bottom bar becomes while a list is selecting: a title
 * (the selection count only, e.g. "3"), an X to exit, a select-all toggle, and the
 * screen's own actions (Share, Delete, More). Screens publish a
 * state through [LocalSetSelectionBarState] when their selection
 * becomes active and clear it on exit; MainActivity swaps the bar
 * without the screens rendering it themselves.
 */
data class SelectionBarState(
    val title: String,
    val allSelected: Boolean,
    val onClose: () -> Unit,
    val onToggleSelectAll: () -> Unit,
    val actions: @Composable RowScope.() -> Unit
)

/** Current selection bar, or null when no list is selecting. */
val LocalSelectionBarState = staticCompositionLocalOf<SelectionBarState?> { null }

/** Provided by MainActivity: publish or clear the selection bar. */
val LocalSetSelectionBarState = staticCompositionLocalOf<(SelectionBarState?) -> Unit> { {} }
