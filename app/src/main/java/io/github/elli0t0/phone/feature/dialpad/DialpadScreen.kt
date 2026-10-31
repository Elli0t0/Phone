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
import io.github.elli0t0.phone.core.design.ConfirmCallSheet
import io.github.elli0t0.phone.core.design.PendingCall
import androidx.compose.material3.DropdownMenuItem
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
            favoritesRepository = app.container.favoritesRepository
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
    val coroutineScope = rememberCoroutineScope()

    val simAccounts = remember(context) { io.github.elli0t0.phone.core.telecom.TelecomHelper.getCallCapablePhoneAccounts(context) }
    var simPickerTargetNumber by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var pendingConfirmCall by remember { androidx.compose.runtime.mutableStateOf<PendingCall?>(null) }
    var searchVisible by remember { androidx.compose.runtime.mutableStateOf(false) }
    var menuExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }

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

    pendingConfirmCall?.let { pending ->
        ConfirmCallSheet(
            pending = pending,
            onConfirm = {
                pendingConfirmCall = null
                lightHaptic()
                callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            },
            onDismiss = { pendingConfirmCall = null }
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
                if (backdropVisible) {
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
        DialpadSuggestionsArea(
            uiState = uiState,
            searchVisible = searchVisible,
            onSearchVisibilityChange = { searchVisible = it },
            animationOption = animationOption,
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
            onFavoriteClick = { contact ->
                lightHaptic()
                val num = contact.primaryNumberPreview ?: ""
                if (num.isNotBlank()) {
                    dialNumber(num, contact.lookupKey)
                }
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
        DialpadKeypadGrid(
            // No tick here: KeypadKey ticks on press-down.
            keypadStyle = LocalAppSettings.current.keypadStyle,
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
                    uiState.rawNumber.isNotBlank() -> {
                        // Typed number + long-press assigns it to
                        // this digit for later speed dialing.
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
                    else -> {
                        // Empty keypad: dial the assigned number, if any.
                        speedDial[digit]?.takeIf { it.isNotBlank() }?.let { dialNumber(it) }
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
