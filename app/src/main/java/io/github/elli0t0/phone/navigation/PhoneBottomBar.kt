package io.github.elli0t0.phone.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import io.github.elli0t0.phone.core.design.BottomBarDark
import io.github.elli0t0.phone.core.design.BottomBarDarkSelected
import io.github.elli0t0.phone.core.design.BottomBarDarkSelectedContent
import io.github.elli0t0.phone.core.design.BottomBarDarkUnselected
import io.github.elli0t0.phone.core.design.BottomBarLight
import io.github.elli0t0.phone.core.design.BottomBarLightSelected
import io.github.elli0t0.phone.core.design.BottomBarLightSelectedContent
import io.github.elli0t0.phone.core.design.BottomBarLightUnselected
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.data.settings.model.ThemeMode

/**
 * Floating pill bottom bar, overlaid on content. 250dp wide (capped at
 * 85% of the screen), 60dp tall, three equal-width items. The selected
 * tab draws its own fully rounded pill with a 4dp inset and the filled
 * icon; unselected tabs show the outlined icon. The pill fades between
 * tabs. The bar floats above the gesture handle: navigation-bar inset
 * + 12dp gap. Very soft shadow, alpha <= 10%.
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
    val barColor = if (darkTheme) BottomBarDark else BottomBarLight
    val selectedPillColor = if (darkTheme) BottomBarDarkSelected else BottomBarLightSelected
    val selectedContentColor = if (darkTheme) BottomBarDarkSelectedContent else BottomBarLightSelectedContent
    val unselectedContentColor = if (darkTheme) BottomBarDarkUnselected else BottomBarLightUnselected
    val pillShape = RoundedCornerShape(PhoneDimens.bottomBarCornerPercent)

    // Width 250dp, capped at 85% of the screen so the bar never
    // overflows small screens. Height fixed at 60dp; the three items
    // share the width equally via weight, so nothing can overflow
    // or clip.
    BoxWithConstraints(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = PhoneDimens.bottomBarBottomMargin)
            .testTag("phone_bottom_bar"),
        contentAlignment = Alignment.Center
    ) {
        val barWidth = minOf(
            PhoneDimens.bottomBarWidth,
            maxWidth * PhoneDimens.bottomBarWidthFraction
        )
        Row(
            modifier = Modifier
                .width(barWidth)
                .height(PhoneDimens.bottomBarHeight)
                .shadow(
                    PhoneDimens.bottomBarElevation,
                    pillShape,
                    spotColor = Color.Black.copy(alpha = PhoneDimens.bottomBarShadowAlpha),
                    ambientColor = Color.Black.copy(alpha = PhoneDimens.bottomBarShadowAlpha)
                )
                .clip(pillShape)
                .background(barColor),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Screen.bottomNavItems.forEach { screen ->
                val selected = currentRoute == screen.route
                val pillColor by animateColorAsState(
                    targetValue = if (selected) selectedPillColor else Color.Transparent,
                    label = "tab_pill"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (selected) selectedContentColor else unselectedContentColor,
                    label = "tab_content"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(PhoneDimens.bottomBarItemInset)
                        .clip(pillShape)
                        .background(pillColor)
                        .clickable {
                            if (currentRoute != screen.route) {
                                // Plain popUpTo + launchSingleTop: no saveState/restoreState.
                                // The saved-state machinery could resurrect a popped
                                // destination (e.g. Settings) when switching tabs.
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id)
                                    launchSingleTop = true
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
                        Icon(
                            painter = painterResource(
                                id = if (selected) screen.iconResSelected else screen.iconRes
                            ),
                            contentDescription = stringResource(id = screen.contentDescriptionRes),
                            tint = contentColor,
                            modifier = Modifier.size(PhoneDimens.bottomBarIconSize)
                        )
                        if (showLabels) {
                            Spacer(modifier = Modifier.height(PhoneDimens.bottomBarLabelIconGap))
                            Text(
                                text = stringResource(id = screen.titleRes),
                                fontSize = PhoneDimens.bottomBarLabelSize,
                                fontWeight = if (selected) {
                                    FontWeight.SemiBold
                                } else {
                                    FontWeight.Normal
                                },
                                color = contentColor,
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
