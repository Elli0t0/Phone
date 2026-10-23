package io.github.elli0t0.phone.feature.call.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.round
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.CallAnswerGreen
import io.github.elli0t0.phone.core.design.CallDeclineRed
import io.github.elli0t0.phone.core.design.InCallControlBackgroundFlat
import io.github.elli0t0.phone.core.design.LiquidGlassChip
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.Motion
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.bouncyClick
import io.github.elli0t0.phone.core.design.liquidGlass
import io.github.elli0t0.phone.core.design.pressScale
import io.github.elli0t0.phone.core.design.rememberLightHaptic
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import io.github.elli0t0.phone.data.settings.model.AnswerSide
import io.github.elli0t0.phone.data.settings.model.AnswerStyle
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

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
private fun backSpecFloat(
    option: AnimationOption,
    damping: Float = 0.6f,
    stiffness: Float = 400f
): AnimationSpec<Float> = when (option) {
    AnimationOption.ON -> spring(dampingRatio = damping, stiffness = stiffness)
    AnimationOption.REDUCED -> tween(durationMillis = Motion.Durations.Short)
    AnimationOption.OFF -> snap()
}

private fun backSpecOffset(
    option: AnimationOption,
    damping: Float = 0.6f,
    stiffness: Float = 400f
): AnimationSpec<Offset> = when (option) {
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

// ----- CLASSIC -----

@Composable
private fun ClassicAnswerControls(
    onAnswer: () -> Unit,
    onDecline: () -> Unit
) {
    val answerOnLeft = LocalAppSettings.current.answerSide == AnswerSide.LEFT
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        if (answerOnLeft) {
            ClassicAnswerButton(isAnswer = true, onClick = onAnswer, haloDelayMillis = 400)
            ClassicAnswerButton(isAnswer = false, onClick = onDecline, haloDelayMillis = 0)
        } else {
            ClassicAnswerButton(isAnswer = false, onClick = onDecline, haloDelayMillis = 0)
            ClassicAnswerButton(isAnswer = true, onClick = onAnswer, haloDelayMillis = 400)
        }
    }
}

@Composable
private fun ClassicAnswerButton(
    isAnswer: Boolean,
    onClick: () -> Unit,
    haloDelayMillis: Int
) {
    val option = LocalAppSettings.current.animationOption
    val source = remember { MutableInteractionSource() }
    val lightHaptic = rememberLightHaptic()
    val color = if (isAnswer) CallAnswerGreen else CallDeclineRed
    val size = PhoneDimens.incomingActionButtonSize * LocalAppSettings.current.answerButtonSize.scale
    val haloTransition = rememberInfiniteTransition(label = "halo")
    val haloP by haloTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, delayMillis = haloDelayMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "haloP"
    )
    val wiggleTransition = rememberInfiniteTransition(label = "wiggle")
    val wiggle by wiggleTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 3000
                0f at 0
                -14f at 167
                14f at 333
                0f at 500
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "wiggleR"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            // Halo behind the button only: never over the label or the
            // other button.
            if (option == AnimationOption.ON) {
                Box(
                    modifier = Modifier
                        .size(size)
                        .graphicsLayer {
                            scaleX = 1f + 0.25f * haloP
                            scaleY = 1f + 0.25f * haloP
                            alpha = 0.35f * (1f - haloP)
                        }
                        .background(color, CircleShape)
                )
            } else if (option == AnimationOption.REDUCED) {
                Box(
                    modifier = Modifier
                        .size(size)
                        .graphicsLayer {
                            scaleX = 1.12f
                            scaleY = 1.12f
                            alpha = 0.18f
                        }
                        .background(color, CircleShape)
                )
            }
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(color)
                    .pressScale(source)
                    .clickable(
                        interactionSource = source,
                        indication = ripple()
                    ) { lightHaptic(); onClick() }
                    .testTag(if (isAnswer) "answer_call_button" else "decline_call_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isAnswer) R.drawable.ic_phone else R.drawable.ic_call_end
                    ),
                    contentDescription = stringResource(
                        id = if (isAnswer) R.string.call_action_answer else R.string.call_action_decline
                    ),
                    tint = Color.White,
                    modifier = Modifier
                        .size(PhoneDimens.incomingActionIconSize)
                        .graphicsLayer { rotationZ = if (isAnswer && option == AnimationOption.ON) wiggle else 0f }
                )
            }
        }
        Spacer(modifier = Modifier.height(PhoneDimens.answerLabelGap))
        Text(
            text = stringResource(
                id = if (isAnswer) R.string.call_action_answer else R.string.call_action_decline
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

// ----- SLIDER -----

@Composable
private fun SliderAnswerControls(
    onAnswer: () -> Unit,
    onDecline: () -> Unit
) {
    val spacing = LocalSpacing.current
    val source = remember { MutableInteractionSource() }
    val lightHaptic = rememberLightHaptic()
    Column(modifier = Modifier.fillMaxWidth()) {
        // Decline: red circle at the left, above the track.
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(PhoneDimens.sliderDeclineSize)
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
            Spacer(modifier = Modifier.height(PhoneDimens.answerLabelGap))
            Text(
                text = stringResource(id = R.string.call_action_decline),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
        Spacer(modifier = Modifier.height(spacing.medium))
        SlideToAnswer(
            label = stringResource(id = R.string.call_slide_to_answer),
            onAnswered = onAnswer,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SlideToAnswer(
    label: String,
    onAnswered: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val option = LocalAppSettings.current.animationOption
    val glass = LocalAppSettings.current.glassCallScreens
    val knob = PhoneDimens.sliderKnobSize
    val pad = PhoneDimens.sliderTrackPadding
    var trackW by remember { mutableIntStateOf(0) }
    val x = remember { Animatable(0f) }
    val density = LocalDensity.current
    val maxX = with(density) { (trackW - (knob + pad * 2).toPx()).coerceAtLeast(1f) }
    val progress = (x.value / maxX).coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(PhoneDimens.sliderTrackHeight)
            .onSizeChanged { trackW = it.width }
            .then(
                if (glass) {
                    Modifier.liquidGlass(CircleShape, forceDarkGlass = true)
                } else {
                    Modifier.background(InCallControlBackgroundFlat, CircleShape)
                }
            )
            .padding(pad)
    ) {
        // Green fill growing behind the knob.
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(with(density) { (x.value + knob.toPx()).toDp() })
                .clip(CircleShape)
                .background(CallAnswerGreen.copy(alpha = 0.55f * progress))
        )
        if (option == AnimationOption.ON) {
            ShimmerLabel(
                text = label,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer { alpha = (1f - progress * 1.4f).coerceIn(0f, 1f) }
            )
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer { alpha = (1f - progress * 1.4f).coerceIn(0f, 1f) }
            )
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(x.value.roundToInt(), 0) }
                .size(knob)
                .clip(CircleShape)
                .background(Color.White)
                .pointerInput(maxX, option) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (x.value >= maxX * 0.85f) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    x.animateTo(maxX, tween(100))
                                    onAnswered()
                                } else {
                                    x.animateTo(0f, backSpecFloat(option))
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { x.animateTo(0f, backSpecFloat(option)) }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        scope.launch { x.snapTo((x.value + dragAmount).coerceIn(0f, maxX)) }
                    }
                },
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
}

/** "Slide to answer" label with a moving shine (Animations On only). */
@Composable
private fun ShimmerLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val s by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerS"
    )
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge.copy(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.45f),
                    Color.White,
                    Color.White.copy(alpha = 0.45f)
                ),
                start = Offset(s * 600f - 300f, 0f),
                end = Offset(s * 600f, 0f)
            )
        )
    )
}

// ----- SWIPE BUTTONS -----

@Composable
private fun SwipeButtonsAnswerControls(
    onAnswer: () -> Unit,
    onDecline: () -> Unit
) {
    var committed by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        SwipeButtonColumn(
            isAnswer = false,
            anyCommitted = committed,
            onCommit = { committed = true; onDecline() }
        )
        SwipeButtonColumn(
            isAnswer = true,
            anyCommitted = committed,
            onCommit = { committed = true; onAnswer() }
        )
    }
}

@Composable
private fun SwipeButtonColumn(
    isAnswer: Boolean,
    anyCommitted: Boolean,
    onCommit: () -> Unit
) {
    val option = LocalAppSettings.current.animationOption
    val transition = rememberInfiniteTransition(label = "chevrons")
    val chevronAlphas = (0 until 3).map { index ->
        val alpha by transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 450, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
                initialStartOffset = androidx.compose.animation.core.StartOffset(index * 300)
            ),
            label = "chevron$index"
        )
        alpha
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Drag hint: three chevrons pulsing in sequence (900 ms loop).
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            chevronAlphas.forEach { animatedAlpha ->
                Icon(
                    painter = painterResource(id = R.drawable.ic_chevron_up),
                    contentDescription = null,
                    tint = Color.White.copy(
                        alpha = if (option == AnimationOption.ON) animatedAlpha else 0.45f
                    ),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(LocalSpacing.current.small))
        SwipeDragButton(isAnswer = isAnswer, anyCommitted = anyCommitted, onCommit = onCommit)
        Spacer(modifier = Modifier.height(PhoneDimens.answerLabelGap))
        Text(
            text = stringResource(
                id = if (isAnswer) R.string.call_action_answer else R.string.call_action_decline
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun SwipeDragButton(
    isAnswer: Boolean,
    anyCommitted: Boolean,
    onCommit: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val option = LocalAppSettings.current.animationOption
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val fade = remember { Animatable(1f) }
    var gone by remember { mutableStateOf(false) }
    var thresholdFired by remember { mutableStateOf(false) }
    var lastSegmentTime by remember { mutableStateOf(0L) }
    var lastSegmentSpeedPx by remember { mutableStateOf(0f) }
    val commitPx = with(density) { PhoneDimens.swipeCommitDistance.toPx() }
    val color = if (isAnswer) CallAnswerGreen else CallDeclineRed
    // When the other button commits, this one only fades out.
    LaunchedEffect(anyCommitted) {
        if (anyCommitted && !gone) {
            fade.animateTo(0f, tween(durationMillis = Motion.Durations.Short))
        }
    }
    Box(
        modifier = Modifier
            .offset { offset.value.round() }
            .graphicsLayer {
                val progress = (offset.value.getDistance() / commitPx).coerceIn(0f, 1f)
                val k = 1f + 0.12f * progress
                scaleX = k
                scaleY = k
                alpha = fade.value
            }
            .drawBehind {
                val progress = (offset.value.getDistance() / commitPx).coerceIn(0f, 1f)
                if (progress > 0f) {
                    val radius = size.maxDimension * 1.1f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                color.copy(alpha = 0.45f * progress),
                                Color.Transparent
                            ),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )
                }
            }
            .size(PhoneDimens.incomingActionButtonSize)
            .clip(CircleShape)
            .background(color)
            .pointerInput(option) {
                detectDragGestures(
                    onDragStart = {
                        thresholdFired = false
                        lastSegmentSpeedPx = 0f
                        lastSegmentTime = 0L
                    },
                    onDragEnd = {
                        scope.launch {
                            val speedDp = with(density) { lastSegmentSpeedPx.toDp().value }
                            val distance = offset.value.getDistance()
                            if (distance >= commitPx || speedDp > 1200f) {
                                gone = true
                                val direction = offset.value / distance.coerceAtLeast(1f)
                                val target = offset.value + direction * with(density) { 120.dp.toPx() }
                                launch { offset.animateTo(target, tween(durationMillis = Motion.Durations.Short)) }
                                fade.animateTo(0f, tween(durationMillis = Motion.Durations.Short))
                                onCommit()
                            } else {
                                offset.animateTo(Offset.Zero, backSpecOffset(option))
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch { offset.animateTo(Offset.Zero, backSpecOffset(option)) }
                    }
                ) { change, drag ->
                    change.consume()
                    val now = change.uptimeMillis
                    if (lastSegmentTime > 0L && now > lastSegmentTime) {
                        val dtSeconds = (now - lastSegmentTime) / 1000f
                        if (dtSeconds > 0f) {
                            lastSegmentSpeedPx = drag.getDistance() / dtSeconds
                        }
                    }
                    lastSegmentTime = now
                    scope.launch {
                        offset.snapTo(offset.value + drag)
                        val distance = offset.value.getDistance()
                        if (!thresholdFired && distance >= commitPx) {
                            thresholdFired = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        } else if (distance < commitPx * 0.8f) {
                            thresholdFired = false
                        }
                    }
                }
            }
            .testTag(if (isAnswer) "answer_call_button" else "decline_call_button"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(
                id = if (isAnswer) R.drawable.ic_phone else R.drawable.ic_call_end
            ),
            contentDescription = stringResource(
                id = if (isAnswer) R.string.call_action_answer else R.string.call_action_decline
            ),
            tint = Color.White,
            modifier = Modifier.size(PhoneDimens.incomingActionIconSize)
        )
    }
}

// ----- LIQUID -----

@Composable
private fun LiquidAnswerControls(
    onAnswer: () -> Unit,
    onDecline: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val option = LocalAppSettings.current.animationOption
    val glass = LocalAppSettings.current.glassCallScreens
    val spacing = LocalSpacing.current
    val x = remember { Animatable(0f) }
    var widthPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val knob = PhoneDimens.liquidKnobSize
    val half = with(density) { ((widthPx - knob.toPx()) / 2f).coerceAtLeast(1f) }
    val idleTransition = rememberInfiniteTransition(label = "liquidIdle")
    val breathe by idleTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )
    val hintPulse by idleTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hintPulse"
    )
    Box(
        modifier = Modifier
            .widthIn(max = PhoneDimens.liquidPillMaxWidth)
            .fillMaxWidth()
            .height(PhoneDimens.liquidPillHeight)
            .onSizeChanged { widthPx = it.width }
            .then(
                if (glass) {
                    Modifier.liquidGlass(CircleShape, forceDarkGlass = true)
                } else {
                    Modifier.background(InCallControlBackgroundFlat, CircleShape)
                }
            )
            .drawBehind {
                val p = (x.value / half).coerceIn(-1f, 1f)
                val tint = if (p > 0) {
                    lerp(Color.Transparent, CallAnswerGreen, p)
                } else {
                    lerp(Color.Transparent, CallDeclineRed, -p)
                }
                drawRect(tint.copy(alpha = 0.45f * abs(p)))
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(id = R.string.call_action_decline),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = spacing.medium)
                .graphicsLayer {
                    val p = (x.value / half).coerceIn(-1f, 1f)
                    alpha = max(
                        if (option == AnimationOption.ON) hintPulse else 0.6f,
                        if (p < 0) -p else 0f
                    )
                }
        )
        Text(
            text = stringResource(id = R.string.call_action_answer),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = spacing.medium)
                .graphicsLayer {
                    val p = (x.value / half).coerceIn(-1f, 1f)
                    alpha = max(
                        if (option == AnimationOption.ON) hintPulse else 0.6f,
                        if (p > 0) p else 0f
                    )
                }
        )
        // Glass knob in the middle: drag right to answer, left to decline.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset { IntOffset(x.value.roundToInt(), 0) }
                .graphicsLayer {
                    val p = (x.value / half).coerceIn(-1f, 1f)
                    val idleScale = if (option == AnimationOption.ON && x.value == 0f) breathe else 1f
                    scaleX = idleScale * (1f + 0.25f * abs(p))
                    scaleY = idleScale * (1f - 0.08f * abs(p))
                }
                .size(knob)
                .clip(CircleShape)
                .background(Color.White)
                .pointerInput(half, option) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                val current = x.value / half
                                when {
                                    current >= 0.7f -> {
                                        x.animateTo(half, tween(100))
                                        onAnswer()
                                    }
                                    current <= -0.7f -> {
                                        x.animateTo(-half, tween(100))
                                        onDecline()
                                    }
                                    else -> x.animateTo(0f, backSpecFloat(option, 0.55f, 380f))
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { x.animateTo(0f, backSpecFloat(option, 0.55f, 380f)) }
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        scope.launch { x.snapTo((x.value + dragAmount).coerceIn(-half, half)) }
                    }
                },
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
}
