package io.github.elli0t0.phone.feature.settings
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import io.github.elli0t0.phone.core.design.PhoneBottomSheet
import androidx.compose.ui.text.font.FontWeight
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.core.design.insetRowPress
import io.github.elli0t0.phone.core.design.pressScale
import io.github.elli0t0.phone.core.design.rememberLightHaptic
import io.github.elli0t0.phone.data.settings.model.CardStyle
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.telecom.UssdHelper
import io.github.elli0t0.phone.core.telecom.UssdSim

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        modifier = Modifier.padding(vertical = PhoneDimens.settingsSectionHeaderVerticalPadding)
    )
}

// Settings list containers: 14dp corners when the Inset card style
// is on, else the default large shape.
@Composable
fun settingsListCardShape(): Shape =
    if (LocalAppSettings.current.effectiveStyle.card == CardStyle.INSET) {
        RoundedCornerShape(14.dp)
    } else {
        MaterialTheme.shapes.extraLarge
    }

// Divider between rows of a settings list card: a 0.5dp hairline
// inset 16dp (settings rows have no avatar) for Inset, else the
// default full-width divider.
@Composable
fun SettingsListDivider(modifier: Modifier = Modifier) {
    if (LocalAppSettings.current.effectiveStyle.card == CardStyle.INSET) {
        HorizontalDivider(
            modifier = modifier.padding(start = 16.dp),
            thickness = 0.5.dp,
            color = Color.White.copy(alpha = 0.12f)
        )
    } else {
        HorizontalDivider(
            modifier = modifier,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

// Settings row: title with the current value as a subtitle.
// Tapping the row opens the picker dialog for that setting.
@Composable
fun SettingsRow(
    title: String,
    subtitle: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    searchId: String? = null,
    onClick: () -> Unit
) {
    val rowSource = remember { MutableInteractionSource() }
    val lightHaptic = rememberLightHaptic()
    val insetCard = LocalAppSettings.current.effectiveStyle.card == CardStyle.INSET
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .searchHighlight(searchId)
            .pressScale(rowSource)
            .clickable(
                interactionSource = rowSource,
                indication = if (insetCard) null else ripple()
            ) { lightHaptic(); onClick() }
            .then(if (insetCard) Modifier.insetRowPress(rowSource) else Modifier)
            .padding(vertical = PhoneDimens.settingsRowVerticalPadding)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = titleColor
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f)
            )
        }
    }
}

// Navigation row: leading icon, title with the current summary as
// a subtitle, chevron. Tapping opens that section's page.
@Composable
fun SettingsNavRow(
    iconRes: Int,
    title: String,
    subtitle: String? = null,
    searchId: String? = null,
    onClick: () -> Unit
) {
    val rowSource = remember { MutableInteractionSource() }
    val lightHaptic = rememberLightHaptic()
    val insetCard = LocalAppSettings.current.effectiveStyle.card == CardStyle.INSET
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .searchHighlight(searchId)
            .pressScale(rowSource)
            .clickable(
                interactionSource = rowSource,
                indication = if (insetCard) null else ripple()
            ) { lightHaptic(); onClick() }
            .then(if (insetCard) Modifier.insetRowPress(rowSource) else Modifier)
            .padding(vertical = PhoneDimens.settingsRowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(PhoneDimens.settingsNavRowIconGap))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f)
                )
            }
        }
        Icon(
            painter = painterResource(id = R.drawable.ic_chevron_right),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

// Switch row: title + description on the left, switch on the right.
@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    searchId: String? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .searchHighlight(searchId)
            .padding(vertical = PhoneDimens.settingsSwitchRowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = PhoneDimens.settingsSwitchRowTextEndPadding)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f)
                )
            }
        }
        AppSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

// Custom switch: wide 52x28dp track with a large white round thumb.
// Material3 Switch has a fixed size, so this draws the exact track/thumb.
// Radio-option picker dialog used by every choice setting.
@Composable
fun <T> SettingsChoiceDialog(
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

// Bottom-sheet version of SettingsChoiceDialog: same radio rows,
// shown as a sheet instead of a centered dialog.
@Composable
fun <T> SettingsChoiceSheet(
    title: String,
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    val spacing = LocalSpacing.current
    val lightHaptic = rememberLightHaptic()
    PhoneBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = spacing.large)
                .padding(bottom = spacing.large)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = spacing.medium)
            )
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

// USSD request state for the Call waiting flow.
sealed interface UssdState {
    data class Sending(val code: String, val simLabel: String) : UssdState
    data class Result(val message: String) : UssdState
}

// Call waiting dialog: title, description, three full-width actions,
// Cancel bottom-right. Activate/deactivate dismiss and run their MMI
// code; Check status keeps the dialog open and shows the carrier's
// reply (or the sending state) inside the dialog instead of a
// separate popup.
@Composable
fun CallWaitingDialog(
    onDismiss: () -> Unit,
    onAction: (String) -> Unit,
    onCheck: () -> Unit,
    ussdState: UssdState? = null
) {
    val spacing = LocalSpacing.current
    // Classic shows the old centered dialog; Clean shows the sheet.
    val sheets = LocalAppSettings.current.effectiveStyle.sheets
    val content: @Composable ColumnScope.() -> Unit = {
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
                onClick = onCheck,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.call_waiting_check))
            }
            when (val state = ussdState) {
                is UssdState.Sending -> {
                    Spacer(modifier = Modifier.height(spacing.medium))
                    Text(
                        text = stringResource(id = R.string.ussd_sending),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is UssdState.Result -> {
                    Spacer(modifier = Modifier.height(spacing.medium))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                null -> Unit
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
    if (sheets) {
        PhoneBottomSheet(onDismissRequest = onDismiss) {
            Column(
                modifier = Modifier
                    .padding(horizontal = spacing.large)
                    .padding(bottom = spacing.large)
            ) {
                content()
            }
        }
    } else {
        Dialog(onDismissRequest = onDismiss) {
            Card {
                Column(
                    modifier = Modifier
                        .padding(horizontal = spacing.large)
                        .padding(bottom = spacing.large)
                ) {
                    content()
                }
            }
        }
    }
}

// SIM picker for the USSD flow: one row per active SIM, Cancel dismisses.
@Composable
fun UssdSimPickerDialog(
    sims: List<UssdSim>,
    onSimSelected: (UssdSim) -> Unit,
    onDismiss: () -> Unit
) {
    val spacing = LocalSpacing.current
    val lightHaptic = rememberLightHaptic()
    // Classic shows the old alert dialog; Clean shows the sheet.
    val sheets = LocalAppSettings.current.effectiveStyle.sheets
    val simRows: @Composable ColumnScope.() -> Unit = {
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
    if (sheets) {
        PhoneBottomSheet(onDismissRequest = onDismiss) {
            Column(
                modifier = Modifier
                    .padding(horizontal = spacing.large)
                    .padding(bottom = spacing.large)
            ) {
                Text(
                    text = stringResource(id = R.string.call_sim_picker_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = spacing.medium)
                )
                simRows()
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
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(text = stringResource(id = R.string.call_sim_picker_title))
            },
            text = {
                Column {
                    simRows()
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            },
            confirmButton = {}
        )
    }
}

// Editor for the three quick-decline messages shown on the incoming call
// screen. Blank entries fall back to the built-in defaults.
@Composable
fun QuickDeclineMessagesDialog(
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

