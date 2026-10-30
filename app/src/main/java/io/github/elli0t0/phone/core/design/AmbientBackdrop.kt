package io.github.elli0t0.phone.core.design

import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import io.github.elli0t0.phone.data.settings.model.AppBackground
import io.github.elli0t0.phone.data.settings.model.AppSettings
import io.github.elli0t0.phone.data.settings.model.Backdrop
import io.github.elli0t0.phone.data.settings.model.CallBackground
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shift a color's hue by [degrees], wrapping around the wheel.
 */
private fun Color.hueShift(degrees: Float): Color {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(toArgb(), hsv)
    hsv[0] = (hsv[0] + degrees + 360f) % 360f
    return Color(AndroidColor.HSVToColor(hsv))
}

/**
 * Ambient backdrop: three large soft radial-gradient blobs in the
 * accent color and two hue-shifted variants, drifting slowly behind
 * the tab content so glass surfaces have something colorful to
 * blur. Drawn at the root (MainActivity), as the first child of the
 * container holding the NavHost, behind it.
 *
 * The blobs drift on a 28s loop only when Animations is On and the
 * app is resumed; otherwise they sit static. The animated phase is
 * read inside the Canvas draw lambda, never via `by` in composition,
 * so drift redraws without recomposing.
 *
 * Nothing is drawn when Backdrop is Off or when Pure black dark
 * mode is on (which forces Off).
 *
 * The style picker (AppBackground) dispatches here: AURORA keeps
 * the blobs below, gradients reuse the call-background brushes,
 * PLAIN draws nothing (the root Box paints the theme background),
 * PHOTO draws the user-chosen image with a dimming veil.
 */
@Composable
fun AmbientBackdrop(modifier: Modifier = Modifier) {
    val settings = LocalAppSettings.current
    if (settings.pureBlackDarkMode || settings.backdrop == Backdrop.OFF) return
    when (settings.appBackground) {
        AppBackground.AURORA -> AuroraBlobsBackdrop(modifier)
        AppBackground.PLAIN -> Unit
        AppBackground.PHOTO -> PhotoBackdrop(modifier, settings)
        else -> GradientBackdrop(modifier, settings)
    }
}

@Composable
private fun GradientBackdrop(modifier: Modifier, settings: AppSettings) {
    val brush = remember(settings.appBackground, settings.accentColor) {
        callBackgroundBrush(settings.appBackground.gradient ?: CallBackground.DEFAULT, settings.accentColor)
    }
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    // Subtle = 0.5, Vivid = 1.0; capped in light theme so text stays readable.
    val alpha = (settings.backdrop.strength * 2.5f).coerceIn(0f, if (isLight) 0.35f else 1f)
    Box(modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }.background(brush))
}

@Composable
private fun PhotoBackdrop(modifier: Modifier, settings: AppSettings) {
    val context = LocalContext.current
    val file = remember(settings.appBackgroundPhotoVersion) { File(context.filesDir, "app_background.jpg") }
    if (!file.exists()) return
    Box(modifier.fillMaxSize()) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(file)
                .memoryCacheKey("app_bg_${settings.appBackgroundPhotoVersion}").build(),
            contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = settings.appBackgroundDim)))
    }
}

@Composable
private fun AuroraBlobsBackdrop(modifier: Modifier = Modifier) {
    val settings = LocalAppSettings.current
    val backdrop = settings.backdrop
    if (settings.pureBlackDarkMode || backdrop == Backdrop.OFF) return
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumed by remember {
        mutableStateOf(
            lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        )
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> resumed = true
                Lifecycle.Event.ON_PAUSE -> resumed = false
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val animated = settings.animationOption == AnimationOption.ON && resumed
    val phase: State<Float>? = if (animated) {
        val transition = rememberInfiniteTransition(label = "ambient")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 28000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ambientPhase"
        )
    } else {
        null
    }
    val accent = Color(settings.accentColor)
    val strength = backdrop.strength
    Canvas(modifier = modifier.fillMaxSize()) {
        val a = (phase?.value ?: 0f) * 2f * PI.toFloat()
        fun blob(c: Color, cx: Float, cy: Float, r: Float) = drawCircle(
            Brush.radialGradient(
                listOf(c.copy(alpha = strength), Color.Transparent),
                Offset(cx, cy),
                r
            ),
            radius = r,
            center = Offset(cx, cy)
        )
        blob(
            accent,
            size.width * (0.25f + 0.10f * cos(a)),
            size.height * (0.30f + 0.06f * sin(a)),
            size.width * 0.9f
        )
        blob(
            accent.hueShift(60f),
            size.width * (0.80f + 0.08f * sin(a)),
            size.height * (0.62f + 0.08f * cos(a)),
            size.width * 0.8f
        )
        blob(
            accent.hueShift(-50f),
            size.width * (0.40f + 0.10f * cos(a + 2f)),
            size.height * 0.92f,
            size.width * 0.9f
        )
    }
}
