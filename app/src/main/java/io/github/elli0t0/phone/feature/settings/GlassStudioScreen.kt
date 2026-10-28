package io.github.elli0t0.phone.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.AmbientBackdrop
import io.github.elli0t0.phone.core.design.GlassSurface
import io.github.elli0t0.phone.core.design.LiquidGlassPillShape
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.PhoneDimens
import io.github.elli0t0.phone.core.design.isDarkGlassTheme
import io.github.elli0t0.phone.data.settings.model.AppSettings
import io.github.elli0t0.phone.data.settings.model.Backdrop
import io.github.elli0t0.phone.data.settings.model.DropletStretch
import io.github.elli0t0.phone.data.settings.model.EdgeLight
import io.github.elli0t0.phone.data.settings.model.GlassHapticFeedback
import io.github.elli0t0.phone.navigation.InnerTopBar
import io.github.elli0t0.phone.navigation.Screen
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Glass Studio page, opened from the Liquid glass section of Settings.
 * A live preview card (one glass pill and one glass key over the real
 * backdrop) sits at the top; every control below applies immediately
 * because the glass modifiers read the settings live. The preview is
 * inert (no click handlers) and has no animations of its own, so it
 * stays still when the Animations setting is Off; Reduce transparency
 * falls back to the solid glass base as everywhere else.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassStudioScreen(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    var showEdgeLightDialog by remember { mutableStateOf(false) }
    var showDropletStretchDialog by remember { mutableStateOf(false) }
    var showHapticDialog by remember { mutableStateOf(false) }
    var showBackdropDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("glass_studio_screen")
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize()) {
            InnerTopBar(
                onBack = onNavigateBack,
                title = stringResource(id = R.string.settings_glass_studio),
                backTestTag = "btn_glass_studio_back"
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = spacing.screenHorizontalPadding,
                    top = spacing.small,
                    end = spacing.screenHorizontalPadding,
                    bottom = spacing.small + innerPadding.calculateBottomPadding()
                ),
                verticalArrangement = Arrangement.spacedBy(spacing.medium)
            ) {
                item {
                    Text(
                        text = stringResource(id = R.string.settings_glass_preview),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(PhoneDimens.glassStudioPreviewHeight)
                            .clip(RoundedCornerShape(PhoneDimens.menuCornerRadius))
                            .testTag("glass_studio_preview"),
                        contentAlignment = Alignment.Center
                    ) {
                        AmbientBackdrop(modifier = Modifier.fillMaxSize())
                        // Mini bottom bar: the bar's own glass
                        // treatment (same GlassSurface parameters,
                        // including the refraction switch) with three
                        // tab icons and the droplet on the middle one.
                        // The old pill + key preview never showed the
                        // bar's glass on-device; this one previews
                        // what Studio actually tunes. Inert: no
                        // gestures, no navigation. The translucent
                        // fill lets the backdrop show through even
                        // where the live blur has nothing to sample.
                        GlassSurface(
                            shape = LiquidGlassPillShape,
                            modifier = Modifier
                                .fillMaxWidth(PhoneDimens.bottomBarWidthFraction)
                                .height(PhoneDimens.bottomBarHeight),
                            containerAlpha = 0.20f,
                            blurRadius = 36.dp,
                            edgeHighlightAlpha = 0.10f,
                            edgeRefraction = settings.edgeRefraction
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PreviewBarIcon(
                                    screen = Screen.Dialpad,
                                    selected = false,
                                    modifier = Modifier.weight(1f)
                                )
                                PreviewBarIcon(
                                    screen = Screen.Recents,
                                    selected = true,
                                    modifier = Modifier.weight(1f)
                                )
                                PreviewBarIcon(
                                    screen = Screen.Contacts,
                                    selected = false,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                item {
                    Card(
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(spacing.medium)) {
                            GlassSliderRow(
                                title = stringResource(id = R.string.settings_glass_blur_amount),
                                valueLabel = String.format(
                                    Locale.US, "%.2fx", settings.glassBlurAmount
                                ),
                                value = settings.glassBlurAmount,
                                onValueChange = {
                                    viewModel.onGlassBlurAmountChanged(
                                        (it * 20).roundToInt() / 20f
                                    )
                                },
                                valueRange = 0.5f..1.5f,
                                steps = 19
                            )

                            Spacer(modifier = Modifier.height(spacing.small))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(spacing.small))

                            GlassSliderRow(
                                title = stringResource(id = R.string.settings_glass_tint),
                                valueLabel = "${(settings.glassTintAmount * 100).roundToInt()}%",
                                value = settings.glassTintAmount,
                                onValueChange = {
                                    viewModel.onGlassTintAmountChanged(
                                        (it * 20).roundToInt() / 20f
                                    )
                                },
                                valueRange = 0f..1f,
                                steps = 19
                            )

                            Spacer(modifier = Modifier.height(spacing.small))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(spacing.small))

                            GlassSliderRow(
                                title = stringResource(id = R.string.settings_glass_richness),
                                valueLabel = String.format(
                                    Locale.US, "%.2fx", settings.glassColorRichness
                                ),
                                value = settings.glassColorRichness,
                                onValueChange = {
                                    viewModel.onGlassColorRichnessChanged(
                                        (it * 20).roundToInt() / 20f
                                    )
                                },
                                valueRange = 1.0f..2.2f,
                                steps = 23
                            )
                        }
                    }
                }
                item {
                    Card(
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(spacing.medium)) {
                            SettingsRow(
                                title = stringResource(id = R.string.settings_edge_light),
                                subtitle = settings.edgeLight.label,
                                onClick = { showEdgeLightDialog = true }
                            )

                            Spacer(modifier = Modifier.height(spacing.small))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(spacing.small))

                            SettingsSwitchRow(
                                title = stringResource(id = R.string.settings_dynamic_highlights),
                                subtitle = stringResource(id = R.string.settings_dynamic_highlights_desc),
                                checked = settings.dynamicHighlights,
                                onCheckedChange = { viewModel.onDynamicHighlightsChanged(it) }
                            )

                            Spacer(modifier = Modifier.height(spacing.small))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(spacing.small))

                            SettingsSwitchRow(
                                title = stringResource(id = R.string.settings_edge_refraction),
                                subtitle = stringResource(id = R.string.settings_edge_refraction_desc),
                                checked = settings.edgeRefraction,
                                onCheckedChange = { viewModel.onEdgeRefractionChanged(it) }
                            )

                            Spacer(modifier = Modifier.height(spacing.small))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(spacing.small))

                            SettingsSwitchRow(
                                title = stringResource(id = R.string.settings_lens_bar),
                                subtitle = stringResource(id = R.string.settings_lens_bar_desc),
                                checked = settings.lensBar,
                                onCheckedChange = { viewModel.onLensBarChanged(it) }
                            )

                            Spacer(modifier = Modifier.height(spacing.small))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(spacing.small))

                            SettingsRow(
                                title = stringResource(id = R.string.settings_droplet_stretch),
                                subtitle = settings.dropletStretch.label,
                                onClick = { showDropletStretchDialog = true }
                            )

                            Spacer(modifier = Modifier.height(spacing.small))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(spacing.small))

                            SettingsRow(
                                title = stringResource(id = R.string.settings_glass_haptic),
                                subtitle = settings.glassHapticFeedback.label,
                                onClick = { showHapticDialog = true }
                            )

                            Spacer(modifier = Modifier.height(spacing.small))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(spacing.small))

                            SettingsRow(
                                title = stringResource(id = R.string.settings_backdrop),
                                // Pure black dark mode forces the backdrop off.
                                subtitle = if (settings.pureBlackDarkMode) {
                                    Backdrop.OFF.label
                                } else {
                                    settings.backdrop.label
                                },
                                onClick = { showBackdropDialog = true }
                            )
                        }
                    }
                }
                item {
                    FilledTonalButton(
                        onClick = { viewModel.onResetGlassDefaults() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(id = R.string.settings_glass_reset))
                    }
                }
            }
        }
    }

    if (showEdgeLightDialog) {
        SettingsChoiceDialog(
            title = stringResource(id = R.string.settings_edge_light),
            options = EdgeLight.entries.map { it.label to it },
            selected = settings.edgeLight,
            onSelect = { viewModel.onEdgeLightChanged(it); showEdgeLightDialog = false },
            onDismiss = { showEdgeLightDialog = false }
        )
    }
    if (showDropletStretchDialog) {
        SettingsChoiceDialog(
            title = stringResource(id = R.string.settings_droplet_stretch),
            options = DropletStretch.entries.map { it.label to it },
            selected = settings.dropletStretch,
            onSelect = { viewModel.onDropletStretchChanged(it); showDropletStretchDialog = false },
            onDismiss = { showDropletStretchDialog = false }
        )
    }
    if (showHapticDialog) {
        SettingsChoiceDialog(
            title = stringResource(id = R.string.settings_glass_haptic),
            options = GlassHapticFeedback.entries.map { it.label to it },
            selected = settings.glassHapticFeedback,
            onSelect = { viewModel.onGlassHapticFeedbackChanged(it); showHapticDialog = false },
            onDismiss = { showHapticDialog = false }
        )
    }
    if (showBackdropDialog) {
        SettingsChoiceDialog(
            title = stringResource(id = R.string.settings_backdrop),
            options = Backdrop.entries.map { it.label to it },
            selected = settings.backdrop,
            onSelect = { viewModel.onBackdropChanged(it); showBackdropDialog = false },
            onDismiss = { showBackdropDialog = false }
        )
    }
}

// Slider row mirroring the Font size scale slider: label + value on
// top, thin-track slider with the vertical-bar thumb below. The
// thumb slot is an experimental Material3 API, so this row needs its
// own opt-in (the page-level opt-in does not cover it).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlassSliderRow(
    title: String,
    valueLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
        )
        Text(
            text = valueLabel,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
    }
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
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
        }
    )
}

/**
 * One inert tab slot of the Studio preview bar: the tab icon, with
 * the neutral droplet behind it when selected, matching the bar's
 * icon tints (selected white / near-black, unselected 80% / 60%).
 */
@Composable
private fun PreviewBarIcon(
    screen: Screen,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val dark = isDarkGlassTheme()
    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 48.dp)
                    .background(
                        color = if (dark) {
                            Color.White.copy(alpha = 0.16f)
                        } else {
                            Color.Black.copy(alpha = 0.08f)
                        },
                        shape = RoundedCornerShape(50)
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(50)
                    )
            )
        }
        Icon(
            painter = painterResource(
                id = if (selected) screen.iconResSelected else screen.iconRes
            ),
            contentDescription = null,
            tint = if (selected) {
                if (dark) Color.White else Color.Black
            } else {
                if (dark) {
                    Color.White.copy(alpha = 0.80f)
                } else {
                    Color.Black.copy(alpha = 0.60f)
                }
            },
            modifier = Modifier.size(PhoneDimens.bottomBarIconSize)
        )
    }
}
