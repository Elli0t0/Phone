package io.github.elli0t0.phone.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import io.github.elli0t0.phone.data.settings.model.TransitionStyle
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
    transitionStyle: TransitionStyle = TransitionStyle.PUSH,
    onContactsBarVisibilityChanged: (Boolean) -> Unit = {},
    pendingDialNumber: String? = null,
    onDialNumberConsumed: () -> Unit = {},
    pendingOpenRecordingPage: Boolean = false,
    onRecordingPageConsumed: () -> Unit = {}
) {
    // Tab transitions: a short 180ms cross-fade only through Motion;
    // REDUCED fades shorter, OFF is instant. The bottom bar lives
    // outside this NavHost (in MainActivity), so it never animates
    // with the screens.
    val tabEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        { Motion.tabEnterTransition(animationOption) }
    val tabExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        { Motion.tabExitTransition(animationOption) }
    // Inner screens (Setup, Settings) slide + parallax + dim through
    // Motion when the transition style is PUSH, fade when FADE;
    // tabs only fade (see tabEnterTransition above).
    val innerEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        { Motion.innerEnterTransition(animationOption, transitionStyle) }
    val innerExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        { Motion.innerExitTransition(animationOption, transitionStyle) }
    val innerPopEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition =
        { Motion.innerPopEnterTransition(animationOption, transitionStyle) }
    val innerPopExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition =
        { Motion.innerPopExitTransition(animationOption, transitionStyle) }
    // Create-contact request (keypad / Recents):
    // the Contacts tab opens the editor prefilled, then clears it.
    var pendingCreateRequest by remember { mutableStateOf<CreateContactRequest?>(null) }
    // Number to add to an existing contact: the Contacts tab opens the
    // in-app picker for it, then clears the pending value.
    var pendingAddNumber by remember { mutableStateOf<String?>(null) }
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
                onCreateContact = { number ->
                    pendingCreateRequest = CreateContactRequest(phone = number)
                    navController.navigate(Screen.Contacts.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onAddToExistingContact = { number ->
                    pendingAddNumber = number
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
                pendingCreateRequest = pendingCreateRequest,
                onPendingCreateRequestConsumed = { pendingCreateRequest = null },
                pendingAddNumber = pendingAddNumber,
                onPendingAddNumberConsumed = { pendingAddNumber = null },
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
                externalNumber = pendingDialNumber,
                onExternalNumberConsumed = onDialNumberConsumed,
                onAddToExistingContact = { number ->
                    pendingAddNumber = number
                    navController.navigate(Screen.Contacts.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onCreateContact = { number ->
                    pendingCreateRequest = CreateContactRequest(phone = number)
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
                onNavigateBack = { navController.popBackStack() },
                openRecordingPage = pendingOpenRecordingPage,
                onRecordingPageConsumed = onRecordingPageConsumed
            )
        }
    }
}
