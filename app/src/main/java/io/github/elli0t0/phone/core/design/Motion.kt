package io.github.elli0t0.phone.core.design

import android.provider.Settings
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.data.settings.model.AnimationOption

/**
 * Central motion behavior for the Animations setting. Every animation
 * in the app resolves its spec through here:
 * ON = full motion, REDUCED = shorter and subtler, OFF = no motion.
 */
object Motion {

    /** Tab-switch fade-in duration. */
    fun tabFadeDurationMs(option: AnimationOption): Int = when (option) {
        AnimationOption.ON -> 120
        AnimationOption.REDUCED -> 60
        AnimationOption.OFF -> 0
    }

    /**
     * Tab-switch enter: cross-fade + a subtle slide-in (5% width).
     * REDUCED fades only; OFF is instant.
     */
    fun tabEnterTransition(option: AnimationOption): EnterTransition {
        if (option == AnimationOption.OFF) return EnterTransition.None
        val duration = tabFadeDurationMs(option)
        val fade = fadeIn(animationSpec = tween(durationMillis = duration))
        if (option == AnimationOption.REDUCED) return fade
        return fade + slideInHorizontally(
            animationSpec = tween(durationMillis = duration)
        ) { it / 20 }
    }

    /**
     * Tab-switch exit: cross-fade + a subtle slide-out (5% width).
     * REDUCED fades only; OFF is instant.
     */
    fun tabExitTransition(option: AnimationOption): ExitTransition {
        if (option == AnimationOption.OFF) return ExitTransition.None
        val duration = tabFadeDurationMs(option)
        val fade = fadeOut(animationSpec = tween(durationMillis = duration))
        if (option == AnimationOption.REDUCED) return fade
        return fade + slideOutHorizontally(
            animationSpec = tween(durationMillis = duration)
        ) { -it / 20 }
    }

    /** Bottom-bar pill + content color transition. */
    fun pillColorSpec(option: AnimationOption): AnimationSpec<Color> = when (option) {
        AnimationOption.ON -> tween(durationMillis = 300)
        AnimationOption.REDUCED -> tween(durationMillis = 100)
        AnimationOption.OFF -> snap()
    }

    /**
     * Inner-screen transition duration (slide/fade).
     */
    fun innerDurationMs(option: AnimationOption): Int = when (option) {
        AnimationOption.ON -> 200
        AnimationOption.REDUCED -> 100
        AnimationOption.OFF -> 0
    }

    /**
     * Inner-screen transition (pushed screens: Settings, contact
     * detail/edit, groups, history, call screens). Spring scale +
     * fade: the new screen scales up from 0.96, the old one scales
     * down to 0.98 while fading. [direction] is kept for call-site
     * compatibility; the animation is the same both ways. REDUCED
     * fades only; OFF is no transition.
     */
    fun innerContentTransform(option: AnimationOption, direction: Int): ContentTransform {
        if (option == AnimationOption.OFF) {
            return ContentTransform(EnterTransition.None, ExitTransition.None)
        }
        val duration = innerDurationMs(option)
        val fadeIn = fadeIn(animationSpec = tween(durationMillis = duration))
        val fadeOut = fadeOut(animationSpec = tween(durationMillis = duration))
        if (option == AnimationOption.REDUCED) {
            return ContentTransform(fadeIn, fadeOut)
        }
        val scaleIn = scaleIn(
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
            initialScale = 0.96f
        )
        val scaleOut = scaleOut(
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
            targetScale = 0.98f
        )
        return ContentTransform(fadeIn + scaleIn, fadeOut + scaleOut)
    }

    /** NavHost enter transition for a pushed inner screen. */
    fun innerEnterTransition(option: AnimationOption): EnterTransition =
        innerContentTransform(option, 1).targetContentEnter

    /** NavHost exit transition for a pushed inner screen. */
    fun innerExitTransition(option: AnimationOption): ExitTransition =
        innerContentTransform(option, 1).initialContentExit

    /** NavHost pop-enter transition for a pushed inner screen. */
    fun innerPopEnterTransition(option: AnimationOption): EnterTransition =
        innerContentTransform(option, -1).targetContentEnter

    /** NavHost pop-exit transition for a pushed inner screen. */
    fun innerPopExitTransition(option: AnimationOption): ExitTransition =
        innerContentTransform(option, -1).initialContentExit

    /** Shared duration tokens (milliseconds). */
    object Durations {
        const val Micro = 100
        const val Short = 180
        const val Medium = 280
        const val Long = 420
    }

    /** Selected option-chip pop scale (Animations ON only). */
    const val StudioChipSelectedScale = 1.06f

    /** Dialed-digit pop-in start scale (animates to 1.0). */
    const val DialedDigitPopStartScale = 0.8f

    /** Shared easing tokens. */
    object Easings {
        val Standard: Easing = FastOutSlowInEasing
        val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        val Exit: Easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)
    }

    /** Snappy spring: quick settle, slight overshoot. */
    fun <T> snappy(): AnimationSpec<T> = spring(dampingRatio = 0.8f, stiffness = 500f)

    /** Bouncy spring: visible overshoot for playful motion. */
    fun <T> bouncy(): AnimationSpec<T> = spring(dampingRatio = 0.55f, stiffness = 380f)

    /**
     * Scales a base duration by the Animations setting.
     * ON keeps it, REDUCED uses ~60%, OFF is instant (0).
     */
    fun scaledDurationMs(baseMs: Int, option: AnimationOption): Int = when (option) {
        AnimationOption.ON -> baseMs
        AnimationOption.REDUCED -> (baseMs * 0.6f).toInt()
        AnimationOption.OFF -> 0
    }

    /**
     * Tween with a base duration + easing, resolved by the Animations
     * setting. OFF returns snap() (instant).
     */
    fun <T> tweenSpec(durationMs: Int, easing: Easing, option: AnimationOption): FiniteAnimationSpec<T> =
        if (option == AnimationOption.OFF) {
            snap()
        } else {
            tween(durationMillis = scaledDurationMs(durationMs, option), easing = easing)
        }

    /**
     * First-load stagger delay for a LazyColumn item: the first 8 rows
     * reveal with a 25ms stagger, later rows appear immediately.
     */
    fun staggerDelayMs(index: Int): Int = if (index < 8) index * 25 else 0

    /**
     * Staggered fade/slide-in for popup menu items (Recents filter).
     * ON fades + slides with a stagger delay, REDUCED fades only at
     * ~60% duration with no stagger/slide, OFF is instant.
     */
    fun popupItemEnter(option: AnimationOption, index: Int): EnterTransition {
        if (option == AnimationOption.OFF) return EnterTransition.None
        val duration = scaledDurationMs(Durations.Short, option)
        val fade = fadeIn(
            animationSpec = tween(
                durationMillis = duration,
                delayMillis = if (option == AnimationOption.ON) staggerDelayMs(index) else 0
            )
        )
        if (option == AnimationOption.REDUCED) return fade
        val slide = slideInVertically(
            animationSpec = tween(
                durationMillis = duration,
                delayMillis = staggerDelayMs(index),
                easing = Easings.Emphasized
            )
        ) { it / 4 }
        return fade + slide
    }
}

/**
 * Press-scale for clickables (keys, buttons, rows, chips). Scales down
 * to [to] while pressed, springs back on release. Only active when the
 * Animations setting is ON; REDUCED and OFF leave the size untouched.
 */
fun Modifier.pressScale(source: MutableInteractionSource, to: Float = 0.92f): Modifier = composed {
    val option = LocalAppSettings.current.animationOption
    if (option != AnimationOption.ON) {
        this
    } else {
        val pressed by source.collectIsPressedAsState()
        // Read the animated value inside graphicsLayer only: reading it
        // during composition would recompose the whole element each frame.
        val scale = animateFloatAsState(
            targetValue = if (pressed) to else 1f,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
            label = "press"
        )
        graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
    }
}

/**
 * Bouncy press for clickables (dialpad keys, Recents cards, action
 * buttons, contact rows, bottom-bar tabs). Scales down to [to] while
 * pressed and springs back with visible overshoot (same spring as
 * Motion.bouncy). Only active when the Animations setting is ON;
 * REDUCED and OFF leave the size untouched.
 */
fun Modifier.bouncyClick(source: MutableInteractionSource, to: Float = 0.90f): Modifier = composed {
    val option = LocalAppSettings.current.animationOption
    if (option != AnimationOption.ON) {
        this
    } else {
        val pressed by source.collectIsPressedAsState()
        val scale = animateFloatAsState(
            targetValue = if (pressed) to else 1f,
            animationSpec = spring(dampingRatio = 0.55f, stiffness = 380f),
            label = "bouncy"
        )
        graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
    }
}

/**
 * First-load reveal for LazyColumn rows. The first 8 rows fade/slide in
 * with a 25ms stagger ([Motion.staggerDelayMs]); later rows appear
 * immediately. ON slides up 16dp + fades, REDUCED fades only at ~60%
 * duration, OFF is instant.
 * [revealed] flips true once (when the first real data arrives); rows
 * composed later (scrolled into view) start at the end state, so they
 * never animate.
 */
fun Modifier.firstLoadReveal(index: Int, revealed: Boolean): Modifier = composed {
    val option = LocalAppSettings.current.animationOption
    if (!revealed && option == AnimationOption.OFF) {
        this
    } else {
        val target = if (revealed) 1f else 0f
        // Read the animated value inside graphicsLayer only: reading it
        // during composition would recompose the whole row each frame,
        // which is what made the Recents reveal lag.
        val progress = animateFloatAsState(
            targetValue = target,
            animationSpec = if (option == AnimationOption.OFF) {
                snap()
            } else {
                tween(
                    durationMillis = Motion.scaledDurationMs(Motion.Durations.Medium, option),
                    delayMillis = Motion.staggerDelayMs(index),
                    easing = Motion.Easings.Emphasized
                )
            },
            label = "reveal"
        )
        val density = LocalDensity.current
        val slidePx = with(density) { 16.dp.toPx() }
        graphicsLayer {
            alpha = progress.value
            if (option == AnimationOption.ON) {
                translationY = (1f - progress.value) * slidePx
            }
        }
    }
}

/**
 * Expanding pulse rings behind [content]. Only animates when the
 * Animations setting is ON; otherwise [content] renders without rings
 * (no loops on REDUCED / OFF).
 */
@Composable
fun PulseRings(
    color: Color,
    modifier: Modifier = Modifier,
    rings: Int = 3,
    periodMs: Int = 2400,
    content: @Composable BoxScope.() -> Unit
) {
    val option = LocalAppSettings.current.animationOption
    val transition = rememberInfiniteTransition(label = "pulse")
    // Progress values are computed unconditionally so composition stays
    // stable even when the option gates the drawing below.
    val progresses = ArrayList<State<Float>>(rings)
    for (i in 0 until rings) {
        progresses += transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = periodMs, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(i * periodMs / rings)
            ),
            label = "ring$i"
        )
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (option == AnimationOption.ON) {
            for (progress in progresses) {
                val p by progress
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            val k = 1f + 0.35f * p
                            scaleX = k
                            scaleY = k
                            alpha = 0.22f * (1f - p)
                        }
                        .background(color = color, shape = CircleShape)
                )
            }
        }
        content()
    }
}

/**
 * Light haptic that follows the system haptics setting
 * (Settings.System.HAPTIC_FEEDBACK_ENABLED). Returns a lambda call sites
 * invoke from click handlers.
 */
@Composable
fun rememberLightHaptic(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    return remember(haptic, context) {
        {
            val enabled = try {
                Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.HAPTIC_FEEDBACK_ENABLED,
                    1
                ) == 1
            } catch (e: Settings.SettingNotFoundException) {
                true
            }
            if (enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }
}
