package io.github.elli0t0.phone.feature.settings
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.feature.setup.SetupChecklist
import io.github.elli0t0.phone.data.settings.model.AccentPreset
import kotlin.math.roundToInt
import io.github.elli0t0.phone.data.settings.model.AppSettings
import io.github.elli0t0.phone.data.settings.model.Backdrop
import androidx.compose.runtime.Composable
import io.github.elli0t0.phone.core.design.Spacing
import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.ExperimentalLayoutApi

/**
 * The Settings LazyColumn sections (Appearance, Liquid glass, Layout,
 * Navigation, Contacts, Calls, Privacy, About, Reset). Extracted from
 * SettingsScreen to keep that file under 400 lines. Each section opens
 * its dialogs via [dialogs]; the dialogs themselves live in the main
 * SettingsScreen function.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
fun androidx.compose.foundation.lazy.LazyListScope.settingsSections(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    dialogs: SettingsDialogState,
    dialerState: io.github.elli0t0.phone.core.telecom.DefaultDialerState,
    showSupplementaryRow: Boolean,
    requiredSetupComplete: Boolean,
    spacing: Spacing,
    context: Context
) {
item {
    Spacer(modifier = Modifier.height(spacing.extraSmall))
}

// Default Phone App Card: hidden once Phone is the default dialer,
// since there is nothing left to do here.
if (!dialerState.isDefault) {
item {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
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
            shape = MaterialTheme.shapes.extraLarge,
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

// Category 1: Appearance & Display
item {
    SettingsSectionHeader(title = stringResource(id = R.string.settings_cat_appearance))
}

item {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(spacing.medium)) {
            SettingsRow(
                title = stringResource(id = R.string.settings_theme),
                subtitle = settings.themeMode.label,
                onClick = { dialogs.showThemeDialog = true }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(spacing.small))

            // Dynamic Color (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SettingsSwitchRow(
                    title = stringResource(id = R.string.settings_dynamic_color),
                    subtitle = stringResource(id = R.string.settings_dynamic_color_desc),
                    checked = settings.dynamicColor,
                    onCheckedChange = { viewModel.onDynamicColorChanged(it) }
                )
                Spacer(modifier = Modifier.height(spacing.small))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(spacing.small))
            }

            SettingsRow(
                title = stringResource(id = R.string.settings_accent_color),
                subtitle = AccentPreset.entries.find { it.colorValue == settings.accentColor }?.label
                    ?: stringResource(id = R.string.settings_custom_color),
                onClick = { dialogs.showAccentDialog = true }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsSwitchRow(
                title = stringResource(id = R.string.settings_pure_black),
                subtitle = stringResource(id = R.string.settings_pure_black_desc),
                checked = settings.pureBlackDarkMode,
                onCheckedChange = { viewModel.onPureBlackDarkModeChanged(it) }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(spacing.small))

            // Font size scale
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(id = R.string.settings_font_scale),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = "${(settings.fontSizeScale * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            // Font size scale: thin 4dp track, vertical-bar thumb,
            // small tick marks at each stop. Value-based Slider
            // with thumb/track slots (Material3 1.3.0 overload).
            Slider(
                value = settings.fontSizeScale,
                onValueChange = { viewModel.onFontSizeScaleChanged((it * 20).roundToInt() / 20f) },
                valueRange = 0.85f..1.30f,
                steps = 8,
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(
                                width = PhoneDimens.settingsSliderThumbWidth,
                                height = PhoneDimens.settingsSliderThumbHeight
                            )
                            .clip(RoundedCornerShape(PhoneDimens.settingsSliderCornerRadius))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                },
                track = { sliderState ->
                    val tickCount = 10
                    val fraction = (
                        sliderState.value - sliderState.valueRange.start
                        ) / (sliderState.valueRange.endInclusive - sliderState.valueRange.start)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(PhoneDimens.settingsSliderTrackHeight)
                            .clip(RoundedCornerShape(PhoneDimens.settingsSliderCornerRadius))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .height(PhoneDimens.settingsSliderTrackHeight)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(tickCount) {
                                Box(
                                    modifier = Modifier
                                        .size(PhoneDimens.settingsSliderTickSize)
                                        .clip(CircleShape)
                                        .background(
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                )
                            }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsRow(
                title = stringResource(id = R.string.settings_header_size),
                subtitle = settings.headerSize.label,
                onClick = { dialogs.showHeaderSizeDialog = true }
            )
        }
    }
}

// Liquid glass: per-section on/off switches.
item {
    SettingsSectionHeader(title = stringResource(id = R.string.settings_cat_liquid_glass))
}

item {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(spacing.medium)) {
            SettingsSwitchRow(
                title = stringResource(id = R.string.settings_glass_bottom_bar),
                subtitle = stringResource(id = R.string.settings_glass_bottom_bar_desc),
                checked = settings.glassBottomBar,
                onCheckedChange = { viewModel.onGlassBottomBarChanged(it) }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsSwitchRow(
                title = stringResource(id = R.string.settings_glass_dialpad),
                subtitle = stringResource(id = R.string.settings_glass_dialpad_desc),
                checked = settings.glassDialpad,
                onCheckedChange = { viewModel.onGlassDialpadChanged(it) }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsSwitchRow(
                title = stringResource(id = R.string.settings_glass_headers),
                subtitle = stringResource(id = R.string.settings_glass_headers_desc),
                checked = settings.glassHeaders,
                onCheckedChange = { viewModel.onGlassHeadersChanged(it) }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsSwitchRow(
                title = stringResource(id = R.string.settings_glass_contact_detail),
                subtitle = stringResource(id = R.string.settings_glass_contact_detail_desc),
                checked = settings.glassContactDetail,
                onCheckedChange = { viewModel.onGlassContactDetailChanged(it) }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsSwitchRow(
                title = stringResource(id = R.string.settings_glass_call_screens),
                subtitle = stringResource(id = R.string.settings_glass_call_screens_desc),
                checked = settings.glassCallScreens,
                onCheckedChange = { viewModel.onGlassCallScreensChanged(it) }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsRow(
                title = stringResource(id = R.string.settings_glass_intensity),
                subtitle = settings.glassIntensity.label,
                onClick = { dialogs.showGlassIntensityDialog = true }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsSwitchRow(
                title = stringResource(id = R.string.settings_reduce_transparency),
                subtitle = stringResource(id = R.string.settings_reduce_transparency_desc),
                checked = settings.reduceTransparency,
                onCheckedChange = { viewModel.onReduceTransparencyChanged(it) }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsSwitchRow(
                title = stringResource(id = R.string.settings_glass_cards),
                subtitle = stringResource(id = R.string.settings_glass_cards_desc),
                checked = settings.glassCards,
                onCheckedChange = { viewModel.onGlassCardsChanged(it) }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsSwitchRow(
                title = stringResource(id = R.string.settings_dynamic_highlights),
                subtitle = stringResource(id = R.string.settings_dynamic_highlights_desc),
                checked = settings.dynamicHighlights,
                onCheckedChange = { viewModel.onDynamicHighlightsChanged(it) }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsRow(
                title = stringResource(id = R.string.settings_backdrop),
                // Pure black dark mode forces the backdrop off.
                subtitle = if (settings.pureBlackDarkMode) {
                    Backdrop.OFF.label
                } else {
                    settings.backdrop.label
                },
                onClick = { dialogs.showBackdropDialog = true }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsRow(
                title = stringResource(id = R.string.settings_glass_studio),
                subtitle = stringResource(id = R.string.settings_glass_studio_desc),
                onClick = { dialogs.showGlassStudioPage = true }
            )
        }
    }
}

}
