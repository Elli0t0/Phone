package io.github.elli0t0.phone.feature.recording

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.elli0t0.phone.PhoneApplication
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.SwitchGreenTrack
import io.github.elli0t0.phone.core.design.SwitchGreenUncheckedTrack
import io.github.elli0t0.phone.core.recording.BackendStatus
import io.github.elli0t0.phone.core.recording.PairResult
import io.github.elli0t0.phone.core.recording.RecordingSession
import io.github.elli0t0.phone.data.settings.model.AutoRecordMode
import io.github.elli0t0.phone.data.settings.model.SwitchStyle
import io.github.elli0t0.phone.feature.settings.LocalSearchAdvanced
import io.github.elli0t0.phone.feature.settings.SettingsChoiceDialog
import io.github.elli0t0.phone.feature.settings.searchHighlight
import io.github.elli0t0.phone.navigation.InnerTopBar

/**
 * Call recording page: built-in ADB backend status + setup actions, a
 * 3-second pipeline test, the active recording (if any), and a row
 * opening the Recordings screen.
 */
@Composable
fun RecordingSettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenRecordings: () -> Unit = {},
    onOpenExcludedNumbers: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as PhoneApplication
    val viewModel: RecordingSettingsViewModel = viewModel(
        factory = RecordingSettingsViewModel.provideFactory(
            app.container.recordingController,
            app.container.recordingsRepository,
            context.applicationContext,
            app.container.settingsRepository
        )
    )
    val status by viewModel.backendStatus.collectAsStateWithLifecycle()
    // Hoisted: the Advanced card's condition sits at LazyListScope
    // level (not a @Composable scope), so the settings read must
    // happen here in the function body, not in the condition.
    val showAdvancedSettings = LocalAppSettings.current.showAdvancedSettings || LocalSearchAdvanced.current
    val session by viewModel.session.collectAsStateWithLifecycle()
    val lastError by viewModel.lastError.collectAsStateWithLifecycle()
    val recordingStats by viewModel.recordingStats.collectAsStateWithLifecycle()
    val retentionDays by viewModel.retentionDays.collectAsStateWithLifecycle()
    val retentionConfirm by viewModel.retentionConfirm.collectAsStateWithLifecycle()
    var showRetentionDialog by remember { mutableStateOf(false) }
    val retentionLabel = stringResource(
        when (retentionDays) {
            7 -> R.string.recordings_retention_7_days
            30 -> R.string.recordings_retention_30_days
            90 -> R.string.recordings_retention_90_days
            else -> R.string.recordings_retention_never
        }
    )
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val testOutcome by viewModel.testOutcome.collectAsStateWithLifecycle()
    val testing by viewModel.testing.collectAsStateWithLifecycle()

    // The pairing code is entered into Phone's own notification, so Pair
    // must ensure notifications are allowed first: on API 33+ a plain
    // notification is silently dropped without POST_NOTIFICATIONS. If the
    // request is denied for good, fall back to the app's notification
    // settings instead of dead-ending.
    val pairPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startPairing()
        } else {
            openNotificationSettings(context)
        }
    }
    val onPairClick = {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.startPairing()
        } else {
            pairPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val backendDiagnostic by viewModel.backendDiagnostic.collectAsStateWithLifecycle()
    val captureDiagnostic by viewModel.captureDiagnostic.collectAsStateWithLifecycle()
    val pairResult by viewModel.pairResult.collectAsStateWithLifecycle()
    val manualPort by viewModel.manualPort.collectAsStateWithLifecycle()
    val detectResult by viewModel.detectResult.collectAsStateWithLifecycle()
    val offlineMode by viewModel.offlineMode.collectAsStateWithLifecycle()
    val autoRecordCalls by viewModel.autoRecordCalls.collectAsStateWithLifecycle()
    val autoRecordMode by viewModel.autoRecordMode.collectAsStateWithLifecycle()
    val autoRecordExcluded by viewModel.autoRecordExcluded.collectAsStateWithLifecycle()
    val offlinePortActive by viewModel.offlinePortActive.collectAsStateWithLifecycle()
    val keyAccepted by viewModel.keyAccepted.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(Unit) { viewModel.refresh() }

    // Returning from system Settings must re-probe: the wireless
    // debugging toggle changed while Phone was in the background.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val statusText = when (status) {
        BackendStatus.NOT_PAIRED -> stringResource(id = R.string.recording_status_not_paired)
        BackendStatus.NOT_RUNNING -> stringResource(id = R.string.recording_status_adb_not_running)
        BackendStatus.READY -> if (offlinePortActive) {
            stringResource(id = R.string.recording_status_offline_active)
        } else {
            stringResource(id = R.string.recording_status_ready)
        }
        BackendStatus.ERROR -> stringResource(id = R.string.recording_status_adb_error)
        // NOT_INSTALLED and PERMISSION_NEEDED never come from the
        // ADB backend; they fall here only to keep the when
        // exhaustive, and read as a generic error.
        else -> stringResource(id = R.string.recording_status_adb_error)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            InnerTopBar(
                onBack = onNavigateBack,
                title = stringResource(id = R.string.settings_call_recording)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                        .searchHighlight("recording_auto")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(id = R.string.recording_auto_record),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = autoRecordCalls,
                                onCheckedChange = { viewModel.setAutoRecordCalls(it) },
                                enabled = !busy,
                                colors = if (LocalAppSettings.current.effectiveStyle.switch == SwitchStyle.GREEN) {
                                    SwitchDefaults.colors(
                                        checkedTrackColor = SwitchGreenTrack,
                                        checkedThumbColor = Color.White,
                                        checkedBorderColor = Color.Transparent,
                                        uncheckedTrackColor = SwitchGreenUncheckedTrack,
                                        uncheckedThumbColor = Color.White,
                                        uncheckedBorderColor = Color.Transparent
                                    )
                                } else {
                                    SwitchDefaults.colors()
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.recording_auto_note),
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (autoRecordCalls) {
                            Spacer(modifier = Modifier.height(8.dp))
                            AutoRecordMode.entries.forEach { mode ->
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable { viewModel.setAutoRecordMode(mode) },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = mode == autoRecordMode,
                                        onClick = { viewModel.setAutoRecordMode(mode) }
                                    )
                                    Text(
                                        text = stringResource(id = mode.labelRes()),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            if (autoRecordMode == AutoRecordMode.EXCEPT_LIST) {
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .searchHighlight("recording_excluded")
                                        .clickable { onOpenExcludedNumbers() }
                                        .padding(top = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(
                                            id = R.string.recording_excluded_numbers,
                                            autoRecordExcluded.size
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_chevron_right),
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                        .searchHighlight("recording_open")
                        .clickable { onOpenRecordings() }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(id = R.string.recordings_open),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(
                                id = R.string.recordings_open_summary,
                                recordingStats.count,
                                Formatter.formatShortFileSize(context, recordingStats.totalBytes)
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                        .clickable { showRetentionDialog = true }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(id = R.string.recordings_auto_delete_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = retentionLabel,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.recordings_auto_delete_summary),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth().searchHighlight("recording_status")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(id = R.string.recording_status_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        // The probe already connected (READY, or ERROR
                        // with a working connection): the key is
                        // accepted, so the state is "Paired", not "pair
                        // me". A failed probe proves nothing about the
                        // key, so it shows no such line.
                        if (status == BackendStatus.READY ||
                            (status == BackendStatus.ERROR && keyAccepted)
                        ) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "✓ " + stringResource(id = R.string.recording_pair_success),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (lastError != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = lastError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        // Copy details covers the probe diagnostic
                        // plus the last capture's line (source, sample
                        // rate, peaks, fallback failure), so every
                        // reason for a silent or failed recording is
                        // in one paste.
                        val detailsText = listOfNotNull(
                            backendDiagnostic,
                            captureDiagnostic
                        ).joinToString("\n").takeIf { it.isNotBlank() }
                        if (detailsText != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            SelectionContainer {
                                Text(
                                    text = detailsText,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            TextButton(onClick = {
                                clipboardManager.setText(AnnotatedString(detailsText))
                            }) {
                                Text(text = stringResource(id = R.string.recording_copy_details))
                            }
                        }
                        val result = pairResult
                        if (result != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            when (result) {
                                PairResult.Paired -> Text(
                                    text = stringResource(id = R.string.recording_pair_success),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                is PairResult.Failed -> Text(
                                    text = result.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (status == BackendStatus.READY ||
                                (status == BackendStatus.ERROR && keyAccepted)
                            ) {
                                // Already paired: re-pairing is an
                                // option, not the action — small
                                // text button instead of the big one.
                                TextButton(
                                    onClick = onPairClick,
                                    enabled = !busy
                                ) {
                                    Text(text = stringResource(id = R.string.recording_action_pair_again))
                                }
                            } else {
                                // Pair is available in every other
                                // state, not only once pairing is
                                // already possible: from NOT_RUNNING
                                // it posts the notification and opens
                                // Developer options, so the user is
                                // never stuck with no way to reach
                                // the code entry.
                                Button(
                                    onClick = onPairClick,
                                    enabled = !busy
                                ) {
                                    Text(text = stringResource(id = R.string.recording_action_pair))
                                }
                            }
                            TextButton(
                                onClick = { viewModel.refresh() },
                                enabled = !busy
                            ) {
                                Text(text = stringResource(id = R.string.recording_action_refresh))
                            }
                        }
                        // The how-to-pair steps only matter before
                        // the key is accepted; afterwards they are
                        // noise next to the Paired line.
                        if (status == BackendStatus.NOT_PAIRED) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(id = R.string.recording_pair_hint),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.recording_adb_needs_wifi),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (showAdvancedSettings) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(id = R.string.recording_advanced_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(id = R.string.recording_connection_port),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().searchHighlight("recording_port"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = manualPort?.toString() ?: "",
                                    onValueChange = { text ->
                                        val digits = text.filter { it.isDigit() }
                                        if (digits.isEmpty()) {
                                            viewModel.setManualPort(null)
                                        } else {
                                            val port = digits.toIntOrNull()
                                            if (port != null && port in 1..65535) {
                                                viewModel.setManualPort(port)
                                            }
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = { viewModel.detectPort() },
                                    enabled = !busy,
                                    modifier = Modifier.searchHighlight("recording_detect")
                                ) {
                                    Text(text = stringResource(id = R.string.recording_action_detect_port))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(id = R.string.recording_connection_port_hint),
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (detectResult != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = detectResult!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.resetPairing() },
                                enabled = !busy,
                                modifier = Modifier.searchHighlight("recording_reset_pairing")
                            ) {
                                Text(text = stringResource(id = R.string.recording_action_reset_pairing))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(id = R.string.recording_reset_hint),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().searchHighlight("recording_offline"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(id = R.string.recording_offline_mode_title),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Switch(
                                    checked = offlineMode,
                                    onCheckedChange = { viewModel.setOfflineMode(it) },
                                    enabled = !busy,
                                    colors = if (LocalAppSettings.current.effectiveStyle.switch == SwitchStyle.GREEN) {
                                        SwitchDefaults.colors(
                                            checkedTrackColor = SwitchGreenTrack,
                                            checkedThumbColor = Color.White,
                                            checkedBorderColor = Color.Transparent,
                                            uncheckedTrackColor = SwitchGreenUncheckedTrack,
                                            uncheckedThumbColor = Color.White,
                                            uncheckedBorderColor = Color.Transparent
                                        )
                                    } else {
                                        SwitchDefaults.colors()
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(id = R.string.recording_offline_mode_desc),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            val active = session as? RecordingSession.Recording
            if (active != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (active.isTest) {
                                    stringResource(id = R.string.recording_test_running)
                                } else {
                                    stringResource(id = R.string.recording_active_call)
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                            Button(onClick = { viewModel.stopActive() }) {
                                Text(text = stringResource(id = R.string.recording_stop))
                            }
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.fillMaxWidth().searchHighlight("recording_test")) {
                    Button(
                        onClick = { viewModel.runTest() },
                        enabled = status == BackendStatus.READY && !busy &&
                            session is RecordingSession.Idle
                    ) {
                        if (testing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = LocalContentColor.current,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = stringResource(id = R.string.recording_test_running))
                        } else {
                            Text(text = stringResource(id = R.string.recording_test))
                        }
                    }
                    if (testOutcome != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        // A null peak means unmeasured, so it keeps
                        // the old line. The ADB mic test shows bytes,
                        // duration and peak; below 2% it is silence,
                        // not success, with the exact next check.
                        val outcome = testOutcome!!
                        val failed = outcome.error != null
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (failed) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else {
                                    MaterialTheme.colorScheme.secondaryContainer
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (outcome.error != null) {
                                    stringResource(
                                        id = R.string.recording_test_failed,
                                        outcome.error!!
                                    )
                                } else if (outcome.peak != null && outcome.peak < 0.02f) {
                                    stringResource(id = R.string.recording_test_mic_silence)
                                } else if (outcome.peak != null) {
                                    stringResource(
                                        id = R.string.recording_test_ok_mic,
                                        outcome.bytes,
                                        outcome.durationMs,
                                        (outcome.peak * 100).toInt()
                                    )
                                } else {
                                    stringResource(
                                        id = R.string.recording_test_ok,
                                        outcome.bytes,
                                        outcome.durationMs
                                    )
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (failed) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                },
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(id = R.string.recording_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    if (showRetentionDialog) {
        SettingsChoiceDialog(
            title = stringResource(id = R.string.recordings_auto_delete_title),
            options = listOf(
                stringResource(id = R.string.recordings_retention_never) to 0,
                stringResource(id = R.string.recordings_retention_7_days) to 7,
                stringResource(id = R.string.recordings_retention_30_days) to 30,
                stringResource(id = R.string.recordings_retention_90_days) to 90
            ),
            selected = retentionDays,
            onSelect = { viewModel.onRetentionPicked(it); showRetentionDialog = false },
            onDismiss = { showRetentionDialog = false }
        )
    }
    retentionConfirm?.let { pending ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissRetentionConfirm() },
            title = { Text(stringResource(id = R.string.recordings_auto_delete_title)) },
            text = {
                Text(
                    stringResource(
                        id = R.string.recordings_auto_delete_confirm,
                        pending.count, pending.days
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmRetention() }) {
                    Text(stringResource(id = R.string.recording_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRetentionConfirm() }) {
                    Text(stringResource(id = R.string.action_cancel))
                }
            }
        )
    }
}

private fun openNotificationSettings(context: android.content.Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
    runCatching { context.startActivity(intent) }
}

/** Localizable labels for the auto-record mode radios: the enum's own label is English-only. */
private fun AutoRecordMode.labelRes(): Int = when (this) {
    AutoRecordMode.ALL -> R.string.recording_auto_mode_all
    AutoRecordMode.CONTACTS_ONLY -> R.string.recording_auto_mode_contacts
    AutoRecordMode.UNKNOWN_ONLY -> R.string.recording_auto_mode_unknown
    AutoRecordMode.EXCEPT_LIST -> R.string.recording_auto_mode_except
}
