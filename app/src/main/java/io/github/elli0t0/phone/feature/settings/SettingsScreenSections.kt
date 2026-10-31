package io.github.elli0t0.phone.feature.settings
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
import io.github.elli0t0.phone.data.settings.model.AccentChoice
import io.github.elli0t0.phone.data.settings.model.accentChoice
import kotlin.math.roundToInt
import io.github.elli0t0.phone.data.settings.model.AppSettings
import androidx.compose.runtime.Composable
import io.github.elli0t0.phone.core.design.Spacing
import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.ExperimentalLayoutApi

/**
 * The Settings section cards (Appearance, Liquid glass). Each card is
 * one page of the restructured Settings: the main page shows a row
 * per section, and SettingsPages renders the card full-screen.
 * The rows open their dialogs via [dialogs]; the dialogs themselves
 * live in the main SettingsScreen function.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppearanceCard(
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
            SettingsSwitchRow(
                title = stringResource(id = R.string.settings_minimal_mode),
                searchId = "minimal_mode",
                subtitle = stringResource(id = R.string.settings_minimal_mode_desc),
                checked = settings.minimalMode,
                onCheckedChange = { viewModel.onMinimalModeChanged(it) }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsRow(
                title = stringResource(id = R.string.settings_app_style),
                subtitle = settings.appStyle.label,
                onClick = { dialogs.showAppStylePage = true }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsRow(
                title = stringResource(id = R.string.settings_theme),
                searchId = "theme",
                subtitle = settings.themeMode.label,
                onClick = { dialogs.showThemeDialog = true }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsRow(
                title = stringResource(id = R.string.settings_accent_color),
                searchId = "accent",
                subtitle = when (settings.accentChoice) {
                    AccentChoice.DYNAMIC -> stringResource(id = R.string.settings_dynamic_color)
                    AccentChoice.PRESETS -> AccentPreset.entries.find { it.colorValue == settings.accentColor }?.label
                        ?: stringResource(id = R.string.settings_custom_color)
                    AccentChoice.CUSTOM -> stringResource(id = R.string.settings_custom_color)
                },
                onClick = { dialogs.showAccentDialog = true }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsSwitchRow(
                title = stringResource(id = R.string.settings_pure_black),
                searchId = "pure_black",
                subtitle = stringResource(id = R.string.settings_pure_black_desc),
                checked = settings.pureBlackDarkMode,
                onCheckedChange = { viewModel.onPureBlackDarkModeChanged(it) }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            // Font size scale
            Column(modifier = Modifier.fillMaxWidth().searchHighlight("font_scale")) {
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

            }
            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsRow(
                title = stringResource(id = R.string.settings_header_size),
                searchId = "header_size",
                subtitle = settings.headerSize.label,
                onClick = { dialogs.showHeaderSizeDialog = true }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsRow(
                title = stringResource(id = R.string.settings_app_background),
                searchId = "app_background",
                titleColor = if (settings.minimalMode) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                subtitle = if (settings.minimalMode) {
                    stringResource(id = R.string.settings_off_in_minimal_mode)
                } else {
                    settings.appBackground.label
                },
                onClick = { dialogs.showAppBackgroundPage = true }
            )
        }
    }
}

@Composable
fun GlassCard(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    dialogs: SettingsDialogState,
    spacing: Spacing
) {
    // Minimal mode turns every glass surface off at the root, so
    // these rows show the stored values dimmed with an "off"
    // subtitle instead of their value while it is on.
    val minimal = settings.minimalMode
    val offSubtitle = stringResource(id = R.string.settings_off_in_minimal_mode)
    val dimmedTitle = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    Card(
        shape = settingsListCardShape(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(spacing.medium)) {
            SettingsSwitchRow(
                title = stringResource(id = R.string.settings_cat_liquid_glass),
                searchId = "glass_master",
                subtitle = if (minimal) offSubtitle else stringResource(id = R.string.settings_glass_master_desc),
                checked = settings.glassHeaders || settings.glassContactDetail ||
                    settings.glassCallScreens || settings.glassBottomBar,
                dimmed = minimal,
                onCheckedChange = { viewModel.onGlassMasterChanged(it) }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsRow(
                title = stringResource(id = R.string.settings_glass_intensity),
                searchId = "glass_intensity",
                titleColor = if (minimal) dimmedTitle else MaterialTheme.colorScheme.onSurface,
                subtitle = if (minimal) offSubtitle else settings.glassIntensity.label,
                onClick = { dialogs.showGlassIntensityDialog = true }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsSwitchRow(
                title = stringResource(id = R.string.settings_reduce_transparency),
                searchId = "reduce_transparency",
                subtitle = stringResource(id = R.string.settings_reduce_transparency_desc),
                checked = settings.reduceTransparency,
                onCheckedChange = { viewModel.onReduceTransparencyChanged(it) }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsSwitchRow(
                title = stringResource(id = R.string.settings_dynamic_highlights),
                searchId = "glass_dynamic_highlights",
                subtitle = if (minimal) offSubtitle else stringResource(id = R.string.settings_dynamic_highlights_desc),
                checked = settings.dynamicHighlights,
                dimmed = minimal,
                onCheckedChange = { viewModel.onDynamicHighlightsChanged(it) }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsRow(
                title = stringResource(id = R.string.settings_glass_studio),
                titleColor = if (minimal) dimmedTitle else MaterialTheme.colorScheme.onSurface,
                subtitle = if (minimal) offSubtitle else stringResource(id = R.string.settings_glass_studio_desc),
                onClick = { dialogs.showGlassStudioPage = true }
            )
        }
    }
}
