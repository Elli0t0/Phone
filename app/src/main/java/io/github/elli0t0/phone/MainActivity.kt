package io.github.elli0t0.phone

import android.content.Intent
import android.net.Uri
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
import io.github.elli0t0.phone.core.design.AmbientBackdrop
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import io.github.elli0t0.phone.core.design.LocalHazeState
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.PhoneTheme
import io.github.elli0t0.phone.core.design.bottomBarSizeSpec
import io.github.elli0t0.phone.core.permissions.SetupPermissions
import io.github.elli0t0.phone.data.settings.model.AppSettings
import io.github.elli0t0.phone.navigation.PhoneBottomBar
import io.github.elli0t0.phone.navigation.PhoneNavHost
import io.github.elli0t0.phone.navigation.Screen
import io.github.elli0t0.phone.core.telecom.PhoneNavLog

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PhoneNavLog.log("MainActivity onCreate taskId=$taskId isFinishing=$isFinishing")
        handleDialIntent(intent)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        PhoneNavLog.log("MainActivity onNewIntent taskId=$taskId isFinishing=$isFinishing")
        handleDialIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        PhoneNavLog.log("MainActivity onStart taskId=$taskId isFinishing=$isFinishing")
    }

    override fun onStop() {
        super.onStop()
        PhoneNavLog.log("MainActivity onStop taskId=$taskId isFinishing=$isFinishing")
    }

    override fun onDestroy() {
        super.onDestroy()
        PhoneNavLog.log("MainActivity onDestroy taskId=$taskId isFinishing=$isFinishing")
    }

    /**
     * External dial request (tap a number in another app, or a dial
     * shortcut): a tel: URI fills the keypad with the number, a bare
     * DIAL / CALL_BUTTON action just opens an empty keypad. Never
     * auto-calls: the user presses the call button. The intent's data
     * is consumed so a recreate / rotation does not re-apply it.
     */
    private fun handleDialIntent(intent: Intent?) {
        val data = intent?.data
        val action = intent?.action
        val number = when {
            data?.scheme == "tel" -> Uri.decode(data.schemeSpecificPart)
                .filter { it.isDigit() || it in "+*#" }
            action == Intent.ACTION_DIAL || action == Intent.ACTION_CALL_BUTTON -> ""
            else -> return
        }
        val app = application as PhoneApplication
        app.container.dialRequests.tryEmit(DialRequest(number, System.nanoTime()))
        intent.data = null
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
    // External dial requests (tap a number in another app): navigate
    // to the keypad tab and hand the number to the keypad once.
    // Requests carry an id so a replayed value (rotation, recompose)
    // is never applied twice.
    var pendingDialNumber by remember { mutableStateOf<String?>(null) }
    var lastHandledDialId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(navController) {
        (context.applicationContext as PhoneApplication).container.dialRequests.collect { request ->
            if (request.id == lastHandledDialId) return@collect
            lastHandledDialId = request.id
            navController.navigate(Screen.Dialpad.route) {
                launchSingleTop = true
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
            }
            if (request.number.isNotEmpty()) pendingDialNumber = request.number
        }
    }

    // The Contacts tab manages its own sub-screens (list/detail/edit/groups)
    // while the top-level route stays "contacts": it reports whether the
    // list is showing, so the bar hides on detail/edit/groups.
    var showContactsBar by remember { mutableStateOf(true) }
    val showBottomBar = Screen.bottomNavItems.any { it.route == currentRoute } &&
        (currentRoute != Screen.Contacts.route || showContactsBar)

    // Shrink on scroll: post-scroll only, consumed delta only, with a
    // directional accumulator — shrink after ~48dp of downward scroll,
    // expand after ~12dp upward, expand when the list is at its top
    // and after ~700ms without scroll, and reset on every destination
    // change. The keypad never shrinks the bar (it does not scroll,
    // and the gate keeps it that way). One animated target; the
    // connection consumes nothing, so lists scroll untouched.
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
    val density = LocalDensity.current
    val shrinkThresholdPx = with(density) { 48.dp.toPx() }
    val expandThresholdPx = with(density) { 12.dp.toPx() }
    var downAccum by remember { mutableFloatStateOf(0f) }
    var upAccum by remember { mutableFloatStateOf(0f) }
    var idleJob by remember { mutableStateOf<Job?>(null) }
    val barScope = rememberCoroutineScope()
    LaunchedEffect(currentRoute) {
        barShrunk = false
        downAccum = 0f
        upAccum = 0f
    }
    val barScrollConnection = remember(shrinkEnabled, currentRoute) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (shrinkEnabled && currentRoute != Screen.Dialpad.route) {
                    idleJob?.cancel()
                    idleJob = barScope.launch {
                        delay(700)
                        barShrunk = false
                        downAccum = 0f
                        upAccum = 0f
                    }
                    when {
                        // At the top of the list: the child consumed
                        // nothing and the drag is left over — expand.
                        consumed.y == 0f && available.y > 0f -> {
                            barShrunk = false
                            downAccum = 0f
                            upAccum = 0f
                        }
                        consumed.y < 0f -> {
                            // Scrolling down the list.
                            upAccum = 0f
                            downAccum += -consumed.y
                            if (downAccum >= shrinkThresholdPx) barShrunk = true
                        }
                        consumed.y > 0f -> {
                            // Scrolling back up.
                            downAccum = 0f
                            upAccum += consumed.y
                            if (upAccum >= expandThresholdPx) barShrunk = false
                        }
                    }
                }
                return Offset.Zero
            }
        }
    }

    // One app-level snackbar host, provided to every screen.
    val snackbarHostState = remember { SnackbarHostState() }

    // Real glass blur: one HazeState at the root; the navigation
    // content is the blur source, glass surfaces are the effects.
    // (1.5.3 has no rememberHazeState helper; the state is a plain
    // @Stable class, so remember { HazeState() } is equivalent.)
    val hazeState = remember { HazeState() }

    // The bottom bar is an overlay: content runs edge-to-edge behind it,
    // and each list keeps its own bottom clearance above the bar.
    // The root Box paints one opaque theme background so the window
    // background never shows through during a tab transition.
    CompositionLocalProvider(
        LocalAppSnackbarHostState provides snackbarHostState,
        LocalHazeState provides hazeState
    ) {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .nestedScroll(barScrollConnection)
    ) {
        // Ambient backdrop first, behind the NavHost: the tab
        // screens leave their gaps transparent (item D) so it
        // shows through, and glass blurs it.
        AmbientBackdrop()
        PhoneNavHost(
            navController = navController,
            startDestination = startDestination,
            defaultStartTabRoute = appSettings.defaultStartTab.route,
            animationOption = appSettings.animationOption,
            onContactsBarVisibilityChanged = { showContactsBar = it },
            pendingDialNumber = pendingDialNumber,
            onDialNumberConsumed = { pendingDialNumber = null },
            modifier = Modifier.fillMaxSize().hazeSource(hazeState)
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
