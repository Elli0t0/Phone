package io.github.elli0t0.phone.core.design

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import io.github.elli0t0.phone.data.settings.model.CallBackground

/**
 * Background for the incoming + ongoing call screens, from the Call
 * background setting. White text and controls sit on all three, so
 * the Accent variant darkens the accent instead of using it raw.
 */
fun callBackgroundBrush(background: CallBackground, accentColor: Long): Brush = when (background) {
    CallBackground.DEFAULT -> Brush.verticalGradient(
        colors = listOf(InCallGradientTop, InCallGradientBottom)
    )
    CallBackground.DARK -> SolidColor(Color.Black)
    CallBackground.ACCENT -> {
        val accent = Color(accentColor)
        Brush.verticalGradient(
            colors = listOf(accent.darken(0.8f), accent.darken(0.35f))
        )
    }
}

private fun Color.darken(factor: Float): Color =
    Color(red * factor, green * factor, blue * factor, alpha)
