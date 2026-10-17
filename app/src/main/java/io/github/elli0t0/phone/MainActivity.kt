package io.github.elli0t0.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import io.github.elli0t0.phone.core.design.Motion
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.PhoneTheme
import io.github.elli0t0.phone.core.design.bottomBarSizeSpec
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
                    // Opaque theme background while the setup state loads:
                    // the window background must never show through.
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    )
                } else {
                    PhoneApp(appSettings = settings, setupCompleted = setupCompleted == true)
                }
            }
        }
    }
}

/**
 * The one app-level snackbar state. Screens show messages through it
 * instead of per-screen hosts that sit under the floating bar.
 */
val LocalAppSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("LocalAppSnackbarHostState not provided")
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

    // Shrink on scroll: scrolling down compacts the bar to icons only,
    // scrolling up restores it. One animated target; the connection
    // consumes nothing, so lists scroll untouched.
    val shrinkEnabled = appSettings.shrinkBottomBarOnScroll
    var barShrunk by remember { mutableStateOf(false) }
    val barShrinkProgress = remember { Animatable(0f) }
    LaunchedEffect(barShrunk, shrinkEnabled) {
        barShrinkProgress.animateTo(
            targetValue = if (barShrunk && shrinkEnabled) 1f else 0f,
            animationSpec = Motion.tweenSpec(
                Motion.Durations.Medium,
                Motion.Easings.Emphasized,
                appSettings.animationOption
            )
        )
    }
    val barScrollConnection = remember(shrinkEnabled) {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (shrinkEnabled) {
                    if (available.y < 0) barShrunk = true
                    else if (available.y > 0) barShrunk = false
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (shrinkEnabled) {
                    if (consumed.y < 0) barShrunk = true
                    else if (consumed.y > 0) barShrunk = false
                }
                return Offset.Zero
            }
        }
    }

    // One app-level snackbar host, provided to every screen.
    val snackbarHostState = remember { SnackbarHostState() }

    // The bottom bar is an overlay: content runs edge-to-edge behind it,
    // and each list keeps its own bottom clearance above the bar.
    // The root Box paints one opaque theme background so the window
    // background never shows through during a tab transition.
    CompositionLocalProvider(LocalAppSnackbarHostState provides snackbarHostState) {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(barScrollConnection)
    ) {
        PhoneNavHost(
            navController = navController,
            startDestination = startDestination,
            defaultStartTabRoute = appSettings.defaultStartTab.route,
            animationOption = appSettings.animationOption,
            onContactsBarVisibilityChanged = { showContactsBar = it },
            modifier = Modifier.fillMaxSize()
        )
        // The bar hides and shows with a slide + fade when entering or
        // leaving inner screens, and slides up on app start. Reduced
        // fades only; Off is instant.
        val animationOption = appSettings.animationOption
        val barEnter: EnterTransition = when (animationOption) {
            AnimationOption.ON -> slideInVertically { it } +
                fadeIn(tween(
                    durationMillis = Motion.scaledDurationMs(
                        Motion.Durations.Medium,
                        animationOption
                    ),
                    easing = Motion.Easings.Emphasized
                ))
            AnimationOption.REDUCED -> fadeIn(tween(
                durationMillis = Motion.scaledDurationMs(
                    Motion.Durations.Medium,
                    animationOption
                ),
                easing = Motion.Easings.Emphasized
            ))
            AnimationOption.OFF -> EnterTransition.None
        }
        val barExit: ExitTransition = when (animationOption) {
            AnimationOption.ON -> slideOutVertically { it } +
                fadeOut(tween(
                    durationMillis = Motion.scaledDurationMs(
                        Motion.Durations.Short,
                        animationOption
                    ),
                    easing = Motion.Easings.Exit
                ))
            AnimationOption.REDUCED -> fadeOut(tween(
                durationMillis = Motion.scaledDurationMs(
                    Motion.Durations.Short,
                    animationOption
                ),
                easing = Motion.Easings.Exit
            ))
            AnimationOption.OFF -> ExitTransition.None
        }
        // Fade scrim behind the floating bar: list content fades out
        // before it can bleed through the bar's tab labels/icons.
        // Height: bar reserved height + nav-bar inset + a fade zone
        // (one reserved height, ~96dp) above the bar. The gradient
        // fades 0% -> max alpha over the fade zone (the top half),
        // then stays at the max alpha under the bar. Plain Box, no
        // clickable, so touches pass through to the list.
        AnimatedVisibility(
            visible = showBottomBar,
            enter = fadeIn(tween(
                durationMillis = Motion.scaledDurationMs(
                    Motion.Durations.Medium,
                    animationOption
                ),
                easing = Motion.Easings.Emphasized
            )),
            exit = fadeOut(tween(
                durationMillis = Motion.scaledDurationMs(
                    Motion.Durations.Short,
                    animationOption
                ),
                easing = Motion.Easings.Exit
            )),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(PhoneDimens.bottomBarReservedHeight * 2)
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.5f to MaterialTheme.colorScheme.background.copy(alpha = PhoneDimens.bottomScrimMaxAlpha),
                            1.0f to MaterialTheme.colorScheme.background.copy(alpha = PhoneDimens.bottomScrimMaxAlpha)
                        )
                    )
            )
        }
        AnimatedVisibility(
            visible = showBottomBar,
            enter = barEnter,
            exit = barExit,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            PhoneBottomBar(
                navController = navController,
                currentRoute = currentRoute,
                showLabels = appSettings.showBottomBarLabels,
                shrinkProgress = { barShrinkProgress.value }
            )
        }
        // One app-level snackbar host above the floating bar: nav
        // inset + bar bottom margin + current bar height + a small
        // gap, so the snackbar is never covered by the bar.
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(
                    bottom = PhoneDimens.bottomBarBottomMargin +
                        bottomBarSizeSpec(appSettings.bottomBarSize).height +
                        LocalSpacing.current.small
                )
        )
    }
    }
}
