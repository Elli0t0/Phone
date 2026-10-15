package io.github.elli0t0.phone.core.design

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import io.github.elli0t0.phone.data.settings.model.CallBackground

/**
 * Background for the incoming + ongoing call screens, from the Call
 * background setting. White text and controls sit on all three, so
 * the Accent variant darkens the accent instead of using it raw.
 * For CUSTOM, callers pass the custom background color as
 * [accentColor] (CallActivity and the studio preview do this).
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
    // Preset gradients: dark enough for white call-screen text.
    CallBackground.SUNSET -> Brush.verticalGradient(
        colors = listOf(Color(0xFF3A1C71), Color(0xFFB84A62))
    )
    CallBackground.OCEAN -> Brush.verticalGradient(
        colors = listOf(Color(0xFF0F2027), Color(0xFF2C5364))
    )
    CallBackground.FOREST -> Brush.verticalGradient(
        colors = listOf(Color(0xFF0B3D2E), Color(0xFF2E7D5B))
    )
    CallBackground.LAVENDER -> Brush.verticalGradient(
        colors = listOf(Color(0xFF4C1D95), Color(0xFF8B5CF6))
    )
    CallBackground.ROSE -> Brush.verticalGradient(
        colors = listOf(Color(0xFF831843), Color(0xFFF472B6))
    )
    CallBackground.MIDNIGHT -> Brush.verticalGradient(
        colors = listOf(Color(0xFF0B1026), Color(0xFF1E3A8A))
    )
    CallBackground.AMBER -> Brush.verticalGradient(
        colors = listOf(Color(0xFF7C2D12), Color(0xFFF59E0B))
    )
    CallBackground.CUSTOM -> SolidColor(Color(accentColor))
}

private fun Color.darken(factor: Float): Color =
    Color(red * factor, green * factor, blue * factor, alpha)
