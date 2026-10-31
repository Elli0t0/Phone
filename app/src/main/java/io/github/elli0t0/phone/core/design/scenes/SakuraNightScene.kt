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
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private data class Petal(
    val baseX: Float,
    val seedY: Float,
    val fall: Float,
    val swayAmp: Float,
    val swayFreq: Float,
    val phase: Float,
    val rotSpeed: Float,
    val size: Float,
    val color: Color
)

/**
 * Sakura Night: a navy sky, a big pale moon with a soft glow,
 * and 25 falling petals rotating with a sine drift.
 */
@Composable
fun SakuraNightScene(intensity: Float, animate: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "sakura")
    val t by transition.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(20_000, easing = LinearEasing)),
        label = "t"
    )
    val petals = remember {
        List(25) { i ->
            val random = Random(i * 31 + 7)
            Petal(
                baseX = random.nextFloat(),
                seedY = random.nextFloat(),
                fall = 0.25f + random.nextFloat() * 0.5f,
                swayAmp = 0.02f + random.nextFloat() * 0.05f,
                swayFreq = 0.5f + random.nextFloat(),
                phase = random.nextFloat() * 2f * PI.toFloat(),
                rotSpeed = 0.5f + random.nextFloat() * 1.5f,
                size = 0.008f + random.nextFloat() * 0.012f,
                color = if (i % 3 == 0) Color.White else Color(0xFFF9C5D5)
            )
        }
    }
    Canvas(modifier.fillMaxSize()) {
        drawRect(
            Brush.verticalGradient(listOf(Color(0xFF0B1030), Color(0xFF2A3A7A))),
            alpha = intensity
        )
        val moonCenter = Offset(size.width * 0.75f, size.height * 0.2f)
        softCircle(
            Color(0xFFFFF4D6),
            moonCenter,
            size.minDimension * 0.30f,
            0.25f * intensity
        )
        drawCircle(
            Color(0xFFFFF4D6),
            radius = size.minDimension * 0.14f,
            center = moonCenter,
            alpha = 0.9f * intensity
        )
        val p = if (animate) quantizePhase(t, 20_000) else 0.3f
        petals.forEach { petal ->
            // y = (seed + p * fall) % 1 * h, x = base + sin(p * 2π * k) * amp
            val y = ((petal.seedY + p * petal.fall) % 1f) * size.height
            val x = petal.baseX * size.width +
                sin(2f * PI.toFloat() * p * petal.swayFreq + petal.phase) *
                petal.swayAmp * size.width
            val angle = (p * petal.rotSpeed * 360f + petal.phase * 57f) % 360f
            rotate(angle, pivot = Offset(x, y)) {
                drawOval(
                    petal.color,
                    topLeft = Offset(x - petal.size * size.width, y - petal.size * size.width * 0.6f),
                    size = Size(petal.size * size.width * 2f, petal.size * size.width * 1.2f),
                    alpha = 0.9f * intensity
                )
            }
        }
    }
}
