package io.github.elli0t0.phone.feature.settings
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.pressScale
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.WrappingRow
import io.github.elli0t0.phone.data.settings.model.AccentPreset
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import io.github.elli0t0.phone.data.settings.model.BottomBarSize
import io.github.elli0t0.phone.data.settings.model.Backdrop
import io.github.elli0t0.phone.data.settings.model.GlassIntensity
import io.github.elli0t0.phone.data.settings.model.HeaderSize
import io.github.elli0t0.phone.data.settings.model.BottomBarStyle
import io.github.elli0t0.phone.data.settings.model.CallDisplay
import io.github.elli0t0.phone.data.settings.model.DefaultSim
import io.github.elli0t0.phone.data.settings.model.CardStyle
import io.github.elli0t0.phone.data.settings.model.KeyStyle
import io.github.elli0t0.phone.data.settings.model.ContactNameFormat
import io.github.elli0t0.phone.data.settings.model.ContactSortOrder
import io.github.elli0t0.phone.data.settings.model.ListDensity
import io.github.elli0t0.phone.data.settings.model.StartTab
import io.github.elli0t0.phone.data.settings.model.ThemeMode
import io.github.elli0t0.phone.data.settings.model.AppSettings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.ExperimentalLayoutApi

/**
 * All Settings dialogs (choice pickers, About, update check, USSD).
 * Extracted from SettingsScreen to keep that file under 400 lines.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
fun SettingsDialogs(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    dialogs: SettingsDialogState,
    updateCheckState: UpdateCheckState?,
    runUpdateCheck: () -> Unit,
    currentVersionName: String,
    lightHaptic: () -> Unit
) {
    val context = LocalContext.current
    with(settings) {
if (dialogs.showResetDialog) {
    AlertDialog(
        onDismissRequest = { dialogs.showResetDialog = false },
        title = { Text(text = stringResource(id = R.string.settings_reset_dialog_title)) },
        text = { Text(text = stringResource(id = R.string.settings_reset_dialog_message)) },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.onResetToDefaults()
                    dialogs.showResetDialog = false
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(text = stringResource(id = R.string.settings_reset_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = { dialogs.showResetDialog = false }) {
                Text(text = stringResource(id = R.string.action_cancel))
            }
        }
    )
}

if (dialogs.showCustomColorDialog) {
    AlertDialog(
        onDismissRequest = { dialogs.showCustomColorDialog = false },
        title = { Text(text = stringResource(id = R.string.settings_custom_color_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = dialogs.customColorHexInput,
                    onValueChange = {
                        dialogs.customColorHexInput = it
                        dialogs.customColorError = false
                    },
                    label = { Text(stringResource(id = R.string.settings_hex_code)) },
                    singleLine = true,
                    isError = dialogs.customColorError
                )
                if (dialogs.customColorError) {
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
                    val cleaned = dialogs.customColorHexInput.trim().removePrefix("#")
                    try {
                        val parsedColorLong = when (cleaned.length) {
                            6 -> ("FF$cleaned".toLong(16))
                            8 -> cleaned.toLong(16)
                            else -> throw IllegalArgumentException()
                        }
                        viewModel.onAccentColorSelected(parsedColorLong, isCustom = true)
                        dialogs.showCustomColorDialog = false
                    } catch (e: Exception) {
                        dialogs.customColorError = true
                    }
                }
            ) {
                Text(text = stringResource(id = R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = { dialogs.showCustomColorDialog = false }) {
                Text(text = stringResource(id = R.string.action_cancel))
            }
        }
    )
}

if (dialogs.showThemeDialog) {
    SettingsChoiceDialog(
        title = stringResource(id = R.string.settings_theme),
        options = ThemeMode.entries.map { it.label to it },
        selected = settings.themeMode,
        onSelect = { viewModel.onThemeModeChanged(it); dialogs.showThemeDialog = false },
        onDismiss = { dialogs.showThemeDialog = false }
    )
}

if (dialogs.showAccentDialog) {
    AlertDialog(
        onDismissRequest = { dialogs.showAccentDialog = false },
        title = { Text(text = stringResource(id = R.string.settings_accent_color)) },
        text = {
            Column {
                WrappingRow(
                    horizontalSpacing = PhoneDimens.settingsAccentSwatchSpacing,
                    verticalSpacing = PhoneDimens.settingsAccentSwatchSpacing,
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
                                    dialogs.showAccentDialog = false
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
                            dialogs.showAccentDialog = false
                            dialogs.customColorHexInput = String.format("#%06X", 0xFFFFFF and settings.accentColor.toInt())
                            dialogs.showCustomColorDialog = true
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
            TextButton(onClick = { dialogs.showAccentDialog = false }) {
                Text(text = stringResource(id = R.string.action_cancel))
            }
        }
    )
}

if (dialogs.showDensityDialog) {
    SettingsChoiceDialog(
        title = stringResource(id = R.string.settings_list_density),
        options = ListDensity.entries.map { it.label to it },
        selected = settings.listDensity,
        onSelect = { viewModel.onListDensityChanged(it); dialogs.showDensityDialog = false },
        onDismiss = { dialogs.showDensityDialog = false }
    )
}

if (dialogs.showCardStyleDialog) {
    SettingsChoiceDialog(
        title = stringResource(id = R.string.settings_card_style),
        options = CardStyle.entries.map { it.label to it },
        selected = settings.cardStyle,
        onSelect = { viewModel.onCardStyleChanged(it); dialogs.showCardStyleDialog = false },
        onDismiss = { dialogs.showCardStyleDialog = false }
    )
}

if (dialogs.showKeyStyleDialog) {
    SettingsChoiceDialog(
        title = stringResource(id = R.string.settings_key_style),
        options = KeyStyle.entries.map { it.label to it },
        selected = settings.keyStyle,
        onSelect = { viewModel.onKeyStyleChanged(it); dialogs.showKeyStyleDialog = false },
        onDismiss = { dialogs.showKeyStyleDialog = false }
    )
}

if (dialogs.showTabDialog) {
    SettingsChoiceDialog(
        title = stringResource(id = R.string.settings_default_tab),
        options = StartTab.entries.map { it.label to it },
        selected = settings.defaultStartTab,
        onSelect = { viewModel.setDefaultStartTab(it); dialogs.showTabDialog = false },
        onDismiss = { dialogs.showTabDialog = false }
    )
}

if (dialogs.showAnimDialog) {
    SettingsChoiceDialog(
        title = stringResource(id = R.string.settings_animations),
        options = AnimationOption.entries.map { it.label to it },
        selected = settings.animationOption,
        onSelect = { viewModel.onAnimationOptionChanged(it); dialogs.showAnimDialog = false },
        onDismiss = { dialogs.showAnimDialog = false }
    )
}

if (dialogs.showBottomBarStyleDialog) {
    SettingsChoiceDialog(
        title = stringResource(id = R.string.settings_bottom_bar_style),
        options = BottomBarStyle.entries.map { it.label to it },
        selected = settings.bottomBarStyle,
        onSelect = { viewModel.onBottomBarStyleChanged(it); dialogs.showBottomBarStyleDialog = false },
        onDismiss = { dialogs.showBottomBarStyleDialog = false }
    )
}

if (dialogs.showBottomBarSizeDialog) {
    SettingsChoiceDialog(
        title = stringResource(id = R.string.settings_bottom_bar_size),
        options = BottomBarSize.entries.map { it.label to it },
        selected = settings.bottomBarSize,
        onSelect = { viewModel.onBottomBarSizeChanged(it); dialogs.showBottomBarSizeDialog = false },
        onDismiss = { dialogs.showBottomBarSizeDialog = false }
    )
}

if (dialogs.showHeaderSizeDialog) {
    SettingsChoiceDialog(
        title = stringResource(id = R.string.settings_header_size),
        options = HeaderSize.entries.map { it.label to it },
        selected = settings.headerSize,
        onSelect = { viewModel.onHeaderSizeChanged(it); dialogs.showHeaderSizeDialog = false },
        onDismiss = { dialogs.showHeaderSizeDialog = false }
    )
}

if (dialogs.showGlassIntensityDialog) {
    SettingsChoiceDialog(
        title = stringResource(id = R.string.settings_glass_intensity),
        options = GlassIntensity.entries.map { it.label to it },
        selected = settings.glassIntensity,
        onSelect = { viewModel.onGlassIntensityChanged(it); dialogs.showGlassIntensityDialog = false },
        onDismiss = { dialogs.showGlassIntensityDialog = false }
    )
}

if (dialogs.showBackdropDialog) {
    SettingsChoiceDialog(
        title = stringResource(id = R.string.settings_backdrop),
        options = Backdrop.entries.map { it.label to it },
        selected = settings.backdrop,
        onSelect = { viewModel.onBackdropChanged(it); dialogs.showBackdropDialog = false },
        onDismiss = { dialogs.showBackdropDialog = false }
    )
}

if (dialogs.showSortDialog) {
    SettingsChoiceDialog(
        title = stringResource(id = R.string.settings_sort_order),
        options = ContactSortOrder.entries.map { it.label to it },
        selected = settings.contactSortOrder,
        onSelect = { viewModel.onContactSortOrderChanged(it); dialogs.showSortDialog = false },
        onDismiss = { dialogs.showSortDialog = false }
    )
}

if (dialogs.showFormatDialog) {
    SettingsChoiceDialog(
        title = stringResource(id = R.string.settings_name_format),
        options = ContactNameFormat.entries.map { it.label to it },
        selected = settings.contactNameFormat,
        onSelect = { viewModel.onContactNameFormatChanged(it); dialogs.showFormatDialog = false },
        onDismiss = { dialogs.showFormatDialog = false }
    )
}

if (dialogs.showCallDisplayDialog) {
    SettingsChoiceDialog(
        title = stringResource(id = R.string.settings_call_display),
        options = CallDisplay.entries.map { it.label to it },
        selected = settings.callDisplay,
        onSelect = { viewModel.setCallDisplay(it); dialogs.showCallDisplayDialog = false },
        onDismiss = { dialogs.showCallDisplayDialog = false }
    )
}

if (dialogs.showDefaultSimDialog) {
    SettingsChoiceDialog(
        title = stringResource(id = R.string.settings_default_sim),
        options = DefaultSim.entries.map { it.label to it },
        selected = settings.defaultSim,
        onSelect = { viewModel.setDefaultSim(it); dialogs.showDefaultSimDialog = false },
        onDismiss = { dialogs.showDefaultSimDialog = false }
    )
}

if (dialogs.showQuickDeclineDialog) {
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
            dialogs.showQuickDeclineDialog = false
        },
        onDismiss = { dialogs.showQuickDeclineDialog = false }
    )
}

    }
}
