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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.LaunchedEffect
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
import io.github.elli0t0.phone.core.recording.BackendStatus
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.Motion
import io.github.elli0t0.phone.core.design.pressScale
import io.github.elli0t0.phone.core.design.rememberLightHaptic
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.bottomBarListBottomPadding
import io.github.elli0t0.phone.core.permissions.SetupPermissions
import io.github.elli0t0.phone.core.telecom.rememberDefaultDialerState
import io.github.elli0t0.phone.core.telecom.UssdHelper
import io.github.elli0t0.phone.core.telecom.UssdSim
import io.github.elli0t0.phone.feature.recording.RecordingSettingsScreen
import io.github.elli0t0.phone.feature.setup.SetupChecklist
import io.github.elli0t0.phone.data.settings.model.AccentPreset
import io.github.elli0t0.phone.data.settings.model.Backdrop
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

    val dialogs = remember { SettingsDialogState() }

    // Live recording status for the main Call recording row (moved
    // from the Calls card): the backend is built-in ADB, the status
    // from the controller. The row refreshes the status on entry, so
    // the subtitle is current, not the last probe from another screen.
    val recordingController =
        (context.applicationContext as PhoneApplication).container.recordingController
    val recordingStatus by recordingController.backendStatus.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { recordingController.refreshBackendStatus() }
    val recordingBackendLabel = stringResource(id = R.string.recording_backend_adb)
    val recordingStatusWord = when (recordingStatus) {
        BackendStatus.READY -> stringResource(id = R.string.recording_status_ready)
        BackendStatus.NOT_PAIRED -> stringResource(id = R.string.recording_status_not_paired_short)
        BackendStatus.NOT_RUNNING -> stringResource(id = R.string.recording_status_needs_wifi)
        else -> null
    }
    val recordingSubtitle = if (recordingStatusWord != null) {
        "$recordingBackendLabel · $recordingStatusWord"
    } else {
        recordingBackendLabel
    }

    // Main-page row summaries: the current value of each section.
    val accentLabel = AccentPreset.entries.find { it.colorValue == settings.accentColor }?.label
        ?: stringResource(id = R.string.settings_custom_color)
    val appearanceSubtitle = "${settings.appStyle.label} · ${settings.themeMode.label} · $accentLabel"
    val glassSubtitle = "${settings.glassIntensity.label} · " +
        if (settings.pureBlackDarkMode) {
            Backdrop.OFF.label
        } else {
            settings.backdrop.label
        }
    val layoutNavSubtitle = "${settings.listDensity.label} · ${settings.defaultStartTab.label}"
    val callsSubtitle = "${settings.callDisplay.label} · ${settings.answerStyle.label}"
    val contactsSubtitle = "${settings.contactSortOrder.label} · ${settings.contactNameFormat.label}"
    val privacySubtitle = stringResource(
        id = if (requiredSetupComplete) {
            R.string.settings_privacy_granted
        } else {
            R.string.settings_privacy_action_needed
        }
    )

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

    SettingsDialogs(
        settings = settings,
        viewModel = viewModel,
        dialogs = dialogs,
        updateCheckState = updateCheckState,
        runUpdateCheck = runUpdateCheck,
        currentVersionName = currentVersionName,
        lightHaptic = lightHaptic
    )
    SettingsDialogsAbout(
        settings = settings,
        viewModel = viewModel,
        dialogs = dialogs,
        updateCheckState = updateCheckState,
        onUpdateCheckStateChange = { updateCheckState = it },
        runUpdateCheck = runUpdateCheck,
        currentVersionName = currentVersionName
    )
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
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
            contentPadding = PaddingValues(bottom = bottomBarListBottomPadding())
        ) {
            item {
                Spacer(modifier = Modifier.height(spacing.extraSmall))
            }
            // Default Phone App Card: hidden once Phone is the default dialer,
            // since there is nothing left to do here.
            if (!dialerState.isDefault) {
            item {
                Card(
                    shape = settingsListCardShape(),
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
                        shape = settingsListCardShape(),
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
            item {
                Card(
                    shape = settingsListCardShape(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(spacing.medium)) {
                        SettingsNavRow(
                            iconRes = R.drawable.ic_settings,
                            title = stringResource(id = R.string.settings_cat_appearance),
                            subtitle = appearanceSubtitle,
                            onClick = { dialogs.showAppearancePage = true }
                        )

                        Spacer(modifier = Modifier.height(spacing.small))
                        SettingsListDivider()
                        Spacer(modifier = Modifier.height(spacing.small))

                        SettingsNavRow(
                            iconRes = R.drawable.ic_filter,
                            title = stringResource(id = R.string.settings_cat_liquid_glass),
                            subtitle = glassSubtitle,
                            onClick = { dialogs.showGlassPage = true }
                        )

                        Spacer(modifier = Modifier.height(spacing.small))
                        SettingsListDivider()
                        Spacer(modifier = Modifier.height(spacing.small))

                        SettingsNavRow(
                            iconRes = R.drawable.ic_fullscreen,
                            title = stringResource(id = R.string.settings_cat_layout_nav),
                            subtitle = layoutNavSubtitle,
                            onClick = { dialogs.showLayoutNavPage = true }
                        )

                        Spacer(modifier = Modifier.height(spacing.small))
                        SettingsListDivider()
                        Spacer(modifier = Modifier.height(spacing.small))

                        SettingsNavRow(
                            iconRes = R.drawable.ic_phone,
                            title = stringResource(id = R.string.settings_cat_calls),
                            subtitle = callsSubtitle,
                            onClick = { dialogs.showCallsPage = true }
                        )

                        Spacer(modifier = Modifier.height(spacing.small))
                        SettingsListDivider()
                        Spacer(modifier = Modifier.height(spacing.small))

                        SettingsNavRow(
                            iconRes = R.drawable.ic_contacts,
                            title = stringResource(id = R.string.settings_cat_contacts),
                            subtitle = contactsSubtitle,
                            onClick = { dialogs.showContactsPage = true }
                        )

                        Spacer(modifier = Modifier.height(spacing.small))
                        SettingsListDivider()
                        Spacer(modifier = Modifier.height(spacing.small))

                        SettingsNavRow(
                            iconRes = R.drawable.ic_record,
                            title = stringResource(id = R.string.settings_call_recording),
                            subtitle = recordingSubtitle,
                            onClick = { dialogs.showRecordingPage = true }
                        )

                        Spacer(modifier = Modifier.height(spacing.small))
                        SettingsListDivider()
                        Spacer(modifier = Modifier.height(spacing.small))

                        SettingsNavRow(
                            iconRes = R.drawable.ic_block,
                            title = stringResource(id = R.string.settings_cat_privacy),
                            subtitle = privacySubtitle,
                            onClick = { dialogs.showPrivacyPage = true }
                        )

                        Spacer(modifier = Modifier.height(spacing.small))
                        SettingsListDivider()
                        Spacer(modifier = Modifier.height(spacing.small))

                        SettingsNavRow(
                            iconRes = R.drawable.ic_info,
                            title = stringResource(id = R.string.settings_cat_about),
                            subtitle = currentVersionName,
                            onClick = { dialogs.showAboutPage = true }
                        )
                    }
                }
            }
            item {
                Card(
                    shape = settingsListCardShape(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(spacing.medium)) {
                        SettingsSwitchRow(
                            title = stringResource(id = R.string.settings_show_advanced),
                            subtitle = stringResource(id = R.string.settings_show_advanced_desc),
                            checked = settings.showAdvancedSettings,
                            onCheckedChange = { viewModel.onShowAdvancedSettingsChanged(it) }
                        )
                    }
                }
            }
            item {
                ResetCard(dialogs = dialogs, spacing = spacing)
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
    BackHandler(enabled = dialogs.showCallBackgroundPage) { dialogs.showCallBackgroundPage = false }
    BackHandler(enabled = dialogs.showAnswerStylePage) { dialogs.showAnswerStylePage = false }
    BackHandler(enabled = dialogs.showAppStylePage) { dialogs.showAppStylePage = false }
    BackHandler(enabled = dialogs.showGlassStudioPage) { dialogs.showGlassStudioPage = false }
    BackHandler(enabled = dialogs.showBlockedNumbers) { dialogs.showBlockedNumbers = false }
    BackHandler(enabled = dialogs.showRecordingPage) { dialogs.showRecordingPage = false }

    if (dialogs.showAnswerStylePage) {
        AnswerStyleScreen(
            selected = settings.answerStyle,
            sliderOnlyWhenLocked = settings.sliderOnlyWhenLocked,
            onSelect = { viewModel.onAnswerStyleChanged(it) },
            onSliderOnlyWhenLockedChange = { viewModel.onSliderOnlyWhenLockedChanged(it) },
            onNavigateBack = { dialogs.showAnswerStylePage = false }
        )
    }

    if (dialogs.showAppStylePage) {
        AppStyleScreen(
            settings = settings,
            viewModel = viewModel,
            onNavigateBack = { dialogs.showAppStylePage = false }
        )
    }

    if (dialogs.showGlassStudioPage) {
        GlassStudioScreen(
            settings = settings,
            viewModel = viewModel,
            onNavigateBack = { dialogs.showGlassStudioPage = false }
        )
    }

    if (dialogs.showCallBackgroundPage) {
        CallBackgroundScreen(
            selected = settings.callBackground,
            accentColor = settings.accentColor,
            onSelect = { viewModel.setCallBackground(it) },
            onNavigateBack = { dialogs.showCallBackgroundPage = false }
        )
    }

    if (dialogs.showBlockedNumbers) {
        BlockedNumbersScreen(onNavigateBack = { dialogs.showBlockedNumbers = false })
    }

    if (dialogs.showRecordingPage) {
        RecordingSettingsScreen(onNavigateBack = { dialogs.showRecordingPage = false })
    }

    BackHandler(enabled = dialogs.showAppearancePage) { dialogs.showAppearancePage = false }
    BackHandler(enabled = dialogs.showGlassPage) { dialogs.showGlassPage = false }
    BackHandler(enabled = dialogs.showLayoutNavPage) { dialogs.showLayoutNavPage = false }
    BackHandler(enabled = dialogs.showCallsPage) { dialogs.showCallsPage = false }
    BackHandler(enabled = dialogs.showContactsPage) { dialogs.showContactsPage = false }
    BackHandler(enabled = dialogs.showPrivacyPage) { dialogs.showPrivacyPage = false }
    BackHandler(enabled = dialogs.showAboutPage) { dialogs.showAboutPage = false }

    if (dialogs.showAppearancePage) {
        AppearancePage(
            settings = settings,
            viewModel = viewModel,
            dialogs = dialogs,
            spacing = spacing,
            onNavigateBack = { dialogs.showAppearancePage = false }
        )
    }

    if (dialogs.showGlassPage) {
        GlassPage(
            settings = settings,
            viewModel = viewModel,
            dialogs = dialogs,
            spacing = spacing,
            onNavigateBack = { dialogs.showGlassPage = false }
        )
    }

    if (dialogs.showLayoutNavPage) {
        LayoutNavPage(
            settings = settings,
            viewModel = viewModel,
            dialogs = dialogs,
            spacing = spacing,
            onNavigateBack = { dialogs.showLayoutNavPage = false }
        )
    }

    if (dialogs.showCallsPage) {
        CallsPage(
            settings = settings,
            viewModel = viewModel,
            dialogs = dialogs,
            showSupplementaryRow = showSupplementaryRow,
            spacing = spacing,
            context = context,
            onNavigateBack = { dialogs.showCallsPage = false }
        )
    }

    if (dialogs.showContactsPage) {
        ContactsPage(
            settings = settings,
            viewModel = viewModel,
            dialogs = dialogs,
            spacing = spacing,
            onNavigateBack = { dialogs.showContactsPage = false }
        )
    }

    if (dialogs.showPrivacyPage) {
        PrivacyPage(
            dialogs = dialogs,
            spacing = spacing,
            context = context,
            onNavigateBack = { dialogs.showPrivacyPage = false }
        )
    }

    if (dialogs.showAboutPage) {
        AboutPage(
            settings = settings,
            viewModel = viewModel,
            dialogs = dialogs,
            spacing = spacing,
            runUpdateCheck = runUpdateCheck,
            onNavigateBack = { dialogs.showAboutPage = false }
        )
    }
}
