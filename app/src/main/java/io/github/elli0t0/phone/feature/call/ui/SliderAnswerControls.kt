package io.github.elli0t0.phone.feature.call.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.CallAnswerGreen
import io.github.elli0t0.phone.core.design.CallDeclineRed
import io.github.elli0t0.phone.core.design.InCallControlBackgroundFlat
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.liquidGlass
import io.github.elli0t0.phone.core.design.pressScale
import io.github.elli0t0.phone.core.design.rememberLightHaptic
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
@Composable
internal fun SliderAnswerControls(
    onAnswer: () -> Unit,
    onDecline: () -> Unit
) {
    val spacing = LocalSpacing.current
    val source = remember { MutableInteractionSource() }
    val lightHaptic = rememberLightHaptic()
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Decline: red circle centered above the track.
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
internal fun SlideToAnswer(
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
internal fun ShimmerLabel(
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
