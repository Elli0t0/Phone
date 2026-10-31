package io.github.elli0t0.phone.core.design

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.os.PowerManager
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
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
import kotlin.random.Random

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
 * Whether animated backdrops may move right now: Animations On,
 * app resumed, battery saver off, Reduce transparency off.
 * Battery saver is re-read on every resume, so toggling it and
 * returning to the app pauses or resumes the scenes.
 */
@Composable
private fun backdropAnimated(): Boolean {
    val settings = LocalAppSettings.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    fun powerSave(): Boolean =
        (context.getSystemService(Context.POWER_SERVICE) as? PowerManager)?.isPowerSaveMode ?: false
    var resumed by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    var saverOn by remember { mutableStateOf(powerSave()) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    resumed = true
                    saverOn = powerSave()
                }
                Lifecycle.Event.ON_PAUSE -> resumed = false
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return settings.animationOption == AnimationOption.ON && resumed && !saverOn &&
        !settings.reduceTransparency
}

/**
 * Shared 0..1 phase for an animated scene, or null when paused.
 * Callers read (phase?.value ?: 0f) inside the draw lambda, so a
 * paused scene shows its static frame and drift redraws without
 * recomposing.
 */
@Composable
private fun backdropPhase(durationMillis: Int): State<Float>? {
    if (!backdropAnimated()) return null
    val transition = rememberInfiniteTransition(label = "backdrop")
    return transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "backdropPhase"
    )
}

/**
 * Backdrop intensity with a Subtle floor: the main backdrop never
 * reaches the renderers with Off (AmbientBackdrop gates it), but
 * the picker previews do, and a preview must show the scene.
 */
private fun effectiveStrength(settings: AppSettings): Float =
    settings.backdrop.strength.coerceAtLeast(0.20f)

/** Backdrop intensity as a 0..1 scale: Subtle 0.5, Vivid 1. */
private fun strengthScale(settings: AppSettings): Float =
    (effectiveStrength(settings) / 0.40f).coerceIn(0f, 1f)

/** One soft radial-gradient blob, shared by the blob scenes. */
private fun DrawScope.softBlob(c: Color, cx: Float, cy: Float, r: Float, alpha: Float) = drawCircle(
    Brush.radialGradient(
        listOf(c.copy(alpha = alpha), Color.Transparent),
        Offset(cx, cy),
        r
    ),
    radius = r,
    center = Offset(cx, cy)
)

/**
 * Ambient backdrop, drawn at the root (MainActivity), as the
 * first child of the container holding the NavHost, behind it.
 *
 * Nothing is drawn when Backdrop is Off or when Pure black dark
 * mode is on (which forces Off). Otherwise the style picker
 * (AppBackground) dispatches to BackdropContent: AURORA keeps the
 * drifting blobs, gradients reuse the call-background brushes,
 * PLAIN draws nothing (the root Box paints the theme background),
 * PHOTO draws the user-chosen image with a dimming veil, and the
 * six custom scenes below paint their own Canvas.
 *
 * Animated scenes move only while backdropAnimated() holds; when
 * paused they show a static frame. Every scene is a single
 * Canvas with remembered constants, so a frame costs math, not
 * allocations. Dark scenes fade in light theme so text stays
 * readable with glass surfaces on top.
 */
@Composable
fun AmbientBackdrop(modifier: Modifier = Modifier) {
    val settings = LocalAppSettings.current
    if (settings.pureBlackDarkMode || settings.backdrop == Backdrop.OFF) return
    BackdropContent(settings.appBackground, modifier)
}

/**
 * One background scene, independent of the Backdrop on/off gate:
 * the main backdrop calls this after gating, and the picker
 * calls it per entry for live previews.
 */
@Composable
fun BackdropContent(background: AppBackground, modifier: Modifier = Modifier) {
    val settings = LocalAppSettings.current
    when (background) {
        AppBackground.AURORA -> AuroraBlobsBackdrop(modifier)
        AppBackground.PLAIN -> Unit
        AppBackground.PHOTO -> PhotoBackdrop(modifier, settings)
        AppBackground.MESH -> MeshBackdrop(modifier)
        AppBackground.NIGHT_SKY -> NightSkyBackdrop(modifier)
        AppBackground.WAVES -> WavesBackdrop(modifier)
        AppBackground.NEON -> NeonBackdrop(modifier)
        AppBackground.MATERIAL_YOU -> MaterialYouBackdrop(modifier, settings)
        AppBackground.GRAIN -> GrainBackdrop(modifier, settings)
        else -> GradientBackdrop(modifier, settings, background)
    }
}

@Composable
private fun GradientBackdrop(modifier: Modifier, settings: AppSettings, background: AppBackground) {
    val brush = remember(background, settings.accentColor) {
        callBackgroundBrush(background.gradient ?: CallBackground.DEFAULT, settings.accentColor)
    }
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    // Subtle = 0.5, Vivid = 1.0; capped in light theme so text stays readable.
    val alpha = (effectiveStrength(settings) * 2.5f).coerceIn(0f, if (isLight) 0.35f else 1f)
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
    val phase = backdropPhase(28000)
    val accent = Color(settings.accentColor)
    val strength = effectiveStrength(settings)
    Canvas(modifier = modifier.fillMaxSize()) {
        val a = (phase?.value ?: 0f) * 2f * PI.toFloat()
        softBlob(
            accent,
            size.width * (0.25f + 0.10f * cos(a)),
            size.height * (0.30f + 0.06f * sin(a)),
            size.width * 0.9f,
            strength
        )
        softBlob(
            accent.hueShift(60f),
            size.width * (0.80f + 0.08f * sin(a)),
            size.height * (0.62f + 0.08f * cos(a)),
            size.width * 0.8f,
            strength
        )
        softBlob(
            accent.hueShift(-50f),
            size.width * (0.40f + 0.10f * cos(a + 2f)),
            size.height * 0.92f,
            size.width * 0.9f,
            strength
        )
    }
}

/**
 * Mesh gradient: four soft color blobs orbiting the center on a
 * 30s loop, radial gradients at 0.55 alpha at full intensity.
 */
@Composable
private fun MeshBackdrop(modifier: Modifier = Modifier) {
    val settings = LocalAppSettings.current
    val phase = backdropPhase(30000)
    val accent = Color(settings.accentColor)
    val scale = strengthScale(settings)
    val colors = remember(accent) {
        arrayOf(accent, accent.hueShift(60f), accent.hueShift(-60f), accent.hueShift(120f))
    }
    val offsets = remember { floatArrayOf(0f, 1.6f, 3.1f, 4.7f) }
    Canvas(modifier = modifier.fillMaxSize()) {
        val a = (phase?.value ?: 0f) * 2f * PI.toFloat()
        for (i in 0..3) {
            val p = a + offsets[i]
            softBlob(
                colors[i],
                size.width * (0.5f + 0.35f * cos(p)),
                size.height * (0.5f + 0.35f * sin(p)),
                size.width * 0.75f,
                0.55f * scale
            )
        }
    }
}

private data class Star(
    val x: Float,
    val y: Float,
    val radius: Float,
    val base: Float,
    val speed: Float,
    val offset: Float
)

/**
 * Night sky: a dark vertical gradient with faint twinkling stars.
 * Star positions and twinkle parameters are fixed (seeded), so a
 * paused scene is a steady star field, not a reshuffled one.
 */
@Composable
private fun NightSkyBackdrop(modifier: Modifier = Modifier) {
    val settings = LocalAppSettings.current
    val phase = backdropPhase(8000)
    val scale = strengthScale(settings)
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val stars = remember {
        val random = Random(7)
        List(90) {
            Star(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radius = 1f + random.nextFloat() * 1.5f,
                base = 0.35f + random.nextFloat() * 0.55f,
                speed = 0.5f + random.nextFloat() * 1.5f,
                offset = random.nextFloat() * 2f * PI.toFloat()
            )
        }
    }
    Box(
        modifier = modifier.fillMaxSize()
            .graphicsLayer { alpha = if (isLight) 0.35f else 1f }
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF0B1026), Color.Black))
            )
        )
        Canvas(Modifier.fillMaxSize()) {
            val a = (phase?.value ?: 0f) * 2f * PI.toFloat()
            for (star in stars) {
                val twinkle = 0.55f + 0.45f * sin(a * star.speed + star.offset)
                drawCircle(
                    Color.White,
                    radius = star.radius * density,
                    center = Offset(star.x * size.width, star.y * size.height),
                    alpha = star.base * twinkle * (0.5f + 0.5f * scale)
                )
            }
        }
    }
}

/**
 * Liquid waves: three slow translucent sine waves filling the
 * bottom of the screen, drifting on a 24s loop. Paths are
 * remembered and reset each frame, never reallocated.
 */
@Composable
private fun WavesBackdrop(modifier: Modifier = Modifier) {
    val settings = LocalAppSettings.current
    val phase = backdropPhase(24000)
    val accent = Color(settings.accentColor)
    val scale = strengthScale(settings)
    val colors = remember(accent) {
        arrayOf(accent, accent.hueShift(40f), accent.hueShift(-40f))
    }
    val alphas = remember { floatArrayOf(0.30f, 0.22f, 0.16f) }
    val baseYs = remember { floatArrayOf(0.72f, 0.80f, 0.88f) }
    val freqs = remember { floatArrayOf(1.5f, 2.0f, 2.5f) }
    val paths = remember { Array(3) { Path() } }
    Canvas(modifier = modifier.fillMaxSize()) {
        val a = (phase?.value ?: 0f) * 2f * PI.toFloat()
        for (i in 0..2) {
            val path = paths[i]
            path.reset()
            val baseY = size.height * baseYs[i]
            val amp = size.height * 0.04f
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
            drawPath(path, colors[i], alpha = alphas[i] * scale)
        }
    }
}

/**
 * Neon glow: a dark base with two soft neon orbs that pulse in
 * opposition on a 5s loop.
 */
@Composable
private fun NeonBackdrop(modifier: Modifier = Modifier) {
    val settings = LocalAppSettings.current
    val phase = backdropPhase(5000)
    val accent = Color(settings.accentColor)
    val scale = strengthScale(settings)
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val orbs = remember(accent) { arrayOf(accent, accent.hueShift(140f)) }
    Box(
        modifier = modifier.fillMaxSize()
            .graphicsLayer { alpha = if (isLight) 0.35f else 1f }
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF0B0B12), Color(0xFF1B1035)))
            )
        )
        Canvas(Modifier.fillMaxSize()) {
            val a = (phase?.value ?: 0f) * 2f * PI.toFloat()
            softBlob(
                orbs[0],
                size.width * 0.25f,
                size.height * 0.35f,
                size.width * 0.7f,
                0.55f * scale * (0.75f + 0.25f * sin(a))
            )
            softBlob(
                orbs[1],
                size.width * 0.75f,
                size.height * 0.65f,
                size.width * 0.7f,
                0.45f * scale * (0.75f + 0.25f * sin(a + PI.toFloat()))
            )
        }
    }
}

/**
 * Material You: a gradient built from the theme palette itself.
 * On Android 12+ with Dynamic color on, the scheme is the system
 * palette; otherwise it is the static palette, same rendering.
 */
@Composable
private fun MaterialYouBackdrop(modifier: Modifier, settings: AppSettings) {
    val scheme = MaterialTheme.colorScheme
    val brush = remember(
        scheme.primaryContainer,
        scheme.secondaryContainer,
        scheme.tertiaryContainer
    ) {
        Brush.verticalGradient(
            listOf(scheme.primaryContainer, scheme.secondaryContainer, scheme.tertiaryContainer)
        )
    }
    val isLight = scheme.background.luminance() > 0.5f
    val alpha = (effectiveStrength(settings) * 2.5f).coerceIn(0f, if (isLight) 0.35f else 1f)
    Box(modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }.background(brush))
}

/**
 * Grain gradient: a palette gradient with a subtle noise texture
 * over it. The noise bitmap is generated once (seeded) and tiled
 * across the canvas, so frames cost draw calls, not allocations.
 */
@Composable
private fun GrainBackdrop(modifier: Modifier, settings: AppSettings) {
    val scheme = MaterialTheme.colorScheme
    val brush = remember(scheme.primaryContainer, scheme.background) {
        Brush.verticalGradient(listOf(scheme.primaryContainer, scheme.background))
    }
    val isLight = scheme.background.luminance() > 0.5f
    val alpha = (effectiveStrength(settings) * 2.5f).coerceIn(0f, if (isLight) 0.35f else 1f)
    val noise = remember {
        val tile = 128
        val bitmap = Bitmap.createBitmap(tile, tile, Bitmap.Config.ARGB_8888)
        val random = Random(1234)
        val pixels = IntArray(tile * tile) {
            val v = random.nextInt(256)
            AndroidColor.argb(255, v, v, v)
        }
        bitmap.setPixels(pixels, 0, tile, 0, 0, tile, tile)
        bitmap.asImageBitmap()
    }
    Box(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }.background(brush))
        Canvas(Modifier.fillMaxSize()) {
            val tileW = noise.width
            val tileH = noise.height
            var ty = 0
            while (ty < size.height) {
                var tx = 0
                while (tx < size.width) {
                    drawImage(noise, dstOffset = IntOffset(tx, ty), alpha = 0.05f)
                    tx += tileW
                }
                ty += tileH
            }
        }
    }
}
