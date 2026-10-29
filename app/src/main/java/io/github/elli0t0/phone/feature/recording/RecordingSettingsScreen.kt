package io.github.elli0t0.phone.feature.recording

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import io.github.elli0t0.phone.data.recordings.RecordingEntity
import io.github.elli0t0.phone.data.settings.model.RecordingBackendKind
import io.github.elli0t0.phone.data.settings.model.SwitchStyle
import io.github.elli0t0.phone.navigation.InnerTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Call recording page: backend choice (Shizuku or built-in ADB),
 * backend status + setup actions, a 3-second pipeline test, the active
 * recording (if any), and the recordings list with playback, delete,
 * and share.
 */
@Composable
fun RecordingSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as PhoneApplication
    val viewModel: RecordingSettingsViewModel = viewModel(
        factory = RecordingSettingsViewModel.provideFactory(
            app.container.recordingController,
            app.container.recordingsRepository,
            context.applicationContext
        )
    )
    val status by viewModel.backendStatus.collectAsStateWithLifecycle()
    val backendKind by viewModel.backendKind.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val lastError by viewModel.lastError.collectAsStateWithLifecycle()
    val recordings by viewModel.recordings.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val testOutcome by viewModel.testOutcome.collectAsStateWithLifecycle()

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
    val playError by viewModel.playError.collectAsStateWithLifecycle()
    val playingId by viewModel.playingId.collectAsStateWithLifecycle()
    val positionMs by viewModel.positionMs.collectAsStateWithLifecycle()
    val backendDiagnostic by viewModel.backendDiagnostic.collectAsStateWithLifecycle()
    val pairResult by viewModel.pairResult.collectAsStateWithLifecycle()
    val manualPort by viewModel.manualPort.collectAsStateWithLifecycle()
    val detectResult by viewModel.detectResult.collectAsStateWithLifecycle()
    val offlineMode by viewModel.offlineMode.collectAsStateWithLifecycle()
    val offlinePortActive by viewModel.offlinePortActive.collectAsStateWithLifecycle()
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

    val statusText = if (backendKind == RecordingBackendKind.ADB) {
        when (status) {
            BackendStatus.NOT_PAIRED -> stringResource(id = R.string.recording_status_not_paired)
            BackendStatus.NOT_RUNNING -> stringResource(id = R.string.recording_status_adb_not_running)
            BackendStatus.PERMISSION_NEEDED -> stringResource(id = R.string.recording_status_permission)
            BackendStatus.NOT_INSTALLED -> stringResource(id = R.string.recording_status_not_installed)
            BackendStatus.READY -> if (offlinePortActive) {
                stringResource(id = R.string.recording_status_offline_active)
            } else {
                stringResource(id = R.string.recording_status_ready)
            }
            BackendStatus.ERROR -> stringResource(id = R.string.recording_status_adb_error)
        }
    } else {
        when (status) {
            BackendStatus.NOT_INSTALLED -> stringResource(id = R.string.recording_status_not_installed)
            BackendStatus.NOT_RUNNING -> stringResource(id = R.string.recording_status_not_running)
            BackendStatus.PERMISSION_NEEDED -> stringResource(id = R.string.recording_status_permission)
            BackendStatus.READY -> stringResource(id = R.string.recording_status_ready)
            BackendStatus.ERROR -> stringResource(id = R.string.recording_status_error)
            BackendStatus.NOT_PAIRED -> stringResource(id = R.string.recording_status_not_paired)
        }
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
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(id = R.string.recording_backend_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        RecordingBackendKind.entries.forEach { kind ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.setBackend(kind) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = backendKind == kind,
                                    onClick = { viewModel.setBackend(kind) }
                                )
                                Text(
                                    text = stringResource(
                                        id = if (kind == RecordingBackendKind.ADB) {
                                            R.string.recording_backend_adb
                                        } else {
                                            R.string.recording_backend_shizuku
                                        }
                                    ),
                                    style = MaterialTheme.typography.bodyMedium
                                )
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
                        if (lastError != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = lastError!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        if (backendKind == RecordingBackendKind.ADB && backendDiagnostic != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            SelectionContainer {
                                Text(
                                    text = backendDiagnostic!!,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            TextButton(onClick = {
                                clipboardManager.setText(AnnotatedString(backendDiagnostic!!))
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
                            if (backendKind == RecordingBackendKind.ADB) {
                                // Pair is available in every non-ready state, not
                                // only once pairing is already possible: from
                                // NOT_RUNNING it posts the notification and opens
                                // Developer options, so the user is never stuck
                                // with no way to reach the code entry.
                                if (status != BackendStatus.READY) {
                                    Button(
                                        onClick = onPairClick,
                                        enabled = !busy
                                    ) {
                                        Text(text = stringResource(id = R.string.recording_action_pair))
                                    }
                                }
                            } else {
                                when (status) {
                                    BackendStatus.NOT_INSTALLED -> Button(
                                        onClick = {
                                            openUrl(
                                                context,
                                                SHIZUKU_DOWNLOAD_URL
                                            )
                                        }
                                    ) {
                                        Text(text = stringResource(id = R.string.recording_action_get_shizuku))
                                    }
                                    BackendStatus.NOT_RUNNING -> Button(
                                        onClick = { openShizuku(context) }
                                    ) {
                                        Text(text = stringResource(id = R.string.recording_action_start_shizuku))
                                    }
                                    BackendStatus.PERMISSION_NEEDED -> Button(
                                        onClick = { viewModel.requestPermission() },
                                        enabled = !busy
                                    ) {
                                        Text(text = stringResource(id = R.string.recording_action_grant))
                                    }
                                    else -> Unit
                                }
                            }
                            TextButton(
                                onClick = { viewModel.refresh() },
                                enabled = !busy
                            ) {
                                Text(text = stringResource(id = R.string.recording_action_refresh))
                            }
                        }
                        if (backendKind == RecordingBackendKind.ADB) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(id = R.string.recording_pair_hint),
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(id = R.string.recording_adb_needs_wifi),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            if (backendKind == RecordingBackendKind.ADB) {
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
                                    enabled = !busy
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
                                enabled = !busy
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
                                modifier = Modifier.fillMaxWidth(),
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
                                    colors = if (LocalAppSettings.current.switchStyle == SwitchStyle.GREEN) {
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
                Column {
                    Button(
                        onClick = { viewModel.runTest() },
                        enabled = status == BackendStatus.READY && !busy &&
                            session is RecordingSession.Idle
                    ) {
                        Text(text = stringResource(id = R.string.recording_test))
                    }
                    if (testOutcome != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (testOutcome!!.error != null) {
                                stringResource(
                                    id = R.string.recording_test_failed,
                                    testOutcome!!.error!!
                                )
                            } else {
                                stringResource(
                                    id = R.string.recording_test_ok,
                                    testOutcome!!.bytes,
                                    testOutcome!!.durationMs
                                )
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
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

            if (playError != null) {
                item {
                    Text(
                        text = playError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (recordings.isEmpty()) {
                item {
                    Text(
                        text = stringResource(id = R.string.recording_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(recordings, key = { it.id }) { entity ->
                    RecordingRow(
                        entity = entity,
                        isPlaying = playingId == entity.id,
                        positionMs = if (playingId == entity.id) positionMs else 0L,
                        onPlayToggle = { viewModel.togglePlay(entity) },
                        onSeek = { viewModel.seekTo(it) },
                        onDelete = { viewModel.delete(entity) },
                        onShare = { viewModel.share(entity, context) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordingRow(
    entity: RecordingEntity,
    isPlaying: Boolean,
    positionMs: Long,
    onPlayToggle: () -> Unit,
    onSeek: (Int) -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    val dateFormat = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    }
    val title = when {
        entity.isTest -> stringResource(id = R.string.recording_test_title)
        !entity.displayName.isNullOrBlank() -> entity.displayName!!
        else -> entity.number
    }
    val durationText = if (entity.durationMs >= 60_000) {
        stringResource(
            id = R.string.recents_duration_min_sec,
            (entity.durationMs / 60_000).toInt(),
            ((entity.durationMs / 1000) % 60).toInt()
        )
    } else {
        stringResource(
            id = R.string.recents_duration_sec,
            (entity.durationMs / 1000).toInt()
        )
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${dateFormat.format(Date(entity.startedAt))}  •  $durationText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPlayToggle) {
                    Icon(
                        painter = painterResource(
                            id = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                        ),
                        contentDescription = null
                    )
                }
                Slider(
                    value = positionMs.toFloat(),
                    onValueChange = { if (isPlaying) onSeek(it.toInt()) },
                    valueRange = 0f..maxOf(1f, entity.durationMs.toFloat()),
                    enabled = isPlaying,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${formatPlayerTime(positionMs)} / ${formatPlayerTime(entity.durationMs)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDelete) {
                    Text(text = stringResource(id = R.string.recording_delete))
                }
                TextButton(onClick = onShare) {
                    Text(text = stringResource(id = R.string.recording_share))
                }
            }
        }
    }
}

private fun formatPlayerTime(ms: Long): String {
    val totalSeconds = ms / 1000
    return "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
}

private fun openUrl(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}

private fun openShizuku(context: android.content.Context) {
    val launch = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
    if (launch != null) {
        runCatching { context.startActivity(launch) }
    } else {
        openUrl(context, SHIZUKU_DOWNLOAD_URL)
    }
}

private fun openNotificationSettings(context: android.content.Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    }
    runCatching { context.startActivity(intent) }
}

private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
private const val SHIZUKU_DOWNLOAD_URL = "https://shizuku.rikka.app/download/"
