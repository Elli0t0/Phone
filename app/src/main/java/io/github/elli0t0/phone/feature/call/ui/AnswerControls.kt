package io.github.elli0t0.phone.feature.call.ui

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.InCallControlBackgroundFlat
import io.github.elli0t0.phone.core.design.LiquidGlassChip
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.Motion
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.pressScale
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import io.github.elli0t0.phone.data.settings.model.AnswerStyle
/**
 * Answer / decline controls for an incoming call, in the style chosen
 * in Settings. One entry point used by the incoming call screen and by
 * the settings previews. Every animation honours the Animations
 * setting: On = full, Reduced = fades only (no springs, no loops),
 * Off = instant. Gestures are not usable with TalkBack, so the
 * container exposes Answer / Decline as custom semantics actions.
 */
@Composable
fun AnswerControls(
    style: AnswerStyle,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onReply: (String) -> Unit = {},
    quickReplyMessages: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    val answerLabel = stringResource(id = R.string.call_action_answer)
    val declineLabel = stringResource(id = R.string.call_action_decline)
    Column(
        modifier = modifier.semantics {
            customActions = listOf(
                CustomAccessibilityAction(answerLabel) { onAnswer(); true },
                CustomAccessibilityAction(declineLabel) { onDecline(); true }
            )
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (quickReplyMessages.isNotEmpty()) {
            AnswerQuickReplies(messages = quickReplyMessages, onReply = onReply)
            Spacer(modifier = Modifier.height(LocalSpacing.current.medium))
        }
        when (style) {
            AnswerStyle.CLASSIC -> ClassicAnswerControls(onAnswer = onAnswer, onDecline = onDecline)
            AnswerStyle.SLIDER -> SliderAnswerControls(onAnswer = onAnswer, onDecline = onDecline)
            AnswerStyle.SWIPE_BUTTONS -> SwipeButtonsAnswerControls(onAnswer = onAnswer, onDecline = onDecline)
            AnswerStyle.LIQUID -> LiquidAnswerControls(onAnswer = onAnswer, onDecline = onDecline)
        }
    }
}

/** Spring-back spec gated by the Animations setting (no springs unless On). */

internal fun backSpecFloat(
    option: AnimationOption,
    damping: Float = 0.6f,
    stiffness: Float = 400f
): AnimationSpec<Float> = when (option) {
    AnimationOption.ON -> spring(dampingRatio = damping, stiffness = stiffness)
    AnimationOption.REDUCED -> tween(durationMillis = Motion.Durations.Short)
    AnimationOption.OFF -> snap()
}

/** Quick-reply chips rendered above the controls (previews / opt-in). */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnswerQuickReplies(
    messages: List<String>,
    onReply: (String) -> Unit
) {
    val glass = LocalAppSettings.current.glassCallScreens
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(PhoneDimens.incomingChipSpacing, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(PhoneDimens.incomingChipSpacing),
        modifier = Modifier.fillMaxWidth()
    ) {
        messages.forEachIndexed { index, message ->
            if (glass) {
                LiquidGlassChip(
                    selected = false,
                    onClick = { onReply(message) },
                    modifier = Modifier.testTag("answer_quick_reply_$index"),
                    forceDarkGlass = true
                ) {
                    Text(
                        text = message,
                        color = Color.White,
                        modifier = Modifier.padding(
                            horizontal = PhoneDimens.incomingChipHorizontalPadding,
                            vertical = PhoneDimens.incomingChipVerticalPadding
                        )
                    )
                }
            } else {
                val chipSource = remember { MutableInteractionSource() }
                AssistChip(
                    onClick = { onReply(message) },
                    label = { Text(text = message) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = InCallControlBackgroundFlat,
                        labelColor = Color.White
                    ),
                    interactionSource = chipSource,
                    modifier = Modifier
                        .pressScale(chipSource)
                        .testTag("answer_quick_reply_$index")
                )
            }
        }
    }
}
