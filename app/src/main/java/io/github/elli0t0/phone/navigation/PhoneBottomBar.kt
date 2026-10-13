package io.github.elli0t0.phone.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import io.github.elli0t0.phone.core.design.BottomBarDark
import io.github.elli0t0.phone.core.design.BottomBarDarkSelected
import io.github.elli0t0.phone.core.design.BottomBarLight
import io.github.elli0t0.phone.core.design.BottomBarLightSelected
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.data.settings.model.ThemeMode

/**
 * Compact floating pill bottom bar. Only the selected tab draws a
 * background pill; unselected tabs draw nothing behind their icon/label.
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
    val selectedColor = if (darkTheme) BottomBarDarkSelected else BottomBarLightSelected

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = PhoneDimens.bottomBarAboveNavBar)
            .testTag("phone_bottom_bar"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(PhoneDimens.bottomBarWidthFraction)
                .widthIn(max = PhoneDimens.bottomBarMaxWidth)
                .height(PhoneDimens.bottomBarHeight)
                .shadow(
                    PhoneDimens.bottomBarElevation,
                    RoundedCornerShape(PhoneDimens.bottomBarCorner)
                )
                .clip(RoundedCornerShape(PhoneDimens.bottomBarCorner))
                .background(barColor)
                .padding(PhoneDimens.bottomBarContentPadding),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Screen.bottomNavItems.forEach { screen ->
                val selected = currentRoute == screen.route
                val pillColor by animateColorAsState(
                    targetValue = if (selected) selectedColor else Color.Transparent,
                    label = "tab_pill"
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(PhoneDimens.bottomBarSelectedCorner))
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
                        .padding(
                            horizontal = PhoneDimens.bottomBarItemPaddingHorizontal,
                            vertical = PhoneDimens.bottomBarItemPaddingVertical
                        )
                        .testTag("tab_${screen.route}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = screen.iconRes),
                        contentDescription = stringResource(id = screen.contentDescriptionRes),
                        tint = if (selected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    if (showLabels) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(id = screen.titleRes),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (selected) {
                                    FontWeight.Bold
                                } else {
                                    FontWeight.Normal
                                }
                            ),
                            color = if (selected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}
