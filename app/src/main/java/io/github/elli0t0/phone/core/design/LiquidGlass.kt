package io.github.elli0t0.phone.core.design

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

// Liquid glass design tokens: frosted translucent surfaces, soft gradients,
// and squircle shapes used on the in-call screen.

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
 * The shadow is drawn OUTSIDE the shape ([outerShadow]), never as a
 * hardware shadow under the translucent fill (that shows through as a
 * dark rectangle). [floating] is true for the surfaces that float above
 * content (bottom bar, header pill); cards and inline surfaces pass false.
 *
 * Real blur (Haze) was checked first: Haze 2.0.1 is the latest stable release,
 * but its new modular 2.x API is unverified against this project's Compose
 * version and cannot be compile-checked locally, so this version ships the
 * translucent fallback. No blur is faked: the surface is honestly translucent.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = LiquidGlassDockShape,
    dark: Boolean = isDarkGlassTheme(),
    containerColor: Color = glassColors(dark).container,
    floating: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .then(if (floating) Modifier.outerShadow(shape, Color.Black.copy(alpha = 0.10f)) else Modifier)
            .clip(shape)
            .background(containerColor)
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.35f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = shape
            ),
        content = content
    )
}

/**
 * The brushes and shadow color of one liquid-glass treatment, built once
 * and remembered. [fill] is the translucent tint gradient, [sheen] the
 * specular highlight drawn over it, [rim] the 1dp refractive border,
 * [shadow] the color used by [outerShadow] for floating surfaces.
 */
private class GlassSpec(
    val fill: Brush,
    val sheen: Brush,
    val rim: Brush,
    val shadow: Color
) {
    companion object {
        fun create(dark: Boolean, base: Color, isSelected: Boolean): GlassSpec {
            val topAlpha = if (dark) 0.24f else 0.78f
            val bottomAlpha = if (dark) 0.12f else 0.52f
            val borderTopAlpha = if (dark) 0.48f else 0.92f
            val borderBottomAlpha = if (dark) 0.14f else 0.30f
            val fillTopAlpha = if (isSelected) 0.22f else 0.08f
            val fillBottomAlpha = if (isSelected) 0.14f else 0.04f
            val rimMidAlpha = if (isSelected) 0.45f else 0.15f
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
                rim = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = borderTopAlpha),
                        base.copy(alpha = rimMidAlpha),
                        Color.White.copy(alpha = borderBottomAlpha)
                    ),
                    start = Offset.Zero,
                    end = Offset.Infinite
                ),
                shadow = base.copy(alpha = 0.35f)
            )
        }
    }
}

/**
 * A shadow drawn only OUTSIDE the shape: the shape interior is cut out
 * (ClipOp.Difference), so no shadow ever shows through a translucent fill.
 * If this does not render correctly on a device, drop shadows completely
 * rather than reintroducing a hardware shadow under a translucent fill.
 */
fun Modifier.outerShadow(
    shape: Shape,
    color: Color,
    blur: Dp = 14.dp,
    dy: Dp = 4.dp
): Modifier = drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    val path = Path().apply {
        when (outline) {
            is Outline.Rectangle -> addRect(outline.rect)
            is Outline.Rounded -> addRoundRect(outline.roundRect)
            is Outline.Generic -> addPath(outline.path)
        }
    }
    clipPath(path, ClipOp.Difference) {
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                asFrameworkPaint().apply {
                    this.color = android.graphics.Color.TRANSPARENT
                    setShadowLayer(blur.toPx(), 0f, dy.toPx(), color.toArgb())
                }
            }
            canvas.drawPath(path, paint)
        }
    }
}

/**
 * True liquid glass: a refractive, tactile surface.
 *
 * Fill: a translucent tint gradient. Sheen: a bright specular highlight
 * at the top-left fading toward the bottom-right, drawn over the fill.
 * Rim: a 1.dp diagonal refractive border (bright white at the top-left,
 * subtle accent tint in the center, soft reflection at the bottom-right).
 * Shadow: only for [floating] surfaces, drawn OUTSIDE the shape via
 * [outerShadow]. Never a hardware shadow under a translucent fill — that
 * shows through as a dark inset rectangle.
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
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color = Color.Unspecified,
    isSelected: Boolean = false,
    forceDarkGlass: Boolean = false,
    floating: Boolean = false,
    elevation: Dp = 8.dp,
): Modifier {
    val dark = isDarkGlassTheme() || forceDarkGlass
    val base = if (tint != Color.Unspecified) tint else MaterialTheme.colorScheme.primary
    val spec = remember(dark, base, isSelected) { GlassSpec.create(dark, base, isSelected) }
    return this
        .then(if (floating) Modifier.outerShadow(shape, spec.shadow) else Modifier)
        .clip(shape)
        .background(spec.fill)
        .background(spec.sheen)
        .border(1.dp, spec.rim, shape)
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
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.liquidGlass(shape, tint, isSelected, forceDarkGlass, floating),
        content = content
    )
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
        content = content
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
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick)
            .liquidGlass(shape, tint, isSelected, forceDarkGlass, floating),
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
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick)
            .liquidGlass(shape, tint, selected, forceDarkGlass, floating),
        content = content
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
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick)
            .liquidGlass(shape, tint, true, forceDarkGlass, floating),
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
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.liquidGlass(shape, tint, false, forceDarkGlass, floating),
        content = content
    )
}
