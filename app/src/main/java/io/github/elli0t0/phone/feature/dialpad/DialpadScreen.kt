package io.github.elli0t0.phone.feature.dialpad

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telecom.TelecomManager
import android.telephony.PhoneNumberUtils
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import io.github.elli0t0.phone.core.design.GlassDropdownMenu
import io.github.elli0t0.phone.core.design.GlassMenuItem
import io.github.elli0t0.phone.core.design.ConfirmCallSheet
import io.github.elli0t0.phone.core.design.PendingCall
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.elli0t0.phone.PhoneApplication
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalMinimal
import io.github.elli0t0.phone.core.design.minimalPalette
import io.github.elli0t0.phone.data.settings.model.KeypadStyle
import io.github.elli0t0.phone.data.settings.model.QuickCallStyle
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.dialogEnter
import io.github.elli0t0.phone.core.design.liquidGlass
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.rememberLightHaptic
import io.github.elli0t0.phone.core.common.PhoneNumberFormatter
import io.github.elli0t0.phone.core.telecom.CallManager
import io.github.elli0t0.phone.data.settings.model.AppBackground
import io.github.elli0t0.phone.data.settings.model.ThemeMode
import io.github.elli0t0.phone.feature.contacts.ui.ContactSearchBar
import io.github.elli0t0.phone.feature.setup.SetupWarningCard
import kotlinx.coroutines.launch

@Composable
fun DialpadScreen(
    onNavigateToSettings: () -> Unit = {},
    onCreateContact: (String) -> Unit = {},
    onAddToExistingContact: (String) -> Unit = {},
    onOpenContact: (Long) -> Unit = {},
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
            callLogRepository = app.container.callLogRepository,
            favoritesRepository = app.container.favoritesRepository,
            settingsRepository = app.container.settingsRepository
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
    val confirmBeforeCall = LocalAppSettings.current.confirmBeforeCall
    val animationOption = LocalAppSettings.current.animationOption
    val keyStyle = LocalAppSettings.current.effectiveStyle.keys
    // Read here, not inside the long-press handler: it runs outside
    // composition and cannot read a CompositionLocal (same reason as
    // dialNumber above).
    val speedDial = LocalAppSettings.current.speedDial
    val speedDialEnabled = LocalAppSettings.current.speedDialEnabled
    val quickCallEnabled = LocalAppSettings.current.quickCallEnabled
    val quickCallStyle = LocalAppSettings.current.quickCallStyle
    val quickCallHidden = LocalAppSettings.current.quickCallHidden
    val coroutineScope = rememberCoroutineScope()

    val simAccounts = remember(context) { io.github.elli0t0.phone.core.telecom.TelecomHelper.getCallCapablePhoneAccounts(context) }
    var simPickerTargetNumber by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var pendingConfirmCall by remember { androidx.compose.runtime.mutableStateOf<PendingCall?>(null) }
    var searchVisible by remember { androidx.compose.runtime.mutableStateOf(false) }
    var menuExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }
    // Speed dial assign/replace (0.19.3): the digit awaiting a
    // contact pick, and the digit whose assignment a typed number
    // would replace (with the existing name and the typed number
    // captured at open time).
    var assignSheetDigit by remember { androidx.compose.runtime.mutableStateOf<Char?>(null) }
    var replaceDialogDigit by remember { androidx.compose.runtime.mutableStateOf<Char?>(null) }
    var replaceDialogExisting by remember { androidx.compose.runtime.mutableStateOf("") }
    var replaceDialogTyped by remember { androidx.compose.runtime.mutableStateOf("") }

    fun dialNumber(number: String, lookupKey: String? = null) {
        if (PhoneNumberUtils.isEmergencyNumber(PhoneNumberFormatter.normalizeDigits(number))) {
            // Emergency: skip the SIM picker and saved-SIM lookup
            // entirely; placeCall hands emergency numbers to the
            // system dialer.
            CallManager.placeCall(context, number)
            return
        }
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

    // Call through the confirm sheet when it is on, exactly like the
    // call button does; the sheet's number (not the typed number) is
    // what the permission launcher dials after confirmation.
    fun callWithConfirm(number: String, lookupKey: String?, label: String) {
        if (confirmBeforeCall &&
            !PhoneNumberUtils.isEmergencyNumber(PhoneNumberFormatter.normalizeDigits(number))
        ) {
            pendingConfirmCall = PendingCall(number = number, lookupKey = lookupKey, label = label)
        } else {
            dialNumber(number, lookupKey)
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

    // Permission launcher for CALL_PHONE. The confirmed number wins
    // over the typed number: strip taps confirm a contact's number,
    // and confirming must dial that number, not what is typed.
    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // On either granted or denied, placeCall handles it (ACTION_CALL or ACTION_DIAL fallback)
        val pending = pendingConfirmCall
        pendingConfirmCall = null
        if (pending != null) {
            dialNumber(pending.number, pending.lookupKey)
        } else {
            dialNumber(uiState.rawNumber)
        }
    }

    // Shared light haptic for every keypad tap (follows the system
    // haptics setting), replacing the two local View-based helpers.
    val lightHaptic = rememberLightHaptic()

    pendingConfirmCall?.let { pending ->
        ConfirmCallSheet(
            pending = pending,
            onConfirm = {
                lightHaptic()
                callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            },
            onDismiss = { pendingConfirmCall = null }
        )
    }

    // Speed dial replace confirmation (0.19.3): a typed number never
    // silently overwrites an assigned digit.
    replaceDialogDigit?.let { digit ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { replaceDialogDigit = null },
            title = { Text(text = stringResource(id = R.string.settings_speed_dial)) },
            text = {
                Text(
                    text = stringResource(
                        id = R.string.speed_dial_replace_message,
                        replaceDialogExisting,
                        replaceDialogTyped
                    )
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    coroutineScope.launch {
                        app.container.settingsRepository.setSpeedDial(
                            speedDial + (digit to replaceDialogTyped)
                        )
                    }
                    replaceDialogDigit = null
                    Toast.makeText(
                        context,
                        context.getString(
                            R.string.dialpad_speed_dial_saved,
                            digit.toString()
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }) {
                    Text(text = stringResource(id = R.string.speed_dial_replace))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { replaceDialogDigit = null }) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        )
    }

    // Speed dial assign sheet (0.19.3): empty keypad + long-press on
    // an unassigned digit opens the contact picker for that digit.
    assignSheetDigit?.let { digit ->
        io.github.elli0t0.phone.feature.contacts.ui.ContactPickerSheet(
            contactsRepository = repository,
            title = stringResource(id = R.string.speed_dial_assign_title, digit.toString()),
            onDismiss = { assignSheetDigit = null },
            onPick = { number ->
                assignSheetDigit = null
                coroutineScope.launch {
                    app.container.settingsRepository.setSpeedDial(
                        speedDial + (digit to number)
                    )
                }
                Toast.makeText(
                    context,
                    context.getString(
                        R.string.dialpad_speed_dial_saved,
                        digit.toString()
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

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

    // Keypad page background: transparent when the ambient backdrop
    // is live so it shows through, like Recents; otherwise the theme
    // background, as before. PLAIN is the only background that draws
    // nothing — including in pure black, where the scene still
    // shows dimmed over the black base (0.18.7).
    // Minimal (0.18.10): the keypad screen's own background
    // (0xFF151517 dark), backgrounds ignored.
    val minimal = LocalMinimal.current
    val backdropVisible = LocalAppSettings.current.let {
        it.appBackground != AppBackground.PLAIN
    }
    // Small screens (< 640dp tall) get smaller keys with tighter
    // rows; the call button stays 72dp.
    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val compactKeys = maxHeight < 640.dp
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (minimal) {
                    minimalPalette().keypadBg
                } else if (backdropVisible) {
                    Color.Transparent
                } else {
                    MaterialTheme.colorScheme.background
                }
            )
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
                GlassDropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.dialogEnter()
                ) {
                    GlassMenuItem(
                        text = stringResource(id = R.string.settings_title),
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
        DialpadSuggestionsArea(
            uiState = uiState,
            searchVisible = searchVisible,
            onSearchVisibilityChange = { searchVisible = it },
            animationOption = animationOption,
            quickCallEnabled = quickCallEnabled,
            quickCallStyle = quickCallStyle,
            onSearchRowClick = { contact ->
                lightHaptic()
                viewModel.onContactSelected(contact)
                viewModel.onNameSearchQueryChanged("")
                searchVisible = false
            },
            onSearchRowCallClick = { contact ->
                lightHaptic()
                val num = contact.primaryNumberPreview ?: ""
                if (num.isNotBlank()) {
                    dialNumber(num, contact.lookupKey)
                }
            },
            onSuggestionSelect = {
                lightHaptic()
                viewModel.onContactSelected(it)
            },
            onRedial = { entry ->
                lightHaptic()
                dialNumber(entry.number)
            },
            onCreateContactAction = {
                lightHaptic()
                onCreateContact(uiState.rawNumber)
            },
            onAddToExistingContactAction = {
                lightHaptic()
                onAddToExistingContact(uiState.rawNumber)
            },
            onMessageAction = {
                lightHaptic()
                context.startActivity(
                    Intent(
                        Intent.ACTION_SENDTO,
                        Uri.parse("smsto:${Uri.encode(uiState.rawNumber)}")
                    )
                )
            }
        )
        // Region + line-type caption under the number, only when
        // the formatter could determine one.
        val numberCaptionInfo = remember(uiState.rawNumber) {
            PhoneNumberFormatter.numberCaption(uiState.rawNumber)
        }
        val numberCaption = numberCaptionInfo?.let {
            "${it.country} · ${stringResource(it.typeRes)}"
        }
        DialpadNumberDisplay(
            displayText = uiState.formattedNumber.ifBlank { uiState.rawNumber },
            rawNumber = uiState.rawNumber,
            animationOption = animationOption,
            lightHaptic = lightHaptic,
            onPaste = viewModel::onNumberPasted,
            onBackspacePressed = viewModel::onBackspacePressed,
            onStartBackspaceRepeat = viewModel::startBackspaceRepeat,
            onStopBackspaceRepeat = viewModel::stopBackspaceRepeat,
            caption = numberCaption
        )
        // Quick call pills (0.19.6): directly above the keys, 12dp
        // above the first key row. Hidden while typing (the
        // suggestion chips take over), while a name search is open,
        // in Minimal mode (the Minimal keypad stays pixel-identical),
        // when the setting is off, and when the style is CLASSIC
        // (the Recent chips in the top area replace the pills).
        // The keys below simply reflow, with no gap.
        if (!minimal && quickCallEnabled && quickCallStyle == QuickCallStyle.PILLS &&
            uiState.rawNumber.isEmpty() && !searchVisible &&
            uiState.quickCall.isNotEmpty()
        ) {
            QuickCallStrip(
                entries = uiState.quickCall,
                onCall = { entry ->
                    lightHaptic()
                    callWithConfirm(entry.number, entry.contact.lookupKey, entry.contact.displayName)
                },
                onMessage = { entry ->
                    lightHaptic()
                    context.startActivity(
                        Intent(
                            Intent.ACTION_SENDTO,
                            Uri.parse("smsto:${Uri.encode(entry.number)}")
                        )
                    )
                },
                onOpenContact = { entry ->
                    onOpenContact(entry.contact.id)
                },
                onRemove = { entry ->
                    coroutineScope.launch {
                        app.container.settingsRepository.setQuickCallHidden(
                            quickCallHidden + entry.contact.lookupKey
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.small, bottom = 12.dp)
            )
        }
        DialpadKeypadGrid(
            // No tick here: KeypadKey ticks on press-down.
            // Minimal (0.18.10): the MINIMAL style is forced,
            // whatever the stored keypad style is.
            keypadStyle = if (minimal) KeypadStyle.MINIMAL else LocalAppSettings.current.keypadStyle,
            // Speed-dial names on keys (0.19.3): Minimal gets none,
            // so the Minimal keypad stays pixel-identical.
            speedDialLabels = if (minimal) emptyMap() else uiState.speedDialLabels,
            onKeyPressed = { digit ->
                viewModel.onKeyPressed(digit)
            },
            onLongPressDigit = { digit ->
                when {
                    digit == '0' -> viewModel.onLongPressZero()
                    digit == '1' -> {
                        // Voicemail: the carrier's number, if any.
                        // getVoiceMailNumber takes an account handle
                        // (there is no zero-arg version); try each
                        // call-capable account, first non-blank wins.
                        // Off when the Speed dial feature is off:
                        // slot 1 is part of the feature.
                        if (speedDialEnabled) {
                            val voicemail = runCatching {
                                val telecom = context.getSystemService(Context.TELECOM_SERVICE)
                                    as? TelecomManager
                                simAccounts.firstNotNullOfOrNull { account ->
                                    runCatching { telecom?.getVoiceMailNumber(account.handle) }
                                        .getOrNull()?.takeIf { it.isNotBlank() }
                                }
                            }.getOrNull()
                            if (voicemail != null) {
                                dialNumber(voicemail)
                            } else {
                                Toast.makeText(
                                    context,
                                    R.string.dialpad_voicemail_unavailable,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    !speedDialEnabled -> {
                        // Feature off: digit long-press does nothing.
                    }
                    uiState.rawNumber.isNotBlank() -> {
                        // Typed number + long-press: assign it, but
                        // NEVER silently overwrite — an assigned digit
                        // asks first ("Replace Maman with 0912...?").
                        val existing = speedDial[digit]?.takeIf { it.isNotBlank() }
                        if (existing != null) {
                            replaceDialogDigit = digit
                            replaceDialogExisting = viewModel.speedDialDisplayName(existing)
                            replaceDialogTyped = uiState.rawNumber
                        } else {
                            coroutineScope.launch {
                                app.container.settingsRepository.setSpeedDial(
                                    speedDial + (digit to uiState.rawNumber)
                                )
                            }
                            Toast.makeText(
                                context,
                                context.getString(
                                    R.string.dialpad_speed_dial_saved,
                                    digit.toString()
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    else -> {
                        // Empty keypad: dial the assigned number, if
                        // any; otherwise open the assign sheet for
                        // this digit.
                        val assigned = speedDial[digit]?.takeIf { it.isNotBlank() }
                        if (assigned != null) {
                            dialNumber(assigned)
                        } else {
                            assignSheetDigit = digit
                        }
                    }
                }
            },
            keySize = if (compactKeys) 64.dp else 72.dp,
            verticalSpacing = if (compactKeys) 8.dp else 12.dp
        )
        DialpadCallButtonRow(
            keyStyle = keyStyle,
            darkTheme = darkTheme,
            onCallClick = {
                if (uiState.canCall) {
                    lightHaptic()
                    val number = uiState.rawNumber
                    if (confirmBeforeCall &&
                        !PhoneNumberUtils.isEmergencyNumber(PhoneNumberFormatter.normalizeDigits(number))
                    ) {
                        pendingConfirmCall = PendingCall(number = number, label = number)
                    } else {
                        callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                    }
                }
            }
        )
    }
    }
}
