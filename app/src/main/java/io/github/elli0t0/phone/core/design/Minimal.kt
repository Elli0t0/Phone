package io.github.elli0t0.phone.core.design

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Minimal mode (0.18.10): the stock-dialer look. Components branch
 * on [LocalMinimal] instead of reading derived settings, so the
 * stored values are never overwritten and switching the mode off
 * restores everything exactly.
 */
val LocalMinimal = staticCompositionLocalOf { false }

/** One minimal palette: dark values are the spec's, light mirrors the same roles. */
data class MinimalPalette(
    val bg: Color,
    val keypadBg: Color,
    val card: Color,
    val divider: Color,
    val navPill: Color,
    val navSelected: Color,
    val text: Color,
    val secondary: Color,
    val callGreen: Color
)

object MinimalColors {
    val dark = MinimalPalette(
        bg = Color(0xFF000000),
        keypadBg = Color(0xFF151517),
        card = Color(0xFF1C1C1E),
        divider = Color(0xFF2C2C2E),
        navPill = Color(0xFF2A2A2C),
        navSelected = Color(0xFF48484A),
        text = Color(0xFFFFFFFF),
        secondary = Color(0xFFA0A0A5),
        callGreen = Color(0xFF4CAF68)
    )
    val light = MinimalPalette(
        bg = Color(0xFFF2F2F7),
        keypadBg = Color(0xFFFFFFFF),
        card = Color(0xFFFFFFFF),
        divider = Color(0xFFE5E5EA),
        navPill = Color(0xFFFFFFFF),
        navSelected = Color(0xFFE5E5EA),
        text = Color(0xFF000000),
        secondary = Color(0xFF6E6E73),
        callGreen = Color(0xFF4CAF68)
    )

    fun palette(isDark: Boolean): MinimalPalette = if (isDark) dark else light
}

/** The minimal palette for the current theme, from the theme background's luminance. */
@Composable
fun minimalPalette(): MinimalPalette =
    MinimalColors.palette(MaterialTheme.colorScheme.background.luminance() <= 0.5f)
