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

private data class LitWindow(val col: Float, val row: Float)

private data class Drop(val x: Float, val y: Float, val speed: Float, val len: Float)

/**
 * Rain City: a dark blue-teal sky, a skyline silhouette with
 * random warm lit windows, diagonal rain streaks, and a soft
 * puddle glow along the bottom.
 */
@Composable
fun RainCityScene(intensity: Float, animate: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "rainCity")
    val t by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(30_000, easing = LinearEasing)),
        label = "t"
    )
    val buildings = remember {
        val random = Random(5)
        buildSkyline(5, 0.07f, 0.16f, 0.25f, 0.60f).map { b ->
            val cols = 3 + random.nextInt(3)
            val rows = 5 + random.nextInt(6)
            val windows = buildList {
                for (c in 0 until cols) {
                    for (r in 0 until rows) {
                        if (random.nextFloat() < 0.35f) {
                            add(LitWindow(c.toFloat() / cols, r.toFloat() / rows))
                        }
                    }
                }
            }
            Pair(b, windows)
        }
    }
    val drops = remember {
        val random = Random(6)
        List(90) {
            Drop(
                x = random.nextFloat(),
                y = random.nextFloat(),
                speed = 0.5f + random.nextFloat() * 0.8f,
                len = 0.02f + random.nextFloat() * 0.03f
            )
        }
    }
    Canvas(modifier.fillMaxSize()) {
        drawRect(
            Brush.verticalGradient(listOf(Color(0xFF081C2C), Color(0xFF0F3A4D))),
            alpha = intensity
        )
        val p = if (animate) quantizePhase(t, 30_000) else 0.3f
        val base = size.height * 0.82f
        buildings.forEach { (b, windows) ->
            val left = b.x * size.width
            val bw = b.w * size.width
            val top = base - b.h * size.height
            val bh = base - top
            drawRect(
                Color(0xFF06121E),
                topLeft = Offset(left, top),
                size = Size(bw, bh),
                alpha = intensity
            )
            windows.forEach { win ->
                drawRect(
                    Color(0xFFFFC46B),
                    topLeft = Offset(
                        left + win.col * bw + bw * 0.15f,
                        top + win.row * bh + bh * 0.08f
                    ),
                    size = Size(bw * 0.18f, bh * 0.05f),
                    alpha = 0.95f * intensity
                )
            }
        }
        softCircle(Color(0xFFFFC46B), Offset(size.width * 0.3f, size.height * 0.92f), size.width * 0.25f, 0.12f * intensity)
        softCircle(Color(0xFF7FD4FF), Offset(size.width * 0.7f, size.height * 0.94f), size.width * 0.30f, 0.10f * intensity)
        drops.forEach { drop ->
            val y = ((drop.y + p * drop.speed) % 1.1f) * size.height
            val x = drop.x * size.width + y * 0.18f
            drawLine(
                Color(0xFFBFE6FF),
                Offset(x, y),
                Offset(x + size.width * 0.012f, y + drop.len * size.height),
                strokeWidth = 1.5f * density,
                alpha = 0.35f * intensity
            )
        }
    }
}
