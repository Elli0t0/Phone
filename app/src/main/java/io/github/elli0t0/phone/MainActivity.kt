package io.github.elli0t0.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.elli0t0.phone.core.design.PhoneTheme
import io.github.elli0t0.phone.data.settings.model.AppSettings
import io.github.elli0t0.phone.navigation.PhoneBottomBar
import io.github.elli0t0.phone.navigation.PhoneNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as PhoneApplication
        setContent {
            val settings by app.container.settingsRepository.settingsFlow.collectAsStateWithLifecycle(
                initialValue = AppSettings()
            )
            PhoneTheme(appSettings = settings) {
                PhoneApp(appSettings = settings)
            }
        }
    }
}

@Composable
fun PhoneApp(appSettings: AppSettings) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            PhoneBottomBar(
                navController = navController,
                currentRoute = currentRoute,
                showLabels = appSettings.showBottomBarLabels
            )
        }
    ) { innerPadding ->
        PhoneNavHost(
            navController = navController,
            startDestination = appSettings.defaultStartTab.route,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
