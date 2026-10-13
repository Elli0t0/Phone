package io.github.elli0t0.phone.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    onContactsBarVisibilityChanged: (Boolean) -> Unit = {}
) {
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
        composable(route = Screen.Recents.route) {
            RecentsScreen(
                onNavigateToContactDetail = {
                    navController.navigate(Screen.Contacts.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        composable(route = Screen.Contacts.route) {
            ContactsScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onShowBottomBarChanged = onContactsBarVisibilityChanged
            )
        }
        composable(route = Screen.Dialpad.route) {
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
