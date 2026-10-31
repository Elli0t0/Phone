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
import androidx.compose.foundation.layout.Box
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
import io.github.elli0t0.phone.core.design.LocalMinimal
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.Motion
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import io.github.elli0t0.phone.data.settings.model.QuickCallStyle
import androidx.compose.foundation.layout.ColumnScope
import io.github.elli0t0.phone.data.contacts.model.ContactSummary

@Composable
fun ColumnScope.DialpadSuggestionsArea(
    uiState: DialpadUiState,
    searchVisible: Boolean,
    onSearchVisibilityChange: (Boolean) -> Unit,
    animationOption: AnimationOption,
    quickCallEnabled: Boolean,
    quickCallStyle: QuickCallStyle,
    onSearchRowClick: (ContactSummary) -> Unit,
    onSearchRowCallClick: (ContactSummary) -> Unit,
    onSuggestionSelect: (ContactSummary) -> Unit,
    onRedial: (QuickRedialEntry) -> Unit,
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
    // Empty keypad: PILLS shows nothing here (the pill row below
    // the number display owns it). CLASSIC restores the pre-strip
    // look exactly via the untouched QuickRedialChips: the "Recent"
    // label + up to 3 text chips at the top. The old favorites row
    // is not restored — the spec names the Recent chips only.
    // T9 chips pin to the top too; search rows fill the area.
    val isEmptyState = uiState.rawNumber.isEmpty() && !showSearchRows && !showChips
    // T9 Matching contacts list above the keypad
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(top = spacing.small),
        contentAlignment = Alignment.TopCenter
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
        // CLASSIC: the pre-strip Recent chips, gated exactly as
        // the original was (non-empty, not Minimal) plus the
        // feature switch and the style. At most 3, as before.
        if (quickCallStyle == QuickCallStyle.CLASSIC && quickCallEnabled &&
            uiState.quickRedial.isNotEmpty() && !LocalMinimal.current
        ) {
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
