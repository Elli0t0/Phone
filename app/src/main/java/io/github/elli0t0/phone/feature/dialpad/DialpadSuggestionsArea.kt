package io.github.elli0t0.phone.feature.dialpad

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.Motion
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import androidx.compose.foundation.layout.ColumnScope
import io.github.elli0t0.phone.data.contacts.model.ContactSummary

@Composable
fun ColumnScope.DialpadSuggestionsArea(
    uiState: DialpadUiState,
    searchVisible: Boolean,
    onSearchVisibilityChange: (Boolean) -> Unit,
    animationOption: AnimationOption,
    onSearchRowClick: (ContactSummary) -> Unit,
    onSearchRowCallClick: (ContactSummary) -> Unit,
    onSuggestionSelect: (ContactSummary) -> Unit,
    onRedial: (QuickRedialEntry) -> Unit,
    onFavoriteClick: (ContactSummary) -> Unit,
    onCreateContactAction: () -> Unit,
    onAddToExistingContactAction: () -> Unit,
    onMessageAction: () -> Unit
) {
    val spacing = LocalSpacing.current
    // No matching contact: offer real actions for the number.
    val noMatchActions: @Composable () -> Unit = {
        DialpadNoMatchActions(
            onCreateContact = {
                onCreateContactAction()
            },
            onAddToContact = {
                onAddToExistingContactAction()
            },
            onMessage = {
                onMessageAction()
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
    val showSearchRows = searchVisible && uiState.nameSearchQuery.isNotBlank() &&
        uiState.nameSearchResults.isNotEmpty()
    val showChips = (!searchVisible || uiState.nameSearchQuery.isBlank()) &&
        uiState.matchingContacts.isNotEmpty()
    val showNoMatchActions = uiState.rawNumber.isNotEmpty() &&
        !showSearchRows && !showChips
    // Empty keypad: favorites and recents share the area,
    // centered with breathing room instead of squished above the keys
    // under a tall blank. Every other state stays pinned to the bottom,
    // so lists grow upward from the keypad.
    val isEmptyState = uiState.rawNumber.isEmpty() && !showSearchRows && !showChips
    // T9 Matching contacts list above the keypad
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(top = spacing.small),
        contentAlignment = if (isEmptyState) Alignment.Center else Alignment.BottomCenter
    ) {
    if (showSearchRows) {
        // Name search: full rows.
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dialpad_search_results")
        ) {
            items(uiState.nameSearchResults, key = { it.id }) { contact ->
                DialpadSearchRow(
                    contact = contact,
                    onClick = {
                        onSearchRowClick(contact)
                    },
                    onCallClick = {
                        onSearchRowCallClick(contact)
                    }
                )
            }
        }
    } else if (showChips) {
        // T9: suggestion chips above the keypad; tap selects.
        DialpadSuggestionChips(
            contacts = uiState.matchingContacts,
            dialedDigits = uiState.rawNumber,
            onSelect = {
                onSuggestionSelect(it)
            },
            modifier = Modifier.fillMaxWidth(),
            contactCounts = uiState.matchingContactCounts
        )
    } else if (isEmptyState) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            if (uiState.favorites.isNotEmpty()) {
                FavoritesStrip(
                    favorites = uiState.favorites,
                    onFavoriteClick = onFavoriteClick,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (uiState.quickRedial.isNotEmpty()) {
                // Empty keypad: one-tap redial for the last 3 recent
                // numbers.
                QuickRedialChips(
                    entries = uiState.quickRedial,
                    onRedial = { entry ->
                        onRedial(entry)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
    }
    // No-match actions animate in/out below the list area
    // (ColumnScope AnimatedVisibility): fade + expand + scale on
    // full motion, fade only on Reduced, instant on Off.
    val noMatchEnter: EnterTransition = when (animationOption) {
        AnimationOption.ON -> fadeIn(
            tween(
                durationMillis = Motion.scaledDurationMs(Motion.Durations.Medium, animationOption),
                easing = Motion.Easings.Emphasized
            )
        ) + expandVertically(
            tween(
                durationMillis = Motion.scaledDurationMs(Motion.Durations.Medium, animationOption),
                easing = Motion.Easings.Emphasized
            )
        ) + scaleIn(
            tween(
                durationMillis = Motion.scaledDurationMs(Motion.Durations.Medium, animationOption),
                easing = Motion.Easings.Emphasized
            ),
            initialScale = 0.92f
        )
        AnimationOption.REDUCED -> fadeIn(
            tween(
                durationMillis = Motion.scaledDurationMs(Motion.Durations.Medium, animationOption),
                easing = Motion.Easings.Emphasized
            )
        )
        AnimationOption.OFF -> EnterTransition.None
    }
    val noMatchExit: ExitTransition = when (animationOption) {
        AnimationOption.ON -> fadeOut(
            tween(
                durationMillis = Motion.scaledDurationMs(Motion.Durations.Short, animationOption),
                easing = Motion.Easings.Exit
            )
        ) + shrinkVertically(
            tween(
                durationMillis = Motion.scaledDurationMs(Motion.Durations.Short, animationOption),
                easing = Motion.Easings.Exit
            )
        ) + scaleOut(
            tween(
                durationMillis = Motion.scaledDurationMs(Motion.Durations.Short, animationOption),
                easing = Motion.Easings.Exit
            ),
            targetScale = 0.92f
        )
        AnimationOption.REDUCED -> fadeOut(
            tween(
                durationMillis = Motion.scaledDurationMs(Motion.Durations.Short, animationOption),
                easing = Motion.Easings.Exit
            )
        )
        AnimationOption.OFF -> ExitTransition.None
    }
    AnimatedVisibility(
        visible = showNoMatchActions,
        enter = noMatchEnter,
        exit = noMatchExit
    ) {
        noMatchActions()
    }
}
