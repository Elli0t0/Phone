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
import io.github.elli0t0.phone.core.design.scenes.AnimeScenes
import io.github.elli0t0.phone.data.settings.model.AppSettings
import io.github.elli0t0.phone.data.settings.model.CallBackground
import io.github.elli0t0.phone.data.settings.model.PaletteBackgrounds
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
 * Backdrop intensity (0.18.7): the appBackgroundIntensity slider,
 * 0.1..1.0. Light theme caps it at 0.6 so text stays readable
 * with glass surfaces on top. This replaces the old Backdrop gate
 * (Off/Subtle/Vivid), whose values migrate to this slider.
 */
@Composable
private fun effectiveStrength(settings: AppSettings): Float {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    return if (isLight) {
        settings.appBackgroundIntensity.coerceAtMost(0.6f)
    } else {
        settings.appBackgroundIntensity
    }
}

/** Backdrop intensity as a 0..1 scale: the slider value itself (light-capped). */
@Composable
private fun strengthScale(settings: AppSettings): Float =
    effectiveStrength(settings).coerceIn(0f, 1f)

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
 * Pure black dark mode still forces a black base, but no longer
 * hides the background: the scene draws at 0.6 over the base, so
 * with the scene's own intensity the total is intensity * 0.6.
 * Otherwise the style picker (AppBackground) dispatches to
 * BackdropContent: AURORA keeps the drifting blobs, gradients
 * reuse the call-background brushes, PLAIN draws nothing (the
 * root Box paints the theme background), PHOTO draws the
 * user-chosen image with a dimming veil, PALETTE draws its mesh,
 * and the custom scenes below paint their own Canvas.
 *
 * Animated scenes move only while backdropAnimated() holds; when
 * paused they show a static frame. Every scene is a single
 * Canvas with remembered constants, so a frame costs math, not
 * allocations. Dark scenes fade in light theme so text stays
 * readable with glass surfaces on top.
 */
@Composable
fun AmbientBackdrop(modifier: Modifier = Modifier) {
    // Minimal (0.18.10): backgrounds are ignored — the theme
    // background (black / light grey) shows, nothing is drawn.
    if (LocalMinimal.current) return
    val settings = LocalAppSettings.current
    if (settings.pureBlackDarkMode) {
        Box(modifier.fillMaxSize().background(Color.Black)) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = 0.6f }) {
                BackdropContent(settings.appBackground, Modifier.fillMaxSize())
            }
        }
        return
    }
    BackdropContent(settings.appBackground, modifier)
}

/**
 * One background scene: the main backdrop calls this directly,
 * and the picker calls it per entry for live previews.
 */
@Composable
fun BackdropContent(background: AppBackground, modifier: Modifier = Modifier) {
    val settings = LocalAppSettings.current
    // Data-driven anime scenes (0.18.9): the registry owns the
    // dispatch, so a new scene needs no edit here.
    val animeSpec = AnimeScenes.find { it.background == background }
    if (animeSpec != null) {
        animeSpec.content(effectiveStrength(settings), backdropAnimated(), modifier)
        return
    }
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
        AppBackground.PALETTE -> PaletteBackdrop(modifier, settings)
        AppBackground.AURORA_GREEN -> AuroraBlobsBackdrop(modifier, AuroraGreenBlobs)
        AppBackground.AURORA_PURPLE -> AuroraBlobsBackdrop(modifier, AuroraPurpleBlobs)
        AppBackground.DUSK -> DuskBackdrop(modifier)
        AppBackground.BOKEH -> BokehBackdrop(modifier)
        else -> GradientBackdrop(modifier, settings, background)
    }
}

@Composable
private fun GradientBackdrop(modifier: Modifier, settings: AppSettings, background: AppBackground) {
    val brush = remember(background, settings.accentColor) {
        callBackgroundBrush(background.gradient ?: CallBackground.DEFAULT, settings.accentColor)
    }
    val alpha = effectiveStrength(settings)
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

/** Fixed hue trios for the Aurora Green / Aurora Purple scenes (0.18.7). */
private val AuroraGreenBlobs = listOf(Color(0xFF15803D), Color(0xFF22C55E), Color(0xFFA3E635))
private val AuroraPurpleBlobs = listOf(Color(0xFF7C3AED), Color(0xFF9333EA), Color(0xFFD946EF))

@Composable
private fun AuroraBlobsBackdrop(modifier: Modifier = Modifier, colors: List<Color>? = null) {
    val settings = LocalAppSettings.current
    val phase = backdropPhase(28000)
    val strength = effectiveStrength(settings)
    val blobs = colors ?: run {
        val accent = Color(settings.accentColor)
        listOf(accent, accent.hueShift(60f), accent.hueShift(-50f))
    }
    Canvas(modifier = modifier.fillMaxSize()) {
        val a = (phase?.value ?: 0f) * 2f * PI.toFloat()
        softBlob(
            blobs[0],
            size.width * (0.25f + 0.10f * cos(a)),
            size.height * (0.30f + 0.06f * sin(a)),
            size.width * 0.9f,
            strength
        )
        softBlob(
            blobs[1],
            size.width * (0.80f + 0.08f * sin(a)),
            size.height * (0.62f + 0.08f * cos(a)),
            size.width * 0.8f,
            strength
        )
        softBlob(
            blobs[2],
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
            .graphicsLayer { alpha = if (isLight) 0.6f else 1f }
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
            .graphicsLayer { alpha = if (isLight) 0.6f else 1f }
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
    val alpha = effectiveStrength(settings)
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
    val alpha = effectiveStrength(settings)
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

/**
 * Palette mesh (0.18.7): the palette's darkest color as the base,
 * with three soft blobs drifting over it on a 30s loop — the same
 * softBlob pattern as MeshBackdrop. Static when animations are
 * reduced (backdropPhase returns null).
 */
@Composable
private fun PaletteBackdrop(modifier: Modifier = Modifier, settings: AppSettings) {
    val phase = backdropPhase(30000)
    val palette = PaletteBackgrounds.find { it.id == settings.appBackgroundPaletteId }
        ?: PaletteBackgrounds.first()
    val colors = remember(palette) { palette.colors.map { Color(it) } }
    val scale = strengthScale(settings)
    val offsets = remember { floatArrayOf(0f, 2.1f, 4.2f) }
    val blobs = remember(colors) { listOf(colors[1], colors[2], colors[1].hueShift(25f)) }
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(colors[0])
        val a = (phase?.value ?: 0f) * 2f * PI.toFloat()
        for (i in 0..2) {
            val p = a + offsets[i]
            softBlob(
                blobs[i],
                size.width * (0.5f + 0.35f * cos(p)),
                size.height * (0.5f + 0.35f * sin(p)),
                size.width * 0.75f,
                0.55f * scale
            )
        }
    }
}

/**
 * Dusk (0.18.7): a vertical dusk gradient with a warm sun glow
 * low on the screen and a faint violet glow high up.
 */
@Composable
private fun DuskBackdrop(modifier: Modifier = Modifier) {
    val settings = LocalAppSettings.current
    val phase = backdropPhase(28000)
    val strength = effectiveStrength(settings)
    Box(modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1E1B4B), Color(0xFF7C2D12), Color(0xFFEA580C))
                )
            )
        )
        Canvas(Modifier.fillMaxSize()) {
            val a = (phase?.value ?: 0f) * 2f * PI.toFloat()
            softBlob(
                Color(0xFFF97316),
                size.width * (0.5f + 0.05f * cos(a)),
                size.height * 0.78f,
                size.width * 0.7f,
                strength * 0.8f
            )
            softBlob(
                Color(0xFFA855F7),
                size.width * (0.5f + 0.05f * sin(a)),
                size.height * 0.25f,
                size.width * 0.6f,
                strength * 0.5f
            )
        }
    }
}

private data class BokehCircle(
    val x: Float,
    val y: Float,
    val radius: Float,
    val base: Float,
    val speed: Float,
    val offset: Float,
    val color: Color
)

/**
 * Bokeh (0.18.7): twelve slow soft circles drifting over a dark
 * base, like out-of-focus lights. Positions and tints are seeded,
 * so a paused scene is steady, not reshuffled.
 */
@Composable
private fun BokehBackdrop(modifier: Modifier = Modifier) {
    val settings = LocalAppSettings.current
    val phase = backdropPhase(20000)
    val scale = strengthScale(settings)
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val circles = remember {
        val random = Random(11)
        val tints = listOf(
            Color.White, Color(0xFFFDE68A), Color(0xFFBFDBFE), Color(0xFFFBCFE8)
        )
        List(12) {
            BokehCircle(
                x = random.nextFloat(),
                y = random.nextFloat(),
                radius = 0.05f + random.nextFloat() * 0.11f,
                base = 0.10f + random.nextFloat() * 0.25f,
                speed = 0.5f + random.nextFloat(),
                offset = random.nextFloat() * 2f * PI.toFloat(),
                color = tints[random.nextInt(tints.size)]
            )
        }
    }
    Box(
        modifier = modifier.fillMaxSize()
            .graphicsLayer { alpha = if (isLight) 0.6f else 1f }
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF111827), Color.Black))
            )
        )
        Canvas(Modifier.fillMaxSize()) {
            val a = (phase?.value ?: 0f) * 2f * PI.toFloat()
            for (circle in circles) {
                softBlob(
                    circle.color,
                    size.width * (circle.x + 0.02f * sin(a * circle.speed + circle.offset)),
                    size.height * (circle.y + 0.02f * cos(a * circle.speed + circle.offset)),
                    size.width * circle.radius,
                    circle.base * scale
                )
            }
        }
    }
}
