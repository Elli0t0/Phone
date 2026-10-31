package io.github.elli0t0.phone.core.design.scenes

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class GlowLine(
    val x: Float,
    val y: Float,
    val len: Float,
    val color: Color,
    val phase: Float
)

/**
 * Neon City: a purple-blue sky, two skyline layers (far and
 * near), and pulsing magenta/cyan glow lines.
 */
@Composable
fun NeonCityScene(intensity: Float, animate: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "neonCity")
    val t by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(16_000, easing = LinearEasing)),
        label = "t"
    )
    val far = remember { buildSkyline(9, 0.08f, 0.18f, 0.20f, 0.45f) }
    val near = remember { buildSkyline(13, 0.06f, 0.14f, 0.30f, 0.60f) }
    val glows = remember {
        val random = Random(15)
        val colors = listOf(Color(0xFFFF2EB8), Color(0xFF22D3EE))
        List(14) {
            GlowLine(
                x = random.nextFloat() * 0.85f,
                y = 0.35f + random.nextFloat() * 0.45f,
                len = 0.05f + random.nextFloat() * 0.10f,
                color = colors[random.nextInt(colors.size)],
                phase = random.nextFloat()
            )
        }
    }
    Canvas(modifier.fillMaxSize()) {
        drawRect(
            Brush.verticalGradient(listOf(Color(0xFF1E1B4B), Color(0xFF4C1D95))),
            alpha = intensity
        )
        val p = if (animate) quantizePhase(t, 16_000) else 0.3f
        far.forEach { b ->
            val top = size.height * 0.72f - b.h * size.height
            drawRect(
                Color(0xFF2D1B69),
                topLeft = Offset(b.x * size.width, top),
                size = Size(b.w * size.width, size.height * 0.72f - top),
                alpha = 0.85f * intensity
            )
        }
        near.forEach { b ->
            val top = size.height * 0.82f - b.h * size.height
            drawRect(
                Color(0xFF12082B),
                topLeft = Offset(b.x * size.width, top),
                size = Size(b.w * size.width, size.height * 0.82f - top),
                alpha = intensity
            )
        }
        glows.forEach { glow ->
            val pulse = 0.7f + 0.3f * sin(2f * PI.toFloat() * (p + glow.phase))
            val y = glow.y * size.height
            val start = Offset(glow.x * size.width, y)
            val end = Offset((glow.x + glow.len) * size.width, y)
            // Wide faint pass + narrow bright pass = glow.
            drawLine(glow.color, start, end, strokeWidth = 6f * density, alpha = 0.18f * pulse * intensity)
            drawLine(glow.color, start, end, strokeWidth = 2f * density, alpha = 0.9f * pulse * intensity)
        }
    }
}
