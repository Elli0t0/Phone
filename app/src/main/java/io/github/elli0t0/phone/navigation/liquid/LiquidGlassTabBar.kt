package io.github.elli0t0.phone.navigation.liquid

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

/**
 * Chromatic aberration on the lens, exactly like the gallery's bar.
 * The old Phone lens bar had it OFF on purpose (green fringe over bright
 * content). If it looks bad on a device, flip this ONE constant to false.
 */
private const val LIQUID_BAR_CHROMATIC = true

/** Scale applied to the tab content that lives in the (invisible) accent layer. */
val LocalLiquidTabScale = staticCompositionLocalOf<() -> Float> { { 1f } }

@Composable
fun RowScope.LiquidTabItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val scale = LocalLiquidTabScale.current
    Column(
        modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .fillMaxHeight()
            .weight(1f)
            .graphicsLayer {
                val s = scale()
                scaleX = s
                scaleY = s
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

/**
 * Liquid-glass floating tab bar (port of the gallery's FloatingBottomBar).
 *
 * Three layers, all in one Box:
 *  1. the visible glass bar + the real tab content,
 *  2. an INVISIBLE copy of the tab content tinted with [accentColor], recorded
 *     into its own layerBackdrop (this is the trick: the droplet lens refracts
 *     page + these accent-colored icons, so icons look magnified inside the glass),
 *  3. the draggable droplet (damped spring, squash/stretch by velocity,
 *     grows to 78/56 while pressed, interactive AGSL highlight).
 *
 * The caller decides the size: pass Modifier.width(..) and a fixed/animated
 * height (e.g. via a layout modifier). All inner layers use fillMaxSize/Height.
 * Requires API 33+ (RuntimeShader + backdrop lens); only compose it behind
 * the existing lens gate.
 */
@Composable
fun LiquidGlassTabBar(
    selectedIndex: Int,
    onSelected: (index: Int) -> Unit,
    backdrop: Backdrop,
    tabsCount: Int,
    isDark: Boolean,
    accentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
    blurScale: Float = 1f,
    // false when Animations = Off/Reduced: no lift, no stretch, droplet snaps.
    motionEnabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val tabsBackdrop = rememberLayerBackdrop()
    val density = LocalDensity.current
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    val animationScope = rememberCoroutineScope()

    var tabWidthPx by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(0f) }

    // Whole bar leans up to 4dp toward the finger while dragging (rubber band).
    val offsetAnimation = remember { Animatable(0f) }
    val panelOffset by remember(density) {
        derivedStateOf {
            if (totalWidthPx == 0f) 0f else {
                val fraction = (offsetAnimation.value / totalWidthPx).fastCoerceIn(-1f, 1f)
                with(density) {
                    4f.dp.toPx() * fraction.sign * EaseOut.transform(abs(fraction))
                }
            }
        }
    }

    var currentIndex by remember { mutableIntStateOf(selectedIndex) }
    val currentOnSelected by rememberUpdatedState(onSelected)

    class DampedDragAnimationHolder {
        var instance: DampedDragAnimation? = null
    }

    val holder = remember { DampedDragAnimationHolder() }

    val dampedDragAnimation = remember(animationScope, tabsCount, density, isLtr, motionEnabled) {
        DampedDragAnimation(
            animationScope = animationScope,
            initialValue = selectedIndex.toFloat(),
            valueRange = 0f..(tabsCount - 1).toFloat(),
            visibilityThreshold = 0.001f,
            initialScale = 1f,
            pressedScale = if (motionEnabled) 78f / 56f else 1f,
            canDrag = { offset ->
                val anim = holder.instance ?: return@DampedDragAnimation true
                if (tabWidthPx == 0f) return@DampedDragAnimation false

                val currentValue = anim.value
                val indicatorX = currentValue * tabWidthPx
                val padding = with(density) { 4.dp.toPx() }
                val globalTouchX = if (isLtr) {
                    val touchX = indicatorX + offset.x
                    padding + touchX
                } else {
                    val touchX = totalWidthPx - padding - tabWidthPx - indicatorX + offset.x
                    touchX
                }
                globalTouchX in 0f..totalWidthPx
            },
            onDragStarted = {},
            onDragStopped = {
                // Int.fastCoerceIn does not exist in Compose UI 1.7
                // (added later); stdlib coerceIn is identical here.
                val targetIndex = targetValue.fastRoundToInt().coerceIn(0, tabsCount - 1)
                currentOnSelected(targetIndex)
                currentIndex = targetIndex
                animateToValue(targetIndex.toFloat())
                animationScope.launch {
                    offsetAnimation.animateTo(0f, spring(1f, 300f, 0.5f))
                }
            },
            onDrag = { _, dragAmount ->
                if (tabWidthPx > 0) {
                    updateValue(
                        (targetValue + dragAmount.x / tabWidthPx * if (isLtr) 1f else -1f)
                            .fastCoerceIn(0f, (tabsCount - 1).toFloat())
                    )
                    animationScope.launch {
                        offsetAnimation.snapTo(offsetAnimation.value + dragAmount.x)
                    }
                }
            }
        ).also { holder.instance = it }
    }

    // External selection changes (taps, back navigation) update currentIndex.
    LaunchedEffect(selectedIndex) {
        currentIndex = selectedIndex
    }

    LaunchedEffect(dampedDragAnimation) {
        snapshotFlow { currentIndex }.drop(1).collectLatest { index ->
            if (motionEnabled) {
                dampedDragAnimation.animateToValue(index.toFloat())
            } else {
                dampedDragAnimation.snapToValue(index.toFloat())
            }
        }
    }

    val interactiveHighlight = remember(animationScope, tabWidthPx) {
        InteractiveHighlight(
            animationScope = animationScope,
            position = { size, _ ->
                Offset(
                    if (isLtr) (dampedDragAnimation.value + 0.5f) * tabWidthPx + panelOffset
                    else size.width - (dampedDragAnimation.value + 0.5f) * tabWidthPx + panelOffset,
                    size.height / 2f
                )
            }
        )
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.CenterStart
    ) {
        // ---- Layer 1: visible glass bar + real tab content ----
        Row(
            Modifier
                .onGloballyPositioned { coords ->
                    totalWidthPx = coords.size.width.toFloat()
                    val contentWidthPx = totalWidthPx - with(density) { 8.dp.toPx() }
                    tabWidthPx = contentWidthPx / tabsCount
                }
                .graphicsLayer { translationX = panelOffset }
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { CircleShape },
                    effects = {
                        vibrancy()
                        blur(4.5f.dp.toPx() * blurScale)
                        lens(
                            24f.dp.toPx(),
                            24f.dp.toPx(),
                            depthEffect = true,
                            chromaticAberration = LIQUID_BAR_CHROMATIC
                        )
                    },
                    highlight = { Highlight.Default.copy(alpha = 1f) },
                    shadow = { null },
                    layerBlock = {
                        // Whole bar grows ~16dp wider while the droplet is pressed.
                        val progress = dampedDragAnimation.pressProgress
                        val scale = lerp(1f, 1f + 16f.dp.toPx() / size.width, progress)
                        scaleX = scale
                        scaleY = scale
                    },
                    onDrawSurface = { drawRect(containerColor) }
                )
                .then(if (motionEnabled) interactiveHighlight.modifier else Modifier)
                .fillMaxSize()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )

        // ---- Layer 2: invisible accent-tinted copy, recorded as tabsBackdrop ----
        CompositionLocalProvider(
            LocalLiquidTabScale provides {
                if (motionEnabled) lerp(1f, 1.2f, dampedDragAnimation.pressProgress) else 1f
            }
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
                    .clearAndSetSemantics {}
                    .alpha(0f)
                    .layerBackdrop(tabsBackdrop)
                    .graphicsLayer { translationX = panelOffset }
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { CircleShape },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            vibrancy()
                            blur(8f.dp.toPx())
                            lens(
                                24f.dp.toPx() * progress,
                                24f.dp.toPx() * progress,
                                depthEffect = true,
                                chromaticAberration = LIQUID_BAR_CHROMATIC
                            )
                        },
                        highlight = {
                            Highlight.Default.copy(alpha = dampedDragAnimation.pressProgress)
                        },
                        onDrawSurface = { drawRect(containerColor) }
                    )
                    .then(if (motionEnabled) interactiveHighlight.modifier else Modifier)
                    .padding(horizontal = 4.dp)
                    // The parameter form of graphicsLayer has no
                    // colorFilter in Compose UI 1.7 (added in 1.8);
                    // the lambda form sets the same property.
                    .graphicsLayer { colorFilter = ColorFilter.tint(accentColor) },
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }

        // ---- Layer 3: the draggable droplet ----
        if (tabWidthPx > 0f) {
            Box(
                Modifier
                    .padding(4.dp)
                    .graphicsLayer {
                        val singleTabWidth = tabWidthPx
                        val progressOffset = dampedDragAnimation.value * singleTabWidth
                        translationX = if (isLtr) {
                            progressOffset + panelOffset
                        } else {
                            -progressOffset + panelOffset
                        }
                    }
                    .then(if (motionEnabled) interactiveHighlight.gestureModifier else Modifier)
                    .then(if (motionEnabled) dampedDragAnimation.modifier else Modifier)
                    .drawBackdrop(
                        backdrop = rememberCombinedBackdrop(backdrop, tabsBackdrop),
                        shape = { CircleShape },
                        effects = {
                            val progress = dampedDragAnimation.pressProgress
                            lens(10f.dp.toPx() * progress, 14f.dp.toPx() * progress, true)
                        },
                        highlight = {
                            Highlight.Default.copy(alpha = dampedDragAnimation.pressProgress)
                        },
                        shadow = { Shadow(alpha = dampedDragAnimation.pressProgress) },
                        innerShadow = {
                            InnerShadow(
                                radius = 8f.dp * dampedDragAnimation.pressProgress,
                                alpha = dampedDragAnimation.pressProgress
                            )
                        },
                        layerBlock = {
                            scaleX = dampedDragAnimation.scaleX
                            scaleY = dampedDragAnimation.scaleY
                            if (motionEnabled) {
                                // Squash & stretch with drag velocity.
                                val velocity = dampedDragAnimation.velocity / 10f
                                scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                                scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                            }
                        },
                        onDrawSurface = {
                            val progress = dampedDragAnimation.pressProgress
                            // Resting droplet = soft neutral pill; pressed = clear glass.
                            drawRect(
                                color = if (isDark) Color.White.copy(0.1f) else Color.Black.copy(0.1f),
                                alpha = 1f - progress
                            )
                            drawRect(Color.Black.copy(alpha = 0.03f * progress))
                        }
                    )
                    .fillMaxHeight()
                    .width(with(density) { tabWidthPx.toDp() })
            )
        }
    }
}
