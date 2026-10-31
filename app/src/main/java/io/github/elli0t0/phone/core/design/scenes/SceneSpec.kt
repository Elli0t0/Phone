package io.github.elli0t0.phone.core.design.scenes

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.luminance
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.data.settings.model.AppBackground

/**
 * One anime-style scene (0.18.9): the AppBackground entry it
 * selects, plus its renderer. Every scene takes the same three
 * arguments — intensity (0..1, already light-capped by the
 * caller), animate (false freezes it at a fixed phase), and a
 * modifier — so the registry below is the only wiring a new
 * scene needs: add its AppBackground entry, its file, and one
 * line here. No bitmaps, no copyrighted characters: everything
 * is drawn procedurally with Canvas primitives.
 */
data class SceneSpec(
    val background: AppBackground,
    val content: @Composable (intensity: Float, animate: Boolean, Modifier) -> Unit
)

/** Blue-leaning animated scenes first, then the plain blue gradients. */
val AnimeScenes: List<SceneSpec> = listOf(
    SceneSpec(AppBackground.BLUE_SKY) { intensity, animate, modifier ->
        BlueSkyScene(intensity, animate, modifier)
    },
    SceneSpec(AppBackground.TWILIGHT) { intensity, animate, modifier ->
        TwilightScene(intensity, animate, modifier)
    },
    SceneSpec(AppBackground.STARRY_NIGHT) { intensity, animate, modifier ->
        StarryNightScene(intensity, animate, modifier)
    },
    SceneSpec(AppBackground.SAKURA_NIGHT) { intensity, animate, modifier ->
        SakuraNightScene(intensity, animate, modifier)
    },
    SceneSpec(AppBackground.RAIN_CITY) { intensity, animate, modifier ->
        RainCityScene(intensity, animate, modifier)
    },
    SceneSpec(AppBackground.OCEAN_SUNSET) { intensity, animate, modifier ->
        OceanSunsetScene(intensity, animate, modifier)
    },
    SceneSpec(AppBackground.NEON_CITY) { intensity, animate, modifier ->
        NeonCityScene(intensity, animate, modifier)
    },
    SceneSpec(AppBackground.COBALT) { intensity, animate, modifier ->
        CobaltScene(intensity, animate, modifier)
    },
    SceneSpec(AppBackground.ICE) { intensity, animate, modifier ->
        IceScene(intensity, animate, modifier)
    },
    SceneSpec(AppBackground.DEEP_SEA) { intensity, animate, modifier ->
        DeepSeaScene(intensity, animate, modifier)
    }
)

/** AppBackground entries owned by the anime registry (the picker excludes them from the classic grid). */
val AnimeSceneBackgrounds: Set<AppBackground> = AnimeScenes.map { it.background }.toSet()

/**
 * Visible motion caps at ~30fps: the 0..1 cycle phase is
 * quantized to one step per ~33ms of its duration, so the drawn
 * frame changes at most 30 times a second no matter the
 * display rate. All element lists are remembered, so a frame
 * costs math plus draw calls, no allocations.
 */
fun quantizePhase(p: Float, durationMillis: Int): Float {
    val steps = durationMillis / 1000f * 30f
    return (p * steps).toInt() / steps
}

/** One soft radial-gradient circle, the scenes' shared glow primitive. */
fun DrawScope.softCircle(color: Color, center: Offset, radius: Float, alpha: Float) {
    drawCircle(
        Brush.radialGradient(
            listOf(color.copy(alpha = alpha), Color.Transparent),
            center,
            radius
        ),
        radius = radius,
        center = center
    )
}

/** A seeded skyline: buildings covering the width, each with a left edge, width and height fraction. */
data class SkylineBuilding(val x: Float, val w: Float, val h: Float)

fun buildSkyline(
    seed: Int,
    minW: Float,
    maxW: Float,
    minH: Float,
    maxH: Float
): List<SkylineBuilding> {
    val random = kotlin.random.Random(seed)
    val out = mutableListOf<SkylineBuilding>()
    var x = 0f
    while (x < 0.98f) {
        var w = minW + random.nextFloat() * (maxW - minW)
        if (x + w > 1f) w = 1f - x
        if (w < 0.02f) break
        out.add(SkylineBuilding(x, w, minH + random.nextFloat() * (maxH - minH)))
        x += w + 0.01f
    }
    return out
}

/** Picker-preview intensity: the current slider value, light-capped like the live renderer. */
@Composable
fun scenePreviewIntensity(): Float {
    val settings = LocalAppSettings.current
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    return if (isLight) settings.appBackgroundIntensity.coerceAtMost(0.6f) else settings.appBackgroundIntensity
}
