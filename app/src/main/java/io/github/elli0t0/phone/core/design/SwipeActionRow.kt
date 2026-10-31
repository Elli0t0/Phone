package io.github.elli0t0.phone.core.design

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Swipe actions for list rows: drag right to call, drag left to
 * message. The row always springs back after the action fires;
 * nothing is ever dismissed or left open.
 *
 * A drag-based row instead of SwipeToDismissBox, for two reasons
 * the box cannot do through its public API: the swipe directions
 * are physical (right = call, left = message) in every layout
 * direction, and the snap-back is instant when the Animations
 * setting is off (spring when on, short tween when reduced).
 *
 * The drag offset lives in plain float state: it is read only
 * inside offset/graphicsLayer/draw lambdas, so a drag frame
 * neither recomposes the row nor allocates. The settle animation
 * runs one coroutine per release, never one per frame.
 *
 * The reveal draws only the exposed strip (matchParentSize +
 * drawBehind): a fillMaxSize background inside a wrap-content
 * Box collapses to a thin strip under LazyColumn's unbounded
 * max height, and the row content itself stays transparent, so
 * nothing bleeds through behind it.
 *
 * When suppressed (selection mode), only the content renders:
 * the drag state is dropped with the branch, so the row can
 * never stay displaced.
 */
@Composable
fun SwipeActionRow(
    enabled: Boolean,
    suppressed: Boolean = false,
    onSwipeCall: () -> Unit,
    onSwipeMessage: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (!enabled || suppressed) {
        Box(modifier = modifier) { content() }
        return
    }

    val animationOption = LocalAppSettings.current.animationOption
    val lightHaptic = rememberLightHaptic()
    val scope = rememberCoroutineScope()
    val layoutDirection = LocalLayoutDirection.current
    var offsetX by remember { mutableFloatStateOf(0f) }
    var rowWidthPx by remember { mutableFloatStateOf(0f) }
    var hapticFired by remember { mutableStateOf(false) }
    var settleJob by remember { mutableStateOf<Job?>(null) }

    // No width yet (first layout) means no threshold can be
    // crossed, so a drag cannot fire before measuring.
    val thresholdPx = if (rowWidthPx > 0f) rowWidthPx * 0.35f else Float.MAX_VALUE

    fun settle() {
        when (animationOption) {
            AnimationOption.OFF -> {
                offsetX = 0f
            }
            AnimationOption.REDUCED -> {
                val from = offsetX
                settleJob = scope.launch {
                    animate(
                        initialValue = from,
                        targetValue = 0f,
                        animationSpec = tween(durationMillis = Motion.Durations.Short)
                    ) { value, _ -> offsetX = value }
                }
            }
            AnimationOption.ON -> {
                val from = offsetX
                settleJob = scope.launch {
                    animate(
                        initialValue = from,
                        targetValue = 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    ) { value, _ -> offsetX = value }
                }
            }
        }
        hapticFired = false
    }

    val dragState = rememberDraggableState { delta ->
        val next = (offsetX + delta).coerceIn(-rowWidthPx, rowWidthPx)
        offsetX = next
        // Light haptic the moment the threshold is crossed, once
        // per crossing; it re-arms when the row comes back under.
        if (!hapticFired && abs(next) >= thresholdPx) {
            lightHaptic()
            hapticFired = true
        } else if (hapticFired && abs(next) < thresholdPx) {
            hapticFired = false
        }
    }

    Box(
        modifier = modifier
            .onSizeChanged { rowWidthPx = it.width.toFloat() }
            .clipToBounds()
    ) {
        // Revealed background: green + call icon on the physical
        // left when the row moves right, blue + message icon on
        // the physical right when it moves left. Only the exposed
        // strip is painted; the row content stays transparent.
        if (offsetX != 0f) {
            val isCall = offsetX > 0f
            val progress = if (rowWidthPx > 0f) {
                (abs(offsetX) / rowWidthPx).coerceIn(0f, 1f)
            } else {
                0f
            }
            val color = if (isCall) SwipeCallGreen else SwipeMessageBlue
            Box(
                Modifier.matchParentSize().drawBehind {
                    val w = abs(offsetX)
                    val left = if (offsetX > 0f) 0f else size.width - w
                    drawRect(color, topLeft = Offset(left, 0f), size = Size(w, size.height))
                }
            )
            // Physical sides: CenterStart is layout-aware, so the
            // physical left is Start in LTR and End in RTL.
            val iconAlignment = when {
                isCall && layoutDirection == LayoutDirection.Ltr -> Alignment.CenterStart
                isCall -> Alignment.CenterEnd
                layoutDirection == LayoutDirection.Ltr -> Alignment.CenterEnd
                else -> Alignment.CenterStart
            }
            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = iconAlignment
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isCall) R.drawable.ic_phone else R.drawable.ic_message
                    ),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .size(PhoneDimens.recentsSwipeRevealIconSize)
                        .graphicsLayer {
                            val scale = 0.6f + 0.6f * progress
                            scaleX = scale
                            scaleY = scale
                            alpha = progress.coerceAtLeast(0.3f)
                        }
                )
            }
        }

        Box(
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(offsetX.roundToInt(), 0) }
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    onDragStarted = { settleJob?.cancel() },
                    onDragStopped = {
                        // Drag deltas are physical: positive is
                        // always a rightward drag, in LTR and RTL.
                        if (offsetX >= thresholdPx) {
                            onSwipeCall()
                        } else if (offsetX <= -thresholdPx) {
                            onSwipeMessage()
                        }
                        settle()
                    }
                )
        ) {
            content()
        }
    }
}
