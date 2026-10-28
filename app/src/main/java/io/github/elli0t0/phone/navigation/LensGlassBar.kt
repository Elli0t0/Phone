package io.github.elli0t0.phone.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlin.math.min

/**
 * Real lens-glass surfaces for the bottom bar, via the Backdrop
 * library. This file is the only place Backdrop classes are touched,
 * and it is only composed behind the Lens bar gate (API 33+, Lens bar
 * switch on, Reduce transparency off), so nothing here loads on older
 * systems: the Haze bar stays the only path there.
 *
 * The lens only bends the content behind the bar: the bar's icons and
 * labels are drawn by the caller on top of these surfaces, never
 * through them.
 */

/** Whole-bar surface: blurred, tinted lens glass with a gradient rim. */
@Composable
fun LensBarSurface(
    backdrop: LayerBackdrop,
    pillShape: Shape,
    blurAmount: Float,
    tintAmount: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { pillShape },
            effects = {
                vibrancy()
                blur(10.dp.toPx() * blurAmount)
                lens(10.dp.toPx(), min(20.dp.toPx(), size.minDimension))
            },
            onDrawSurface = {
                drawRect(Color.Black.copy(alpha = 0.20f * tintAmount))
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.40f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    ),
                    cornerRadius = CornerRadius(size.height / 2f, size.height / 2f),
                    style = Stroke(1.dp.toPx())
                )
            }
        )
    )
}

/**
 * Selected-tab droplet: a pure lens, no blur, so content under it
 * stays sharp and bends. liftValue is read only inside the effects
 * lambda, which the library re-evaluates on snapshot reads without
 * recomposition; the droplet also grows with lift. The amount is
 * clamped to the surface size, so it stays valid even when the bar
 * shrinks.
 */
@Composable
fun LensDropletSurface(
    backdrop: LayerBackdrop,
    pillShape: Shape,
    liftValue: () -> Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { pillShape },
            effects = {
                vibrancy()
                val lift = liftValue()
                lens(
                    refractionHeight = 12.dp.toPx() * (1f + 0.5f * lift),
                    refractionAmount = min(
                        28.dp.toPx() * (1f + 0.6f * lift),
                        size.minDimension
                    ),
                    chromaticAberration = true
                )
            },
            onDrawSurface = {
                drawRect(Color.White.copy(alpha = 0.10f))
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.65f),
                            Color.White.copy(alpha = 0.30f)
                        )
                    ),
                    cornerRadius = CornerRadius(size.height / 2f, size.height / 2f),
                    style = Stroke(1.dp.toPx())
                )
            }
        )
    )
}
