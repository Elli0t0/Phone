package io.github.elli0t0.phone.core.design

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
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
}
