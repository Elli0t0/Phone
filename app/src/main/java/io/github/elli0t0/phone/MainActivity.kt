package io.github.elli0t0.phone

import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
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
import io.github.elli0t0.phone.navigation.ExternalContactRequest
import io.github.elli0t0.phone.navigation.ExternalContactRequests

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PhoneNavLog.log("MainActivity onCreate taskId=$taskId isFinishing=$isFinishing")
        handleExternalContactIntent(intent)
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
        handleExternalContactIntent(intent)
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
     * System contact intents (INSERT / INSERT_OR_EDIT, see the manifest
     * filters) are served by the app's own screens: a create request
     * with the intent's name/phone/email prefilled, or — when the
     * intent targets an existing contact and carries a phone number —
     * an append request opening that contact's editor with the number
     * as a new phone row.
     */
    private fun handleExternalContactIntent(intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_INSERT && action != Intent.ACTION_INSERT_OR_EDIT) return
        val name = intent.getStringExtra(ContactsContract.Intents.Insert.NAME).orEmpty()
        val phone = intent.getStringExtra(ContactsContract.Intents.Insert.PHONE).orEmpty()
        val email = intent.getStringExtra(ContactsContract.Intents.Insert.EMAIL).orEmpty()
        var appended = false
        val data = intent.data
        if (action == Intent.ACTION_INSERT_OR_EDIT && data != null && phone.isNotBlank()) {
            val resolved = resolveContact(data)
            if (resolved != null) {
                ExternalContactRequests.post(
                    ExternalContactRequest.Append(resolved.first, resolved.second, phone)
                )
                appended = true
            }
        }
        if (!appended) {
            ExternalContactRequests.post(ExternalContactRequest.Create(name, phone, email))
        }
        PhoneNavLog.log("MainActivity external contact intent action=$action append=$appended")
    }

    /** Resolves a contact or raw-contact URI to (contact id, lookup key). */
    private fun resolveContact(uri: Uri): Pair<Long, String>? = runCatching {
        val segments = uri.pathSegments
        when {
            segments.contains("raw_contacts") -> {
                val rawId = ContentUris.parseId(uri)
                contentResolver.query(
                    ContactsContract.RawContacts.CONTENT_URI,
                    arrayOf(ContactsContract.RawContacts.CONTACT_ID),
                    "${ContactsContract.RawContacts._ID} = ?",
                    arrayOf(rawId.toString()),
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val contactId = cursor.getLong(0)
                        contactId to lookupKeyFor(contactId)
                    } else {
                        null
                    }
                }
            }
            segments.contains("contacts") -> {
                val contactId = ContentUris.parseId(uri)
                contactId to lookupKeyFor(contactId)
            }
            else -> null
        }
    }.getOrNull()

    private fun lookupKeyFor(contactId: Long): String {
        val uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contactId)
        contentResolver.query(
            uri,
            arrayOf(ContactsContract.Contacts.LOOKUP_KEY),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0) ?: ""
        }
        return ""
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
