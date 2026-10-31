package io.github.elli0t0.phone.feature.settings
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.data.settings.model.AppSettings
import io.github.elli0t0.phone.data.settings.model.BottomBarChoice
import io.github.elli0t0.phone.data.settings.model.bottomBarChoice
import androidx.compose.runtime.Composable
import io.github.elli0t0.phone.core.design.Spacing
import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.ExperimentalLayoutApi

/**
 * The Settings section cards (Layout, Navigation). Each card is one
 * part of the Layout & navigation page of the restructured Settings.
 * The rows open their dialogs via [dialogs]; the dialogs themselves
 * live in the main SettingsScreen function.
 */
@Composable
fun LayoutCard(
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
                title = stringResource(id = R.string.settings_list_density),
                searchId = "list_density",
                subtitle = settings.listDensity.label,
                onClick = { dialogs.showDensityDialog = true }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsSwitchRow(
                title = stringResource(id = R.string.settings_swipe_actions),
                searchId = "swipe_actions",
                subtitle = stringResource(id = R.string.settings_swipe_actions_desc),
                checked = settings.swipeActionsEnabled,
                onCheckedChange = { viewModel.onSwipeActionsEnabledChanged(it) }
            )
        }
    }
}

@Composable
fun NavigationCard(
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
                title = stringResource(id = R.string.settings_default_tab),
                searchId = "default_tab",
                subtitle = settings.defaultStartTab.label,
                onClick = { dialogs.showTabDialog = true }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsSwitchRow(
                title = stringResource(id = R.string.settings_bottom_labels),
                searchId = "bottom_labels",
                subtitle = stringResource(id = R.string.settings_bottom_labels_desc),
                checked = settings.showBottomBarLabels,
                onCheckedChange = { viewModel.onShowBottomBarLabelsChanged(it) }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsRow(
                title = stringResource(id = R.string.settings_bottom_bar_style),
                searchId = "bottom_bar_style",
                subtitle = settings.bottomBarChoice.label,
                onClick = { dialogs.showBottomBarStyleDialog = true }
            )

            // Lens glass needs Android 13; below it the lens bar
            // falls back to the glass bar.
            if (settings.bottomBarChoice == BottomBarChoice.LENS) {
                Text(
                    text = stringResource(id = R.string.settings_lens_bar_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = spacing.small)
                )
            }

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsRow(
                title = stringResource(id = R.string.settings_bottom_bar_size),
                searchId = "bottom_bar_size",
                subtitle = settings.bottomBarSize.label,
                onClick = { dialogs.showBottomBarSizeDialog = true }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsSwitchRow(
                title = stringResource(id = R.string.settings_bottom_bar_shrink),
                searchId = "bottom_bar_shrink",
                subtitle = stringResource(id = R.string.settings_bottom_bar_shrink_desc),
                checked = settings.shrinkBottomBarOnScroll,
                onCheckedChange = { viewModel.onShrinkBottomBarOnScrollChanged(it) }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsRow(
                title = stringResource(id = R.string.settings_animations),
                searchId = "animations",
                subtitle = settings.animationOption.label,
                onClick = { dialogs.showAnimDialog = true }
            )
        }
    }
}

/**
 * The Keypad page card (0.18.6): every keypad-related setting in
 * one place — the keypad style and key style pickers, dialpad
 * tones, and the speed-dial assignments (assigned by long-pressing
 * a keypad digit, managed here).
 */
@Composable
fun KeypadCard(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    dialogs: SettingsDialogState,
    spacing: Spacing
) {
    var showSpeedDialDialog by remember { mutableStateOf(false) }
    Card(
        shape = settingsListCardShape(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(spacing.medium)) {
            SettingsRow(
                title = stringResource(id = R.string.settings_keypad_style),
                searchId = "keypad_style",
                subtitle = settings.keypadStyle.label,
                onClick = { dialogs.showKeypadStylePage = true }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsRow(
                title = stringResource(id = R.string.settings_key_style),
                searchId = "key_style",
                subtitle = settings.keyStyle.label,
                onClick = { dialogs.showKeyStyleDialog = true }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsSwitchRow(
                title = stringResource(id = R.string.settings_dialpad_tones),
                searchId = "dialpad_tones",
                subtitle = stringResource(id = R.string.settings_dialpad_tones_desc),
                checked = settings.dialpadTones,
                onCheckedChange = { viewModel.onDialpadTonesChanged(it) }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsRow(
                title = stringResource(id = R.string.settings_speed_dial),
                searchId = "speed_dial",
                subtitle = stringResource(id = R.string.settings_speed_dial_desc),
                onClick = { showSpeedDialDialog = true }
            )
        }
    }
    if (showSpeedDialDialog) {
        SpeedDialDialog(
            settings = settings,
            viewModel = viewModel,
            onDismiss = { showSpeedDialDialog = false }
        )
    }
}

@Composable
private fun SpeedDialDialog(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.settings_speed_dial)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(id = R.string.settings_speed_dial_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (settings.speedDial.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.settings_speed_dial_empty),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    settings.speedDial.entries.sortedBy { it.key }.forEach { (digit, number) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$digit \u2192 $number",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.onSpeedDialEntryRemoved(digit) }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_delete),
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.action_close))
            }
        }
    )
}
