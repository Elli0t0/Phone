package io.github.elli0t0.phone.core.design

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.graphics.Color
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
     * detail/edit, groups, history). direction > 0 slides the new
     * screen in from the right, < 0 from the left, 0 fades only.
     * REDUCED never slides, only fades shorter; OFF is no transition.
     */
    fun innerContentTransform(option: AnimationOption, direction: Int): ContentTransform {
        if (option == AnimationOption.OFF) {
            return ContentTransform(EnterTransition.None, ExitTransition.None)
        }
        val duration = innerDurationMs(option)
        val fadeIn = fadeIn(animationSpec = tween(durationMillis = duration))
        val fadeOut = fadeOut(animationSpec = tween(durationMillis = duration))
        if (option == AnimationOption.REDUCED || direction == 0) {
            return ContentTransform(fadeIn, fadeOut)
        }
        val slideIn = slideInHorizontally(
            animationSpec = tween(durationMillis = duration)
        ) { if (direction > 0) it / 3 else -it / 3 }
        val slideOut = slideOutHorizontally(
            animationSpec = tween(durationMillis = duration)
        ) { if (direction > 0) -it / 3 else it / 3 }
        return ContentTransform(fadeIn + slideIn, fadeOut + slideOut)
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
}
