package io.github.elli0t0.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.elli0t0.phone.core.design.PhoneTheme
import io.github.elli0t0.phone.core.permissions.SetupPermissions
import io.github.elli0t0.phone.data.settings.model.AppSettings
import io.github.elli0t0.phone.navigation.PhoneBottomBar
import io.github.elli0t0.phone.navigation.PhoneNavHost
import io.github.elli0t0.phone.navigation.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as PhoneApplication
        setContent {
            val settings by app.container.settingsRepository.settingsFlow.collectAsStateWithLifecycle(
                initialValue = AppSettings()
            )
            val setupCompleted by app.container.setupRepository.setupCompletedFlow.collectAsStateWithLifecycle(
                initialValue = null
            )
            PhoneTheme(appSettings = settings) {
                if (setupCompleted == null) {
                    Box(modifier = Modifier.fillMaxSize())
                } else {
                    PhoneApp(appSettings = settings, setupCompleted = setupCompleted == true)
                }
            }
        }
    }
}

@Composable
fun PhoneApp(appSettings: AppSettings, setupCompleted: Boolean = true) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showSetup = !setupCompleted && SetupPermissions.isAnyMissing(context)
    val startDestination = if (showSetup) {
        Screen.Setup.route
    } else {
        appSettings.defaultStartTab.route
    }
    // The Contacts tab manages its own sub-screens (list/detail/edit/groups)
    // while the top-level route stays "contacts": it reports whether the
    // list is showing, so the bar hides on detail/edit/groups.
    var showContactsBar by remember { mutableStateOf(true) }
    val showBottomBar = Screen.bottomNavItems.any { it.route == currentRoute } &&
        (currentRoute != Screen.Contacts.route || showContactsBar)

    // The bottom bar is an overlay: content runs edge-to-edge behind it,
    // and each list keeps its own bottom clearance above the bar.
    Box(modifier = Modifier.fillMaxSize()) {
        PhoneNavHost(
            navController = navController,
            startDestination = startDestination,
            defaultStartTabRoute = appSettings.defaultStartTab.route,
            onContactsBarVisibilityChanged = { showContactsBar = it },
            modifier = Modifier.fillMaxSize()
        )
        if (showBottomBar) {
            PhoneBottomBar(
                navController = navController,
                currentRoute = currentRoute,
                showLabels = appSettings.showBottomBarLabels,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}
