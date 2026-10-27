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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.CallAnswerGreen
import io.github.elli0t0.phone.core.design.CallDeclineRed
import io.github.elli0t0.phone.core.design.InCallControlBackgroundFlat
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.liquidGlass
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
@Composable
internal fun LiquidAnswerControls(
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
    // Commit target: the track end minus a small padding, so the
    // stretched knob never leaves the pill.
    val travel = with(density) {
        (half - PhoneDimens.liquidKnobEndPadding.toPx()).coerceAtLeast(1f)
    }
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
            .clip(CircleShape)
            .drawBehind {
                val p = (x.value / travel).coerceIn(-1f, 1f)
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
                    val p = (x.value / travel).coerceIn(-1f, 1f)
                    val base = if (option == AnimationOption.ON) hintPulse else 0.6f
                    alpha = if (p >= 0f) base * (1f - p) else max(base, -p)
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
                    val p = (x.value / travel).coerceIn(-1f, 1f)
                    val base = if (option == AnimationOption.ON) hintPulse else 0.6f
                    alpha = if (p <= 0f) base * (1f + p) else max(base, p)
                }
        )
        // Glass knob in the middle: drag right to answer, left to decline.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset { IntOffset(x.value.roundToInt(), 0) }
                .graphicsLayer {
                    val p = (x.value / travel).coerceIn(-1f, 1f)
                    val idleScale = if (option == AnimationOption.ON && x.value == 0f) breathe else 1f
                    scaleX = idleScale * (1f + 0.10f * abs(p))
                    scaleY = idleScale * (1f - 0.08f * abs(p))
                }
                .size(knob)
                .clip(CircleShape)
                .background(Color.White)
                .pointerInput(travel, option) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                val current = x.value / travel
                                when {
                                    current >= 0.7f -> {
                                        x.animateTo(travel, tween(100))
                                        onAnswer()
                                    }
                                    current <= -0.7f -> {
                                        x.animateTo(-travel, tween(100))
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
                        scope.launch { x.snapTo((x.value + dragAmount).coerceIn(-travel, travel)) }
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
