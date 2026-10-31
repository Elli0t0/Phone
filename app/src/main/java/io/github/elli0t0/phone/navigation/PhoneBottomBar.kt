package io.github.elli0t0.phone.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.coroutines.launch
import com.kyant.backdrop.backdrops.LayerBackdrop
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import io.github.elli0t0.phone.core.design.BottomBarDark
import io.github.elli0t0.phone.core.design.BottomBarDarkSelectedContent
import io.github.elli0t0.phone.core.design.BottomBarDarkUnselected
import io.github.elli0t0.phone.core.design.BottomBarLight
import io.github.elli0t0.phone.core.design.BottomBarLightSelectedContent
import io.github.elli0t0.phone.core.design.BottomBarLightUnselected
import io.github.elli0t0.phone.core.design.GlassSurface
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.Motion
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.bottomBarSizeSpec
import io.github.elli0t0.phone.core.design.glassPress
import io.github.elli0t0.phone.core.design.rememberGlassHaptic
import io.github.elli0t0.phone.data.settings.model.BottomBarStyle
import io.github.elli0t0.phone.data.settings.model.ThemeMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Floating liquid-glass bottom bar, overlaid on content. Stadium pill,
 * 75% of the screen width for Medium (62% Small, 82% Large, Medium
 * capped at 360dp), 64dp tall for Medium, three equal-width items,
 * 26dp icons. The lens bar scales the same way from its 78%
 * reference width (capped at 380dp). The bar glass is a 36dp Haze
 * blur with a 0.20 fill, a 1dp vertical-gradient rim (white 38% at
 * the top edge to 6% at the bottom) and faint highlights at the
 * left/right ends, with no drop shadow; list content scrolls visibly
 * behind it, blurred. One shared droplet indicator is drawn behind
 * the items: it slides to the selected item with a Bouncy spring and
 * stretches ~20dp mid-flight (a sine bulge on its width), so the
 * droplet feels liquid. The droplet is neutral (white 16% in dark,
 * black 8% in light, no primary tint) with a 1dp white border whose
 * alpha rises to ~40% mid-flight and settles at ~22% at rest. The
 * pill progress is read only inside layout/draw lambdas, so the
 * flight never recomposes. The icon cross-fades outline to filled
 * (Short) with a scale pop 1 to 1.15 to 1 (Bouncy) on selection;
 * the label cross-fades between Normal and SemiBold with its color
 * (Short). Selected icons are filled pure white (dark) / near-black
 * (light); unselected are outline white 80% (dark) / black 60%
 * (light). Labels show unless the setting turns them off. The bar
 * floats above the gesture handle: navigation-bar inset + 12dp gap.
 * Style (Glass/Solid) and size (S/M/L) come from Settings;
 * shrinkProgress compacts the bar to icons only as it goes 0 to 1.
 * Drag: one pointerInput on the item Row owns all touches (the items
 * have no clickable; accessibility goes through semantics Role.Tab).
 * While dragging, the droplet follows the finger with lift (12%) and
 * speed stretch; on release it springs to the nearest tab and that
 * tab is selected. A tap selects the tab under the finger. All
 * animated values are read only inside layout/draw/graphicsLayer
 * lambdas. With Animations Off/Reduced there is no lift or stretch:
 * the droplet just snaps to the tab.
 */
@Composable
fun PhoneBottomBar(
    navController: NavController,
    currentRoute: String?,
    showLabels: Boolean = true,
    modifier: Modifier = Modifier,
    // 0 = full height with labels, 1 = compact icons-only. Read only
    // in layout/draw lambdas, so shrinking never recomposes.
    shrinkProgress: () -> Float = { 0f },
    // Non-null only when the Lens bar gate passes (API 33+, Lens bar
    // switch on, Reduce transparency off, Glass style on): the bar
    // then samples this backdrop instead of using Haze.
    lensBackdrop: LayerBackdrop? = null
) {
    val darkTheme = when (LocalAppSettings.current.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val selectedContentColor = if (darkTheme) BottomBarDarkSelectedContent else BottomBarLightSelectedContent
    val unselectedContentColor = if (darkTheme) BottomBarDarkUnselected else BottomBarLightUnselected
    val barColor = if (darkTheme) BottomBarDark else BottomBarLight
    val sizeSpec = bottomBarSizeSpec(LocalAppSettings.current.bottomBarSize)
    val pillShape = RoundedCornerShape(PhoneDimens.bottomBarCornerPercent)
    val lightHaptic = rememberGlassHaptic()
    val animationOption = LocalAppSettings.current.animationOption
    val glassBottomBar = LocalAppSettings.current.glassBottomBar
    val selectedIndex = Screen.bottomNavItems
        .indexOfFirst { it.route == currentRoute }
        .coerceAtLeast(0)

    // Navigation shared by taps (semantics), drag release, and the
    // Row gesture: saveState/restoreState keeps each tab's saved
    // state (including list scroll position) instead of recreating
    // it. The popped tabs stay popped — only their saved state is
    // restored on return, so no popped destination (e.g. Settings)
    // comes back.
    // The gesture block below is created once (its keys are only the
    // tab count and layout direction), so anything it captures must
    // stay fresh across compositions: the route and animation option
    // are read through rememberUpdatedState. Reading the plain
    // parameters there froze the first composition's values, and
    // navigation back to that tab was then skipped by the guard —
    // a different tab each time the bar re-entered composition.
    val currentRouteState = rememberUpdatedState(currentRoute)
    val animationOptionState = rememberUpdatedState(animationOption)
    fun navigateTo(screen: Screen) {
        if (currentRouteState.value != screen.route) {
            lightHaptic()
            navController.navigate(screen.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // Drag state. dragX is the droplet center in px (physical, from
    // the bar's left edge); lift is 0 resting, 1 finger down.
    // releaseSpringActive covers the release spring so the
    // external-change snap below cannot cut it short.
    val dragX = remember { Animatable(0f) }
    val lift = remember { Animatable(0f) }
    val dragSpeed = remember { Animatable(0f) }
    var dragging by remember { mutableStateOf(false) }
    var releaseSpringActive by remember { mutableStateOf(false) }
    // The gesture block is a restricted scope: it cannot call
    // Animatable functions or launch directly. All animated work
    // goes through this external scope instead.
    val scope = rememberCoroutineScope()
    // One press source per item, hoisted so the Row gesture can emit
    // press/release into the item under the finger and glassPress
    // keeps working without a clickable.
    val itemSources = remember {
        Screen.bottomNavItems.map { MutableInteractionSource() }
    }
    val n = Screen.bottomNavItems.size
    val layoutDirection = LocalLayoutDirection.current
    val rtl = layoutDirection == LayoutDirection.Rtl
    // Physical tab index under x, and physical center of a logical
    // tab index, for a bar w px wide. RTL mirrors both: logical
    // tab 0 sits at the physical right.
    fun indexAt(x: Float, w: Float): Int {
        val raw = (x / (w / n)).toInt().coerceIn(0, n - 1)
        return if (rtl) n - 1 - raw else raw
    }
    fun centerOf(i: Int, w: Float): Float {
        val idx = if (rtl) n - 1 - i else i
        return (w / n) * idx + w / (2f * n)
    }

    // Liquid pill flight: from the previously selected item to the new
    // one, progress 0 to 1. Reduced animates a plain tween with no
    // stretch; Off snaps.
    var pillFromIndex by remember { mutableStateOf(selectedIndex) }
    var pillToIndex by remember { mutableStateOf(selectedIndex) }
    val pillProgress = remember { Animatable(1f) }
    LaunchedEffect(selectedIndex) {
        if (selectedIndex != pillToIndex) {
            pillFromIndex = pillToIndex
            pillToIndex = selectedIndex
            pillProgress.snapTo(0f)
            pillProgress.animateTo(
                targetValue = 1f,
                animationSpec = when (animationOption) {
                    AnimationOption.OFF -> snap()
                    AnimationOption.REDUCED -> Motion.tweenSpec(
                        Motion.Durations.Medium,
                        Motion.Easings.Emphasized,
                        animationOption
                    )
                    AnimationOption.ON -> spring(dampingRatio = 0.62f, stiffness = 380f)
                }
            )
        }
    }

    // One-shot specular sweep across the bar when the tab changes.
    // Skipped on first composition and when Animations is OFF.
    val sweepProgress = remember { Animatable(0f) }
    var sweepStarted by remember { mutableStateOf(false) }
    LaunchedEffect(selectedIndex) {
        if (!sweepStarted) {
            sweepStarted = true
        } else if (animationOption != AnimationOption.OFF) {
            sweepProgress.snapTo(0f)
            sweepProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = Motion.scaledDurationMs(
                        Motion.Durations.Long,
                        animationOption
                    ),
                    easing = Motion.Easings.Emphasized
                )
            )
        }
    }

    // Width from the size spec (62/75/82% for S/M/L, Medium capped
    // at 360dp). Height from the spec (52/64/68dp). The three items
    // share the width equally via weight, so nothing can overflow
    // or clip.
    BoxWithConstraints(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = PhoneDimens.bottomBarBottomMargin)
            .testTag("phone_bottom_bar"),
        contentAlignment = Alignment.Center
    ) {
        // Lens bar sizing: 78% of the screen for Medium, capped at
        // 380dp, scaled by the size setting like the Haze bar, so
        // Small shortens the whole row, not just the items inside
        // it (Small 64%, Large 85%). The Haze bar keeps the size
        // setting directly.
        val barWidth = if (lensBackdrop != null) {
            val lensFraction =
                0.78f * (sizeSpec.widthFraction / PhoneDimens.bottomBarWidthFraction)
            minOf(maxWidth * lensFraction, 380.dp)
        } else {
            (maxWidth * sizeSpec.widthFraction).let { rawWidth ->
                sizeSpec.maxWidth?.let { cap -> minOf(rawWidth, cap) } ?: rawWidth
            }
        }
        val density = LocalDensity.current
        val barWidthPx = with(density) { barWidth.toPx() }
        val pillWidth = barWidth / Screen.bottomNavItems.size -
            PhoneDimens.bottomBarItemInset * 2
        val pillWidthPx = with(density) { pillWidth.toPx() }
        // Studio droplet stretch scales the bar's stretch (Normal =
        // current behavior, Off removes it, Extra stretches further).
        val stretchPx = with(density) { PhoneDimens.bottomBarPillStretch.toPx() } *
            LocalAppSettings.current.dropletStretch.factor
        // A tab change from outside the bar (back navigation, deep
        // link) snaps the drag position to that tab's center. Skipped
        // while dragging or during the release spring, so it cannot
        // cut the spring short; the droplet follows the flight then.
        LaunchedEffect(selectedIndex) {
            if (!dragging && !releaseSpringActive) {
                dragX.snapTo(centerOf(selectedIndex, barWidthPx))
            }
        }
        // Crossfade spec: a plain tween, so it is a FiniteAnimationSpec
        // (Motion.tweenSpec returns AnimationSpec, which Crossfade
        // rejects). Duration is scaled by the Animations setting.
        val iconSpec: FiniteAnimationSpec<Float> = tween(
            durationMillis = Motion.scaledDurationMs(
                Motion.Durations.Short,
                animationOption
            ),
            easing = Motion.Easings.Standard
        )

        // Bar height from the size spec: Medium is 64dp, so the
        // lens bar keeps its height there and shrinks/grows with
        // the setting like the Haze bar. The progress is read only
        // in this layout lambda, so shrinking never recomposes.
        val barHeight = sizeSpec.height
        val barHeightModifier = Modifier.layout { measurable, constraints ->
            val shrink = shrinkProgress().coerceIn(0f, 1f)
            val h = with(this) {
                (barHeight - PhoneDimens.bottomBarShrinkDelta * shrink).roundToPx()
            }
            val placeable = measurable.measure(
                constraints.copy(minHeight = h, maxHeight = h)
            )
            layout(placeable.width, h) { placeable.place(0, 0) }
        }
        val barContent: @Composable BoxScope.() -> Unit = {
            // Accent glow color, read here in composable scope: a
            // drawBehind block cannot read MaterialTheme.
            val accentGlow = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            // The one shared droplet, behind the items. Lens mode
            // draws only the lens droplet; the Haze path layers a
            // soft accent glow (radial gradient, scaled up, no real
            // blur) behind a gradient-fill lens with a crescent top
            // highlight and a 1dp gradient rim, all inside the pill
            // clip.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset {
                        val p = pillProgress.value.coerceIn(0f, 1f)
                        // Sine bulge: 0 at both ends, max mid-flight.
                        val bulge = if (animationOption == AnimationOption.ON) {
                            sin(p * PI).toFloat() * stretchPx
                        } else {
                            0f
                        }
                        // Desired left edge, physical px from the
                        // bar's left edge. While dragging, or while
                        // the release spring is still running, the
                        // droplet follows the finger; otherwise it
                        // follows the flight between tab centers.
                        // Centers are physical via centerOf, so the
                        // flight path is identical to the old lerp
                        // in LTR and correct in RTL.
                        val targetLeft = if (dragging || dragX.isRunning || releaseSpringActive) {
                            (dragX.value - pillWidthPx / 2f).coerceIn(0f, barWidthPx - pillWidthPx)
                        } else {
                            lerp(
                                centerOf(pillFromIndex, barWidthPx),
                                centerOf(pillToIndex, barWidthPx),
                                p
                            ) - pillWidthPx / 2f - bulge / 2f
                        }
                        // The droplet is aligned to CenterStart: the
                        // bar's left edge in LTR, its right edge in
                        // RTL. Convert the desired left edge to an
                        // offset from that placement.
                        val baseLeft = if (rtl) barWidthPx - pillWidthPx else 0f
                        IntOffset((targetLeft - baseLeft).roundToInt(), 0)
                    }
                    .graphicsLayer {
                        val p = pillProgress.value.coerceIn(0f, 1f)
                        val bulge = if (animationOption == AnimationOption.ON) {
                            sin(p * PI).toFloat() * stretchPx
                        } else {
                            0f
                        }
                        // Speed stretch: low-passed drag speed, capped
                        // at 25%. Lift magnifies the droplet 12%.
                        // Both are 0 when Animations is Off/Reduced.
                        val speedStretch = if (animationOption == AnimationOption.ON) {
                            minOf(abs(dragSpeed.value) / 4000f, 0.25f)
                        } else {
                            0f
                        }
                        val liftScale = 1f + 0.12f * lift.value
                        scaleX = (1f + bulge / pillWidthPx) * liftScale * (1f + speedStretch)
                        scaleY = liftScale
                    }
                    .width(pillWidth)
                    .fillMaxHeight()
                    .padding(vertical = PhoneDimens.bottomBarItemInset)
                    // Lens only: contain the droplet's effect strictly
                    // to its pill. The surface clips itself, but an
                    // effect overshoot (refraction spread past the
                    // surface box) would otherwise paint past the
                    // pill and read as a ghost behind nearby tabs.
                    // The Haze branch is untouched: its glow is meant
                    // to extend past the pill and fade.
                    .then(
                        if (lensBackdrop != null) Modifier.clip(pillShape) else Modifier
                    )
            ) {
                if (lensBackdrop != null) {
                    // Lens mode draws only the lens droplet: no Haze
                    // droplet, no accent glow behind it.
                    LensDropletSurface(
                        backdrop = lensBackdrop,
                        pillShape = pillShape,
                        liftValue = { lift.value },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Soft accent glow behind the lens.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = 1.35f
                                scaleY = 1.35f
                            }
                            .drawBehind {
                                drawRect(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            accentGlow,
                                            Color.Transparent
                                        ),
                                        center = Offset(size.width / 2f, size.height / 2f),
                                        radius = minOf(size.width, size.height) / 2f
                                    )
                                )
                            }
                    )
                    HazeDropletLens(pillShape)
                }
            }
            Row(
                modifier = Modifier.fillMaxSize()
                    .pointerInput(n, layoutDirection) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val w = size.width.toFloat()
                            if (w <= 0f) return@awaitEachGesture
                            dragging = true
                            releaseSpringActive = false
                            scope.launch {
                                dragX.stop()
                                lift.stop()
                                dragSpeed.stop()
                                dragSpeed.snapTo(0f)
                                dragX.snapTo(down.position.x.coerceIn(0f, w))
                            }
                            var press: PressInteraction.Press? = null
                            var last = indexAt(down.position.x, w)
                            try {
                                press = PressInteraction.Press(down.position)
                                itemSources[last].tryEmit(press)
                                if (animationOptionState.value == AnimationOption.ON) {
                                    scope.launch {
                                        lift.animateTo(
                                            1f,
                                            spring(dampingRatio = 0.6f, stiffness = 400f)
                                        )
                                    }
                                }
                                var lastX = down.position.x
                                var lastT = down.uptimeMillis
                                var smoothSpeed = 0f
                                while (true) {
                                    val ev = awaitPointerEvent()
                                    val c = ev.changes.first()
                                    val x = c.position.x.coerceIn(0f, w)
                                    scope.launch { dragX.snapTo(x) }
                                    if (animationOptionState.value == AnimationOption.ON) {
                                        val dtMs = (c.uptimeMillis - lastT).coerceAtLeast(1L)
                                        val rate = abs(x - lastX) / (dtMs / 1000f)
                                        smoothSpeed += (rate - smoothSpeed) * 0.25f
                                        scope.launch { dragSpeed.snapTo(smoothSpeed) }
                                    }
                                    lastX = x
                                    lastT = c.uptimeMillis
                                    val i = indexAt(x, w)
                                    if (i != last) {
                                        press?.let {
                                            itemSources[last].tryEmit(PressInteraction.Release(it))
                                        }
                                        last = i
                                        lightHaptic()
                                        press = PressInteraction.Press(c.position)
                                        itemSources[last].tryEmit(press)
                                    }
                                    c.consume()
                                    if (!c.pressed) break
                                }
                                // Release: lift down, spring to the
                                // nearest tab's center, select it. The
                                // flight is suppressed for this change
                                // (from/to preset, progress snapped to
                                // 1) so the release spring owns the
                                // motion with no jump when it lands.
                                // Off/Reduced: no lift or spring, the
                                // droplet just snaps to the tab.
                                press?.let {
                                    itemSources[last].tryEmit(PressInteraction.Release(it))
                                }
                                press = null
                                dragging = false
                                scope.launch {
                                    dragSpeed.snapTo(0f)
                                    pillProgress.stop()
                                    pillProgress.snapTo(1f)
                                }
                                pillFromIndex = pillToIndex
                                pillToIndex = last
                                if (animationOptionState.value == AnimationOption.ON) {
                                    releaseSpringActive = true
                                    scope.launch {
                                        lift.animateTo(
                                            0f,
                                            // MediumBouncy is 0.5f; the
                                            // named constant does not
                                            // exist in this Compose
                                            // version's animation-core.
                                            spring(dampingRatio = 0.5f, stiffness = 300f)
                                        )
                                    }
                                    scope.launch {
                                        dragX.animateTo(
                                            centerOf(last, w),
                                            spring(dampingRatio = 0.62f, stiffness = 380f)
                                        )
                                        releaseSpringActive = false
                                    }
                                } else {
                                    scope.launch { dragX.snapTo(centerOf(last, w)) }
                                }
                                navigateTo(Screen.bottomNavItems[last])
                            } finally {
                                // Cancelled mid-drag: release the press
                                // and clear the flags so nothing sticks.
                                press?.let {
                                    itemSources[last].tryEmit(PressInteraction.Release(it))
                                }
                                if (dragging) {
                                    dragging = false
                                    releaseSpringActive = false
                                }
                            }
                        }
                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Screen.bottomNavItems.forEachIndexed { index, screen ->
                    val selected = currentRoute == screen.route
                    // Captured under a different name: inside the
                    // semantics block below, `selected` would resolve
                    // to the receiver's own property, not this value.
                    val tabSelected = selected
                    // Icon pop: 1 to 1.18 to 1 over 300ms when the tab
                    // is newly selected. Skipped on first composition
                    // and when motion is Reduced/Off.
                    val iconScale = remember { Animatable(1f) }
                    val firstFrame = remember { mutableStateOf(true) }
                    LaunchedEffect(selected) {
                        if (firstFrame.value) {
                            firstFrame.value = false
                        } else if (selected && animationOption == AnimationOption.ON) {
                            iconScale.animateTo(
                                targetValue = 1f,
                                animationSpec = keyframes {
                                    durationMillis = 300
                                    1.18f at 120
                                    1f at 300
                                }
                            )
                        }
                    }
                    // Selected icon lifts 1.5dp: animated when motion
                    // is ON, instant when Reduced/Off.
                    val iconLift by animateDpAsState(
                        targetValue = if (selected) -1.5.dp else 0.dp,
                        animationSpec = if (animationOption == AnimationOption.ON) {
                            spring(dampingRatio = 0.7f, stiffness = 400f)
                        } else {
                            snap()
                        },
                        label = "tabIconLift"
                    )
                    // Press source shared with the Row gesture: the
                    // gesture emits press/release into the item under
                    // the finger, so glassPress still scales + sheens
                    // while pressed. No clickable: the gesture owns
                    // touches, and activation goes through semantics.
                    val itemSource = itemSources[index]
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(PhoneDimens.bottomBarItemInset)
                            .clip(pillShape)
                            .glassPress(itemSource)
                            .semantics {
                                role = Role.Tab
                                // Qualified: the unqualified name would
                                // resolve to the enclosing `selected`
                                // val, not this property.
                                this.selected = tabSelected
                                onClick {
                                    navigateTo(screen)
                                    true
                                }
                            }
                            .testTag("tab_${screen.route}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier.graphicsLayer {
                                    scaleX = iconScale.value
                                    scaleY = iconScale.value
                                }.offset(y = iconLift)
                            ) {
                                Crossfade(
                                    targetState = selected,
                                    animationSpec = iconSpec,
                                    label = "tab_icon"
                                ) { isSelected ->
                                    Icon(
                                        painter = painterResource(
                                            id = if (isSelected) {
                                                screen.iconResSelected
                                            } else {
                                                screen.iconRes
                                            }
                                        ),
                                        contentDescription = stringResource(
                                            id = screen.contentDescriptionRes
                                        ),
                                        tint = if (isSelected) {
                                            selectedContentColor
                                        } else {
                                            unselectedContentColor
                                        },
                                        modifier = Modifier.size(sizeSpec.iconSize)
                                    )
                                }
                            }
                            if (showLabels) {
                                // Labels fade and collapse to zero height
                                // as the bar compacts. Progress is read
                                // only in layout/draw lambdas.
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .graphicsLayer {
                                            alpha = 1f - shrinkProgress().coerceIn(0f, 1f)
                                        }
                                        .layout { measurable, constraints ->
                                            val p = shrinkProgress().coerceIn(0f, 1f)
                                            val maxH = if (constraints.hasBoundedHeight) {
                                                (constraints.maxHeight * (1f - p))
                                                    .roundToInt().coerceAtLeast(0)
                                            } else {
                                                constraints.maxHeight
                                            }
                                            val placeable = measurable.measure(
                                                constraints.copy(maxHeight = maxH)
                                            )
                                            layout(placeable.width, placeable.height) {
                                                placeable.place(0, 0)
                                            }
                                        }
                                ) {
                                    Spacer(
                                        modifier = Modifier.height(
                                            PhoneDimens.bottomBarLabelIconGap
                                        )
                                    )
                                    Crossfade(
                                        targetState = selected,
                                        animationSpec = iconSpec,
                                        label = "tab_label"
                                    ) { isSelected ->
                                        Text(
                                            text = stringResource(id = screen.titleRes),
                                            fontSize = sizeSpec.labelSize,
                                            fontWeight = if (isSelected) {
                                                FontWeight.SemiBold
                                            } else {
                                                FontWeight.Normal
                                            },
                                            color = if (isSelected) {
                                                selectedContentColor
                                            } else {
                                                unselectedContentColor
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // Specular sweep: a white band that crosses the bar once
            // per tab change, fading in and out. Drawn on top of the
            // items; it handles no input, so taps pass through.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(pillShape)
                    .drawBehind {
                        val p = sweepProgress.value.coerceIn(0f, 1f)
                        if (p > 0f && p < 1f) {
                            val bandWidth = size.width * 0.4f
                            val x = lerp(-bandWidth, size.width + bandWidth, p)
                            val alpha = sin(p * PI).toFloat() * 0.28f
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = alpha),
                                        Color.Transparent
                                    ),
                                    startX = x - bandWidth / 2f,
                                    endX = x + bandWidth / 2f
                                )
                            )
                        }
                    }
            )
        }
        // The bar itself lifts slightly with the finger (3%).
        // The lift value is read only in this graphicsLayer lambda.
        val barLiftModifier = Modifier.graphicsLayer {
            val s = 1f + 0.03f * lift.value
            scaleX = s
            scaleY = s
        }
        if (lensBackdrop != null) {
            // Real lens-glass bar: the surface samples the backdrop
            // behind the bar; the droplet and items draw on top of
            // it, never through it.
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .then(barHeightModifier)
                    .then(barLiftModifier)
                    .clip(pillShape)
            ) {
                LensBarSurface(
                    backdrop = lensBackdrop,
                    pillShape = pillShape,
                    blurAmount = LocalAppSettings.current.glassBlurAmount,
                    tintAmount = LocalAppSettings.current.glassTintAmount,
                    modifier = Modifier.fillMaxSize()
                )
                barContent()
            }
        } else if (LocalAppSettings.current.bottomBarStyle == BottomBarStyle.GLASS && glassBottomBar) {
            GlassSurface(
                shape = pillShape,
                modifier = Modifier.width(barWidth).then(barHeightModifier).then(barLiftModifier),
                // Thin fill so the blur and the backdrop show through,
                // 36dp blur, vertical-gradient rim, faint end
                // highlights, and no drop shadow under the glass.
                // Near-opaque fill so list text never shows
                // through the bar: 0.80 dark, 0.85 light.
                containerAlpha = if (darkTheme) 0.80f else 0.85f,
                blurRadius = 36.dp,
                // Studio edge light scales the bar's rim (Soft =
                // current behavior); the top is brighter now.
                rim = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(
                            alpha = (0.45f * LocalAppSettings.current.edgeLight.factor).coerceIn(0f, 1f)
                        ),
                        Color.White.copy(
                            alpha = (0.06f * LocalAppSettings.current.edgeLight.factor).coerceIn(0f, 1f)
                        )
                    )
                ),
                edgeHighlightAlpha = 0.10f,
                edgeRefraction = LocalAppSettings.current.edgeRefraction
            ) {
                // Very light white wash from the top for the thick
                // glass look; under the droplet and the items.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(pillShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                barContent()
            }
        } else {
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .then(barHeightModifier)
                    .then(barLiftModifier)
                    .shadow(
                        PhoneDimens.bottomBarElevation,
                        pillShape,
                        spotColor = Color.Black.copy(alpha = PhoneDimens.bottomBarShadowAlpha),
                        ambientColor = Color.Black.copy(alpha = PhoneDimens.bottomBarShadowAlpha)
                    )
                    .clip(pillShape)
                    .background(barColor)
            ) {
                barContent()
            }
        }
    }
}

/**
 * The Haze droplet lens (gradient fill, gradient rim, crescent top
 * highlight), kept for the Haze bar path.
 */
@Composable
private fun HazeDropletLens(pillShape: RoundedCornerShape) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(pillShape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.30f),
                        Color.White.copy(alpha = 0.10f)
                    )
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.65f),
                        Color.White.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.30f)
                    )
                ),
                pillShape
            )
            .drawBehind {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.35f),
                            Color.Transparent
                        ),
                        endY = size.height * 0.45f
                    ),
                    cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                )
            }
    )
}
