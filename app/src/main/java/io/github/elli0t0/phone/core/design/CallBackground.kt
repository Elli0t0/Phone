package io.github.elli0t0.phone.core.design

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import io.github.elli0t0.phone.data.settings.model.CallBackground

/**
 * Background for the incoming + ongoing call screens, from the Call
 * background setting. White text and controls sit on all three, so
 * the Accent variant darkens the accent instead of using it raw.
 * For CUSTOM, callers pass the custom background color as
 * [accentColor] (CallActivity and the studio preview do this).
 * AURORA is a static fallback here (the animated version is
 * [CallBackgroundLayer]); studio swatches use this brush.
 */
fun callBackgroundBrush(background: CallBackground, accentColor: Long): Brush = when (background) {
    CallBackground.DEFAULT -> Brush.verticalGradient(
        colors = listOf(InCallGradientTop, InCallGradientBottom)
    )
    CallBackground.DARK -> SolidColor(Color.Black)
    CallBackground.ACCENT -> {
        val accent = Color(accentColor)
        Brush.verticalGradient(
            colors = listOf(accent.darken(0.8f), accent.darken(0.35f))
        )
    }
    // Preset gradients: dark enough for white call-screen text.
    CallBackground.SUNSET -> Brush.verticalGradient(
        colors = listOf(Color(0xFF3A1C71), Color(0xFFB84A62))
    )
    CallBackground.OCEAN -> Brush.verticalGradient(
        colors = listOf(Color(0xFF0F2027), Color(0xFF2C5364))
    )
    CallBackground.FOREST -> Brush.verticalGradient(
        colors = listOf(Color(0xFF0B3D2E), Color(0xFF2E7D5B))
    )
    CallBackground.LAVENDER -> Brush.verticalGradient(
        colors = listOf(Color(0xFF4C1D95), Color(0xFF8B5CF6))
    )
    CallBackground.ROSE -> Brush.verticalGradient(
        colors = listOf(Color(0xFF831843), Color(0xFFF472B6))
    )
    CallBackground.MIDNIGHT -> Brush.verticalGradient(
        colors = listOf(Color(0xFF0B1026), Color(0xFF1E3A8A))
    )
    CallBackground.AMBER -> Brush.verticalGradient(
        colors = listOf(Color(0xFF7C2D12), Color(0xFFF59E0B))
    )
    CallBackground.AURORA -> Brush.verticalGradient(
        colors = listOf(AuroraBaseTop, AuroraBaseBottom)
    )
    CallBackground.CUSTOM -> SolidColor(Color(accentColor))
}

private fun Color.darken(factor: Float): Color =
    Color(red * factor, green * factor, blue * factor, alpha)

/**
 * The full call-screen background: the animated aurora for AURORA,
 * the avatar-colored animated gradient for DEFAULT when a contact
 * is known ([avatarColors]), the static brush for every other mode
 * (and for DEFAULT with no contact, which keeps the current blue).
 */
@Composable
fun CallBackgroundLayer(
    background: CallBackground,
    accentColor: Long,
    modifier: Modifier = Modifier,
    avatarColors: Pair<Color, Color>? = null
) {
    if (background == CallBackground.AURORA) {
        AuroraBackground(accentColor = accentColor, modifier = modifier)
    } else if (background == CallBackground.DEFAULT && avatarColors != null) {
        AvatarGradientBackground(avatarColors = avatarColors, modifier = modifier)
    } else {
        Box(modifier = modifier.background(callBackgroundBrush(background, accentColor)))
    }
}

/**
 * Avatar-colored call background: a darkened vertical gradient of
 * the avatar's two tones, with three soft radial blobs (both tones
 * plus their blend) drifting very slowly. Animations ON: blobs drift
 * on 20-30s loops. REDUCED: the same blobs, static. OFF: base
 * gradient only. Cheap by design: radial falloff + alpha, no blur
 * pass and no full-screen shader, mirroring the aurora.
 */
@Composable
fun AvatarGradientBackground(
    avatarColors: Pair<Color, Color>,
    modifier: Modifier = Modifier
) {
    val option = LocalAppSettings.current.animationOption
    BoxWithConstraints(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            avatarColors.first.darken(0.35f),
                            avatarColors.second.darken(0.35f)
                        )
                    )
                )
        )
        if (option != AnimationOption.OFF) {
            if (option == AnimationOption.ON) {
                AvatarBlobsAnimated(avatarColors, maxWidth, maxHeight)
            } else {
                AvatarBlobsStatic(avatarColors, maxWidth, maxHeight)
            }
        }
    }
}

@Composable
private fun AvatarBlobsAnimated(
    avatarColors: Pair<Color, Color>,
    maxWidth: Dp,
    maxHeight: Dp
) {
    val transition = rememberInfiniteTransition(label = "avatarBlobs")
    val ax by transition.animateFloat(
        initialValue = -0.06f,
        targetValue = 0.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatarAx"
    )
    val ay by transition.animateFloat(
        initialValue = -0.04f,
        targetValue = 0.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatarAy"
    )
    val bx by transition.animateFloat(
        initialValue = 0.06f,
        targetValue = -0.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatarBx"
    )
    val by by transition.animateFloat(
        initialValue = 0.04f,
        targetValue = -0.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatarBy"
    )
    val cx by transition.animateFloat(
        initialValue = -0.05f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 30000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatarCx"
    )
    val cy by transition.animateFloat(
        initialValue = 0.05f,
        targetValue = -0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatarCy"
    )
    AvatarBlobs(
        avatarColors = avatarColors,
        maxWidth = maxWidth,
        maxHeight = maxHeight,
        drifts = listOf(ax to ay, bx to by, cx to cy)
    )
}

@Composable
private fun AvatarBlobsStatic(
    avatarColors: Pair<Color, Color>,
    maxWidth: Dp,
    maxHeight: Dp
) {
    AvatarBlobs(
        avatarColors = avatarColors,
        maxWidth = maxWidth,
        maxHeight = maxHeight,
        drifts = listOf(0f to 0f, 0f to 0f, 0f to 0f)
    )
}

@Composable
private fun AvatarBlobs(
    avatarColors: Pair<Color, Color>,
    maxWidth: Dp,
    maxHeight: Dp,
    drifts: List<Pair<Float, Float>>
) {
    val blobSize = maxWidth * 1.1f
    val blend = Color(
        red = (avatarColors.first.red + avatarColors.second.red) / 2f,
        green = (avatarColors.first.green + avatarColors.second.green) / 2f,
        blue = (avatarColors.first.blue + avatarColors.second.blue) / 2f,
        alpha = 1f
    )
    Box(modifier = Modifier.fillMaxSize()) {
        AuroraBlob(
            color = avatarColors.first,
            size = blobSize,
            offsetX = maxWidth * -0.2f + maxWidth * drifts[0].first,
            offsetY = maxHeight * -0.15f + maxHeight * drifts[0].second
        )
        AuroraBlob(
            color = avatarColors.second,
            size = blobSize,
            offsetX = maxWidth * 0.35f + maxWidth * drifts[1].first,
            offsetY = maxHeight * 0.05f + maxHeight * drifts[1].second
        )
        AuroraBlob(
            color = blend,
            size = blobSize,
            offsetX = maxWidth * -0.15f + maxWidth * drifts[2].first,
            offsetY = maxHeight * 0.45f + maxHeight * drifts[2].second
        )
    }
}

// Aurora blob softness: radial falloff + graphicsLayer alpha.
private const val AuroraBlobAlpha = 0.75f

/**
 * Animated liquid aurora: a dark base gradient with three large
 * radial-gradient blobs (accent, teal, violet) drifting slowly.
 * Animations ON: blobs drift on independent loops. REDUCED: the same
 * blobs, static (no loops). OFF: base gradient only.
 */
@Composable
fun AuroraBackground(
    accentColor: Long,
    modifier: Modifier = Modifier
) {
    val option = LocalAppSettings.current.animationOption
    BoxWithConstraints(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(colors = listOf(AuroraBaseTop, AuroraBaseBottom))
                )
        )
        if (option != AnimationOption.OFF) {
            if (option == AnimationOption.ON) {
                AuroraBlobsAnimated(accentColor, maxWidth, maxHeight)
            } else {
                AuroraBlobsStatic(accentColor, maxWidth, maxHeight)
            }
        }
    }
}

@Composable
private fun AuroraBlobsAnimated(
    accentColor: Long,
    maxWidth: Dp,
    maxHeight: Dp
) {
    val transition = rememberInfiniteTransition(label = "aurora")
    val ax by transition.animateFloat(
        initialValue = -0.06f,
        targetValue = 0.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraAx"
    )
    val ay by transition.animateFloat(
        initialValue = -0.04f,
        targetValue = 0.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraAy"
    )
    val bx by transition.animateFloat(
        initialValue = 0.06f,
        targetValue = -0.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraBx"
    )
    val by by transition.animateFloat(
        initialValue = 0.04f,
        targetValue = -0.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 13000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraBy"
    )
    val cx by transition.animateFloat(
        initialValue = -0.05f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 17000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraCx"
    )
    val cy by transition.animateFloat(
        initialValue = 0.05f,
        targetValue = -0.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraCy"
    )
    AuroraBlobs(
        accentColor = accentColor,
        maxWidth = maxWidth,
        maxHeight = maxHeight,
        drifts = listOf(ax to ay, bx to by, cx to cy)
    )
}

@Composable
private fun AuroraBlobsStatic(
    accentColor: Long,
    maxWidth: Dp,
    maxHeight: Dp
) {
    AuroraBlobs(
        accentColor = accentColor,
        maxWidth = maxWidth,
        maxHeight = maxHeight,
        drifts = listOf(0f to 0f, 0f to 0f, 0f to 0f)
    )
}

@Composable
private fun AuroraBlobs(
    accentColor: Long,
    maxWidth: Dp,
    maxHeight: Dp,
    drifts: List<Pair<Float, Float>>
) {
    val blobSize = maxWidth * 1.1f
    Box(modifier = Modifier.fillMaxSize()) {
        // Accent blob, top-left.
        AuroraBlob(
            color = Color(accentColor),
            size = blobSize,
            offsetX = maxWidth * -0.2f + maxWidth * drifts[0].first,
            offsetY = maxHeight * -0.15f + maxHeight * drifts[0].second
        )
        // Teal blob, center-right.
        AuroraBlob(
            color = AuroraTeal,
            size = blobSize,
            offsetX = maxWidth * 0.35f + maxWidth * drifts[1].first,
            offsetY = maxHeight * 0.05f + maxHeight * drifts[1].second
        )
        // Violet blob, bottom-left.
        AuroraBlob(
            color = AuroraViolet,
            size = blobSize,
            offsetX = maxWidth * -0.15f + maxWidth * drifts[2].first,
            offsetY = maxHeight * 0.45f + maxHeight * drifts[2].second
        )
    }
}

/**
 * One aurora blob: a radial gradient fading to transparent, softened
 * further by graphicsLayer alpha — the blur without a blur pass.
 */
@Composable
private fun AuroraBlob(
    color: Color,
    size: Dp,
    offsetX: Dp,
    offsetY: Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .offset(x = offsetX, y = offsetY)
            .graphicsLayer { alpha = AuroraBlobAlpha }
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(color, Color.Transparent)
                )
            )
    )
}
