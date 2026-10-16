package io.github.elli0t0.phone.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.elli0t0.phone.PhoneApplication
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.callBackgroundBrush
import io.github.elli0t0.phone.core.design.pressScale
import io.github.elli0t0.phone.core.design.rememberLightHaptic
import io.github.elli0t0.phone.data.settings.model.AnswerGesture
import io.github.elli0t0.phone.data.settings.model.AnswerSide
import io.github.elli0t0.phone.data.settings.model.CallBackground
import io.github.elli0t0.phone.data.settings.model.EndCallAlignment
import io.github.elli0t0.phone.data.settings.model.OngoingCallButton
import kotlin.math.roundToInt

/**
 * The option sections under the call-background preview: layout preset,
 * answer gesture, answer side, call controls, end-call position,
 * background style, dim, and number visibility. Every change saves
 * immediately and shows in the preview. Split out of
 * CallBackgroundScreen so the studio stays small enough to ship.
 */
@Composable
fun CallStudioSections(
    selected: CallBackground,
    accentColor: Long,
    onSelect: (CallBackground) -> Unit,
    dimDraft: Float,
    onDimDraftChange: (Float) -> Unit,
    onEditCustomColor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val lightHaptic = rememberLightHaptic()
    val settings = LocalAppSettings.current
    val context = LocalContext.current
    val app = context.applicationContext as PhoneApplication
    // Same instance SettingsScreen uses (same nav entry owner).
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.provideFactory(app.container.settingsRepository)
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        // Layout preset: one tap that sets gesture, answer side,
        // and end-call position together. Any manual change after
        // applying a preset leaves no preset selected (custom).
        Text(
            text = "Layout preset",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            CallLayoutPreset.entries.forEach { preset ->
                val presetSelected = settings.answerGesture == preset.gesture &&
                    settings.answerSide == preset.side &&
                    settings.endCallAlignment == preset.endAlignment
                FilterChip(
                    selected = presetSelected,
                    onClick = {
                        settingsViewModel.onAnswerGestureChanged(preset.gesture)
                        settingsViewModel.onAnswerSideChanged(preset.side)
                        settingsViewModel.onEndCallAlignmentChanged(preset.endAlignment)
                    },
                    label = { Text(text = preset.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("layout_preset_${preset.name.lowercase()}")
                )
            }
        }

        // Answer gesture: tap the buttons, or swipe the bar.
        Text(
            text = "Answer gesture",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            AnswerGesture.entries.forEach { gesture ->
                FilterChip(
                    selected = settings.answerGesture == gesture,
                    onClick = { settingsViewModel.onAnswerGestureChanged(gesture) },
                    label = { Text(text = gesture.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("answer_gesture_${gesture.name.lowercase()}")
                )
            }
        }

        // Answer side: which side the answer button (or swipe
        // direction) sits on.
        Text(
            text = "Answer side",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            AnswerSide.entries.forEach { side ->
                FilterChip(
                    selected = settings.answerSide == side,
                    onClick = { settingsViewModel.onAnswerSideChanged(side) },
                    label = { Text(text = side.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("answer_side_${side.name.lowercase()}")
                )
            }
        }

        // Call controls: which ongoing-call buttons show, in order.
        // Shown buttons can be reordered (up/down) or hidden;
        // hidden buttons can be added back.
        Text(
            text = "Call controls",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val shownButtons = settings.ongoingCallButtons
        val hiddenButtons = OngoingCallButton.entries - shownButtons.toSet()
        Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
            shownButtons.forEachIndexed { index, button ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    Text(
                        text = button.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            val list = shownButtons.toMutableList()
                            val item = list.removeAt(index)
                            list.add(index - 1, item)
                            settingsViewModel.onOngoingCallButtonsChanged(list)
                        },
                        enabled = index > 0,
                        modifier = Modifier.testTag("control_move_up_${button.name}")
                    ) {
                        Text(text = "↑")
                    }
                    TextButton(
                        onClick = {
                            val list = shownButtons.toMutableList()
                            val item = list.removeAt(index)
                            list.add(index + 1, item)
                            settingsViewModel.onOngoingCallButtonsChanged(list)
                        },
                        enabled = index < shownButtons.lastIndex,
                        modifier = Modifier.testTag("control_move_down_${button.name}")
                    ) {
                        Text(text = "↓")
                    }
                    Switch(
                        checked = true,
                        onCheckedChange = {
                            settingsViewModel.onOngoingCallButtonsChanged(
                                shownButtons - button
                            )
                        },
                        modifier = Modifier.testTag("control_toggle_${button.name}")
                    )
                }
            }
            hiddenButtons.forEach { button ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    Text(
                        text = button.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = false,
                        onCheckedChange = {
                            settingsViewModel.onOngoingCallButtonsChanged(
                                shownButtons + button
                            )
                        },
                        modifier = Modifier.testTag("control_toggle_${button.name}")
                    )
                }
            }
        }

        // End call position: where the red end-call button sits
        // on the ongoing call screen.
        Text(
            text = "End call position",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.small)
        ) {
            EndCallAlignment.entries.forEach { alignment ->
                FilterChip(
                    selected = settings.endCallAlignment == alignment,
                    onClick = { settingsViewModel.onEndCallAlignmentChanged(alignment) },
                    label = { Text(text = alignment.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("end_call_alignment_${alignment.name.lowercase()}")
                )
            }
        }

        // Background style: swatch + label per option.
        Text(
            text = stringResource(id = R.string.settings_call_background_style),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing.medium),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            CallBackground.entries.forEach { option ->
                val isSelected = option == selected
                val optionSource = remember { MutableInteractionSource() }
                val optionColor = if (option == CallBackground.CUSTOM) {
                    settings.callBackgroundCustomColor
                } else {
                    accentColor
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .pressScale(optionSource)
                        .clickable(
                            interactionSource = optionSource,
                            indication = ripple()
                        ) {
                            lightHaptic()
                            if (option == CallBackground.CUSTOM && isSelected) {
                                // Tapping the selected Custom swatch edits its color.
                                onEditCustomColor()
                            } else {
                                onSelect(option)
                            }
                        }
                        .testTag("call_background_option_${option.name}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(callBackgroundBrush(option, optionColor))
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    )
                                } else {
                                    Modifier
                                }
                            )
                    )
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(top = spacing.extraSmall)
                    )
                }
            }
        }

        // Dim: black scrim over the background, 0-100%.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = R.string.settings_call_background_dim),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${(dimDraft * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = dimDraft,
            onValueChange = onDimDraftChange,
            onValueChangeFinished = {
                settingsViewModel.onCallBackgroundDimChanged(dimDraft)
            },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth().testTag("call_background_dim_slider")
        )
        Text(
            text = stringResource(id = R.string.settings_call_background_dim_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Number under the caller name.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = R.string.settings_call_show_number),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(id = R.string.settings_call_show_number_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = settings.showNumberOnCallScreen,
                onCheckedChange = { settingsViewModel.onShowNumberOnCallScreenChanged(it) },
                modifier = Modifier.testTag("call_show_number_switch")
            )
        }
    }
}

/** One-tap layout presets: fixed combinations of gesture, answer side,
 * and end-call position. Not stored — the selected preset is derived
 * from the current values, so a manual tweak shows no preset selected. */
private enum class CallLayoutPreset(
    val label: String,
    val gesture: AnswerGesture,
    val side: AnswerSide,
    val endAlignment: EndCallAlignment
) {
    CLASSIC("Classic", AnswerGesture.TAP, AnswerSide.RIGHT, EndCallAlignment.CENTER),
    LEFT_HANDED("Left-handed", AnswerGesture.TAP, AnswerSide.LEFT, EndCallAlignment.LEFT),
    SWIPE("Swipe", AnswerGesture.SWIPE, AnswerSide.RIGHT, EndCallAlignment.CENTER)
}
