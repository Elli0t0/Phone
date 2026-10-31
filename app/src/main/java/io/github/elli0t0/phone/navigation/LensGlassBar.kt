package io.github.elli0t0.phone.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlin.math.min

// Bar blur radius in dp (0.19.5): one place to tune how much the
// page shows through the lens bar. The droplet stays blur-free.
private const val BAR_BLUR_DP = 2.5f

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
                // Text under the bar is only very slightly blurred.
                // The black tint stays 0.20.
                blur(BAR_BLUR_DP.dp.toPx() * blurAmount)
                // Aberration off, explicitly: the library default must
                // not separate channels behind the tabs (a green
                // fringe over bright page content reads as a glow
                // blob, and it is not the bar's own color).
                lens(
                    10.dp.toPx(),
                    min(20.dp.toPx(), size.minDimension),
                    chromaticAberration = false
                )
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
 * stays sharp and bends. liftValue is read only inside the effects,
 * graphicsLayer, and draw lambdas, which re-evaluate on snapshot
 * reads without recomposition; the droplet also grows with lift.
 * The amount is clamped to the surface size, so it stays valid even
 * when the bar shrinks. Nothing here clips: while the finger is
 * held, the droplet grows bigger than the bar and overflows its
 * frame above and below (the caller scales it and leaves the
 * container unclipped), magnifying the content under it. A soft
 * shadow under the lens fades in only while lifted.
 */
@Composable
fun LensDropletSurface(
    backdrop: LayerBackdrop,
    pillShape: Shape,
    liftValue: () -> Float,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Soft shadow under the lens, only while lifted: a band
        // straddling the lens's bottom edge, darkest at the edge
        // and fading downward. Drawn before the lens surface, so
        // only the part below the lens shows.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .align(Alignment.BottomCenter)
                .offset(y = 12.dp)
                .graphicsLayer { alpha = 0.18f * liftValue() }
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black, Color.Transparent)
                    ),
                    RoundedCornerShape(50)
                )
        )
        Box(
            modifier = Modifier.fillMaxSize()
                .drawBackdrop(
                backdrop = backdrop,
                shape = { pillShape },
                effects = {
                    // No vibrancy here: it over-saturated avatars
                    // into cyan blobs. The lens alone bends the
                    // content underneath, sharp and unblurred.
                    val lift = liftValue()
                    // Aberration OFF: with it on, the channel separation
                    // fringed the droplet's rim rainbow and threw a green
                    // channel blob beside it. The bend itself (refraction)
                    // is unchanged, so the lens look stays.
                    lens(
                        refractionHeight = 10.dp.toPx() * (1f + 0.4f * lift),
                        refractionAmount = min(
                            20.dp.toPx() * (1f + 0.45f * lift),
                            size.minDimension
                        ),
                        chromaticAberration = false
                    )
                },
                onDrawSurface = {
                    val lift = liftValue()
                    drawRect(Color.White.copy(alpha = 0.10f))
                    // Thin bright rim, stronger with lift: the bright
                    // end goes from white 55% at rest to 85% lifted.
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.55f + 0.30f * lift),
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
}
