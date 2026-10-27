package io.github.elli0t0.phone.feature.call.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.CallAnswerGreen
import io.github.elli0t0.phone.core.design.CallDeclineRed
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.Motion
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
@Composable
internal fun SwipeButtonsAnswerControls(
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
internal fun SwipeButtonColumn(
    isAnswer: Boolean,
    anyCommitted: Boolean,
    onCommit: () -> Unit
) {
    var dragging by remember { mutableStateOf(false) }
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
        // Drag hint: three chevrons pulsing in sequence (900 ms loop),
        // hidden while the button is being dragged.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer { alpha = if (dragging) 0f else 1f }
        ) {
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
        SwipeDragButton(
            isAnswer = isAnswer,
            anyCommitted = anyCommitted,
            onCommit = onCommit,
            onDraggingChange = { dragging = it }
        )
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
internal fun SwipeDragButton(
    isAnswer: Boolean,
    anyCommitted: Boolean,
    onCommit: () -> Unit,
    onDraggingChange: (Boolean) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val option = LocalAppSettings.current.animationOption
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var flying by remember { mutableStateOf(false) }
    val flyProgress = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }
    var gone by remember { mutableStateOf(false) }
    var thresholdFired by remember { mutableStateOf(false) }
    var springJob by remember { mutableStateOf<Job?>(null) }
    var lastSegmentTime by remember { mutableStateOf(0L) }
    var lastSegmentSpeedPx by remember { mutableStateOf(0f) }
    val commitPx = with(density) { PhoneDimens.swipeCommitDistance.toPx() }
    val flyDistancePx = with(density) { 120.dp.toPx() }
    val color = if (isAnswer) CallAnswerGreen else CallDeclineRed
    // When the other button commits, this one only fades out.
    LaunchedEffect(anyCommitted) {
        if (anyCommitted && !gone) {
            fade.animateTo(0f, tween(durationMillis = Motion.Durations.Short))
        }
    }
    val shownOffset = if (flying) {
        val direction = dragOffset / dragOffset.getDistance().coerceAtLeast(1f)
        dragOffset + direction * (flyDistancePx * flyProgress.value)
    } else {
        dragOffset
    }
    Box(
        modifier = Modifier
            .offset { IntOffset(shownOffset.x.roundToInt(), shownOffset.y.roundToInt()) }
            .graphicsLayer {
                val progress = (dragOffset.getDistance() / commitPx).coerceIn(0f, 1f)
                val k = 1f + 0.12f * progress
                scaleX = k
                scaleY = k
                alpha = fade.value
            }
            .drawBehind {
                val progress = (dragOffset.getDistance() / commitPx).coerceIn(0f, 1f)
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
                        springJob?.cancel()
                        onDraggingChange(true)
                        thresholdFired = false
                        lastSegmentSpeedPx = 0f
                        lastSegmentTime = 0L
                    },
                    onDragEnd = {
                        onDraggingChange(false)
                        springJob = scope.launch {
                            val speedDp = with(density) { lastSegmentSpeedPx.toDp().value }
                            val distance = dragOffset.getDistance()
                            if (distance >= commitPx || speedDp > 1200f) {
                                gone = true
                                flying = true
                                launch {
                                    flyProgress.animateTo(
                                        1f,
                                        tween(durationMillis = Motion.Durations.Short)
                                    )
                                }
                                fade.animateTo(0f, tween(durationMillis = Motion.Durations.Short))
                                onCommit()
                            } else {
                                val from = dragOffset
                                Animatable(1f).animateTo(0f, backSpecFloat(option)) {
                                    dragOffset = from * value
                                }
                            }
                        }
                    },
                    onDragCancel = {
                        onDraggingChange(false)
                        springJob = scope.launch {
                            val from = dragOffset
                            Animatable(1f).animateTo(0f, backSpecFloat(option)) {
                                dragOffset = from * value
                            }
                        }
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
                    dragOffset += drag
                    val distance = dragOffset.getDistance()
                    if (!thresholdFired && distance >= commitPx) {
                        thresholdFired = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } else if (distance < commitPx * 0.8f) {
                        thresholdFired = false
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
