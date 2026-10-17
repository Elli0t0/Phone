package io.github.elli0t0.phone.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.pm.PackageInfoCompat
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.elli0t0.phone.PhoneApplication
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.Motion
import io.github.elli0t0.phone.core.design.pressScale
import io.github.elli0t0.phone.core.design.rememberLightHaptic
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.permissions.SetupPermissions
import io.github.elli0t0.phone.core.telecom.rememberDefaultDialerState
import io.github.elli0t0.phone.core.telecom.UssdHelper
import io.github.elli0t0.phone.core.telecom.UssdSim
import io.github.elli0t0.phone.feature.setup.SetupChecklist
import io.github.elli0t0.phone.data.settings.model.AccentPreset
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import io.github.elli0t0.phone.data.settings.model.BottomBarSize
import io.github.elli0t0.phone.data.settings.model.HeaderSize
import io.github.elli0t0.phone.data.settings.model.BottomBarStyle
import io.github.elli0t0.phone.data.settings.model.CallBackground
import io.github.elli0t0.phone.data.settings.model.CallDisplay
import io.github.elli0t0.phone.data.settings.model.DefaultSim
import io.github.elli0t0.phone.data.settings.model.CardStyle
import io.github.elli0t0.phone.data.settings.model.ContactNameFormat
import io.github.elli0t0.phone.data.settings.model.ContactSortOrder
import io.github.elli0t0.phone.data.settings.model.ListDensity
import io.github.elli0t0.phone.data.settings.model.StartTab
import io.github.elli0t0.phone.data.settings.model.ThemeMode
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.URL
import java.net.UnknownHostException
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as PhoneApplication
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.provideFactory(app.container.settingsRepository)
    )
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val spacing = LocalSpacing.current
    val dialerState = rememberDefaultDialerState()
    val lightHaptic = rememberLightHaptic()

    // Permissions & setup card visibility: hidden once every required permission
    // and the default dialer role are granted. Re-checked on every resume so the
    // card reappears if anything is revoked, and disappears once granted.
    val lifecycleOwner = LocalLifecycleOwner.current
    var requiredSetupComplete by remember {
        mutableStateOf(SetupPermissions.isRequiredSetupComplete(context))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                requiredSetupComplete = SetupPermissions.isRequiredSetupComplete(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showResetDialog by remember { mutableStateOf(false) }
    var showCustomColorDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAccentDialog by remember { mutableStateOf(false) }
    var showDensityDialog by remember { mutableStateOf(false) }
    var showCardStyleDialog by remember { mutableStateOf(false) }
    var showTabDialog by remember { mutableStateOf(false) }
    var showAnimDialog by remember { mutableStateOf(false) }
    var showBottomBarStyleDialog by remember { mutableStateOf(false) }
    var showBottomBarSizeDialog by remember { mutableStateOf(false) }
    var showHeaderSizeDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var showFormatDialog by remember { mutableStateOf(false) }
    var showCallDisplayDialog by remember { mutableStateOf(false) }
    var showCallBackgroundPage by remember { mutableStateOf(false) }
    var showDefaultSimDialog by remember { mutableStateOf(false) }
    var showQuickDeclineDialog by remember { mutableStateOf(false) }
    var showBlockedNumbers by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    // Update check state; null = idle. The state drives the result dialog.
    var updateCheckState by remember { mutableStateOf<UpdateCheckState?>(null) }
    val updateScope = rememberCoroutineScope()
    val currentVersionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }
            .getOrNull()?.versionName ?: ""
    }
    val runUpdateCheck: () -> Unit = {
        updateCheckState = UpdateCheckState.Checking
        updateScope.launch {
            updateCheckState = when (val result = fetchLatestRelease()) {
                is FetchResult.Success -> if (compareVersions(result.release.tag, currentVersionName) > 0) {
                    UpdateCheckState.Available(result.release.tag, result.release.url)
                } else {
                    UpdateCheckState.UpToDate
                }
                FetchResult.RateLimited -> UpdateCheckState.RateLimited
                FetchResult.Offline -> UpdateCheckState.Offline
                FetchResult.Error -> UpdateCheckState.Error
            }
        }
    }
    var showCallWaitingDialog by remember { mutableStateOf(false) }
    // USSD flow: picked code -> SIM picker (dual SIM) -> sending -> result.
    // ussdRequestId invalidates late carrier callbacks after dismissal.
    var pendingUssdCode by remember { mutableStateOf<String?>(null) }
    var ussdSims by remember { mutableStateOf<List<UssdSim>>(emptyList()) }
    var showUssdSimPicker by remember { mutableStateOf(false) }
    var ussdState by remember { mutableStateOf<UssdState?>(null) }
    var ussdRequestId by remember { mutableIntStateOf(0) }
    // The system call-settings screen is a dead row when nothing
    // handles its intent, so it is hidden instead of doing nothing.
    // resolveActivity can throw on some devices, so a failure also
    // hides the row instead of crashing Settings.
    val showSupplementaryRow = remember {
        runCatching {
            Intent(TelecomManager.ACTION_SHOW_CALL_SETTINGS)
                .resolveActivity(context.packageManager) != null
        }.getOrDefault(false)
    }
    val dialUssdFallback: (String) -> Unit = { code ->
        runCatching {
            context.startActivity(
                Intent(
                    Intent.ACTION_DIAL,
                    Uri.parse("tel:${Uri.encode(code)}")
                )
            )
        }
    }
    val runUssd: (String, UssdSim) -> Unit = { code, sim ->
        val requestId = ussdRequestId + 1
        ussdRequestId = requestId
        ussdState = UssdState.Sending(code, sim.label)
        val started = UssdHelper.sendUssdRequest(
            context,
            sim.subscriptionId,
            code,
            onResponse = { response ->
                if (requestId == ussdRequestId) {
                    ussdState = UssdState.Result(response)
                }
            },
            onCarrierError = { reason ->
                if (requestId == ussdRequestId) {
                    ussdState = UssdState.Result(
                        if (reason == TelephonyManager.USSD_ERROR_SERVICE_UNAVAIL) {
                            context.getString(R.string.ussd_error_unavailable)
                        } else {
                            context.getString(R.string.ussd_error_failed)
                        }
                    )
                }
            }
        )
        if (!started) {
            ussdState = null
            dialUssdFallback(code)
        }
    }
    val pickSimForUssd: (String) -> Unit = { code ->
        val sims = UssdHelper.getActiveSims(context)
        when {
            sims.size > 1 -> {
                pendingUssdCode = code
                ussdSims = sims
                showUssdSimPicker = true
            }
            sims.size == 1 -> runUssd(code, sims[0])
            else -> dialUssdFallback(code)
        }
    }
    var customColorHexInput by remember { mutableStateOf("") }
    var customColorError by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(text = stringResource(id = R.string.settings_reset_dialog_title)) },
            text = { Text(text = stringResource(id = R.string.settings_reset_dialog_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onResetToDefaults()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(text = stringResource(id = R.string.settings_reset_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        )
    }

    if (showCustomColorDialog) {
        AlertDialog(
            onDismissRequest = { showCustomColorDialog = false },
            title = { Text(text = stringResource(id = R.string.settings_custom_color_dialog_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = customColorHexInput,
                        onValueChange = {
                            customColorHexInput = it
                            customColorError = false
                        },
                        label = { Text("Hex code (e.g. #0072DE)") },
                        singleLine = true,
                        isError = customColorError
                    )
                    if (customColorError) {
                        Text(
                            text = "Invalid hex color format",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = PhoneDimens.settingsErrorTextTopPadding)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleaned = customColorHexInput.trim().removePrefix("#")
                        try {
                            val parsedColorLong = when (cleaned.length) {
                                6 -> ("FF$cleaned".toLong(16))
                                8 -> cleaned.toLong(16)
                                else -> throw IllegalArgumentException()
                            }
                            viewModel.onAccentColorSelected(parsedColorLong, isCustom = true)
                            showCustomColorDialog = false
                        } catch (e: Exception) {
                            customColorError = true
                        }
                    }
                ) {
                    Text(text = stringResource(id = R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomColorDialog = false }) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        )
    }

    if (showThemeDialog) {
        SettingsChoiceDialog(
            title = stringResource(id = R.string.settings_theme),
            options = ThemeMode.entries.map { it.label to it },
            selected = settings.themeMode,
            onSelect = { viewModel.onThemeModeChanged(it); showThemeDialog = false },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showAccentDialog) {
        AlertDialog(
            onDismissRequest = { showAccentDialog = false },
            title = { Text(text = stringResource(id = R.string.settings_accent_color)) },
            text = {
                Column {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(PhoneDimens.settingsAccentSwatchSpacing),
                        verticalArrangement = Arrangement.spacedBy(PhoneDimens.settingsAccentSwatchSpacing),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AccentPreset.entries.forEach { preset ->
                            val isSelected = !settings.dynamicColor &&
                                !settings.isCustomAccent &&
                                settings.accentColor == preset.colorValue
                            val presetSource = remember { MutableInteractionSource() }
                            Box(
                                modifier = Modifier
                                    .size(PhoneDimens.settingsAccentSwatchSize)
                                    .clip(CircleShape)
                                    .background(Color(preset.colorValue))
                                    .pressScale(presetSource)
                                    .clickable(
                                        interactionSource = presetSource,
                                        indication = ripple()
                                    ) {
                                        lightHaptic()
                                        viewModel.onAccentColorSelected(preset.colorValue, isCustom = false)
                                        showAccentDialog = false
                                    }
                                    .border(
                                        width = if (isSelected) PhoneDimens.settingsAccentSwatchBorderSelected else PhoneDimens.settingsAccentSwatchBorderUnselected,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_check),
                                        contentDescription = preset.label,
                                        tint = Color.White,
                                        modifier = Modifier.size(PhoneDimens.settingsAccentSwatchCheckSize)
                                    )
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                showAccentDialog = false
                                customColorHexInput = String.format("#%06X", 0xFFFFFF and settings.accentColor.toInt())
                                showCustomColorDialog = true
                            },
                            shape = RoundedCornerShape(PhoneDimens.settingsCustomColorButtonCornerRadius),
                            modifier = Modifier.height(PhoneDimens.settingsAccentSwatchSize)
                        ) {
                            Text(
                                text = if (settings.isCustomAccent && !settings.dynamicColor) {
                                    "Custom \u2713"
                                } else {
                                    stringResource(id = R.string.settings_custom_color)
                                },
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAccentDialog = false }) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        )
    }

    if (showDensityDialog) {
        SettingsChoiceDialog(
            title = stringResource(id = R.string.settings_list_density),
            options = ListDensity.entries.map { it.label to it },
            selected = settings.listDensity,
            onSelect = { viewModel.onListDensityChanged(it); showDensityDialog = false },
            onDismiss = { showDensityDialog = false }
        )
    }

    if (showCardStyleDialog) {
        SettingsChoiceDialog(
            title = stringResource(id = R.string.settings_card_style),
            options = CardStyle.entries.map { it.label to it },
            selected = settings.cardStyle,
            onSelect = { viewModel.onCardStyleChanged(it); showCardStyleDialog = false },
            onDismiss = { showCardStyleDialog = false }
        )
    }

    if (showTabDialog) {
        SettingsChoiceDialog(
            title = stringResource(id = R.string.settings_default_tab),
            options = StartTab.entries.map { it.label to it },
            selected = settings.defaultStartTab,
            onSelect = { viewModel.setDefaultStartTab(it); showTabDialog = false },
            onDismiss = { showTabDialog = false }
        )
    }

    if (showAnimDialog) {
        SettingsChoiceDialog(
            title = stringResource(id = R.string.settings_animations),
            options = AnimationOption.entries.map { it.label to it },
            selected = settings.animationOption,
            onSelect = { viewModel.onAnimationOptionChanged(it); showAnimDialog = false },
            onDismiss = { showAnimDialog = false }
        )
    }

    if (showBottomBarStyleDialog) {
        SettingsChoiceDialog(
            title = stringResource(id = R.string.settings_bottom_bar_style),
            options = BottomBarStyle.entries.map { it.label to it },
            selected = settings.bottomBarStyle,
            onSelect = { viewModel.onBottomBarStyleChanged(it); showBottomBarStyleDialog = false },
            onDismiss = { showBottomBarStyleDialog = false }
        )
    }

    if (showBottomBarSizeDialog) {
        SettingsChoiceDialog(
            title = stringResource(id = R.string.settings_bottom_bar_size),
            options = BottomBarSize.entries.map { it.label to it },
            selected = settings.bottomBarSize,
            onSelect = { viewModel.onBottomBarSizeChanged(it); showBottomBarSizeDialog = false },
            onDismiss = { showBottomBarSizeDialog = false }
        )
    }

    if (showHeaderSizeDialog) {
        SettingsChoiceDialog(
            title = stringResource(id = R.string.settings_header_size),
            options = HeaderSize.entries.map { it.label to it },
            selected = settings.headerSize,
            onSelect = { viewModel.onHeaderSizeChanged(it); showHeaderSizeDialog = false },
            onDismiss = { showHeaderSizeDialog = false }
        )
    }

    if (showSortDialog) {
        SettingsChoiceDialog(
            title = stringResource(id = R.string.settings_sort_order),
            options = ContactSortOrder.entries.map { it.label to it },
            selected = settings.contactSortOrder,
            onSelect = { viewModel.onContactSortOrderChanged(it); showSortDialog = false },
            onDismiss = { showSortDialog = false }
        )
    }

    if (showFormatDialog) {
        SettingsChoiceDialog(
            title = stringResource(id = R.string.settings_name_format),
            options = ContactNameFormat.entries.map { it.label to it },
            selected = settings.contactNameFormat,
            onSelect = { viewModel.onContactNameFormatChanged(it); showFormatDialog = false },
            onDismiss = { showFormatDialog = false }
        )
    }

    if (showCallDisplayDialog) {
        SettingsChoiceDialog(
            title = stringResource(id = R.string.settings_call_display),
            options = CallDisplay.entries.map { it.label to it },
            selected = settings.callDisplay,
            onSelect = { viewModel.setCallDisplay(it); showCallDisplayDialog = false },
            onDismiss = { showCallDisplayDialog = false }
        )
    }

    if (showDefaultSimDialog) {
        SettingsChoiceDialog(
            title = stringResource(id = R.string.settings_default_sim),
            options = DefaultSim.entries.map { it.label to it },
            selected = settings.defaultSim,
            onSelect = { viewModel.setDefaultSim(it); showDefaultSimDialog = false },
            onDismiss = { showDefaultSimDialog = false }
        )
    }

    if (showQuickDeclineDialog) {
        val defaultMessages = listOf(
            stringResource(id = R.string.quick_reply_cant_talk),
            stringResource(id = R.string.quick_reply_call_back),
            stringResource(id = R.string.quick_reply_in_meeting)
        )
        val effectiveMessages = defaultMessages.mapIndexed { index, default ->
            settings.quickDeclineOverrides.getOrNull(index)
                ?.takeIf { it.isNotBlank() } ?: default
        }
        QuickDeclineMessagesDialog(
            initialMessages = effectiveMessages,
            onSave = { edited ->
                // Unchanged-from-default entries store blank, so a later
                // locale change still picks up the translated default.
                val overrides = edited.mapIndexed { index, text ->
                    val trimmed = text.trim()
                    if (trimmed == defaultMessages.getOrNull(index)) "" else trimmed
                }
                viewModel.setQuickDeclineMessages(overrides)
                showQuickDeclineDialog = false
            },
            onDismiss = { showQuickDeclineDialog = false }
        )
    }

    if (showAboutDialog) {
        val packageInfo = remember {
            runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
        }
        val versionName = packageInfo?.versionName ?: ""
        val versionCode = packageInfo?.let { PackageInfoCompat.getLongVersionCode(it) } ?: 0L
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(text = stringResource(id = R.string.settings_about_phone)) },
            text = {
                Column {
                    Text(
                        text = stringResource(
                            id = R.string.settings_about_version,
                            versionName,
                            versionCode
                        )
                    )
                    Spacer(modifier = Modifier.height(spacing.small))
                    Text(
                        text = "Checking for updates contacts api.github.com only when you tap it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAboutDialog = false
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/Elli0t0/Phone")
                                )
                            )
                        }
                    }
                ) {
                    Text(text = stringResource(id = R.string.settings_view_source))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        )
    }

    if (updateCheckState != null) {
        val state = updateCheckState
        AlertDialog(
            onDismissRequest = { updateCheckState = null },
            title = { Text(text = "Check for update") },
            text = {
                when (state) {
                    UpdateCheckState.Checking -> {
                        Text(text = "Checking for updates…")
                    }
                    is UpdateCheckState.Available -> {
                        Text(text = "A new version is available: ${state.tag}.")
                    }
                    UpdateCheckState.UpToDate -> {
                        Text(text = "You're up to date ($currentVersionName).")
                    }
                    UpdateCheckState.RateLimited -> {
                        Text(text = "Rate limited by GitHub. Try again later.")
                    }
                    UpdateCheckState.Offline -> {
                        Text(text = "You're offline. Check your connection and try again.")
                    }
                    UpdateCheckState.Error -> {
                        Text(text = "Couldn't check for updates.")
                    }
                    null -> Unit
                }
            },
            confirmButton = {
                when (state) {
                    is UpdateCheckState.Available -> {
                        Button(
                            onClick = {
                                updateCheckState = null
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(state.url))
                                    )
                                }
                            }
                        ) {
                            Text(text = "View release")
                        }
                    }
                    UpdateCheckState.RateLimited,
                    UpdateCheckState.Offline,
                    UpdateCheckState.Error -> {
                        Button(onClick = runUpdateCheck) {
                            Text(text = "Retry")
                        }
                    }
                    else -> {
                        TextButton(onClick = { updateCheckState = null }) {
                            Text(text = stringResource(id = R.string.action_ok))
                        }
                    }
                }
            },
        )
    }

    if (showCallWaitingDialog) {
        CallWaitingDialog(
            onDismiss = { showCallWaitingDialog = false },
            onAction = { code ->
                showCallWaitingDialog = false
                pickSimForUssd(code)
            }
        )
    }

    if (showUssdSimPicker) {
        UssdSimPickerDialog(
            sims = ussdSims,
            onSimSelected = { sim ->
                showUssdSimPicker = false
                pendingUssdCode?.let { runUssd(it, sim) }
                pendingUssdCode = null
            },
            onDismiss = {
                showUssdSimPicker = false
                pendingUssdCode = null
            }
        )
    }

    when (val state = ussdState) {
        is UssdState.Sending -> {
            AlertDialog(
                onDismissRequest = {
                    ussdRequestId += 1
                    ussdState = null
                },
                title = { Text(text = stringResource(id = R.string.settings_call_waiting)) },
                text = { Text(text = stringResource(id = R.string.ussd_sending)) },
                confirmButton = {},
                dismissButton = {
                    TextButton(
                        onClick = {
                            ussdRequestId += 1
                            ussdState = null
                        }
                    ) {
                        Text(text = stringResource(id = R.string.action_cancel))
                    }
                }
            )
        }
        is UssdState.Result -> {
            AlertDialog(
                onDismissRequest = { ussdState = null },
                title = { Text(text = stringResource(id = R.string.ussd_result_title)) },
                text = { Text(text = state.message) },
                confirmButton = {
                    TextButton(onClick = { ussdState = null }) {
                        Text(text = stringResource(id = R.string.action_ok))
                    }
                }
            )
        }
        null -> Unit
    }
    Scaffold(
        modifier = modifier.fillMaxSize().testTag("settings_screen"),
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = spacing.small),
                title = { Text(text = stringResource(id = R.string.settings_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = stringResource(id = R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = spacing.screenHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            item {
                Spacer(modifier = Modifier.height(spacing.extraSmall))
            }

            // Default Phone App Card: hidden once Phone is the default dialer,
            // since there is nothing left to do here.
            if (!dialerState.isDefault) {
            item {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("default_phone_app_card")
                ) {
                    Column(modifier = Modifier.padding(spacing.medium)) {
                        Text(
                            text = stringResource(id = R.string.settings_default_phone_app),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(spacing.extraSmall))
                        Text(
                            text = stringResource(id = R.string.settings_default_phone_app_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(spacing.medium))

                        Button(
                            onClick = dialerState.requestDefaultDialer,
                            modifier = Modifier.testTag("set_as_default_button")
                        ) {
                            Text(text = stringResource(id = R.string.settings_set_as_default))
                        }
                    }
                }
            }
            }

            // Permissions & setup: shown only while a required permission or the
            // default dialer role is missing.
            if (!requiredSetupComplete) {
                item {
                    SettingsSectionHeader(title = stringResource(id = R.string.settings_permissions_setup_title))
                }

                item {
                    Card(
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(spacing.medium)) {
                            Text(
                                text = stringResource(id = R.string.settings_permissions_setup_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(spacing.medium))
                            SetupChecklist(
                                modifier = Modifier.fillMaxWidth(),
                                scrollable = false
                            )
                        }
                    }
                }
            }

            // Category 1: Appearance & Display
            item {
                SettingsSectionHeader(title = stringResource(id = R.string.settings_cat_appearance))
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(spacing.medium)) {
                        SettingsRow(
                            title = stringResource(id = R.string.settings_theme),
                            subtitle = settings.themeMode.label,
                            onClick = { showThemeDialog = true }
                        )

                        Spacer(modifier = Modifier.height(spacing.small))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(spacing.small))

                        // Dynamic Color (Android 12+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            SettingsSwitchRow(
                                title = stringResource(id = R.string.settings_dynamic_color),
                                subtitle = stringResource(id = R.string.settings_dynamic_color_desc),
                                checked = settings.dynamicColor,
                                onCheckedChange = { viewModel.onDynamicColorChanged(it) }
                            )
                            Spacer(modifier = Modifier.height(spacing.small))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(spacing.small))
                        }

                        SettingsRow(
                            title = stringResource(id = R.string.settings_accent_color),
                            subtitle = AccentPreset.entries.find { it.colorValue == settings.accentColor }?.label
                                ?: stringResource(id = R.string.settings_custom_color),
                            onClick = { showAccentDialog = true }
                        )

                        Spacer(modifier = Modifier.height(spacing.small))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(spacing.small))

                        SettingsSwitchRow(
                            title = stringResource(id = R.string.settings_pure_black),
                            subtitle = stringResource(id = R.string.settings_pure_black_desc),
                            checked = settings.pureBlackDarkMode,
                            onCheckedChange = { viewModel.onPureBlackDarkModeChanged(it) }
                        )

                        Spacer(modifier = Modifier.height(spacing.small))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(spacing.small))

                        // Font size scale
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(id = R.string.settings_font_scale),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                            Text(
                                text = "${(settings.fontSizeScale * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        // Font size scale: thin 4dp track, vertical-bar thumb,
                        // small tick marks at each stop. Value-based Slider
                        // with thumb/track slots (Material3 1.3.0 overload).
                        Slider(
                            value = settings.fontSizeScale,
                            onValueChange = { viewModel.onFontSizeScaleChanged((it * 20).roundToInt() / 20f) },
                            valueRange = 0.85f..1.30f,
                            steps = 8,
                            thumb = {
                                Box(
                                    modifier = Modifier
                                        .size(
                                            width = PhoneDimens.settingsSliderThumbWidth,
                                            height = PhoneDimens.settingsSliderThumbHeight
                                        )
                                        .clip(RoundedCornerShape(PhoneDimens.settingsSliderCornerRadius))
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            },
                            track = { sliderState ->
                                val tickCount = 10
                                val fraction = (
                                    sliderState.value - sliderState.valueRange.start
                                    ) / (sliderState.valueRange.endInclusive - sliderState.valueRange.start)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(PhoneDimens.settingsSliderTrackHeight)
                                        .clip(RoundedCornerShape(PhoneDimens.settingsSliderCornerRadius))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction)
                                            .height(PhoneDimens.settingsSliderTrackHeight)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        repeat(tickCount) {
                                            Box(
                                                modifier = Modifier
                                                    .size(PhoneDimens.settingsSliderTickSize)
                                                    .clip(CircleShape)
                                                    .background(
                                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(spacing.small))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(spacing.small))

                        SettingsRow(
                            title = stringResource(id = R.string.settings_header_size),
                            subtitle = settings.headerSize.label,
                            onClick = { showHeaderSizeDialog = true }
                        )
                    }
                }
            }

            // Liquid glass: per-section on/off switches.
            item {
                SettingsSectionHeader(title = stringResource(id = R.string.settings_cat_liquid_glass))
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(spacing.medium)) {
                        SettingsSwitchRow(
                            title = stringResource(id = R.string.settings_glass_bottom_bar),
                            subtitle = stringResource(id = R.string.settings_glass_bottom_bar_desc),
                            checked = settings.glassBottomBar,
                            onCheckedChange = { viewModel.onGlassBottomBarChanged(it) }
                        )

                        Spacer(modifier = Modifier.height(spacing.small))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(spacing.small))

                        SettingsSwitchRow(
                            title = stringResource(id = R.string.settings_glass_dialpad),
                            subtitle = stringResource(id = R.string.settings_glass_dialpad_desc),
                            checked = settings.glassDialpad,
                            onCheckedChange = { viewModel.onGlassDialpadChanged(it) }
                        )

                        Spacer(modifier = Modifier.height(spacing.small))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(spacing.small))

                        SettingsSwitchRow(
                            title = stringResource(id = R.string.settings_glass_headers),
                            subtitle = stringResource(id = R.string.settings_glass_headers_desc),
                            checked = settings.glassHeaders,
                            onCheckedChange = { viewModel.onGlassHeadersChanged(it) }
                        )

                        Spacer(modifier = Modifier.height(spacing.small))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(spacing.small))

                        SettingsSwitchRow(
                            title = stringResource(id = R.string.settings_glass_contact_detail),
                            subtitle = stringResource(id = R.string.settings_glass_contact_detail_desc),
                            checked = settings.glassContactDetail,
                            onCheckedChange = { viewModel.onGlassContactDetailChanged(it) }
                        )

                        Spacer(modifier = Modifier.height(spacing.small))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(spacing.small))

                        SettingsSwitchRow(
                            title = stringResource(id = R.string.settings_glass_call_screens),
                            subtitle = stringResource(id = R.string.settings_glass_call_screens_desc),
                            checked = settings.glassCallScreens,
                            onCheckedChange = { viewModel.onGlassCallScreensChanged(it) }
                        )
                    }
                }
            }

            // Category 2: Layout & Density
            item {
                SettingsSectionHeader(title = stringResource(id = R.string.settings_cat_layout))
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(spacing.medium)) {
                        SettingsRow(
                            title = stringResource(id = R.string.settings_list_density),
                            subtitle = settings.listDensity.label,
                            onClick = { showDensityDialog = true }
                        )

                        Spacer(modifier = Modifier.height(spacing.small))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(spacing.small))

                        SettingsRow(
                            title = stringResource(id = R.string.settings_card_style),
                            subtitle = settings.cardStyle.label,
                            onClick = { showCardStyleDialog = true }
                        )
                    }
                }
            }

            // Category 3: Navigation & Behavior
            item {
                SettingsSectionHeader(title = stringResource(id = R.string.settings_cat_navigation))
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(spacing.medium)) {
                        SettingsRow(
                            title = stringResource(id = R.string.settings_default_tab),
                            subtitle = settings.defaultStartTab.label,
                            onClick = { showTabDialog = true }
                        )

                        Spacer(modifier = Modifier.height(spacing.small))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(spacing.small))

                        SettingsSwitchRow(
                            title = stringResource(id = R.string.settings_bottom_labels),
                            subtitle = stringResource(id = R.string.settings_bottom_labels_desc),
                            checked = settings.showBottomBarLabels,
                            onCheckedChange = { viewModel.onShowBottomBarLabelsChanged(it) }
                        )

                        Spacer(modifier = Modifier.height(spacing.small))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(spacing.small))

                        SettingsRow(
                            title = stringResource(id = R.string.settings_bottom_bar_style),
                            subtitle = settings.bottomBarStyle.label,
                            onClick = { showBottomBarStyleDialog = true }
                        )

                        Spacer(modifier = Modifier.height(spacing.small))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(spacing.small))

                        SettingsRow(
                            title = stringResource(id = R.string.settings_bottom_bar_size),
                            subtitle = settings.bottomBarSize.label,
                            onClick = { showBottomBarSizeDialog = true }
                        )

                        Spacer(modifier = Modifier.height(spacing.small))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(spacing.small))

                        SettingsSwitchRow(
                            title = stringResource(id = R.string.settings_bottom_bar_shrink),
                            subtitle = stringResource(id = R.string.settings_bottom_bar_shrink_desc),
                            checked = settings.shrinkBottomBarOnScroll,
                            onCheckedChange = { viewModel.onShrinkBottomBarOnScrollChanged(it) }
                        )

                        Spacer(modifier = Modifier.height(spacing.small))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(spacing.small))

                        SettingsRow(
                            title = stringResource(id = R.string.settings_animations),
                            subtitle = settings.animationOption.label,
                            onClick = { showAnimDialog = true }
                        )
                    }
                }
            }

            // Category 4: Contacts Display
            item {
                SettingsSectionHeader(title = stringResource(id = R.string.settings_cat_contacts))
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(spacing.medium)) {
                        SettingsRow(
                            title = stringResource(id = R.string.settings_sort_order),
                            subtitle = settings.contactSortOrder.label,
                            onClick = { showSortDialog = true }
                        )

                        Spacer(modifier = Modifier.height(spacing.small))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(spacing.small))

                        SettingsRow(
                            title = stringResource(id = R.string.settings_name_format),
                            subtitle = settings.contactNameFormat.label,
                            onClick = { showFormatDialog = true }
                        )
                    }
                }
            }

            // Calls
            item {
                SettingsSectionHeader(title = stringResource(id = R.string.settings_cat_calls))
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(spacing.medium)) {
                        SettingsRow(
                            title = stringResource(id = R.string.settings_block_numbers),
                            onClick = { showBlockedNumbers = true }
                        )

                        if (showSupplementaryRow) {
                            Spacer(modifier = Modifier.height(spacing.small))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(spacing.small))

                            SettingsRow(
                                title = stringResource(id = R.string.settings_supplementary),
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(TelecomManager.ACTION_SHOW_CALL_SETTINGS)
                                        )
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(spacing.small))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(spacing.small))

                        // Call waiting is a carrier supplementary service:
                        // the MMI code is dialed and the carrier executes it.
                        SettingsRow(
                            title = stringResource(id = R.string.settings_call_waiting),
                            onClick = { showCallWaitingDialog = true }
                        )

                        Spacer(modifier = Modifier.height(spacing.small))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(spacing.small))

                        SettingsRow(
                            title = stringResource(id = R.string.settings_call_display),
                            subtitle = settings.callDisplay.label,
                            onClick = { showCallDisplayDialog = true }
                        )

                        Spacer(modifier = Modifier.height(spacing.small))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(spacing.small))

                        SettingsRow(
                            title = stringResource(id = R.string.settings_call_background),
                            subtitle = settings.callBackground.label,
                            onClick = { showCallBackgroundPage = true }
                        )

                        Spacer(modifier = Modifier.height(spacing.small))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(spacing.small))

                        SettingsRow(
                            title = stringResource(id = R.string.settings_default_sim),
                            subtitle = settings.defaultSim.label,
                            onClick = { showDefaultSimDialog = true }
                        )

                        Spacer(modifier = Modifier.height(spacing.small))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(spacing.small))

                        SettingsRow(
                            title = stringResource(id = R.string.settings_quick_decline),
                            onClick = { showQuickDeclineDialog = true }
                        )
                    }
                }
            }

            // Privacy
            item {
                SettingsSectionHeader(title = stringResource(id = R.string.settings_cat_privacy))
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(spacing.medium)) {
                        SettingsRow(
                            title = stringResource(id = R.string.settings_permissions),
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.fromParts("package", context.packageName, null)
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // About
            item {
                SettingsSectionHeader(title = stringResource(id = R.string.settings_cat_about))
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(spacing.medium)) {
                        SettingsRow(
                            title = stringResource(id = R.string.settings_about_phone),
                            onClick = { showAboutDialog = true }
                        )
                        SettingsRow(
                            title = "Check for update",
                            onClick = runUpdateCheck
                        )
                    }
                }
            }

            // Category 5: Reset
            item {
                SettingsSectionHeader(title = stringResource(id = R.string.settings_cat_reset))
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(spacing.medium)) {
                        SettingsRow(
                            title = stringResource(id = R.string.settings_reset_button),
                            titleColor = MaterialTheme.colorScheme.error,
                            onClick = { showResetDialog = true }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(spacing.large))
            }
        }
    }

    // Full-screen pages, emitted after the list Scaffold so they draw
    // on top of it. Emitted before the Scaffold, they composed behind
    // the list, so tapping the row visibly did nothing. System Back
    // closes the open page instead of leaving Settings.
    BackHandler(enabled = showCallBackgroundPage) { showCallBackgroundPage = false }
    BackHandler(enabled = showBlockedNumbers) { showBlockedNumbers = false }

    if (showCallBackgroundPage) {
        CallBackgroundScreen(
            selected = settings.callBackground,
            accentColor = settings.accentColor,
            onSelect = { viewModel.setCallBackground(it) },
            onNavigateBack = { showCallBackgroundPage = false }
        )
    }

    if (showBlockedNumbers) {
        BlockedNumbersScreen(onNavigateBack = { showBlockedNumbers = false })
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = PhoneDimens.settingsSectionHeaderVerticalPadding)
    )
}

// Settings row: title with the current value as a subtitle.
// Tapping the row opens the picker dialog for that setting.
@Composable
private fun SettingsRow(
    title: String,
    subtitle: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    val rowSource = remember { MutableInteractionSource() }
    val lightHaptic = rememberLightHaptic()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(rowSource)
            .clickable(
                interactionSource = rowSource,
                indication = ripple()
            ) { lightHaptic(); onClick() }
            .padding(vertical = PhoneDimens.settingsRowVerticalPadding)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = titleColor
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Switch row: title + description on the left, switch on the right.
@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = PhoneDimens.settingsSwitchRowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = PhoneDimens.settingsSwitchRowTextEndPadding)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        SettingsSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

// Custom switch: wide 52x28dp track with a large white round thumb.
// Material3 Switch has a fixed size, so this draws the exact track/thumb.
@Composable
private fun SettingsSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    // Thumb slide respects the Animations setting: ON keeps the
    // default spring, REDUCED shortens to a fade-speed tween,
    // OFF snaps instantly.
    val animationOption = LocalAppSettings.current.animationOption
    val thumbSpec: AnimationSpec<Dp> = when (animationOption) {
        AnimationOption.ON -> spring(stiffness = Spring.StiffnessMediumLow)
        AnimationOption.REDUCED -> tween(
            durationMillis = Motion.scaledDurationMs(200, animationOption),
            easing = Motion.Easings.Standard
        )
        AnimationOption.OFF -> snap()
    }
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) {
            PhoneDimens.settingsSwitchWidth - PhoneDimens.settingsSwitchThumbSize -
                PhoneDimens.settingsSwitchThumbPadding * 2
        } else {
            PhoneDimens.settingsSwitchThumbPadding
        },
        animationSpec = thumbSpec
    )
    Box(
        modifier = Modifier
            .size(
                width = PhoneDimens.settingsSwitchWidth,
                height = PhoneDimens.settingsSwitchHeight
            )
            .clip(RoundedCornerShape(PhoneDimens.settingsSwitchCornerPercent))
            .background(
                if (checked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(PhoneDimens.settingsSwitchThumbSize)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

// Radio-option picker dialog used by every choice setting.
@Composable
private fun <T> SettingsChoiceDialog(
    title: String,
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    val lightHaptic = rememberLightHaptic()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column {
                options.forEach { (label, value) ->
                    val optionSource = remember { MutableInteractionSource() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressScale(optionSource)
                            .clickable(
                                interactionSource = optionSource,
                                indication = ripple()
                            ) { lightHaptic(); onSelect(value) }
                            .padding(vertical = PhoneDimens.settingsRowVerticalPadding),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = value == selected,
                            onClick = { onSelect(value) }
                        )
                        Spacer(modifier = Modifier.width(PhoneDimens.settingsChoiceOptionGap))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.action_cancel))
            }
        }
    )
}

// USSD request state for the Call waiting flow.
private sealed interface UssdState {
    data class Sending(val code: String, val simLabel: String) : UssdState
    data class Result(val message: String) : UssdState
}

// Call waiting dialog: title, description, three full-width actions,
// Cancel bottom-right. Tapping an action runs its MMI code on the
// chosen SIM (SIM picker first on dual SIM).
@Composable
private fun CallWaitingDialog(
    onDismiss: () -> Unit,
    onAction: (String) -> Unit
) {
    val spacing = LocalSpacing.current
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(spacing.large)) {
                Text(
                    text = stringResource(id = R.string.settings_call_waiting),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(spacing.medium))
                Text(
                    text = stringResource(id = R.string.call_waiting_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(spacing.medium))
                FilledTonalButton(
                    onClick = { onAction("*43#") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.call_waiting_activate))
                }
                Spacer(modifier = Modifier.height(spacing.small))
                FilledTonalButton(
                    onClick = { onAction("#43#") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.call_waiting_deactivate))
                }
                Spacer(modifier = Modifier.height(spacing.small))
                FilledTonalButton(
                    onClick = { onAction("*#43#") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.call_waiting_check))
                }
                Spacer(modifier = Modifier.height(spacing.medium))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(text = stringResource(id = R.string.action_cancel))
                    }
                }
            }
        }
    }
}

// SIM picker for the USSD flow: one row per active SIM, Cancel dismisses.
@Composable
private fun UssdSimPickerDialog(
    sims: List<UssdSim>,
    onSimSelected: (UssdSim) -> Unit,
    onDismiss: () -> Unit
) {
    val spacing = LocalSpacing.current
    val lightHaptic = rememberLightHaptic()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.call_sim_picker_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                sims.forEach { sim ->
                    val simSource = remember { MutableInteractionSource() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(PhoneDimens.settingsSimRowCornerRadius))
                            .pressScale(simSource)
                            .clickable(
                                interactionSource = simSource,
                                indication = ripple()
                            ) { lightHaptic(); onSimSelected(sim) }
                            .padding(
                                horizontal = spacing.small,
                                vertical = spacing.medium
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_sim_card),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(PhoneDimens.settingsSimRowIconSize)
                        )
                        Spacer(modifier = Modifier.width(spacing.medium))
                        Text(
                            text = sim.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.action_cancel))
            }
        }
    )
}

// Editor for the three quick-decline messages shown on the incoming call
// screen. Blank entries fall back to the built-in defaults.
@Composable
private fun QuickDeclineMessagesDialog(
    initialMessages: List<String>,
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var message1 by remember { mutableStateOf(initialMessages.getOrNull(0) ?: "") }
    var message2 by remember { mutableStateOf(initialMessages.getOrNull(1) ?: "") }
    var message3 by remember { mutableStateOf(initialMessages.getOrNull(2) ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.settings_quick_decline)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(PhoneDimens.settingsChoiceOptionGap)) {
                OutlinedTextField(
                    value = message1,
                    onValueChange = { message1 = it },
                    label = { Text(text = "1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = message2,
                    onValueChange = { message2 = it },
                    label = { Text(text = "2") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = message3,
                    onValueChange = { message3 = it },
                    label = { Text(text = "3") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(listOf(message1, message2, message3)) }) {
                Text(text = stringResource(id = R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.action_cancel))
            }
        }
    )
}

/** Result of an update check, driving the About update dialog. */
private sealed interface UpdateCheckState {
    data object Checking : UpdateCheckState
    data class Available(val tag: String, val url: String) : UpdateCheckState
    data object UpToDate : UpdateCheckState
    data object RateLimited : UpdateCheckState
    data object Offline : UpdateCheckState
    data object Error : UpdateCheckState
}

private data class LatestRelease(val tag: String, val url: String)

private sealed interface FetchResult {
    data class Success(val release: LatestRelease) : FetchResult
    data object RateLimited : FetchResult
    data object Offline : FetchResult
    data object Error : FetchResult
}

// Newest release from the GitHub API. Pre-releases are included (the app
// ships pre-releases), so the releases list — newest first — is used
// instead of /releases/latest. The connection is always disconnected,
// and rate limiting (403/429) and offline are reported distinctly.
private suspend fun fetchLatestRelease(): FetchResult = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
        connection = URL("https://api.github.com/repos/Elli0t0/Phone/releases?per_page=1")
            .openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "Phone-App")
        connection.connectTimeout = 5000
        connection.readTimeout = 5000
        when (connection.responseCode) {
            HttpURLConnection.HTTP_OK -> {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val release = JSONArray(body).optJSONObject(0) ?: return@withContext FetchResult.Error
                FetchResult.Success(
                    LatestRelease(
                        tag = release.getString("tag_name").removePrefix("v"),
                        url = release.getString("html_url")
                    )
                )
            }
            HttpURLConnection.HTTP_FORBIDDEN, 429 -> FetchResult.RateLimited
            else -> FetchResult.Error
        }
    } catch (_: UnknownHostException) {
        FetchResult.Offline
    } catch (_: ConnectException) {
        FetchResult.Offline
    } catch (_: Exception) {
        FetchResult.Error
    } finally {
        connection?.disconnect()
    }
}

// Dotted version comparison: > 0 when [a] is newer than [b].
private fun compareVersions(a: String, b: String): Int {
    val ap = a.removePrefix("v").split(".")
    val bp = b.removePrefix("v").split(".")
    for (i in 0 until maxOf(ap.size, bp.size)) {
        val d = (ap.getOrNull(i)?.toIntOrNull() ?: 0) - (bp.getOrNull(i)?.toIntOrNull() ?: 0)
        if (d != 0) return d
    }
    return 0
}
