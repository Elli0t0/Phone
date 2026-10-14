package io.github.elli0t0.phone.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

// Liquid glass design tokens: frosted translucent surfaces, soft gradients,
// and squircle shapes used on the in-call screen.

val LiquidGlassBackgroundTop = Color(0xFF2B2A36)
val LiquidGlassBackgroundBottom = Color(0xFF100F15)
val LiquidGlassGlowPrimary = Color(0xFF4A7CFF)
val LiquidGlassGlowSecondary = Color(0xFF9B6DFF)
val LiquidGlassSurface = Color.White.copy(alpha = 0.10f)
val LiquidGlassSurfaceStrong = Color.White.copy(alpha = 0.16f)
val LiquidGlassBorder = Color.White.copy(alpha = 0.22f)
val LiquidGlassOnSurface = Color.White
val LiquidGlassOnSurfaceMuted = Color.White.copy(alpha = 0.75f)
val LiquidGlassOnSurfaceFaint = Color.White.copy(alpha = 0.55f)
val LiquidGlassActive = Color.White
val LiquidGlassActiveContent = Color(0xFF1C1B1F)
// Light-theme glass: dark tints over a light background.
val LiquidGlassSurfaceLight = Color.Black.copy(alpha = 0.06f)
val LiquidGlassSurfaceStrongLight = Color.Black.copy(alpha = 0.10f)
val LiquidGlassBorderLight = Color.Black.copy(alpha = 0.12f)
val LiquidGlassOnSurfaceLight = Color(0xFF1C1C1E)
val LiquidGlassOnSurfaceMutedLight = Color(0xFF1C1C1E).copy(alpha = 0.75f)
val LiquidGlassOnSurfaceFaintLight = Color(0xFF1C1C1E).copy(alpha = 0.55f)
val LiquidGlassActiveLight = Color(0xFF1C1C1E)
val LiquidGlassActiveContentLight = Color.White
val LiquidGlassEndCall = Color(0xFFE53935)
val LiquidGlassHold = Color(0xFFFFB74D)

/**
 * Full glass color set for one theme. Screens that draw text or icons
 * on a [GlassSurface] resolve this instead of hardcoding the dark set.
 */
data class GlassColors(
    val container: Color,
    val containerStrong: Color,
    val border: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val onSurfaceFaint: Color,
    val active: Color,
    val activeContent: Color
)

/** Glass colors for the dark (white tints) or light (dark tints) theme. */
fun glassColors(dark: Boolean): GlassColors = if (dark) {
    GlassColors(
        container = LiquidGlassSurface,
        containerStrong = LiquidGlassSurfaceStrong,
        border = LiquidGlassBorder,
        onSurface = LiquidGlassOnSurface,
        onSurfaceMuted = LiquidGlassOnSurfaceMuted,
        onSurfaceFaint = LiquidGlassOnSurfaceFaint,
        active = LiquidGlassActive,
        activeContent = LiquidGlassActiveContent
    )
} else {
    GlassColors(
        container = LiquidGlassSurfaceLight,
        containerStrong = LiquidGlassSurfaceStrongLight,
        border = LiquidGlassBorderLight,
        onSurface = LiquidGlassOnSurfaceLight,
        onSurfaceMuted = LiquidGlassOnSurfaceMutedLight,
        onSurfaceFaint = LiquidGlassOnSurfaceFaintLight,
        active = LiquidGlassActiveLight,
        activeContent = LiquidGlassActiveContentLight
    )
}

/**
 * Whether the currently applied theme is dark, judged by the theme
 * background luminance. This follows the app ThemeMode setting, pure
 * black dark mode, and dynamic color, unlike isSystemInDarkTheme().
 */
@Composable
fun isDarkGlassTheme(): Boolean =
    MaterialTheme.colorScheme.background.luminance() < 0.5f

/** Fully rounded pill shape for the dock and the end-call button. */
val LiquidGlassPillShape = RoundedCornerShape(32.dp)

/** Large rounded shape for floating glass containers. */
val LiquidGlassDockShape = RoundedCornerShape(36.dp)

/**
 * Squircle (superellipse) shape: a square with continuously curved corners,
 * rounder than a rounded rectangle. Exponent 4 sits between a circle (2)
 * and a square (infinity).
 */
val SquircleShape: Shape = GenericShape { size, _ ->
    val width = size.width
    val height = size.height
    val centerX = width / 2f
    val centerY = height / 2f
    val exponent = 2f / 4f
    val steps = 48
    for (i in 0..steps) {
        val angle = (i.toFloat() / steps) * (2f * PI.toFloat())
        val cosAngle = cos(angle)
        val sinAngle = sin(angle)
        val x = centerX + (width / 2f) * sign(cosAngle) * abs(cosAngle).pow(exponent)
        val y = centerY + (height / 2f) * sign(sinAngle) * abs(sinAngle).pow(exponent)
        if (i == 0) {
            moveTo(x, y)
        } else {
            lineTo(x, y)
        }
    }
    close()
}

/**
 * Reusable glass surface: a translucent fill with a thin translucent border.
 * Placed over a gradient or image background it reads as frosted glass.
 *
 * Colors default to the current theme (white tints on dark, dark tints
 * on light). Pass explicit colors when the surface sits over a background
 * that does not follow the theme, e.g. the always-dark call background.
 *
 * Real blur (Haze) was checked first: Haze 2.0.1 is the latest stable release,
 * but its new modular 2.x API is unverified against this project's Compose
 * version and cannot be compile-checked locally, so this version ships the
 * translucent fallback. No blur is faked: the surface is honestly translucent.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = LiquidGlassDockShape,
    dark: Boolean = isDarkGlassTheme(),
    containerColor: Color = glassColors(dark).container,
    borderColor: Color = glassColors(dark).border,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .border(1.dp, borderColor, shape),
        content = content
    )
}
