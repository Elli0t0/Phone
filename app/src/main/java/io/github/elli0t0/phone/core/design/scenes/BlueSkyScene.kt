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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

private data class Cloud(
    val baseX: Float,
    val y: Float,
    val speed: Float,
    val scale: Float
)

/**
 * Blue Sky: a bright blue vertical gradient with five big soft
 * white clouds drifting at different speeds (parallax).
 */
@Composable
fun BlueSkyScene(intensity: Float, animate: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "blueSky")
    val t by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(60_000, easing = LinearEasing)),
        label = "t"
    )
    val clouds = remember {
        val random = Random(3)
        List(5) { i ->
            Cloud(
                baseX = random.nextFloat(),
                y = 0.10f + random.nextFloat() * 0.45f,
                speed = 0.35f + i * 0.22f,
                scale = 0.7f + random.nextFloat() * 0.8f
            )
        }
    }
    Canvas(modifier.fillMaxSize()) {
        drawRect(
            Brush.verticalGradient(listOf(Color(0xFF2B6FD6), Color(0xFF8EC9FF))),
            alpha = intensity
        )
        val p = if (animate) quantizePhase(t, 60_000) else 0.3f
        clouds.forEach { cloud ->
            val x = ((cloud.baseX + p * cloud.speed) % 1.3f - 0.15f) * size.width
            val cy = cloud.y * size.height
            val r = size.width * 0.13f * cloud.scale
            // A cloud is a cluster of soft white puffs.
            softCircle(Color.White, Offset(x, cy), r, 0.95f * intensity)
            softCircle(Color.White, Offset(x + r * 0.7f, cy + r * 0.15f), r * 0.75f, 0.9f * intensity)
            softCircle(Color.White, Offset(x - r * 0.7f, cy + r * 0.2f), r * 0.7f, 0.9f * intensity)
            softCircle(Color.White, Offset(x + r * 0.15f, cy - r * 0.45f), r * 0.7f, 0.9f * intensity)
        }
    }
}
