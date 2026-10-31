package io.github.elli0t0.phone.core.design.scenes

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.sin

/**
 * Ocean Sunset: an orange-pink sky, a low sun with a reflected
 * column on the water, and layered sine waves in three blues.
 */
@Composable
fun OceanSunsetScene(intensity: Float, animate: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "oceanSunset")
    val t by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(28_000, easing = LinearEasing)),
        label = "t"
    )
    val waveColors = remember {
        arrayOf(Color(0xFF1E5AA8), Color(0xFF2B7AD6), Color(0xFF4AA3E8))
    }
    val baseYs = remember { floatArrayOf(0.62f, 0.72f, 0.82f) }
    val alphas = remember { floatArrayOf(0.85f, 0.7f, 0.55f) }
    val freqs = remember { floatArrayOf(1.5f, 2.0f, 2.5f) }
    val paths = remember { Array(3) { Path() } }
    Canvas(modifier.fillMaxSize()) {
        drawRect(
            Brush.verticalGradient(
                listOf(Color(0xFFFF8A3D), Color(0xFFFF6B8A), Color(0xFF8A4A9A))
            ),
            alpha = intensity
        )
        val p = if (animate) quantizePhase(t, 28_000) else 0.3f
        val a = p * 2f * PI.toFloat()
        softCircle(
            Color(0xFFFFD9A0),
            Offset(size.width * 0.5f, size.height * 0.56f),
            size.width * 0.30f,
            0.7f * intensity
        )
        drawCircle(
            Color(0xFFFFE3B3),
            radius = size.minDimension * 0.09f,
            center = Offset(size.width * 0.5f, size.height * 0.56f),
            alpha = 0.95f * intensity
        )
        for (i in 0..2) {
            val path = paths[i]
            path.reset()
            val baseY = size.height * baseYs[i]
            val amp = size.height * 0.02f
            path.moveTo(0f, baseY)
            var x = 0f
            while (x <= size.width) {
                val y = baseY + amp * sin(
                    2f * PI.toFloat() * freqs[i] * (x / size.width) + a + i.toFloat()
                )
                path.lineTo(x, y)
                x += 8f
            }
            path.lineTo(size.width, size.height)
            path.lineTo(0f, size.height)
            path.close()
            drawPath(path, waveColors[i], alpha = alphas[i] * intensity)
        }
        // Reflected sun column: short dashes narrowing toward the bottom.
        for (k in 0 until 9) {
            val y = size.height * (0.62f + k * 0.045f)
            val dashW = size.width * (0.16f - k * 0.012f) * (0.8f + 0.2f * sin(a + k.toFloat()))
            drawRect(
                Color(0xFFFFD9A0),
                topLeft = Offset(size.width * 0.5f - dashW / 2f, y),
                size = Size(dashW, 3f * density),
                alpha = (0.55f - k * 0.04f) * intensity
            )
        }
    }
}
