package io.github.elli0t0.phone.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.elli0t0.phone.core.design.Motion
import io.github.elli0t0.phone.data.settings.model.AnimationOption
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
    animationOption: AnimationOption = AnimationOption.ON,
    onContactsBarVisibilityChanged: (Boolean) -> Unit = {}
) {
    // Tab transitions: cross-fade + a subtle slide (5% width) through
    // Motion; REDUCED fades only, OFF is instant. The bottom bar lives
    // outside this NavHost (in MainActivity), so it never animates
    // with the screens.
    val tabEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        { Motion.tabEnterTransition(animationOption) }
    val tabExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        { Motion.tabExitTransition(animationOption) }
    // Inner screens (Setup, Settings) slide + fade through Motion;
    // tabs only fade (see tabEnterTransition above).
    val innerEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        { Motion.innerEnterTransition(animationOption) }
    val innerExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        { Motion.innerExitTransition(animationOption) }
    val innerPopEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        { Motion.innerPopEnterTransition(animationOption) }
    val innerPopExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        { Motion.innerPopExitTransition(animationOption) }
    // Number from the keypad with no matching contact: the Contacts tab
    // opens the editor prefilled with it, then clears the pending value.
    var pendingCreateNumber by remember { mutableStateOf<String?>(null) }
    // Contact id from Recents: the Contacts tab opens that contact's
    // detail, then clears the pending value.
    var pendingOpenContactId by remember { mutableStateOf<Long?>(null) }
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(
            route = Screen.Setup.route,
            enterTransition = innerEnterTransition,
            exitTransition = innerExitTransition,
            popEnterTransition = innerPopEnterTransition,
            popExitTransition = innerPopExitTransition
        ) {
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
                onNavigateToContactDetail = { id ->
                    pendingOpenContactId = id
                    navController.navigate(Screen.Contacts.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
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
                onShowBottomBarChanged = onContactsBarVisibilityChanged,
                pendingCreateNumber = pendingCreateNumber,
                onPendingCreateNumberConsumed = { pendingCreateNumber = null },
                pendingOpenContactId = pendingOpenContactId,
                onPendingOpenContactIdConsumed = { pendingOpenContactId = null }
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
                },
                onCreateContact = { number ->
                    pendingCreateNumber = number
                    navController.navigate(Screen.Contacts.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
        composable(
            route = Screen.Settings.route,
            enterTransition = innerEnterTransition,
            exitTransition = innerExitTransition,
            popEnterTransition = innerPopEnterTransition,
            popExitTransition = innerPopExitTransition
        ) {
            io.github.elli0t0.phone.feature.settings.SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
