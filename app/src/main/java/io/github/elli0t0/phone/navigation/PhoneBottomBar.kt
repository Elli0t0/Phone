package io.github.elli0t0.phone.navigation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.lerp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import io.github.elli0t0.phone.core.design.BottomBarDarkSelected
import io.github.elli0t0.phone.core.design.BottomBarDarkSelectedContent
import io.github.elli0t0.phone.core.design.BottomBarDarkUnselected
import io.github.elli0t0.phone.core.design.BottomBarLightSelected
import io.github.elli0t0.phone.core.design.BottomBarLightSelectedContent
import io.github.elli0t0.phone.core.design.BottomBarLightUnselected
import io.github.elli0t0.phone.core.design.GlassSurface
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.Motion
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.pressScale
import io.github.elli0t0.phone.core.design.rememberLightHaptic
import io.github.elli0t0.phone.data.settings.model.ThemeMode
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Floating glass bottom bar, overlaid on content. 60% of the screen
 * width, 60dp tall, three equal-width items. One shared selection
 * pill is drawn behind the items: it slides to the selected item with
 * a Bouncy spring and stretches ~14dp mid-flight (a sine bulge on its
 * width), so the pill feels liquid. The pill progress is read only
 * inside layout/draw lambdas, so the flight never recomposes.
 * The icon cross-fades outline to filled (Short) with a scale pop
 * 1 to 1.15 to 1 (Bouncy) on selection; the label cross-fades between
 * Normal and SemiBold with its color (Short). The bar is real glass:
 * content scrolls visibly behind it. Labels show unless the setting
 * turns them off. The bar floats above the gesture handle:
 * navigation-bar inset + 12dp gap.
 */
@Composable
fun PhoneBottomBar(
    navController: NavController,
    currentRoute: String?,
    showLabels: Boolean = true,
    modifier: Modifier = Modifier
) {
    val darkTheme = when (LocalAppSettings.current.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val selectedPillColor = if (darkTheme) BottomBarDarkSelected else BottomBarLightSelected
    val selectedContentColor = if (darkTheme) BottomBarDarkSelectedContent else BottomBarLightSelectedContent
    val unselectedContentColor = if (darkTheme) BottomBarDarkUnselected else BottomBarLightUnselected
    val pillShape = RoundedCornerShape(PhoneDimens.bottomBarCornerPercent)
    val lightHaptic = rememberLightHaptic()
    val animationOption = LocalAppSettings.current.animationOption
    val selectedIndex = Screen.bottomNavItems
        .indexOfFirst { it.route == currentRoute }
        .coerceAtLeast(0)

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
                    AnimationOption.ON -> Motion.bouncy()
                }
            )
        }
    }

    // Width 60% of the screen. Height fixed at 60dp; the three items
    // share the width equally via weight, so nothing can overflow
    // or clip.
    BoxWithConstraints(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = PhoneDimens.bottomBarBottomMargin)
            .testTag("phone_bottom_bar"),
        contentAlignment = Alignment.Center
    ) {
        val barWidth = maxWidth * PhoneDimens.bottomBarWidthFraction
        val density = LocalDensity.current
        val itemWidthPx = with(density) {
            (barWidth / Screen.bottomNavItems.size).toPx()
        }
        val pillWidth = barWidth / Screen.bottomNavItems.size -
            PhoneDimens.bottomBarItemInset * 2
        val pillWidthPx = with(density) { pillWidth.toPx() }
        val stretchPx = with(density) { PhoneDimens.bottomBarPillStretch.toPx() }
        val insetPx = with(density) { PhoneDimens.bottomBarItemInset.toPx() }
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

        GlassSurface(
            shape = pillShape,
            modifier = Modifier
                .width(barWidth)
                .height(PhoneDimens.bottomBarHeight)
        ) {
            // The one shared pill, behind the items.
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset {
                        val p = pillProgress.value.coerceIn(0f, 1f)
                        val fromX = pillFromIndex * itemWidthPx + insetPx
                        val toX = pillToIndex * itemWidthPx + insetPx
                        // Sine bulge: 0 at both ends, max mid-flight.
                        // The offset shifts left by half the bulge so
                        // the pill stays centered on its path.
                        val bulge = if (animationOption == AnimationOption.ON) {
                            sin(p * PI).toFloat() * stretchPx
                        } else {
                            0f
                        }
                        IntOffset((lerp(fromX, toX, p) - bulge / 2f).roundToInt(), 0)
                    }
                    .graphicsLayer {
                        val p = pillProgress.value.coerceIn(0f, 1f)
                        val bulge = if (animationOption == AnimationOption.ON) {
                            sin(p * PI).toFloat() * stretchPx
                        } else {
                            0f
                        }
                        scaleX = 1f + bulge / pillWidthPx
                    }
                    .width(pillWidth)
                    .fillMaxHeight()
                    .padding(vertical = PhoneDimens.bottomBarItemInset)
                    .clip(pillShape)
                    .background(selectedPillColor)
            )
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Screen.bottomNavItems.forEach { screen ->
                    val selected = currentRoute == screen.route
                    // Icon pop: 1 to 1.15 to 1 over 300ms when the tab
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
                                    1.15f at 120
                                    1f at 300
                                }
                            )
                        }
                    }
                    // Shared press interaction source: pressScale and
                    // the clickable observe the same press state, so
                    // the item scales to 0.92 while pressed, ripples,
                    // and haptics.
                    val itemSource = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(PhoneDimens.bottomBarItemInset)
                            .clip(pillShape)
                            .pressScale(itemSource)
                            .clickable(
                                interactionSource = itemSource,
                                indication = LocalIndication.current
                            ) {
                                if (currentRoute != screen.route) {
                                    lightHaptic()
                                    // saveState/restoreState: switching tabs keeps
                                    // each tab's saved state (including list
                                    // scroll position) instead of recreating it.
                                    // The popped tabs stay popped — only their
                                    // saved state is restored on return, so no
                                    // popped destination (e.g. Settings) comes
                                    // back.
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
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
                                }
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
                                        modifier = Modifier.size(PhoneDimens.bottomBarIconSize)
                                    )
                                }
                            }
                            if (showLabels) {
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
                                        fontSize = PhoneDimens.bottomBarLabelSize,
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
    }
}
