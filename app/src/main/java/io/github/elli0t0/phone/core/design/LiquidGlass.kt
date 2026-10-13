package io.github.elli0t0.phone.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
val LiquidGlassEndCall = Color(0xFFE53935)
val LiquidGlassHold = Color(0xFFFFB74D)

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
 * Frosted glass container: a translucent fill with a thin translucent border.
 * Placed over a gradient background it reads as frosted glass.
 */
@Composable
fun FrostedGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = LiquidGlassDockShape,
    containerColor: Color = LiquidGlassSurface,
    borderColor: Color = LiquidGlassBorder,
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
