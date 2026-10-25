package io.github.elli0t0.phone.feature.dialpad

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.elli0t0.phone.PhoneApplication
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.KeypadCallButtonGreenDark
import io.github.elli0t0.phone.core.design.KeypadCallButtonGreenLight


import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.Motion
import io.github.elli0t0.phone.core.design.OnAccentBlue
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.liquidGlass
import io.github.elli0t0.phone.core.design.bouncyClick
import io.github.elli0t0.phone.core.design.rememberLightHaptic
import io.github.elli0t0.phone.core.telecom.CallManager
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import io.github.elli0t0.phone.data.settings.model.ThemeMode
import io.github.elli0t0.phone.feature.contacts.ui.ContactSearchBar
import io.github.elli0t0.phone.feature.setup.SetupWarningCard
import kotlinx.coroutines.launch

@Composable
fun DialpadScreen(
    onNavigateToSettings: () -> Unit = {},
    onCreateContact: (String) -> Unit = {},
    onAddToExistingContact: (String) -> Unit = {},
    externalNumber: String? = null,
    onExternalNumberConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as PhoneApplication
    val repository = app.container.contactsRepository
    val viewModel: DialpadViewModel = viewModel(
        factory = DialpadViewModel.provideFactory(
            repository = repository,
            savedStateHandle = androidx.lifecycle.SavedStateHandle(),
            callLogRepository = app.container.callLogRepository
        )
    )

    // External dial request: fill the keypad once, then consume.
    LaunchedEffect(externalNumber) {
        if (externalNumber != null) {
            if (externalNumber.isNotEmpty()) viewModel.onExternalDialNumber(externalNumber)
            onExternalNumberConsumed()
        }
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = LocalSpacing.current
    // Whether the call button uses its dark or light green, following the
    // theme setting (Light/Dark/System), same logic as the bottom bar.
    val darkTheme = when (LocalAppSettings.current.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    // Read here, not inside dialNumber: the handler is not a
    // @Composable context, so it cannot read a CompositionLocal.
    val defaultSim = LocalAppSettings.current.defaultSim
    val animationOption = LocalAppSettings.current.animationOption
    val glassDialpad = LocalAppSettings.current.glassDialpad
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val simAccounts = remember(context) { io.github.elli0t0.phone.core.telecom.TelecomHelper.getCallCapablePhoneAccounts(context) }
    var simPickerTargetNumber by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var searchVisible by remember { androidx.compose.runtime.mutableStateOf(false) }
    var menuExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }

    fun dialNumber(number: String, lookupKey: String? = null) {
        if (simAccounts.size <= 1) {
            CallManager.placeCall(context, number)
            return
        }
        coroutineScope.launch {
            when (val res = io.github.elli0t0.phone.core.telecom.SimCallHelper.resolveSim(
                context, number, lookupKey, simAccounts, app.container.contactSimRepository,
                defaultSim
            )) {
                is io.github.elli0t0.phone.core.telecom.SimCallResolution.UseSim ->
                    CallManager.placeCall(context, number, res.sim.handle)
                is io.github.elli0t0.phone.core.telecom.SimCallResolution.ShowDialog -> {
                    simPickerTargetNumber = number
                }
            }
        }
    }

    if (simPickerTargetNumber != null) {
        io.github.elli0t0.phone.feature.call.ui.DualSimPickerDialog(
            simAccounts = simAccounts,
            targetNumber = simPickerTargetNumber.orEmpty(),
            onSimSelected = { sim ->
                val num = simPickerTargetNumber.orEmpty()
                simPickerTargetNumber = null
                CallManager.placeCall(context, num, sim.handle)
            },
            onDismiss = {
                simPickerTargetNumber = null
            }
        )
    }

    // Permission launcher for CALL_PHONE
    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // On either granted or denied, placeCall handles it (ACTION_CALL or ACTION_DIAL fallback)
        dialNumber(uiState.rawNumber)
    }

    // Shared light haptic for every keypad tap (follows the system
    // haptics setting), replacing the two local View-based helpers.
    val lightHaptic = rememberLightHaptic()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.stopBackspaceRepeat()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            viewModel.stopBackspaceRepeat()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Keypad page background: the theme background, like every other page.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = spacing.screenHorizontalPadding)
            // Top: status-bar inset (the dialpad has no Scaffold of its own).
            // Bottom: clearance above the overlaid bottom bar
            // (bar height + nav-bar inset + 16dp gap).
            .padding(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                bottom = PhoneDimens.bottomBarReservedHeight +
                    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            )
            .testTag("dialpad_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top-right actions: search + menu, like Recents and Contacts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (searchVisible) viewModel.onNameSearchQueryChanged("")
                    searchVisible = !searchVisible
                },
                modifier = Modifier.testTag("button_search_dialpad")
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search),
                    contentDescription = stringResource(id = R.string.cd_search),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("button_dialpad_menu")
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_more_vert),
                        contentDescription = stringResource(id = R.string.cd_more_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.settings_title)) },
                        onClick = { menuExpanded = false; onNavigateToSettings() },
                        modifier = Modifier.testTag("settings_button")
                    )
                }
            }
        }
        if (searchVisible) {
            ContactSearchBar(
                query = uiState.nameSearchQuery,
                onQueryChange = viewModel::onNameSearchQueryChanged,
                onClear = {
                    viewModel.onNameSearchQueryChanged("")
                    searchVisible = false
                },
                placeholderText = stringResource(id = R.string.contacts_search_hint),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = spacing.small)
            )
        }
        SetupWarningCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.small)
        )
            // No matching contact: offer real actions for the number.
            val noMatchActions: @Composable () -> Unit = {
                DialpadNoMatchActions(
                    onCreateContact = {
                        lightHaptic()
                        onCreateContact(uiState.rawNumber)
                    },
                    onAddToContact = {
                        lightHaptic()
                        onAddToExistingContact(uiState.rawNumber)
                    },
                    onMessage = {
                        lightHaptic()
                        context.startActivity(
                            Intent(
                                Intent.ACTION_SENDTO,
                                Uri.parse("smsto:${Uri.encode(uiState.rawNumber)}")
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            val showSearchRows = searchVisible && uiState.nameSearchQuery.isNotBlank() &&
                uiState.nameSearchResults.isNotEmpty()
            val showChips = (!searchVisible || uiState.nameSearchQuery.isBlank()) &&
                uiState.matchingContacts.isNotEmpty()
            val showNoMatchActions = uiState.rawNumber.isNotEmpty() &&
                !showSearchRows && !showChips
            // T9 Matching contacts list above the keypad
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = spacing.small),
                contentAlignment = Alignment.BottomCenter
            ) {
            if (showSearchRows) {
                // Name search: full rows.
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialpad_search_results")
                ) {
                    items(uiState.nameSearchResults, key = { it.id }) { contact ->
                        DialpadSearchRow(
                            contact = contact,
                            onClick = {
                                lightHaptic()
                                viewModel.onContactSelected(contact)
                                viewModel.onNameSearchQueryChanged("")
                                searchVisible = false
                            },
                            onCallClick = {
                                lightHaptic()
                                val num = contact.primaryNumberPreview ?: ""
                                if (num.isNotBlank()) {
                                    dialNumber(num, contact.lookupKey)
                                }
                            }
                        )
                    }
                }
            } else if (showChips) {
                // T9: suggestion chips above the keypad; tap selects.
                DialpadSuggestionChips(
                    contacts = uiState.matchingContacts,
                    dialedDigits = uiState.rawNumber,
                    onSelect = {
                        lightHaptic()
                        viewModel.onContactSelected(it)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    contactCounts = uiState.matchingContactCounts
                )
            } else if (uiState.rawNumber.isEmpty() && uiState.quickRedial.isNotEmpty()) {
                // Empty keypad: one-tap redial for the last 3 recent
                // numbers, sitting above the keypad.
                QuickRedialChips(
                    entries = uiState.quickRedial,
                    onRedial = { entry ->
                        lightHaptic()
                        dialNumber(entry.number)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            }
            // No-match actions animate in/out below the list area
            // (ColumnScope AnimatedVisibility): fade + expand + scale on
            // full motion, fade only on Reduced, instant on Off.
            val noMatchEnter: EnterTransition = when (animationOption) {
                AnimationOption.ON -> fadeIn(
                    tween(
                        durationMillis = Motion.scaledDurationMs(Motion.Durations.Medium, animationOption),
                        easing = Motion.Easings.Emphasized
                    )
                ) + expandVertically(
                    tween(
                        durationMillis = Motion.scaledDurationMs(Motion.Durations.Medium, animationOption),
                        easing = Motion.Easings.Emphasized
                    )
                ) + scaleIn(
                    tween(
                        durationMillis = Motion.scaledDurationMs(Motion.Durations.Medium, animationOption),
                        easing = Motion.Easings.Emphasized
                    ),
                    initialScale = 0.92f
                )
                AnimationOption.REDUCED -> fadeIn(
                    tween(
                        durationMillis = Motion.scaledDurationMs(Motion.Durations.Medium, animationOption),
                        easing = Motion.Easings.Emphasized
                    )
                )
                AnimationOption.OFF -> EnterTransition.None
            }
            val noMatchExit: ExitTransition = when (animationOption) {
                AnimationOption.ON -> fadeOut(
                    tween(
                        durationMillis = Motion.scaledDurationMs(Motion.Durations.Short, animationOption),
                        easing = Motion.Easings.Exit
                    )
                ) + shrinkVertically(
                    tween(
                        durationMillis = Motion.scaledDurationMs(Motion.Durations.Short, animationOption),
                        easing = Motion.Easings.Exit
                    )
                ) + scaleOut(
                    tween(
                        durationMillis = Motion.scaledDurationMs(Motion.Durations.Short, animationOption),
                        easing = Motion.Easings.Exit
                    ),
                    targetScale = 0.92f
                )
                AnimationOption.REDUCED -> fadeOut(
                    tween(
                        durationMillis = Motion.scaledDurationMs(Motion.Durations.Short, animationOption),
                        easing = Motion.Easings.Exit
                    )
                )
                AnimationOption.OFF -> ExitTransition.None
            }
            AnimatedVisibility(
                visible = showNoMatchActions,
                enter = noMatchEnter,
                exit = noMatchExit
            ) {
                noMatchActions()
            }

        // Formatted Number Display (supports long click to paste).
        // Always rendered: a fixed slot, so typing the first digit
        // never shifts the layout. Number centered, regular weight,
        // auto-shrinks on overflow; backspace sits at its right.
        val displayText = uiState.formattedNumber.ifBlank { uiState.rawNumber }
        var displayFontSize by remember { mutableStateOf(PhoneDimens.keypadNumberDisplaySize) }
        LaunchedEffect(displayText) {
            displayFontSize = PhoneDimens.keypadNumberDisplaySize
        }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = {
                                val clipText = clipboardManager.getText()?.text
                                if (!clipText.isNullOrBlank()) {
                                    lightHaptic()
                                    viewModel.onNumberPasted(clipText)
                                }
                            }
                        )
                    }
                    .testTag("dialpad_number_display"),
                contentAlignment = Alignment.Center
            ) {
                // Hidden full-string Text: measures overflow and drives
                // the auto-shrink, exactly as the single Text did.
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = displayFontSize,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    onTextLayout = { layoutResult ->
                        if (layoutResult.hasVisualOverflow) {
                            val shrunk = displayFontSize * PhoneDimens.keypadNumberDisplayShrinkFactor
                            displayFontSize = if (shrunk.value < PhoneDimens.keypadNumberDisplayMinSize.value) {
                                PhoneDimens.keypadNumberDisplayMinSize
                            } else {
                                shrunk
                            }
                        }
                    },
                    modifier = Modifier.graphicsLayer { alpha = 0f }.clearAndSetSemantics { }
                )
                // Visible per-character number: each digit pops in
                // (scale + fade on ON, fade only on REDUCED, instant
                // on OFF) as it is typed; the row grows/shrinks
                // smoothly as digits are added or deleted.
                Row(
                    modifier = Modifier.animateContentSize(
                        animationSpec = if (animationOption == AnimationOption.OFF) {
                            snap()
                        } else if (animationOption == AnimationOption.REDUCED) {
                            tween(durationMillis = Motion.scaledDurationMs(Motion.Durations.Micro, animationOption))
                        } else {
                            spring(dampingRatio = 0.8f, stiffness = 500f)
                        }
                    ).semantics(mergeDescendants = true) { contentDescription = displayText },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    displayText.forEachIndexed { index, char ->
                        key(index) {
                            var entered by remember {
                                mutableStateOf(animationOption == AnimationOption.OFF)
                            }
                            LaunchedEffect(Unit) { entered = true }
                            val progress by animateFloatAsState(
                                targetValue = if (entered) 1f else 0f,
                                animationSpec = if (animationOption == AnimationOption.OFF) {
                                    snap()
                                } else {
                                    tween(
                                        durationMillis = Motion.scaledDurationMs(Motion.Durations.Short, animationOption),
                                        easing = Motion.Easings.Emphasized
                                    )
                                },
                                label = "digit$index"
                            )
                            Text(
                                text = char.toString(),
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = displayFontSize,
                                    fontWeight = FontWeight.Normal
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.graphicsLayer {
                                    alpha = progress
                                    if (animationOption == AnimationOption.ON) {
                                        val s = Motion.DialedDigitPopStartScale +
                                            (1f - Motion.DialedDigitPopStartScale) * progress
                                        scaleX = s
                                        scaleY = s
                                    }
                                }
                            )
                        }
                    }
                }
                // Backspace at the right of the number, with accelerated repeat deletion.
                // Fades + scales out when the number is empty; the
                // number stays centered because the backspace overlays it.
                // (AnimatedVisibility cannot be used here: the enclosing
                // Column resolves it to the ColumnScope overload, which
                // cannot be called inside this Box.)
                val backspaceVisible = uiState.rawNumber.isNotEmpty()
                val backspaceAlpha by animateFloatAsState(
                    targetValue = if (backspaceVisible) 1f else 0f,
                    animationSpec = tween(durationMillis = Motion.scaledDurationMs(Motion.Durations.Short, animationOption)),
                    label = "backspaceAlpha"
                )
                val backspaceScale by animateFloatAsState(
                    targetValue = if (backspaceVisible) 1f else 0.8f,
                    animationSpec = tween(durationMillis = Motion.scaledDurationMs(Motion.Durations.Short, animationOption)),
                    label = "backspaceScale"
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(56.dp)
                        .graphicsLayer {
                            alpha = backspaceAlpha
                            scaleX = backspaceScale
                            scaleY = backspaceScale
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .then(
                                if (backspaceVisible) {
                                    Modifier.pointerInput(Unit) {
                                        awaitEachGesture {
                                            val down = awaitFirstDown(requireUnconsumed = false)
                                            // Consume the press so the parent number
                                            // row's long-press (paste) never fires
                                            // from a backspace long-press.
                                            down.consume()
                                            lightHaptic()
                                            viewModel.onBackspacePressed()
                                            viewModel.startBackspaceRepeat(onTickHaptic = { lightHaptic() })
                                            try {
                                                waitForUpOrCancellation()
                                            } finally {
                                                viewModel.stopBackspaceRepeat()
                                            }
                                        }
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            .testTag("btn_backspace"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_backspace),
                            contentDescription = stringResource(id = R.string.cd_backspace_button),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(spacing.small))

        // Keypad Grid (Keys 1-9, *, 0, #)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = spacing.small),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DIAL_KEYS_GRID.forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    rowKeys.forEach { dialKey ->
                        DialKeyView(
                            dialKey = dialKey,
                            onClick = {
                                lightHaptic()
                                viewModel.onKeyPressed(dialKey.digit)
                            },
                            onLongClick = if (dialKey.digit == '0') {
                                {
                                    lightHaptic()
                                    viewModel.onLongPressZero()
                                }
                            } else null
                        )
                    }
                }
            }
        }

        // Action row: Left spacer, Call Button, Backspace Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = PhoneDimens.keypadCallButtonBottomPadding),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Placeholder empty box for balance (same size as backspace)
            Box(modifier = Modifier.size(56.dp))

            // Call button: glossy liquid-glass emerald, or the flat green
            // FAB when the dialpad's liquid glass is off. With no digits
            // it does nothing.
            val callButtonSource = remember { MutableInteractionSource() }
            if (glassDialpad) {
            Box(
                modifier = Modifier
                    .size(PhoneDimens.callButtonSize)
                    .clip(CircleShape)
                    .bouncyClick(callButtonSource)
                    .clickable(
                        interactionSource = callButtonSource,
                        indication = null
                    ) {
                        if (uiState.canCall) {
                            lightHaptic()
                            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                        }
                    }
                    // Solid green under the glass: the glass fill alone
                    // (22% alpha) washes out to near-white in light
                    // theme, so the button was nearly invisible.
                    .background(
                        color = if (darkTheme) KeypadCallButtonGreenDark else KeypadCallButtonGreenLight,
                        shape = CircleShape
                    )
                    .liquidGlass(
                        shape = CircleShape,
                        tint = if (darkTheme) KeypadCallButtonGreenDark else KeypadCallButtonGreenLight,
                        elevation = 10.dp,
                        isSelected = true
                    )
                    .testTag("btn_call"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_phone),
                    contentDescription = stringResource(id = R.string.cd_call_button),
                    tint = OnAccentBlue,
                    modifier = Modifier.size(32.dp)
                )
            }
            } else {
                FloatingActionButton(
                    onClick = {
                        if (uiState.canCall) {
                            lightHaptic()
                            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                        }
                    },
                    modifier = Modifier
                        .size(PhoneDimens.callButtonSize)
                        .testTag("btn_call"),
                    shape = CircleShape,
                    containerColor = if (darkTheme) KeypadCallButtonGreenDark else KeypadCallButtonGreenLight,
                    contentColor = OnAccentBlue,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_phone),
                        contentDescription = stringResource(id = R.string.cd_call_button),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            // Right spacer for balance (backspace now sits by the number display).
            Box(modifier = Modifier.size(56.dp))
        }
    }
}
