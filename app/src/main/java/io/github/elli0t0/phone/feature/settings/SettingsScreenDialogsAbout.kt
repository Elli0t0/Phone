package io.github.elli0t0.phone.feature.settings
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.data.settings.model.AppSettings
import androidx.compose.ui.platform.LocalContext
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.telecom.PhoneNavLog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.ExperimentalLayoutApi

/**
 * All Settings dialogs (choice pickers, About, update check, USSD).
 * Extracted from SettingsScreen to keep that file under 400 lines.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
fun SettingsDialogsAbout(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    dialogs: SettingsDialogState,
    updateCheckState: UpdateCheckState?,
    onUpdateCheckStateChange: (UpdateCheckState?) -> Unit,
    runUpdateCheck: () -> Unit,
    currentVersionName: String
) {
    val context = LocalContext.current
    val spacing = LocalSpacing.current
    with(settings) {
if (dialogs.showAboutDialog) {
    val packageInfo = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0) }.getOrNull()
    }
    val versionName = packageInfo?.versionName ?: ""
    val versionCode = packageInfo?.let { PackageInfoCompat.getLongVersionCode(it) } ?: 0L
    AlertDialog(
        onDismissRequest = { dialogs.showAboutDialog = false },
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
                Spacer(modifier = Modifier.height(spacing.small))
                if (settings.showAdvancedSettings || LocalSearchAdvanced.current) TextButton(
                    onClick = {
                        dialogs.showAboutDialog = false
                        dialogs.showDebugLogDialog = true
                    }
                ) {
                    Text(text = stringResource(id = R.string.settings_debug_log))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    dialogs.showAboutDialog = false
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
            TextButton(onClick = { dialogs.showAboutDialog = false }) {
                Text(text = stringResource(id = R.string.action_cancel))
            }
        }
    )
}

if (dialogs.showDebugLogDialog) {
    AlertDialog(
        onDismissRequest = { dialogs.showDebugLogDialog = false },
        title = { Text(text = stringResource(id = R.string.settings_debug_log)) },
        text = {
            Text(
                text = PhoneNavLog.snapshot(),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            )
        },
        confirmButton = {
            TextButton(onClick = { dialogs.showDebugLogDialog = false }) {
                Text(text = stringResource(id = R.string.action_cancel))
            }
        }
    )
}

if (updateCheckState != null) {
    val state = updateCheckState
    AlertDialog(
        onDismissRequest = { onUpdateCheckStateChange(null) },
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
                            onUpdateCheckStateChange(null)
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
                    TextButton(onClick = { onUpdateCheckStateChange(null) }) {
                        Text(text = stringResource(id = R.string.action_ok))
                    }
                }
            }
        },
    )
}

if (dialogs.showCallWaitingDialog) {
    CallWaitingDialog(
        onDismiss = {
            dialogs.showCallWaitingDialog = false
            dialogs.ussdState = null
        },
        onAction = { code ->
            dialogs.showCallWaitingDialog = false
            dialogs.pickSimForUssd(context, code)
        },
        // Check keeps the dialog open: the reply lands in ussdState
        // and renders inside this dialog (see below).
        onCheck = { dialogs.pickSimForUssd(context, "*#43#") },
        ussdState = dialogs.ussdState
    )
}

if (dialogs.showUssdSimPicker) {
    UssdSimPickerDialog(
        sims = dialogs.ussdSims,
        onSimSelected = { sim ->
            dialogs.showUssdSimPicker = false
            dialogs.pendingUssdCode?.let { dialogs.runUssd(context, it, sim) }
            dialogs.pendingUssdCode = null
        },
        onDismiss = {
            dialogs.showUssdSimPicker = false
            dialogs.pendingUssdCode = null
        }
    )
}

// While the call-waiting dialog is open its own body shows the
// sending/result states; the separate popups below only serve the
// activate/deactivate flows (which dismiss that dialog first).
if (!dialogs.showCallWaitingDialog) when (val state = dialogs.ussdState) {
    is UssdState.Sending -> {
        AlertDialog(
            onDismissRequest = {
                dialogs.ussdRequestId += 1
                dialogs.ussdState = null
            },
            title = { Text(text = stringResource(id = R.string.settings_call_waiting)) },
            text = { Text(text = stringResource(id = R.string.ussd_sending)) },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        dialogs.ussdRequestId += 1
                        dialogs.ussdState = null
                    }
                ) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        )
    }
    is UssdState.Result -> {
        AlertDialog(
            onDismissRequest = { dialogs.ussdState = null },
            title = { Text(text = stringResource(id = R.string.ussd_result_title)) },
            text = { Text(text = state.message) },
            confirmButton = {
                TextButton(onClick = { dialogs.ussdState = null }) {
                    Text(text = stringResource(id = R.string.action_ok))
                }
            }
        )
    }
    null -> Unit
}
    }
}
