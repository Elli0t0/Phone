package io.github.elli0t0.phone.feature.dialpad

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.PhoneNumberUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.DropdownMenu
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
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.rememberLightHaptic
import io.github.elli0t0.phone.core.telecom.CallManager
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
    val coroutineScope = rememberCoroutineScope()

    val simAccounts = remember(context) { io.github.elli0t0.phone.core.telecom.TelecomHelper.getCallCapablePhoneAccounts(context) }
    var simPickerTargetNumber by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var searchVisible by remember { androidx.compose.runtime.mutableStateOf(false) }
    var menuExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }

    fun dialNumber(number: String, lookupKey: String? = null) {
        if (PhoneNumberUtils.isEmergencyNumber(number)) {
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
        DialpadNumberDisplay(
            displayText = uiState.formattedNumber.ifBlank { uiState.rawNumber },
            rawNumber = uiState.rawNumber,
            animationOption = animationOption,
            lightHaptic = lightHaptic,
            onPaste = viewModel::onNumberPasted,
            onBackspacePressed = viewModel::onBackspacePressed,
            onStartBackspaceRepeat = viewModel::startBackspaceRepeat,
            onStopBackspaceRepeat = viewModel::stopBackspaceRepeat
        )
        Spacer(modifier = Modifier.height(spacing.small))
        DialpadKeypadGrid(
            onKeyPressed = { digit ->
                lightHaptic()
                viewModel.onKeyPressed(digit)
            },
            onLongPressZero = {
                lightHaptic()
                viewModel.onLongPressZero()
            }
        )
        DialpadCallButtonRow(
            glassDialpad = glassDialpad,
            darkTheme = darkTheme,
            onCallClick = {
                if (uiState.canCall) {
                    lightHaptic()
                    callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                }
            }
        )
    }
}
