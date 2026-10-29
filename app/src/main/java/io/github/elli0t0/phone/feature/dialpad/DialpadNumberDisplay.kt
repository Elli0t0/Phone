package io.github.elli0t0.phone.feature.dialpad

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.Motion
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.data.settings.model.AnimationOption

@Composable
fun DialpadNumberDisplay(
    displayText: String,
    rawNumber: String,
    animationOption: AnimationOption,
    lightHaptic: () -> Unit,
    onPaste: (String) -> Unit,
    onBackspacePressed: () -> Unit,
    onStartBackspaceRepeat: ((() -> Unit)?) -> Unit,
    onStopBackspaceRepeat: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    // Formatted Number Display. Long-press pastes the clipboard
    // number; the paste target excludes the backspace slot (see
    // below), so a backspace long-press can never paste.
    // Always rendered: a fixed slot, so typing the first digit
    // never shifts the layout. Number centered, regular weight,
    // auto-shrinks on overflow; the backspace owns a reserved
    // 56dp slot at the right, so the number never runs under it.
    // At the minimum size a still-overflowing number falls back
    // to a single line truncated in the middle ("…" keeps the
    // leading and trailing digits visible). MiddleEllipsis does
    // not exist in this Compose version, so the truncation is
    // computed from the measured line width (see below).
    var displayFontSize by remember { mutableStateOf(PhoneDimens.keypadNumberDisplaySize) }
    var overflowAtMin by remember { mutableStateOf(false) }
    var middleTruncated by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(displayText) {
        displayFontSize = PhoneDimens.keypadNumberDisplaySize
        overflowAtMin = false
        middleTruncated = null
    }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("dialpad_number_display"),
            contentAlignment = Alignment.Center
        ) {
            // Long-press paste target: the number area except the
            // backspace slot (end padding matches the backspace's
            // CenterEnd slot, RTL included). It is a SIBLING of the
            // backspace, not an ancestor: hit-testing picks the
            // topmost sibling, so a backspace press never reaches
            // this detector at all. The old parent-level detector
            // relied on the backspace consuming the down event, but
            // a long-press wait that already started is not reliably
            // cancelled by that — which pasted before deleting.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = 56.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                val clipText = clipboardManager.getText()?.text
                                if (!clipText.isNullOrBlank()) {
                                    lightHaptic()
                                    onPaste(clipText)
                                }
                            }
                        )
                    }
            )
            // Hidden full-string Text: measures overflow and drives
            // the auto-shrink, exactly as the single Text did. It
            // spans the number area (full width minus the reserved
            // backspace slot), so it overflows at the same point
            // the visible number would.
            Text(
                text = displayText,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = displayFontSize,
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                onTextLayout = { layoutResult ->
                    if (layoutResult.hasVisualOverflow) {
                        if (displayFontSize <= PhoneDimens.keypadNumberDisplayMinSize) {
                            // One-shot middle truncation: the fraction
                            // of the line that fits the number area,
                            // minus a 2-char margin for the ellipsis
                            // and non-tabular glyphs, split across
                            // both ends so head and tail stay visible.
                            val lineWidth = layoutResult.getLineRight(0) - layoutResult.getLineLeft(0)
                            if (lineWidth > 0) {
                                val fit = ((displayText.length * (layoutResult.size.width / lineWidth)).toInt() - 2)
                                    .coerceAtLeast(2)
                                val first = (fit + 1) / 2
                                middleTruncated = displayText.take(first) + "…" + displayText.takeLast(fit - first)
                            }
                            overflowAtMin = true
                        } else {
                            val shrunk = displayFontSize * PhoneDimens.keypadNumberDisplayShrinkFactor
                            displayFontSize = if (shrunk.value < PhoneDimens.keypadNumberDisplayMinSize.value) {
                                PhoneDimens.keypadNumberDisplayMinSize
                            } else {
                                shrunk
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(end = 56.dp).graphicsLayer { alpha = 0f }.clearAndSetSemantics { }
            )
            // Over-long number at minimum size: the
            // middle-truncated line instead of the per-digit row.
            if (overflowAtMin && middleTruncated != null) {
                Text(
                    text = middleTruncated!!,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = displayFontSize,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.fillMaxWidth().padding(end = 56.dp)
                        .semantics { contentDescription = displayText }
                )
            } else
            // Visible per-character number: each digit pops in
            // (scale + fade on ON, fade only on REDUCED, instant
            // on OFF) as it is typed; the row grows/shrinks
            // smoothly as digits are added or deleted. The end
            // padding reserves the backspace slot, so the row
            // centers in the number area, clear of the backspace.
            Row(
                modifier = Modifier.padding(end = 56.dp).animateContentSize(
                    animationSpec = if (animationOption == AnimationOption.OFF) {
                        snap()
                    } else if (animationOption == AnimationOption.REDUCED) {
                        tween(durationMillis = Motion.scaledDurationMs(Motion.Durations.Micro, animationOption))
                    } else {
                        spring(dampingRatio = 0.8f, stiffness = 500f)
                    }
                ).semantics(mergeDescendants = true) { contentDescription = displayText },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                displayText.forEachIndexed { index, char ->
                    key(index) {
                        var entered by remember {
                            mutableStateOf(animationOption == AnimationOption.OFF)
                        }
                        LaunchedEffect(Unit) { entered = true }
                        val progress by animateFloatAsState(
                            targetValue = if (entered) 1f else 0f,
                            animationSpec = when (animationOption) {
                                AnimationOption.OFF -> snap()
                                // Spring pop (scale overshoots slightly);
                                // REDUCED keeps the fade-only tween.
                                AnimationOption.ON -> spring(dampingRatio = 0.6f, stiffness = 500f)
                                else -> tween(
                                    durationMillis = Motion.scaledDurationMs(Motion.Durations.Short, animationOption),
                                    easing = Motion.Easings.Emphasized
                                )
                            },
                            label = "digit$index"
                        )
                        Text(
                            text = char.toString(),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = displayFontSize,
                                fontWeight = FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.graphicsLayer {
                                // The spring overshoots past 1: clamp
                                // alpha (scale keeps the pop).
                                alpha = progress.coerceIn(0f, 1f)
                                if (animationOption == AnimationOption.ON) {
                                    val s = Motion.DialedDigitPopStartScale +
                                        (1f - Motion.DialedDigitPopStartScale) * progress
                                    scaleX = s
                                    scaleY = s
                                }
                            }
                        )
                    }
                }
            }
            // Backspace at the right of the number, with accelerated repeat deletion.
            // Fades + scales out when the number is empty; the
            // number stays centered because the backspace overlays it.
            // (AnimatedVisibility cannot be used here: the enclosing
            // Column resolves it to the ColumnScope overload, which
            // cannot be called inside this Box.)
            val backspaceVisible = rawNumber.isNotEmpty()
            val backspaceAlpha by animateFloatAsState(
                targetValue = if (backspaceVisible) 1f else 0f,
                animationSpec = tween(durationMillis = Motion.scaledDurationMs(Motion.Durations.Short, animationOption)),
                label = "backspaceAlpha"
            )
            val backspaceScale by animateFloatAsState(
                targetValue = if (backspaceVisible) 1f else 0.8f,
                animationSpec = tween(durationMillis = Motion.scaledDurationMs(Motion.Durations.Short, animationOption)),
                label = "backspaceScale"
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(56.dp)
                    .graphicsLayer {
                        alpha = backspaceAlpha
                        scaleX = backspaceScale
                        scaleY = backspaceScale
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .then(
                            if (backspaceVisible) {
                                Modifier.pointerInput(Unit) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        // Consume the press as well: belt
                                        // and braces next to the geometric
                                        // separation (the paste target
                                        // excludes this slot entirely).
                                        down.consume()
                                        lightHaptic()
                                        onBackspacePressed()
                                        onStartBackspaceRepeat({ lightHaptic() })
                                        try {
                                            waitForUpOrCancellation()
                                        } finally {
                                            onStopBackspaceRepeat()
                                        }
                                    }
                                }
                            } else {
                                Modifier
                            }
                        )
                        .testTag("btn_backspace"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_backspace),
                        contentDescription = stringResource(id = R.string.cd_backspace_button),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
}
