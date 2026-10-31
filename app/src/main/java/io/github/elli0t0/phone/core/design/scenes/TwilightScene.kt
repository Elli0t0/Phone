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
import kotlin.random.Random

private data class Streak(val baseX: Float, val y: Float, val speed: Float, val length: Float)

/**
 * Twilight: a deep-indigo to pink horizon gradient, a soft sun
 * glow at the horizon, and thin orange cloud streaks drifting
 * slowly across the sky.
 */
@Composable
fun TwilightScene(intensity: Float, animate: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "twilight")
    val t by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(40_000, easing = LinearEasing)),
        label = "t"
    )
    val streaks = remember {
        val random = Random(4)
        List(5) {
            Streak(
                baseX = random.nextFloat(),
                y = 0.26f + random.nextFloat() * 0.34f,
                speed = 0.15f + random.nextFloat() * 0.25f,
                length = 0.12f + random.nextFloat() * 0.22f
            )
        }
    }
    Canvas(modifier.fillMaxSize()) {
        drawRect(
            Brush.verticalGradient(listOf(Color(0xFF1B1F4B), Color(0xFFE8728A))),
            alpha = intensity
        )
        val p = if (animate) quantizePhase(t, 40_000) else 0.3f
        softCircle(
            Color(0xFFFFB347),
            Offset(size.width * 0.5f, size.height * 0.66f),
            size.width * 0.42f,
            0.75f * intensity
        )
        drawCircle(
            Color(0xFFFFD9A0),
            radius = size.minDimension * 0.09f,
            center = Offset(size.width * 0.5f, size.height * 0.66f),
            alpha = 0.9f * intensity
        )
        streaks.forEach { streak ->
            val x = ((streak.baseX + p * streak.speed) % 1.3f - 0.15f) * size.width
            drawOval(
                Color(0xFFFFB38A),
                topLeft = Offset(x, streak.y * size.height),
                size = Size(streak.length * size.width, 4f * density),
                alpha = 0.35f * intensity
            )
        }
    }
}
