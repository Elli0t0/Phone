package io.github.elli0t0.phone.feature.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.elli0t0.phone.PhoneApplication
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LiquidGlassChip
import io.github.elli0t0.phone.core.design.LiquidGlassIconButton
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.Motion
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.callBackgroundBrush
import io.github.elli0t0.phone.core.design.pressScale
import io.github.elli0t0.phone.core.design.rememberLightHaptic
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import io.github.elli0t0.phone.data.settings.model.AnswerSide
import io.github.elli0t0.phone.data.settings.model.CallBackground
import io.github.elli0t0.phone.data.settings.model.AnswerButtonSize
import io.github.elli0t0.phone.data.settings.model.CallerInfoPosition
import io.github.elli0t0.phone.data.settings.model.ControlGridAlignment
import io.github.elli0t0.phone.data.settings.model.EndCallButtonSize
import io.github.elli0t0.phone.data.settings.model.EndCallAlignment
import io.github.elli0t0.phone.data.settings.model.OngoingCallButton
import kotlin.math.roundToInt

/**
 * The option sections under the call-background preview: layout preset,
 * answer side, call controls, end-call position,
 * background style, dim, and number visibility. Every change saves
 * immediately and shows in the preview. Split out of
 * CallBackgroundScreen so the studio stays small enough to ship.
 * Detailed groups hide unless advanced settings are on; background
 * style and number visibility always show.
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
    // Detailed groups hide unless advanced settings are on.
    val advanced = settings.showAdvancedSettings
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
        // Layout preset: one tap that sets answer side and end-call
        // position together. Any manual change after applying a
        // preset leaves no preset selected (custom).
        if (advanced) {
        StudioGroupCard(title = stringResource(id = R.string.studio_layout_preset)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                CallLayoutPreset.entries.forEach { preset ->
                    val presetSelected = settings.answerSide == preset.side &&
                        settings.endCallAlignment == preset.endAlignment
                    StudioOptionChip(
                        selected = presetSelected,
                        onClick = {
                            settingsViewModel.onAnswerSideChanged(preset.side)
                            settingsViewModel.onEndCallAlignmentChanged(preset.endAlignment)
                        },
                        label = stringResource(id = preset.labelRes),
                        testTag = "layout_preset_${preset.name.lowercase()}"
                    )
                }
            }
        }
        }

        // Answer side: which side the answer button (or swipe
        // direction) sits on.
        if (advanced) {
        StudioGroupCard(title = stringResource(id = R.string.studio_answer_side)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                AnswerSide.entries.forEach { side ->
                    StudioOptionChip(
                        selected = settings.answerSide == side,
                        onClick = {
                            settingsViewModel.onAnswerSideChanged(side)
                        },
                        label = side.label,
                        testTag = "answer_side_${side.name.lowercase()}"
                    )
                }
            }
        }
        }

        // Answer button size: how big the answer/decline buttons
        // (or the swipe thumb) are on the incoming call screen.
        if (advanced) {
        StudioGroupCard(title = stringResource(id = R.string.studio_answer_button_size)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                AnswerButtonSize.entries.forEach { size ->
                    StudioOptionChip(
                        selected = settings.answerButtonSize == size,
                        onClick = {
                            settingsViewModel.onAnswerButtonSizeChanged(size)
                        },
                        label = size.label,
                        testTag = "answer_button_size_${size.name.lowercase()}"
                    )
                }
            }
        }
        }

        // Quick replies: show the quick-reply chips on incoming calls.
        if (advanced) {
        StudioGroupCard(title = null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(id = R.string.studio_quick_replies),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                AppSwitch(
                    checked = settings.showQuickReplies,
                    onCheckedChange = { settingsViewModel.onShowQuickRepliesChanged(it) },
                    modifier = Modifier.testTag("quick_replies_switch")
                )
            }
        }
        }

        // Call controls: which ongoing-call buttons show, in order.
        // Shown buttons can be reordered (up/down) or hidden;
        // hidden buttons can be added back.
        if (advanced) {
        val shownButtons = settings.ongoingCallButtons
        val hiddenButtons = OngoingCallButton.entries - shownButtons.toSet()
        StudioGroupCard(title = stringResource(id = R.string.studio_call_controls)) {
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
                    ReorderButton(
                        iconRes = R.drawable.ic_chevron_up,
                        contentDescription = stringResource(id = R.string.call_control_move_up),
                        enabled = index > 0,
                        onClick = {
                            val list = shownButtons.toMutableList()
                            val item = list.removeAt(index)
                            list.add(index - 1, item)
                            settingsViewModel.onOngoingCallButtonsChanged(list)
                        },
                        testTag = "control_move_up_${button.name}"
                    )
                    ReorderButton(
                        iconRes = R.drawable.ic_chevron_down,
                        contentDescription = stringResource(id = R.string.call_control_move_down),
                        enabled = index < shownButtons.lastIndex,
                        onClick = {
                            val list = shownButtons.toMutableList()
                            val item = list.removeAt(index)
                            list.add(index + 1, item)
                            settingsViewModel.onOngoingCallButtonsChanged(list)
                        },
                        testTag = "control_move_down_${button.name}"
                    )
                    AppSwitch(
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
                    AppSwitch(
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
        }
        }

        // Control grid alignment: how the ongoing-call buttons
        // spread across each row.
        if (advanced) {
        StudioGroupCard(title = stringResource(id = R.string.studio_control_grid_alignment)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                ControlGridAlignment.entries.forEach { alignment ->
                    StudioOptionChip(
                        selected = settings.controlGridAlignment == alignment,
                        onClick = {
                            settingsViewModel.onControlGridAlignmentChanged(alignment)
                        },
                        label = alignment.label,
                        testTag = "control_grid_alignment_${alignment.name.lowercase()}"
                    )
                }
            }
        }
        }

        // End call position: where the red end-call button sits
        // on the ongoing call screen.
        if (advanced) {
        StudioGroupCard(title = stringResource(id = R.string.studio_end_call_position)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                EndCallAlignment.entries.forEach { alignment ->
                    StudioOptionChip(
                        selected = settings.endCallAlignment == alignment,
                        onClick = {
                            settingsViewModel.onEndCallAlignmentChanged(alignment)
                        },
                        label = alignment.label,
                        testTag = "end_call_alignment_${alignment.name.lowercase()}"
                    )
                }
            }
        }
        }

        // End call size: how big the red end-call button is.
        if (advanced) {
        StudioGroupCard(title = stringResource(id = R.string.studio_end_call_size)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                EndCallButtonSize.entries.forEach { size ->
                    StudioOptionChip(
                        selected = settings.endCallButtonSize == size,
                        onClick = {
                            settingsViewModel.onEndCallButtonSizeChanged(size)
                        },
                        label = size.label,
                        testTag = "end_call_size_${size.name.lowercase()}"
                    )
                }
            }
        }
        }

        // Caller info position: name/number at the top, or
        // vertically centered above the controls.
        if (advanced) {
        StudioGroupCard(title = stringResource(id = R.string.studio_caller_info_position)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                CallerInfoPosition.entries.forEach { position ->
                    StudioOptionChip(
                        selected = settings.callerInfoPosition == position,
                        onClick = {
                            settingsViewModel.onCallerInfoPositionChanged(position)
                        },
                        label = position.label,
                        testTag = "caller_info_position_${position.name.lowercase()}"
                    )
                }
            }
        }
        }

        // Background style: swatch + label per option.
        StudioGroupCard(title = stringResource(id = R.string.settings_call_background_style)) {
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
        }

        // Dim: black scrim over the background, 0-100%.
        if (advanced) {
        StudioGroupCard(title = null) {
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
        }
        }

        // Number under the caller name.
        StudioGroupCard(title = null) {
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
                AppSwitch(
                    checked = settings.showNumberOnCallScreen,
                    onCheckedChange = { settingsViewModel.onShowNumberOnCallScreenChanged(it) },
                    modifier = Modifier.testTag("call_show_number_switch")
                )
            }
        }
    }
}

/** One-tap layout presets: fixed combinations of answer side and
 * end-call position. Not stored — the selected preset is derived
 * from the current values, so a manual tweak shows no preset selected. */
private enum class CallLayoutPreset(
    val labelRes: Int,
    val side: AnswerSide,
    val endAlignment: EndCallAlignment
) {
    CLASSIC(R.string.studio_preset_classic, AnswerSide.RIGHT, EndCallAlignment.CENTER),
    LEFT_HANDED(R.string.studio_preset_left_handed, AnswerSide.LEFT, EndCallAlignment.LEFT)
}

/**
 * Option-selector chip for the Call Studio: selected chips are a
 * filled accent pill with onPrimary text (readable on the accent),
 * unselected chips are liquid glass. Both keep the animated
 * selection — a scale pop on Animations ON, and an animated label
 * color (a fade on REDUCED, instant on OFF).
 */
@Composable
fun StudioOptionChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val option = LocalAppSettings.current.animationOption
    val spacing = LocalSpacing.current
    val scale by animateFloatAsState(
        targetValue = if (option == AnimationOption.ON && selected) {
            Motion.StudioChipSelectedScale
        } else {
            1f
        },
        animationSpec = tween(
            durationMillis = Motion.scaledDurationMs(Motion.Durations.Short, option)
        ),
        label = "studioChipScale"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(
            durationMillis = Motion.scaledDurationMs(Motion.Durations.Short, option)
        ),
        label = "studioChipLabel"
    )
    val chipModifier = modifier
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .testTag(testTag)
    if (selected) {
        // Filled accent pill: solid primary, onPrimary text.
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = chipModifier
                .clip(RoundedCornerShape(percent = 50))
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onClick)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = labelColor,
                modifier = Modifier.padding(
                    horizontal = spacing.medium,
                    vertical = spacing.small
                )
            )
        }
    } else {
        LiquidGlassChip(
            selected = selected,
            onClick = onClick,
            modifier = chipModifier
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = labelColor,
                modifier = Modifier.padding(
                    horizontal = spacing.medium,
                    vertical = spacing.small
                )
            )
        }
    }
}

/**
 * One 40dp glass reorder button (chevron up/down) in the Call
 * controls list. Disabled buttons are dimmed and swallow the tap.
 */
@Composable
private fun ReorderButton(
    iconRes: Int,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    LiquidGlassIconButton(
        onClick = { if (enabled) onClick() },
        modifier = modifier
            .size(PhoneDimens.studioReorderButtonSize)
            .graphicsLayer {
                alpha = if (enabled) 1f else PhoneDimens.studioReorderDisabledAlpha
            }
            .testTag(testTag)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(PhoneDimens.studioReorderIconSize)
        )
    }
}
