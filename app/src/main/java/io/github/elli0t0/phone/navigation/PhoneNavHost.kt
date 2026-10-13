package io.github.elli0t0.phone.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.elli0t0.phone.feature.contacts.ContactsScreen
import io.github.elli0t0.phone.feature.dialpad.DialpadScreen
import io.github.elli0t0.phone.feature.recents.RecentsScreen

@Composable
fun PhoneNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Recents.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(route = Screen.Recents.route) {
            RecentsScreen(
                onNavigateToContactDetail = {
                    navController.navigate(Screen.Contacts.route)
                }
            )
        }
        composable(route = Screen.Contacts.route) {
            ContactsScreen()
        }
        composable(route = Screen.Dialpad.route) {
            DialpadScreen()
        }
    }
}
