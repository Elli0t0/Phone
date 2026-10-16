package io.github.elli0t0.phone.feature.call.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.common.PhoneNumberFormatter
import io.github.elli0t0.phone.data.settings.model.AccentPreset
import io.github.elli0t0.phone.data.settings.model.AnswerGesture
import io.github.elli0t0.phone.data.settings.model.AnswerSide
import io.github.elli0t0.phone.data.settings.model.CallerInfoPosition
import io.github.elli0t0.phone.core.design.CallAnswerGreen
import io.github.elli0t0.phone.core.design.CallDeclineRed
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.PulseRings
import io.github.elli0t0.phone.core.design.callBackgroundBrush
import io.github.elli0t0.phone.core.design.pressScale
import io.github.elli0t0.phone.core.design.rememberLightHaptic
import io.github.elli0t0.phone.data.call.OngoingCallModel
import io.github.elli0t0.phone.data.settings.model.CallBackground
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IncomingCallScreen(
    callModel: OngoingCallModel,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onDeclineWithMessage: (String) -> Unit = {},
    quickDeclineMessages: List<String>,
    callBackground: CallBackground = CallBackground.DEFAULT,
    accentColor: Long = AccentPreset.ONE_UI_BLUE.colorValue,
    // Black scrim over the background (0 = none). Number under the name.
    dim: Float = 0f,
    showNumber: Boolean = true,
    answerGesture: AnswerGesture = AnswerGesture.TAP,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val lightHaptic = rememberLightHaptic()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(callBackgroundBrush(callBackground, accentColor))
            .testTag("incoming_call_screen")
    ) {
        if (dim > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dim.coerceIn(0f, 1f)))
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spacing.screenHorizontalPadding, vertical = spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Caller info position: top (default), or centered in the
            // space above the quick responses via spacers around it.
            val callerInfoCentered =
                LocalAppSettings.current.callerInfoPosition == CallerInfoPosition.CENTER
            if (callerInfoCentered) {
                Spacer(modifier = Modifier.weight(1f))
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = PhoneDimens.incomingHeaderTopPadding)
            ) {
            Text(
                text = stringResource(id = R.string.call_incoming),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = spacing.medium)
            )

            val hasName = !callModel.displayName.isNullOrBlank() && callModel.displayName != callModel.number
            val formattedNumber = PhoneNumberFormatter.format(callModel.number)
            val titleText = if (hasName) callModel.displayName!! else formattedNumber
            val initial = titleText.trim().take(1).uppercase().ifBlank { "?" }

            Box(
                modifier = Modifier
                    .size(PhoneDimens.incomingAvatarSize)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(spacing.medium))

            Text(
                text = titleText,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = PhoneDimens.incomingTitleSize
                ),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = spacing.extraSmall)
            )

            if (hasName && showNumber && callModel.number.isNotBlank()) {
                Text(
                    text = formattedNumber,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }

        if (callerInfoCentered) {
            Spacer(modifier = Modifier.weight(1f))
        }

        // Quick responses: decline the call and send the message as a reply SMS
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = spacing.medium)
        ) {
            Text(
                text = stringResource(id = R.string.quick_reply_title),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = spacing.small)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(PhoneDimens.incomingChipSpacing, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(PhoneDimens.incomingChipSpacing),
                modifier = Modifier.fillMaxWidth()
            ) {
                quickDeclineMessages.forEachIndexed { index, message ->
                    AssistChip(
                        onClick = { onDeclineWithMessage(message) },
                        label = { Text(text = message) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color.White.copy(alpha = 0.12f),
                            labelColor = Color.White
                        ),
                        modifier = Modifier.testTag("quick_reply_$index")
                    )
                }
            }
        }

        if (answerGesture == AnswerGesture.SWIPE) {
            // Swipe to answer / decline instead of buttons.
            SwipeAnswerBar(
                onAnswer = onAnswer,
                onDecline = onDecline,
                mirror = LocalAppSettings.current.answerSide == AnswerSide.LEFT,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = PhoneDimens.incomingActionsBottomPadding)
            )
        } else {
            // Answer & Decline Buttons, in the configured side order.
            val answerOnLeft = LocalAppSettings.current.answerSide == AnswerSide.LEFT
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = PhoneDimens.incomingActionsBottomPadding),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (answerOnLeft) {
                    AnswerButton(onAnswer = onAnswer)
                    DeclineButton(onDecline = onDecline)
                } else {
                    DeclineButton(onDecline = onDecline)
                    AnswerButton(onAnswer = onAnswer)
                }
            }
        }
        }
    }
}

/**
 * Swipe to answer / decline: drag the thumb right to answer, left to
 * decline. The thumb follows the finger; releasing past the threshold
 * answers or declines, otherwise the thumb snaps back to the center.
 */
@Composable
private fun SwipeAnswerBar(
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    mirror: Boolean = false,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val lightHaptic = rememberLightHaptic()
    var dragOffset by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val thresholdPx = remember(density) { with(density) { 96.dp.toPx() } }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(PhoneDimens.incomingActionButtonSize)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.15f))
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            dragOffset = (dragOffset + dragAmount)
                                .coerceIn(-thresholdPx * 1.5f, thresholdPx * 1.5f)
                        },
                        onDragEnd = {
                            when {
                                dragOffset > thresholdPx -> {
                                    lightHaptic()
                                    if (mirror) onDecline() else onAnswer()
                                }
                                dragOffset < -thresholdPx -> {
                                    lightHaptic()
                                    if (mirror) onAnswer() else onDecline()
                                }
                            }
                            dragOffset = 0f
                        },
                        onDragCancel = { dragOffset = 0f }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(
                    id = if (mirror) R.drawable.ic_phone else R.drawable.ic_call_end
                ),
                contentDescription = null,
                tint = if (mirror) CallAnswerGreen else CallDeclineRed,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = spacing.medium)
                    .size(PhoneDimens.incomingActionIconSize)
            )
            Icon(
                painter = painterResource(
                    id = if (mirror) R.drawable.ic_call_end else R.drawable.ic_phone
                ),
                contentDescription = null,
                tint = if (mirror) CallDeclineRed else CallAnswerGreen,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = spacing.medium)
                    .size(PhoneDimens.incomingActionIconSize)
            )
            Box(
                modifier = Modifier
                    .offset { IntOffset(dragOffset.roundToInt(), 0) }
                    .size(PhoneDimens.incomingActionButtonSize)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_phone),
                    contentDescription = null,
                    tint = CallAnswerGreen,
                    modifier = Modifier.size(PhoneDimens.incomingActionIconSize)
                )
            }
        }
        Spacer(modifier = Modifier.height(spacing.extraSmall))
        Text(
            text = if (mirror) {
                "Swipe left to answer · Swipe right to decline"
            } else {
                "Swipe right to answer · Swipe left to decline"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

/** Decline button: red circle with glow ring + label. */
@Composable
private fun DeclineButton(onDecline: () -> Unit) {
    val spacing = LocalSpacing.current
    val source = remember { MutableInteractionSource() }
    val lightHaptic = rememberLightHaptic()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PulseRings(
            color = CallDeclineRed,
            modifier = Modifier.size(PhoneDimens.incomingActionButtonSize)
        ) {
            Box(
                modifier = Modifier
                    .size(PhoneDimens.incomingActionButtonSize)
                    .clip(CircleShape)
                    .background(CallDeclineRed)
                    .pressScale(source)
                    .clickable(
                        interactionSource = source,
                        indication = ripple()
                    ) { lightHaptic(); onDecline() }
                    .testTag("decline_call_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_call_end),
                    contentDescription = stringResource(id = R.string.call_action_decline),
                    tint = Color.White,
                    modifier = Modifier.size(PhoneDimens.incomingActionIconSize)
                )
            }
        }
        Spacer(modifier = Modifier.height(spacing.extraSmall))
        Text(
            text = stringResource(id = R.string.call_action_decline),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

/** Answer button: green circle with glow ring + label. */
@Composable
private fun AnswerButton(onAnswer: () -> Unit) {
    val spacing = LocalSpacing.current
    val source = remember { MutableInteractionSource() }
    val lightHaptic = rememberLightHaptic()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PulseRings(
            color = CallAnswerGreen,
            modifier = Modifier.size(PhoneDimens.incomingActionButtonSize)
        ) {
            Box(
                modifier = Modifier
                    .size(PhoneDimens.incomingActionButtonSize)
                    .clip(CircleShape)
                    .background(CallAnswerGreen)
                    .pressScale(source)
                    .clickable(
                        interactionSource = source,
                        indication = ripple()
                    ) { lightHaptic(); onAnswer() }
                    .testTag("answer_call_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_phone),
                    contentDescription = stringResource(id = R.string.call_action_answer),
                    tint = Color.White,
                    modifier = Modifier.size(PhoneDimens.incomingActionIconSize)
                )
            }
        }
        Spacer(modifier = Modifier.height(spacing.extraSmall))
        Text(
            text = stringResource(id = R.string.call_action_answer),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}
