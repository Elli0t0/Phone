package io.github.elli0t0.phone.navigation.liquid

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import io.github.elli0t0.phone.core.design.BottomBarDark
import io.github.elli0t0.phone.core.design.BottomBarDarkSelectedContent
import io.github.elli0t0.phone.core.design.BottomBarDarkUnselected
import io.github.elli0t0.phone.core.design.BottomBarLight
import io.github.elli0t0.phone.core.design.BottomBarLightSelectedContent
import io.github.elli0t0.phone.core.design.BottomBarLightUnselected
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.Motion
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.bottomBarSizeSpec
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import io.github.elli0t0.phone.navigation.Screen
import kotlin.math.roundToInt

/**
 * Lens-path bottom bar of the Phone app, built on [LiquidGlassTabBar].
 * Called from PhoneBottomBar ONLY when lensBackdrop != null (API 33+ gate
 * already passed). The Haze bar and the Minimal bar are untouched.
 *
 * [onNavigate] is PhoneBottomBar's existing local navigateTo(screen)
 * (haptic + navController.navigate with saveState/restoreState).
 */
@Composable
internal fun LiquidPhoneBottomBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
    lensBackdrop: LayerBackdrop,
    darkTheme: Boolean,
    showLabels: Boolean,
    shrinkProgress: () -> Float,
    modifier: Modifier = Modifier
) {
    val settings = LocalAppSettings.current
    val sizeSpec = bottomBarSizeSpec(settings.bottomBarSize)
    val animationOption = settings.animationOption
    val selectedContentColor = if (darkTheme) BottomBarDarkSelectedContent else BottomBarLightSelectedContent
    val unselectedContentColor = if (darkTheme) BottomBarDarkUnselected else BottomBarLightUnselected
    val barColor = if (darkTheme) BottomBarDark else BottomBarLight
    val selectedIndex = Screen.bottomNavItems
        .indexOfFirst { it.route == currentRoute }
        .coerceAtLeast(0)

    BoxWithConstraints(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = PhoneDimens.bottomBarBottomMargin)
            .testTag("phone_bottom_bar"),
        contentAlignment = Alignment.Center
    ) {
        // Same width rule the old lens bar used (78% reference, scaled by size, cap 380dp).
        val lensFraction = 0.78f * (sizeSpec.widthFraction / PhoneDimens.bottomBarWidthFraction)
        val barWidth = minOf(maxWidth * lensFraction, 380.dp)
        val barHeight = sizeSpec.height

        // Height follows the size setting and shrinks with shrinkProgress.
        // Read only inside the layout lambda, so shrinking never recomposes.
        val barHeightModifier = Modifier.layout { measurable, constraints ->
            val shrink = shrinkProgress().coerceIn(0f, 1f)
            val h = (barHeight - PhoneDimens.bottomBarShrinkDelta * shrink).roundToPx()
            val placeable = measurable.measure(constraints.copy(minHeight = h, maxHeight = h))
            layout(placeable.width, h) { placeable.place(0, 0) }
        }

        LiquidGlassTabBar(
            selectedIndex = selectedIndex,
            onSelected = { index -> onNavigate(Screen.bottomNavItems[index]) },
            backdrop = lensBackdrop,
            tabsCount = Screen.bottomNavItems.size,
            isDark = darkTheme,
            accentColor = MaterialTheme.colorScheme.primary,
            containerColor = barColor.copy(alpha = (0.40f * settings.glassTintAmount).coerceIn(0f, 1f)),
            blurScale = settings.glassBlurAmount,
            motionEnabled = animationOption == AnimationOption.ON,
            modifier = Modifier.width(barWidth).then(barHeightModifier)
        ) {
            Screen.bottomNavItems.forEach { screen ->
                val isSelected = currentRoute == screen.route
                val accent = LocalLiquidTabAccent.current
                LiquidTabItem(
                    onClick = { onNavigate(screen) },
                    modifier = Modifier
                        .semantics { this.selected = isSelected }
                        .testTag("tab_${screen.route}")
                ) {
                    Crossfade(
                        targetState = isSelected,
                        animationSpec = tween(
                            durationMillis = Motion.scaledDurationMs(Motion.Durations.Short, animationOption),
                            easing = Motion.Easings.Standard
                        ),
                        label = "tab_icon"
                    ) { sel ->
                        Icon(
                            painter = painterResource(if (sel) screen.iconResSelected else screen.iconRes),
                            contentDescription = stringResource(screen.contentDescriptionRes),
                            tint = accent ?: if (sel) selectedContentColor else unselectedContentColor,
                            modifier = Modifier.size(sizeSpec.iconSize)
                        )
                    }
                    if (showLabels) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .graphicsLayer { alpha = 1f - shrinkProgress().coerceIn(0f, 1f) }
                                .layout { measurable, constraints ->
                                    val p = shrinkProgress().coerceIn(0f, 1f)
                                    val maxH = if (constraints.hasBoundedHeight) {
                                        (constraints.maxHeight * (1f - p)).roundToInt().coerceAtLeast(0)
                                    } else constraints.maxHeight
                                    val placeable = measurable.measure(constraints.copy(maxHeight = maxH))
                                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                                }
                        ) {
                            Spacer(Modifier.height(PhoneDimens.bottomBarLabelIconGap))
                            Text(
                                text = stringResource(screen.titleRes),
                                fontSize = sizeSpec.labelSize,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = accent ?: if (isSelected) selectedContentColor else unselectedContentColor,
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
