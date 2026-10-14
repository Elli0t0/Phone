package io.github.elli0t0.phone.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.elli0t0.phone.feature.contacts.ContactsScreen
import io.github.elli0t0.phone.feature.dialpad.DialpadScreen
import io.github.elli0t0.phone.feature.recents.RecentsScreen
import io.github.elli0t0.phone.feature.setup.SetupScreen

@Composable
fun PhoneNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Recents.route,
    defaultStartTabRoute: String = Screen.Recents.route,
    animationsEnabled: Boolean = true,
    onContactsBarVisibilityChanged: (Boolean) -> Unit = {}
) {
    // Tab transitions: a short fade-in and NO exit animation, so the old
    // tab never turns semi-transparent over a different background (the
    // default crossfade is what made tab switches flash). Animation Off
    // means no transition at all. The bottom bar lives outside this
    // NavHost (in MainActivity), so it never animates with the screens.
    val tabEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        { if (animationsEnabled) fadeIn(animationSpec = tween(120)) else EnterTransition.None }
    val tabExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        { ExitTransition.None }
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(route = Screen.Setup.route) {
            SetupScreen(
                onSetupFinished = {
                    navController.navigate(defaultStartTabRoute) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(
            route = Screen.Recents.route,
            enterTransition = tabEnterTransition,
            exitTransition = tabExitTransition,
            popEnterTransition = tabEnterTransition,
            popExitTransition = tabExitTransition
        ) {
            RecentsScreen(
                onNavigateToContactDetail = {
                    navController.navigate(Screen.Contacts.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        composable(
            route = Screen.Contacts.route,
            enterTransition = tabEnterTransition,
            exitTransition = tabExitTransition,
            popEnterTransition = tabEnterTransition,
            popExitTransition = tabExitTransition
        ) {
            ContactsScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onShowBottomBarChanged = onContactsBarVisibilityChanged
            )
        }
        composable(
            route = Screen.Dialpad.route,
            enterTransition = tabEnterTransition,
            exitTransition = tabExitTransition,
            popEnterTransition = tabEnterTransition,
            popExitTransition = tabExitTransition
        ) {
            DialpadScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        composable(route = Screen.Settings.route) {
            io.github.elli0t0.phone.feature.settings.SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
