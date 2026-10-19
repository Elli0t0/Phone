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
import io.github.elli0t0.phone.data.settings.model.GlassIntensity
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
    var showGlassIntensityDialog by remember { mutableStateOf(false) }
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
                        label = { Text(stringResource(id = R.string.settings_hex_code)) },
                        singleLine = true,
                        isError = customColorError
                    )
                    if (customColorError) {
                        Text(
                            text = stringResource(id = R.string.settings_invalid_hex),
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

    if (showGlassIntensityDialog) {
        SettingsChoiceDialog(
            title = stringResource(id = R.string.settings_glass_intensity),
            options = GlassIntensity.entries.map { it.label to it },
            selected = settings.glassIntensity,
            onSelect = { viewModel.onGlassIntensityChanged(it); showGlassIntensityDialog = false },
            onDismiss = { showGlassIntensityDialog = false }
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
                        text = stringResource(id = R.string.settings_about_update_note),
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
            title = { Text(text = stringResource(id = R.string.update_check_title)) },
            text = {
                when (state) {
                    UpdateCheckState.Checking -> {
                        Text(text = stringResource(id = R.string.update_check_checking))
                    }
                    is UpdateCheckState.Available -> {
                        Text(text = stringResource(id = R.string.update_check_available, state.tag))
                    }
                    UpdateCheckState.UpToDate -> {
                        Text(text = stringResource(id = R.string.update_check_up_to_date, currentVersionName))
                    }
                    UpdateCheckState.RateLimited -> {
                        Text(text = stringResource(id = R.string.update_check_rate_limited))
                    }
                    UpdateCheckState.Offline -> {
                        Text(text = stringResource(id = R.string.update_check_offline))
                    }
                    UpdateCheckState.Error -> {
                        Text(text = stringResource(id = R.string.update_check_error))
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
                            Text(text = stringResource(id = R.string.update_check_view_release))
                        }
                    }
                    UpdateCheckState.RateLimited,
                    UpdateCheckState.Offline,
                    UpdateCheckState.Error -> {
                        Button(onClick = runUpdateCheck) {
                            Text(text = stringResource(id = R.string.update_check_retry))
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

                        Spacer(modifier = Modifier.height(spacing.small))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(spacing.small))

                        SettingsRow(
                            title = stringResource(id = R.string.settings_glass_intensity),
                            subtitle = settings.glassIntensity.label,
                            onClick = { showGlassIntensityDialog = true }
                        )

                        Spacer(modifier = Modifier.height(spacing.small))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(spacing.small))

                        SettingsSwitchRow(
                            title = stringResource(id = R.string.settings_reduce_transparency),
                            subtitle = stringResource(id = R.string.settings_reduce_transparency_desc),
                            checked = settings.reduceTransparency,
                            onCheckedChange = { viewModel.onReduceTransparencyChanged(it) }
                        )

                        Spacer(modifier = Modifier.height(spacing.small))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(spacing.small))

                        SettingsSwitchRow(
                            title = stringResource(id = R.string.settings_glass_cards),
                            subtitle = stringResource(id = R.string.settings_glass_cards_desc),
                            checked = settings.glassCards,
                            onCheckedChange = { viewModel.onGlassCardsChanged(it) }
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
                            title = stringResource(id = R.string.update_check_title),
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
