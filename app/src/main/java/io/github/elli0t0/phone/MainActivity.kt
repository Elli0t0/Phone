package io.github.elli0t0.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // The bar floats only over the 3 main tabs; it is hidden on
            // Settings, contact details, edit, permissions setup and in-call.
            if (currentRoute in Screen.bottomNavItems.map { it.route }) {
                PhoneBottomBar(
                    navController = navController,
                    currentRoute = currentRoute,
                    showLabels = appSettings.showBottomBarLabels
                )
            }
        }
    ) { innerPadding ->
        // Only the status-bar (top) inset applies here. Lists on the main tabs
        // scroll edge-to-edge behind the floating bar and carry their own
        // bottom content padding, so no clipped container appears behind it.
        PhoneNavHost(
            navController = navController,
            startDestination = startDestination,
            defaultStartTabRoute = appSettings.defaultStartTab.route,
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
        )
    }
}
