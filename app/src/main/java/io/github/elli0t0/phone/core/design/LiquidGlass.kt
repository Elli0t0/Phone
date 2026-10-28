package io.github.elli0t0.phone.core.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.ui.composed
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Outline
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import io.github.elli0t0.phone.data.settings.model.GlassIntensity
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

// Liquid glass design tokens: frosted translucent surfaces, soft gradients,
// and squircle shapes used on the in-call screen.

/**
 * The app's one blur source state, provided at the root (MainActivity).
 * Null outside the main activity (e.g. the call activity), where glass
 * falls back to the translucent treatment.
 */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/** True when a glass surface can actually blur (API 31+ RenderEffect). */
private fun blurAvailable(hazeState: HazeState?, reduceTransparency: Boolean): Boolean =
    hazeState != null &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        !reduceTransparency

/**
 * The blur layer for one glass surface: the content behind it, blurred
 * at the intensity's radius, under the spec tint (dark #1C1C1E at 55%,
 * light white at 60%, scaled by GlassIntensity). Applied after clip so
 * the blur stays inside the shape, before the fill/sheen/rim.
 * Internal (not private) so the sticky top bar and the bottom-bar
 * band can share it. Fade bands add a background-color gradient
 * overlay on top of this uniform blur: Haze 1.5.3's hazeEffect has
 * no progressive parameter, so the fade is painted over the blur
 * rather than varying the blur itself.
 */
@Composable
internal fun glassBlurModifier(
    dark: Boolean,
    blurRadius: Dp? = null
): Modifier {
    val hazeState = LocalHazeState.current
    val settings = LocalAppSettings.current
    if (!blurAvailable(hazeState, settings.reduceTransparency)) return Modifier
    // An explicit radius wins (the bottom bar wants ~36dp); otherwise
    // the radius follows GlassIntensity. The Studio blur amount
    // multiplies the final radius (default 1x = current behavior).
    val radius = (blurRadius ?: when (settings.glassIntensity) {
        GlassIntensity.SUBTLE -> PhoneDimens.glassBlurRadiusSubtle
        GlassIntensity.STANDARD -> PhoneDimens.glassBlurRadiusStandard
        GlassIntensity.STRONG -> PhoneDimens.glassBlurRadiusStrong
    }) * settings.glassBlurAmount
    val scale = when (settings.glassIntensity) {
        GlassIntensity.SUBTLE -> 0.6f
        GlassIntensity.STANDARD -> 1.0f
        GlassIntensity.STRONG -> 1.4f
    }
    val baseAlpha = if (dark) {
        PhoneDimens.glassBlurTintAlphaDark
    } else {
        PhoneDimens.glassBlurTintAlphaLight
    }
    // The Studio tint amount multiplies the tint alpha (default 1x
    // = current behavior; 0x leaves the blur untinted).
    val tint = (if (dark) GlassBlurTintDark else GlassBlurTintLight)
        .copy(alpha = (baseAlpha * scale * settings.glassTintAmount).coerceIn(0f, 0.85f))
    return Modifier.hazeEffect(
        state = hazeState!!,
        style = HazeStyle(
            backgroundColor = Color.Transparent,
            tint = HazeTint(tint),
            blurRadius = radius
        )
    )
}

/**
 * Near-opaque base for glass when blur is off: Reduce transparency
 * (solid) and API < 31 both fall back to a fill at 92% alpha, so list
 * text can never collide with bar labels.
 */
@Composable
internal fun glassSolidBaseModifier(dark: Boolean): Modifier {
    val settings = LocalAppSettings.current
    val solid = settings.reduceTransparency ||
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S
    if (!solid) return Modifier
    val base = if (dark) GlassBlurTintDark else MaterialTheme.colorScheme.surface
    return Modifier.background(base.copy(alpha = 0.92f))
}

val LiquidGlassBackgroundTop = Color(0xFF2B2A36)
val LiquidGlassBackgroundBottom = Color(0xFF100F15)
val LiquidGlassGlowPrimary = Color(0xFF4A7CFF)
val LiquidGlassGlowSecondary = Color(0xFF9B6DFF)
val LiquidGlassSurface = Color.White.copy(alpha = 0.10f)
val LiquidGlassSurfaceStrong = Color.White.copy(alpha = 0.16f)
val LiquidGlassOnSurface = Color.White
val LiquidGlassOnSurfaceMuted = Color.White.copy(alpha = 0.75f)
val LiquidGlassOnSurfaceFaint = Color.White.copy(alpha = 0.55f)
val LiquidGlassActive = Color.White
val LiquidGlassActiveContent = Color(0xFF1C1B1F)
// Light-theme glass: dark tints over a light background.
val LiquidGlassSurfaceLight = Color.Black.copy(alpha = 0.06f)
val LiquidGlassSurfaceStrongLight = Color.Black.copy(alpha = 0.10f)
val LiquidGlassOnSurfaceLight = Color(0xFF1C1C1E)
val LiquidGlassOnSurfaceMutedLight = Color(0xFF1C1C1E).copy(alpha = 0.75f)
val LiquidGlassOnSurfaceFaintLight = Color(0xFF1C1C1E).copy(alpha = 0.55f)
val LiquidGlassActiveLight = Color(0xFF1C1C1E)
val LiquidGlassActiveContentLight = Color.White
val LiquidGlassEndCall = Color(0xFFE53935)
val LiquidGlassHold = Color(0xFFFFB74D)

/**
 * Full glass color set for one theme. Screens that draw text or icons
 * on a [GlassSurface] resolve this instead of hardcoding the dark set.
 */
data class GlassColors(
    val container: Color,
    val containerStrong: Color,
    val onSurface: Color,
    val onSurfaceMuted: Color,
    val onSurfaceFaint: Color,
    val active: Color,
    val activeContent: Color
)

/**
 * Glass colors for the dark (white tints) or light (dark tints) theme.
 * No real blur is available, so the container fill is near-opaque
 * (>= 0.88 alpha) for legibility: list text must not show through the
 * floating bar or the header pill.
 */
fun glassColors(dark: Boolean): GlassColors = if (dark) {
    GlassColors(
        container = Color(0xFF2C2C2E).copy(alpha = 0.88f),
        containerStrong = LiquidGlassSurfaceStrong,
        onSurface = LiquidGlassOnSurface,
        onSurfaceMuted = LiquidGlassOnSurfaceMuted,
        onSurfaceFaint = LiquidGlassOnSurfaceFaint,
        active = LiquidGlassActive,
        activeContent = LiquidGlassActiveContent
    )
} else {
    GlassColors(
        container = Color.White.copy(alpha = 0.88f),
        containerStrong = LiquidGlassSurfaceStrongLight,
        onSurface = LiquidGlassOnSurfaceLight,
        onSurfaceMuted = LiquidGlassOnSurfaceMutedLight,
        onSurfaceFaint = LiquidGlassOnSurfaceFaintLight,
        active = LiquidGlassActiveLight,
        activeContent = LiquidGlassActiveContentLight
    )
}

/**
 * Whether the currently applied theme is dark, judged by the theme
 * background luminance. This follows the app ThemeMode setting, pure
 * black dark mode, and dynamic color, unlike isSystemInDarkTheme().
 */
@Composable
fun isDarkGlassTheme(): Boolean =
    MaterialTheme.colorScheme.background.luminance() < 0.5f

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
 * Reusable glass surface: a translucent fill with a 1dp vertical-gradient
 * border (white 35% to 5%). Placed over a gradient or image background it
 * reads as frosted glass.
 *
 * Colors default to the current theme (white tints on dark, dark tints
 * on light). Pass explicit colors when the surface sits over a background
 * that does not follow the theme, e.g. the always-dark call background.
 *
 * No shadow is drawn on glass: sheen + rim only (a shadow under a
 * translucent fill shows through as a dark rectangle). [floating] is
 * true for the surfaces that float above content (bottom bar, header
 * pill); cards and inline surfaces pass false.
 *
 * Real blur: when a HazeState is provided at the root (Haze 1.5.3,
 * verified hazeSource/hazeEffect API) and the device runs API 31+,
 * the content behind the surface is really blurred under the spec
 * tint; otherwise it falls back to a near-opaque fill (see
 * [glassSolidBaseModifier]). When blur is active the container fill
 * thins to [PhoneDimens.glassBlurContainerAlpha]: the previous 0.88
 * fill sat on top of the effect and hid the blur entirely, which
 * made the bar read as flat dark gray. Without blur the opaque
 * fill stays, for legibility and the API < 31 fallback.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = LiquidGlassDockShape,
    dark: Boolean = isDarkGlassTheme(),
    containerColor: Color = glassColors(dark).container,
    floating: Boolean = true,
    // Fill alpha while live blur is on (defaults to the standard
    // thinned fill; the bottom bar passes 0.20f so the blur and the
    // backdrop show through clearly).
    containerAlpha: Float = PhoneDimens.glassBlurContainerAlpha,
    // Blur radius override; null keeps the GlassIntensity radius.
    blurRadius: Dp? = null,
    // 1dp rim override; null keeps the standard vertical sheen.
    rim: Brush? = null,
    // Faint vertical highlights fading inward from the left/right
    // ends, drawn over the fill and under the rim; 0 disables them.
    edgeHighlightAlpha: Float = 0f,
    // Edge refraction on the blur layer only (the floating bottom
    // bar passes true; every other caller keeps the plain blur).
    // The icons and the droplet live in the content layer above the
    // blur, so they are never warped.
    edgeRefraction: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val settings = LocalAppSettings.current
    val blurOn = blurAvailable(LocalHazeState.current, settings.reduceTransparency)
    // Studio edge light scales the rim and end highlights (Soft =
    // current behavior); an explicit rim is scaled at its call site.
    val edgeLightFactor = settings.edgeLight.factor
    val effectiveEdgeHighlightAlpha = (edgeHighlightAlpha * edgeLightFactor).coerceIn(0f, 1f)
    val effectiveContainer =
        if (blurOn) containerColor.copy(alpha = containerAlpha) else containerColor
    val edgeHighlightModifier =
        if (effectiveEdgeHighlightAlpha > 0f) {
            Modifier.drawBehind {
                val fadeWidth = size.width * 0.12f
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = effectiveEdgeHighlightAlpha),
                            Color.Transparent
                        ),
                        startX = 0f,
                        endX = fadeWidth
                    )
                )
                drawRect(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = effectiveEdgeHighlightAlpha)
                        ),
                        startX = size.width - fadeWidth,
                        endX = size.width
                    )
                )
            }
        } else {
            Modifier
        }
    val effectiveRim = rim ?: Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = (0.35f * edgeLightFactor).coerceIn(0f, 1f)),
            Color.White.copy(alpha = (0.05f * edgeLightFactor).coerceIn(0f, 1f))
        )
    )
    // Solid fallback + fill + highlights + rim, shared by both
    // layouts below so the refracted and plain bars match exactly.
    val fillChain = Modifier
        .then(glassSolidBaseModifier(dark))
        .background(effectiveContainer)
        .then(edgeHighlightModifier)
        .border(
            width = 1.dp,
            brush = effectiveRim,
            shape = shape
        )
    // Edge refraction: an AGSL warp of the blurred backdrop, pulling
    // samples slightly toward the center near the left/right ends so
    // the backdrop reads magnified there (cubic falloff, zero at the
    // center, 3% of the width at most). API 33+ only (RuntimeShader);
    // Reduce transparency already disables blur, which disables this
    // too. Any failure (bad shader, missing effect) falls back to the
    // plain blur: the branch below only runs when the effect exists.
    // Note: this surface has no saturation RenderEffect to chain
    // with (its blur is a Haze effect, not a RenderEffect); the
    // saturation effect in liquidGlass() is untouched, and chaining
    // would only apply if both effects ever shared one layer.
    val refractionWanted = edgeRefraction && blurOn &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val refractionShader = remember(refractionWanted) {
        if (refractionWanted) {
            runCatching { RuntimeShader(EdgeRefractionAgSL) }.getOrNull()
        } else {
            null
        }
    }
    val refractionEffect = remember(refractionShader) {
        refractionShader?.let {
            runCatching {
                RenderEffect.createRuntimeShaderEffect(it, "inputShader")
                    .asComposeRenderEffect()
            }.getOrNull()
        }
    }
    if (refractionEffect != null) {
        // Refracted layout: three stacked layers, all clipped to the
        // shape. The blur layer carries the warp; the fill layer and
        // the content layer sit above it, unwarped. The blur and fill
        // layers are matchParentSize, so the content layer alone sizes
        // the bar exactly as the single Box did. Neither lower layer
        // handles input, so taps and drags still hit the content.
        Box(modifier = modifier) {
            Box(
                modifier = Modifier.matchParentSize()
                    .clip(shape)
                    .then(glassBlurModifier(dark, blurRadius))
                    .onSizeChanged { size ->
                        runCatching {
                            refractionShader?.setFloatUniform(
                                "size",
                                size.width.toFloat(),
                                size.height.toFloat()
                            )
                        }
                    }
                    .graphicsLayer { renderEffect = refractionEffect }
            )
            Box(
                modifier = Modifier.matchParentSize()
                    .clip(shape)
                    .then(fillChain)
            )
            Box(
                modifier = Modifier.clip(shape),
                content = content
            )
        }
    } else {
        Box(
            modifier = modifier
                .clip(shape)
                .then(glassBlurModifier(dark, blurRadius))
                .then(fillChain),
            content = content
        )
    }
}

/**
 * AGSL for the bottom bar's edge refraction. Samples the layer's
 * blurred backdrop with a horizontal warp: near the left/right ends
 * the sample coordinate is pulled toward the center, so a small
 * output span covers a smaller input span and the backdrop reads
 * magnified there. The pull is cubic in the distance from the
 * center, so it is zero and flat at the center and strongest at the
 * very ends, with no seam. Vertical coordinates are untouched, and
 * the sample is clamped to the layer so no edge pixels are invented.
 * Until the layer size is known the shader passes through.
 */
private const val EdgeRefractionAgSL = """
    uniform float2 size;
    uniform shader inputShader;
    vec4 main(vec2 fragCoord) {
        if (size.x < 1.0 || size.y < 1.0) {
            return inputShader.eval(fragCoord);
        }
        float halfW = size.x * 0.5;
        float dist = abs(fragCoord.x - halfW) / halfW;
        float e = dist * dist * dist;
        float sampleX = fragCoord.x - sign(fragCoord.x - halfW) * e * 0.03 * size.x;
        sampleX = clamp(sampleX, 0.0, size.x);
        return inputShader.eval(vec2(sampleX, fragCoord.y));
    }
"""

/**
 * The brushes of one liquid-glass treatment, built once
 * and remembered. [fill] is the translucent tint gradient, [sheen] the
 * specular highlight drawn over it, [rim] the 1dp refractive border.
 */
private class GlassSpec(
    val fill: Brush,
    val sheen: Brush,
    val rim: Brush
) {
    companion object {
        fun create(
            dark: Boolean,
            base: Color,
            isSelected: Boolean,
            intensity: GlassIntensity = GlassIntensity.STANDARD,
            reduceTransparency: Boolean = false,
            edgeLightFactor: Float = 1f
        ): GlassSpec {
            // Glass intensity scales every glass alpha: Subtle softens the
            // effect, Strong deepens it. Reduce transparency makes the
            // fill more opaque (less see-through) without changing the
            // sheen or rim.
            val scale = when (intensity) {
                GlassIntensity.SUBTLE -> 0.6f
                GlassIntensity.STANDARD -> 1.0f
                GlassIntensity.STRONG -> 1.4f
            }
            val fillBoost = if (reduceTransparency) 1.8f else 1.0f
            fun scaled(alpha: Float, boost: Float = 1.0f) =
                (alpha * scale * boost).coerceIn(0f, 1f)
            val topAlpha = scaled(if (dark) 0.24f else 0.78f)
            val bottomAlpha = scaled(if (dark) 0.12f else 0.52f)
            val fillTopAlpha = scaled(if (isSelected) 0.22f else 0.08f, fillBoost)
            val fillBottomAlpha = scaled(if (isSelected) 0.14f else 0.04f, fillBoost)
            return GlassSpec(
                fill = Brush.linearGradient(
                    colors = listOf(
                        base.copy(alpha = fillTopAlpha),
                        base.copy(alpha = fillBottomAlpha)
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite
                ),
                sheen = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = topAlpha),
                        Color.White.copy(alpha = bottomAlpha)
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite
                ),
                // 1dp rim: vertical white gradient, 35% at the top
                // edge fading to 5% at the bottom edge, scaled by
                // the Studio edge light (Soft = current behavior).
                rim = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = (scaled(0.35f) * edgeLightFactor).coerceIn(0f, 1f)),
                        Color.White.copy(alpha = (scaled(0.05f) * edgeLightFactor).coerceIn(0f, 1f))
                    )
                )
            )
        }
    }
}

/**
 * True liquid glass: a refractive, tactile surface.
 *
 * Blur: when a HazeState is provided at the root and the device runs
 * API 31+, the content behind the surface is really blurred (radius
 * by GlassIntensity) under the spec tint, so list text can no longer
 * collide with bar labels. Without a HazeState, on API < 31, or with
 * Reduce transparency on, the surface falls back to a near-opaque
 * fill (92% alpha) — never just translucent.
 *
 * Fill: a translucent tint gradient. Sheen: a bright specular highlight
 * at the top-left fading toward the bottom-right, drawn over the fill.
 * Rim: a 1.dp vertical border (white 35% at the top edge to 5% at the
 * bottom edge). No shadow is drawn on glass — sheen + rim only. A
 * hardware shadow under a translucent fill shows through as a dark
 * inset rectangle, so the spec's outer-only shadow is intentionally
 * omitted: blur + tint carry the separation instead.
 *
 * Dark detection follows the app theme (ThemeMode), not the system theme,
 * so glass matches the app setting in both Light and Dark themes.
 * [forceDarkGlass] forces the dark treatment, e.g. surfaces over the
 * always-dark call background. [floating] is true only for the bottom bar,
 * header pill, details/edit bar, and popups; cards, keys, chips, icon
 * buttons, and gem buttons pass false and get sheen + rim only.
 *
 * [elevation] is ignored (no hardware shadow is drawn). It is kept for
 * source compatibility while call sites migrate to [floating].
 */
@Composable
/**
 * [blur] skips the live-blur stack (haze effect + its opaque fallback)
 * for surfaces that already have their own opaque background: the haze
 * tint would otherwise wash that background out (e.g. the green call
 * button under a white light-theme tint). Fill, sheen and rim stay.
 *
 * [premium] upgrades the treatment: a 1dp diagonal specular border
 * (bright top-left, faint bottom-right) instead of the vertical rim,
 * a soft inner top highlight, a soft diffuse shadow, and a ~1.7x
 * saturation boost on the blurred backdrop (API 31+ live blur only,
 * guarded so a failure just drops the boost). [tilt] shifts the
 * specular border by ~20% of the size; it is a [State] read inside
 * the draw lambda, so tilt updates redraw without recomposing.
 * Premium surfaces keep achromatic children (text/icons), so the
 * backdrop saturation never tints content.
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color = Color.Unspecified,
    isSelected: Boolean = false,
    forceDarkGlass: Boolean = false,
    floating: Boolean = false,
    elevation: Dp = 8.dp,
    blur: Boolean = true,
    premium: Boolean = false,
    tilt: State<Offset>? = null,
): Modifier {
    val dark = isDarkGlassTheme() || forceDarkGlass
    val base = if (tint != Color.Unspecified) tint else MaterialTheme.colorScheme.primary
    val settings = LocalAppSettings.current
    val edgeLightFactor = settings.edgeLight.factor
    val spec = remember(dark, base, isSelected, settings.glassIntensity, settings.reduceTransparency, edgeLightFactor) {
        GlassSpec.create(dark, base, isSelected, settings.glassIntensity, settings.reduceTransparency, edgeLightFactor)
    }
    val blurOn = blur && blurAvailable(LocalHazeState.current, settings.reduceTransparency)
    val saturationEffect = remember(premium, blurOn, settings.glassColorRichness) {
        if (!premium || !blurOn || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            null
        } else {
            runCatching {
                // Studio color richness: the saturation boost on the
                // blurred backdrop (default 1.7x = current behavior).
                val matrix = ColorMatrix().apply { setSaturation(settings.glassColorRichness) }
                RenderEffect.createColorFilterEffect(ColorMatrixColorFilter(matrix))
                    .asComposeRenderEffect()
            }.getOrNull()
        }
    }
    return this
        .then(
            if (premium) {
                Modifier.shadow(
                    elevation = 6.dp,
                    shape = shape,
                    spotColor = Color.Black.copy(alpha = 0.20f)
                )
            } else {
                Modifier
            }
        )
        .clip(shape)
        .then(if (blur) glassBlurModifier(dark) else Modifier)
        .then(if (blur) glassSolidBaseModifier(dark) else Modifier)
        .background(spec.fill)
        .background(spec.sheen)
        .then(if (premium) Modifier else Modifier.border(1.dp, spec.rim, shape))
        .then(
            if (premium) {
                Modifier.drawBehind {
                    val t = tilt?.value ?: Offset.Zero
                    val shiftX = t.x * size.width * 0.2f
                    val shiftY = t.y * size.height * 0.2f
                    // Inset by half the stroke: the stroke is centered
                    // on the outline, and the clip above would cut its
                    // outer half, leaving a half-width border.
                    // DrawScope has no drawOutline in this Compose
                    // version, so the outline is drawn by subclass:
                    // rect, round rect, or generic path.
                    val strokePx = 1.dp.toPx()
                    // Diagonal specular border, scaled by the Studio
                    // edge light (Soft = current behavior).
                    val borderBrush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = (0.55f * edgeLightFactor).coerceIn(0f, 1f)),
                            Color.White.copy(alpha = (0.08f * edgeLightFactor).coerceIn(0f, 1f))
                        ),
                        start = Offset(shiftX, shiftY),
                        end = Offset(size.width + shiftX, size.height + shiftY)
                    )
                    val borderStroke = Stroke(width = strokePx)
                    val borderSize = Size(size.width - strokePx, size.height - strokePx)
                    translate(strokePx / 2f, strokePx / 2f) {
                        when (val outline = shape.createOutline(borderSize, layoutDirection, this)) {
                            is Outline.Rectangle -> drawRect(
                                brush = borderBrush,
                                size = borderSize,
                                style = borderStroke
                            )
                            is Outline.Rounded -> drawRoundRect(
                                brush = borderBrush,
                                size = borderSize,
                                // RoundRect has no single radius, only
                                // per-corner radii; premium shapes use
                                // uniform corners, so top-left stands
                                // for all four.
                                cornerRadius = outline.roundRect.topLeftCornerRadius,
                                style = borderStroke
                            )
                            is Outline.Generic -> drawPath(
                                path = outline.path,
                                brush = borderBrush,
                                style = borderStroke
                            )
                        }
                    }
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = (0.14f * edgeLightFactor).coerceIn(0f, 1f)),
                                Color.Transparent
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height * 0.45f)
                        ),
                        size = Size(size.width, size.height * 0.45f)
                    )
                }
            } else {
                Modifier
            }
        )
        .then(
            if (saturationEffect != null) {
                Modifier.graphicsLayer { renderEffect = saturationEffect }
            } else {
                Modifier
            }
        )
}

/**
 * Glass press: a press effect for glass surfaces (keys, chips, icon
 * buttons, gem buttons, bar items). While pressed, the surface scales
 * to 0.95 (spring), a white overlay at 0.12 brightens it, and a light
 * haptic fires on press down. Only active when the Animations setting
 * is ON; REDUCED and OFF leave the surface untouched.
 */
fun Modifier.glassPress(source: MutableInteractionSource): Modifier = composed {
    val option = LocalAppSettings.current.animationOption
    if (option != AnimationOption.ON) {
        this
    } else {
        val pressed by source.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (pressed) 0.95f else 1f,
            animationSpec = spring(dampingRatio = 0.55f, stiffness = 500f),
            label = "glassPressScale"
        )
        val sheen by animateFloatAsState(
            targetValue = if (pressed) 1f else 0f,
            animationSpec = tween(durationMillis = 180),
            label = "glassPressSheen"
        )
        // Light haptic the moment the press lands (Studio glass
        // haptic setting; LIGHT by default).
        val glassHaptic = rememberGlassHaptic()
        LaunchedEffect(pressed) {
            if (pressed) glassHaptic()
        }
        this
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawWithContent {
                drawContent()
                drawRect(Color.White.copy(alpha = 0.12f * sheen))
            }
    }
}

/**
 * Dialog enter: the popup scales 0.92→1 and fades in (Medium
 * duration, Emphasized easing). Enter-only; dismiss is instant.
 * Inactive when the Animations setting is OFF.
 */
fun Modifier.dialogEnter(): Modifier = composed {
    val option = LocalAppSettings.current.animationOption
    if (option == AnimationOption.OFF) {
        this
    } else {
        var entered by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { entered = true }
        val progress by animateFloatAsState(
            targetValue = if (entered) 1f else 0f,
            animationSpec = tween(
                durationMillis = Motion.scaledDurationMs(Motion.Durations.Medium, option),
                easing = Motion.Easings.Emphasized
            ),
            label = "dialogEnter"
        )
        val scale = 0.92f + 0.08f * progress
        graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = progress
        }
    }
}

/**
 * Liquid-glass card. Content lays out in a Column (like Card), so
 * multi-child content stacks vertically instead of overlapping —
 * GlassSurface's Box content stacks children on top of each other.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color = Color.Unspecified,
    isSelected: Boolean = false,
    forceDarkGlass: Boolean = false,
    floating: Boolean = false,
    premium: Boolean = false,
    tilt: State<Offset>? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.liquidGlass(shape, tint, isSelected, forceDarkGlass, floating, premium = premium, tilt = tilt),
        content = {
            // Text without an explicit color falls back to black
            // inside glass, which vanishes on dark glass: pin the
            // content color to onSurface for everything inside.
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurface
            ) {
                content()
            }
        }
    )
}

/**
 * A list card that is glass when the Glass cards setting is on and a
 * solid surface card otherwise. Used by the Contacts and Recents list
 * cards (ContactRow, ContactGroupCard, FavoritesHintRow, DaySectionCard).
 * Content lays out in a Column (like Card).
 */
@Composable
fun GlassListCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    if (LocalAppSettings.current.glassCards) {
        LiquidGlassCard(modifier = modifier, shape = shape, content = content)
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            content = {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onSurface
                ) {
                    content()
                }
            }
        )
    }
}

/**
 * Liquid-glass pill: a fully rounded horizontal container, e.g. the
 * bottom bar. Content is a Row (chips, buttons, labels).
 */
@Composable
fun LiquidGlassPill(
    modifier: Modifier = Modifier,
    shape: Shape = LiquidGlassPillShape,
    tint: Color = Color.Unspecified,
    isSelected: Boolean = false,
    forceDarkGlass: Boolean = false,
    floating: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.liquidGlass(shape, tint, isSelected, forceDarkGlass, floating),
        content = {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurface
            ) {
                content()
            }
        }
    )
}

/**
 * Liquid-glass icon button: a circular glass button around a single icon.
 * The ripple is clipped to the button shape.
 */
@Composable
fun LiquidGlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    tint: Color = Color.Unspecified,
    isSelected: Boolean = false,
    forceDarkGlass: Boolean = false,
    floating: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val source = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(shape)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .liquidGlass(shape, tint, isSelected, forceDarkGlass, floating)
            .glassPress(source),
        content = content
    )
}

/**
 * Liquid-glass chip: a small selectable pill. [selected] boosts the
 * tint and specular highlight.
 */
@Composable
fun LiquidGlassChip(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(percent = 50),
    tint: Color = Color.Unspecified,
    forceDarkGlass: Boolean = false,
    floating: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val source = remember { MutableInteractionSource() }
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(shape)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .liquidGlass(shape, tint, selected, forceDarkGlass, floating)
            .glassPress(source),
        content = {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurface
            ) {
                content()
            }
        }
    )
}

/**
 * Liquid-glass gem button: a saturated, jewel-like circular button for
 * the green Answer and red Decline / End Call buttons. Pass the gem
 * color as [tint]; the selected treatment makes it read as solid glass.
 */
@Composable
fun LiquidGlassGemButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color,
    shape: Shape = CircleShape,
    forceDarkGlass: Boolean = false,
    floating: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val source = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(shape)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .liquidGlass(shape, tint, true, forceDarkGlass, floating)
            .glassPress(source),
        content = content
    )
}

/**
 * Liquid-glass popup surface: a larger glass container for dialogs,
 * menus, and bottom sheets. Popups float above content, so [floating]
 * defaults to true and the shadow is drawn outside the shape.
 * Content lays out in a Column.
 */
@Composable
fun LiquidGlassPopupSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    tint: Color = Color.Unspecified,
    forceDarkGlass: Boolean = false,
    floating: Boolean = true,
    premium: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.liquidGlass(shape, tint, false, forceDarkGlass, floating, premium = premium),
        content = {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurface
            ) {
                content()
            }
        }
    )
}
