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

    val dialogs = remember { SettingsDialogState() }

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
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            settingsSections(
                settings = settings,
                viewModel = viewModel,
                dialogs = dialogs,
                dialerState = dialerState,
                showSupplementaryRow = showSupplementaryRow,
                requiredSetupComplete = requiredSetupComplete
            )
            settingsSectionsLayoutNav(
                settings = settings,
                viewModel = viewModel,
                dialogs = dialogs,
                dialerState = dialerState,
                showSupplementaryRow = showSupplementaryRow
            )
            settingsSectionsContinued(
                settings = settings,
                viewModel = viewModel,
                dialogs = dialogs,
                dialerState = dialerState,
                showSupplementaryRow = showSupplementaryRow,
                runUpdateCheck = runUpdateCheck
            )
        }
    }

    // Full-screen pages, emitted after the list Scaffold so they draw
    // on top of it. Emitted before the Scaffold, they composed behind
    // the list, so tapping the row visibly did nothing. System Back
    // closes the open page instead of leaving Settings.
    BackHandler(enabled = dialogs.showCallBackgroundPage) { dialogs.showCallBackgroundPage = false }
    BackHandler(enabled = dialogs.showBlockedNumbers) { dialogs.showBlockedNumbers = false }

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
}
