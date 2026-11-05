package io.github.elli0t0.phone.feature.settings

import android.content.Context
import android.telecom.TelecomManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.GlassDropdownMenu
import io.github.elli0t0.phone.core.design.GlassMenuItem
import io.github.elli0t0.phone.core.design.Spacing
import io.github.elli0t0.phone.core.design.dialogEnter
import io.github.elli0t0.phone.data.contacts.ContactsRepository
import io.github.elli0t0.phone.data.contacts.util.findByNumber
import io.github.elli0t0.phone.data.settings.model.AppSettings
import io.github.elli0t0.phone.feature.contacts.ui.ContactAvatar
import io.github.elli0t0.phone.feature.contacts.ui.ContactPickerSheet
import io.github.elli0t0.phone.navigation.InnerTopBar

/**
 * Speed dial manager (0.19.3): replaces the old Speed dial dialog.
 * Slots 1-9: slot 1 is the carrier voicemail (locked, never stored
 * in the map); slots 2-9 resolve their name and avatar live from
 * contacts by number, falling back to the raw number. Tap a slot to
 * assign or change it via the contact picker; the row menu offers
 * Change and Remove; Clear all (with confirm) empties the map.
 */
@Composable
fun SpeedDialScreen(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    contactsRepository: ContactsRepository,
    spacing: Spacing,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val contacts by contactsRepository.getContactsFlow()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var pickerDigit by remember { mutableStateOf<Char?>(null) }
    var menuDigit by remember { mutableStateOf<Char?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    // Slot 1's number, if the carrier reports one (same lookup the
    // keypad's long-press 1 uses). Null keeps the row text-only.
    val voicemailNumber = remember(context) {
        runCatching {
            val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            io.github.elli0t0.phone.core.telecom.TelecomHelper
                .getCallCapablePhoneAccounts(context)
                .firstNotNullOfOrNull { account ->
                    runCatching { telecom?.getVoiceMailNumber(account.handle) }
                        .getOrNull()?.takeIf { it.isNotBlank() }
                }
        }.getOrNull()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().testTag("speed_dial_page")
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = inner.calculateBottomPadding())
        ) {
            InnerTopBar(
                onBack = onNavigateBack,
                title = stringResource(id = R.string.settings_speed_dial),
                backTestTag = "btn_speed_dial_back"
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.medium),
                verticalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
            SettingsSwitchRow(
                title = stringResource(id = R.string.settings_speed_dial),
                searchId = "speed_dial_enabled",
                subtitle = stringResource(id = R.string.settings_speed_dial_desc),
                checked = settings.speedDialEnabled,
                onCheckedChange = { viewModel.onSpeedDialEnabledChanged(it) }
            )
            SettingsSwitchRow(
                title = stringResource(id = R.string.settings_speed_dial_show_on_keys),
                searchId = "speed_dial_show_on_keys",
                subtitle = stringResource(id = R.string.settings_speed_dial_show_on_keys_desc),
                checked = settings.showSpeedDialOnKeys,
                enabled = settings.speedDialEnabled,
                onCheckedChange = { viewModel.onShowSpeedDialOnKeysChanged(it) }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            ('1'..'9').forEach { digit ->
                if (digit == '1') {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.medium)
                    ) {
                        SpeedDialDigitBadge(digit = digit)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = voicemailNumber
                                    ?: stringResource(id = R.string.call_type_voicemail),
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(id = R.string.call_type_voicemail),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    val number = settings.speedDial[digit]
                    val contact = number?.let { contacts.findByNumber(it) }
                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pickerDigit = digit }
                                .padding(vertical = spacing.extraSmall),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(spacing.medium)
                        ) {
                            SpeedDialDigitBadge(digit = digit)
                            if (contact != null) {
                                ContactAvatar(
                                    photoUri = contact.photoUri,
                                    displayName = contact.displayName,
                                    size = 40.dp
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                if (number.isNullOrBlank()) {
                                    Text(
                                        text = stringResource(id = R.string.speed_dial_empty_slot),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Text(
                                        text = contact?.displayName?.takeIf { it.isNotBlank() }
                                            ?: number,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = number,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            IconButton(onClick = { menuDigit = digit }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_more_vert),
                                    contentDescription = null
                                )
                            }
                        }
                        GlassDropdownMenu(
                            expanded = menuDigit == digit,
                            onDismissRequest = { menuDigit = null },
                            modifier = Modifier.dialogEnter()
                        ) {
                            GlassMenuItem(
                                text = stringResource(id = R.string.speed_dial_change),
                                onClick = {
                                    menuDigit = null
                                    pickerDigit = digit
                                }
                            )
                            if (!number.isNullOrBlank()) {
                                GlassMenuItem(
                                    text = stringResource(id = R.string.speed_dial_remove),
                                    onClick = {
                                        menuDigit = null
                                        viewModel.onSpeedDialEntryRemoved(digit)
                                    }
                                )
                            }
                        }
                    }
                }
                if (digit != '9') {
                    SettingsListDivider()
                }
            }

            Spacer(modifier = Modifier.height(spacing.small))
            OutlinedButton(
                onClick = { showClearConfirm = true },
                enabled = settings.speedDial.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.speed_dial_clear_all))
            }
            }
        }
    }

    pickerDigit?.let { digit ->
        ContactPickerSheet(
            contactsRepository = contactsRepository,
            title = stringResource(id = R.string.speed_dial_assign_title, digit.toString()),
            onDismiss = { pickerDigit = null },
            onPick = { number ->
                pickerDigit = null
                viewModel.onSpeedDialEntrySet(digit, number)
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(text = stringResource(id = R.string.speed_dial_clear_all)) },
            text = { Text(text = stringResource(id = R.string.speed_dial_clear_all_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    viewModel.onSpeedDialCleared()
                }) {
                    Text(text = stringResource(id = R.string.speed_dial_clear_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun SpeedDialDigitBadge(digit: Char) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = digit.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
