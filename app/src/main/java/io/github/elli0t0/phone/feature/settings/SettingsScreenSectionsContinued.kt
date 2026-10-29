package io.github.elli0t0.phone.feature.settings
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.telecom.TelecomManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.elli0t0.phone.PhoneApplication
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.recording.BackendStatus
import io.github.elli0t0.phone.data.settings.model.AnswerStyle
import io.github.elli0t0.phone.data.settings.model.AppSettings
import androidx.compose.runtime.Composable
import io.github.elli0t0.phone.core.design.Spacing
import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.ExperimentalLayoutApi

/**
 * The Settings section cards (Contacts, Calls, Privacy, About,
 * Reset). Each card is one page of the restructured Settings; the
 * call-recording row moved to the main page, so it is no longer
 * part of the Calls card. The rows open their dialogs via [dialogs];
 * the dialogs themselves live in the main SettingsScreen function.
 */
@Composable
fun ContactsCard(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    dialogs: SettingsDialogState,
    spacing: Spacing
) {
    Card(
        shape = settingsListCardShape(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(spacing.medium)) {
            SettingsRow(
                title = stringResource(id = R.string.settings_sort_order),
                subtitle = settings.contactSortOrder.label,
                onClick = { dialogs.showSortDialog = true }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsRow(
                title = stringResource(id = R.string.settings_name_format),
                subtitle = settings.contactNameFormat.label,
                onClick = { dialogs.showFormatDialog = true }
            )
        }
    }
}

@Composable
fun CallsCard(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    dialogs: SettingsDialogState,
    spacing: Spacing,
    showSupplementaryRow: Boolean,
    context: Context
) {
    Card(
        shape = settingsListCardShape(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(spacing.medium)) {
            SettingsRow(
                title = stringResource(id = R.string.settings_block_numbers),
                onClick = { dialogs.showBlockedNumbers = true }
            )

            if (showSupplementaryRow) {
                Spacer(modifier = Modifier.height(spacing.small))
                SettingsListDivider()
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
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            // Call waiting is a carrier supplementary service:
            // the MMI code is dialed and the carrier executes it.
            SettingsRow(
                title = stringResource(id = R.string.settings_call_waiting),
                onClick = { dialogs.showCallWaitingDialog = true }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsRow(
                title = stringResource(id = R.string.settings_call_display),
                subtitle = settings.callDisplay.label,
                onClick = { dialogs.showCallDisplayDialog = true }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsRow(
                title = stringResource(id = R.string.settings_answer_style),
                // Slider + locked-only reads confusingly as just
                // "Slider": spell out the effective behavior.
                subtitle = if (settings.answerStyle == AnswerStyle.SLIDER &&
                    settings.sliderOnlyWhenLocked
                ) {
                    "${AnswerStyle.SLIDER.label} (locked) / ${AnswerStyle.CLASSIC.label} (unlocked)"
                } else {
                    settings.answerStyle.label
                },
                onClick = { dialogs.showAnswerStylePage = true }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsRow(
                title = stringResource(id = R.string.settings_call_background),
                subtitle = settings.callBackground.label,
                onClick = { dialogs.showCallBackgroundPage = true }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsRow(
                title = stringResource(id = R.string.settings_default_sim),
                subtitle = settings.defaultSim.label,
                onClick = { dialogs.showDefaultSimDialog = true }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsRow(
                title = stringResource(id = R.string.settings_quick_decline),
                onClick = { dialogs.showQuickDeclineDialog = true }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

        }
    }
}

@Composable
fun PrivacyCard(
    dialogs: SettingsDialogState,
    spacing: Spacing,
    context: Context
) {
    Card(
        shape = settingsListCardShape(),
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

@Composable
fun AboutCard(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    dialogs: SettingsDialogState,
    spacing: Spacing,
    runUpdateCheck: () -> Unit
) {
    Card(
        shape = settingsListCardShape(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(spacing.medium)) {
            SettingsRow(
                title = stringResource(id = R.string.settings_about_phone),
                onClick = { dialogs.showAboutDialog = true }
            )
            SettingsRow(
                title = stringResource(id = R.string.update_check_title),
                onClick = runUpdateCheck
            )
        }
    }
}

@Composable
fun ResetCard(
    dialogs: SettingsDialogState,
    spacing: Spacing
) {
    Card(
        shape = settingsListCardShape(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(spacing.medium)) {
            SettingsRow(
                title = stringResource(id = R.string.settings_reset_button),
                titleColor = MaterialTheme.colorScheme.error,
                onClick = { dialogs.showResetDialog = true }
            )
        }
    }
}
