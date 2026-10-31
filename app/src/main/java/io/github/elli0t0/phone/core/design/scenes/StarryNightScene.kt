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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin
import kotlin.random.Random

private data class NightStar(
    val x: Float,
    val y: Float,
    val radius: Float,
    val base: Float,
    val speed: Float,
    val offset: Float
)

/**
 * Starry Night: a deep blue gradient, 80 twinkling stars, a
 * shooting star every ~8s, and a faint mountain silhouette
 * along the bottom.
 */
@Composable
fun StarryNightScene(intensity: Float, animate: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "starryNight")
    val t by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(24_000, easing = LinearEasing)),
        label = "t"
    )
    val stars = remember {
        val random = Random(24)
        List(80) {
            NightStar(
                x = random.nextFloat(),
                y = random.nextFloat() * 0.8f,
                radius = 1f + random.nextFloat() * 1.5f,
                base = 0.35f + random.nextFloat() * 0.55f,
                speed = 0.5f + random.nextFloat() * 1.5f,
                offset = random.nextFloat() * 2f * PI.toFloat()
            )
        }
    }
    val peaks = remember {
        listOf(0.12f to 0.72f, 0.25f to 0.78f, 0.38f to 0.66f, 0.52f to 0.76f, 0.68f to 0.70f, 0.82f to 0.80f)
    }
    val mountainPath = remember { Path() }
    Canvas(modifier.fillMaxSize()) {
        drawRect(
            Brush.verticalGradient(listOf(Color(0xFF070B26), Color(0xFF1B2A5E))),
            alpha = intensity
        )
        val p = if (animate) quantizePhase(t, 24_000) else 0.3f
        stars.forEach { star ->
            val twinkle = 0.55f + 0.45f * sin(2f * PI.toFloat() * (p * 8f * star.speed + star.offset))
            drawCircle(
                Color.White,
                radius = star.radius * density,
                center = Offset(star.x * size.width, star.y * size.height),
                alpha = star.base * twinkle * intensity
            )
        }
        // One shooting star per 8s third of the cycle, visible for
        // the first ~2s of its third, head first with a fading tail.
        val s = p * 3f
        val local = s - floor(s)
        if (local < 0.3f) {
            val q = local / 0.3f
            val hx = size.width * (0.75f - 0.45f * q)
            val hy = size.height * (0.12f + 0.28f * q)
            drawLine(
                Color.White,
                Offset(hx, hy),
                Offset(hx + size.width * 0.12f, hy - size.height * 0.07f),
                strokeWidth = 2f * density,
                alpha = (1f - q) * 0.9f * intensity
            )
            drawCircle(
                Color.White,
                radius = 2.5f * density,
                center = Offset(hx, hy),
                alpha = (1f - q) * intensity
            )
        }
        mountainPath.reset()
        mountainPath.moveTo(0f, size.height)
        mountainPath.lineTo(0f, size.height * 0.84f)
        peaks.forEach { (xF, yF) ->
            mountainPath.lineTo(xF * size.width, yF * size.height)
        }
        mountainPath.lineTo(size.width, size.height * 0.84f)
        mountainPath.lineTo(size.width, size.height)
        mountainPath.close()
        drawPath(mountainPath, Color(0xFF0A0F24), alpha = 0.95f * intensity)
    }
}
