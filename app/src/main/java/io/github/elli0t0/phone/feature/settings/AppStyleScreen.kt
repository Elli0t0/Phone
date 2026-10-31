package io.github.elli0t0.phone.feature.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.common.bidiWrap
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.design.fontFamilyFor
import io.github.elli0t0.phone.core.design.isDarkGlassTheme
import io.github.elli0t0.phone.data.settings.model.AppSettings
import io.github.elli0t0.phone.data.settings.model.AppStyle
import io.github.elli0t0.phone.data.settings.model.CardStyle
import io.github.elli0t0.phone.data.settings.model.FontStyle
import io.github.elli0t0.phone.data.settings.model.KeyStyle
import io.github.elli0t0.phone.data.settings.model.StylePreset
import io.github.elli0t0.phone.data.settings.model.SwitchStyle

/**
 * The App style page: two preset cards (Classic, Clean) with a mini
 * mock of each look, a "Try it" preview of the current look, and a
 * Customize section with the individual options. Picking a preset
 * applies it immediately (settings apply live); preset users see a
 * Customize entry that flips to Custom keeping the preset's values,
 * and changing any individual option flips the style to Custom.
 */
@Composable
fun AppStyleScreen(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    // Customize always shows (0.18.6): the advanced gate is gone,
    // and picking any individual option flips the style to Custom.
    var showFontDialog by remember { mutableStateOf(false) }
    var showCardDialog by remember { mutableStateOf(false) }
    var showSwitchDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("app_style_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Title row in InnerTopBar's shape, plus the Custom badge
            // on the title (InnerTopBar itself has no badge slot).
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusBarTop + 8.dp)
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = stringResource(id = R.string.cd_back)
                    )
                }
                Text(
                    text = bidiWrap(stringResource(id = R.string.settings_app_style)),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                if (settings.appStyle == AppStyle.CUSTOM) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.settings_app_style_custom),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(spacing.small))
                }
            }
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
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.medium)) {
                        Box(modifier = Modifier.weight(1f)) {
                            AppStyleTile(
                                style = AppStyle.CLASSIC,
                                selected = settings.appStyle == AppStyle.CLASSIC,
                                onClick = { viewModel.onAppStyleChanged(AppStyle.CLASSIC) }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            AppStyleTile(
                                style = AppStyle.CLEAN,
                                selected = settings.appStyle == AppStyle.CLEAN,
                                onClick = { viewModel.onAppStyleChanged(AppStyle.CLEAN) }
                            )
                        }
                    }
                }
                item {
                    SettingsSectionHeader(title = stringResource(id = R.string.settings_app_style_try_it))
                }
                item {
                    Card(
                        shape = settingsListCardShape(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        StyleMock(
                            preset = settings.effectiveStyle,
                            modifier = Modifier.padding(spacing.medium)
                        )
                    }
                }
                item {
                    SettingsSectionHeader(title = stringResource(id = R.string.settings_app_style_customize))
                }
                item {
                    Card(
                        shape = settingsListCardShape(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(spacing.medium)) {
                            SettingsRow(
                                title = stringResource(id = R.string.settings_font_style),
                                searchId = "style_font",
                                subtitle = settings.fontStyle.label,
                                onClick = { showFontDialog = true }
                            )

                            Spacer(modifier = Modifier.height(spacing.small))
                            SettingsListDivider()
                            Spacer(modifier = Modifier.height(spacing.small))

                            SettingsRow(
                                title = stringResource(id = R.string.settings_card_style),
                                searchId = "style_card",
                                titleColor = if (settings.minimalMode) {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                subtitle = if (settings.minimalMode) {
                                    stringResource(id = R.string.settings_off_in_minimal_mode)
                                } else {
                                    settings.cardStyle.label
                                },
                                enabled = !settings.minimalMode,
                                onClick = { showCardDialog = true }
                            )

                            Spacer(modifier = Modifier.height(spacing.small))
                            SettingsListDivider()
                            Spacer(modifier = Modifier.height(spacing.small))

                            SettingsRow(
                                title = stringResource(id = R.string.settings_switch_style),
                                searchId = "style_switch",
                                subtitle = settings.switchStyle.label,
                                onClick = { showSwitchDialog = true }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFontDialog) {
        StyleChoicePicker(
            title = stringResource(id = R.string.settings_font_style),
            options = FontStyle.entries.map { it.label to it },
            selected = settings.fontStyle,
            sheets = settings.effectiveStyle.sheets,
            onSelect = { viewModel.onFontStyleChanged(it); showFontDialog = false },
            onDismiss = { showFontDialog = false }
        )
    }
    if (showCardDialog) {
        StyleChoicePicker(
            title = stringResource(id = R.string.settings_card_style),
            options = CardStyle.entries.map { it.label to it },
            selected = settings.cardStyle,
            sheets = settings.effectiveStyle.sheets,
            onSelect = { viewModel.onCardStyleChanged(it); showCardDialog = false },
            onDismiss = { showCardDialog = false }
        )
    }
    if (showSwitchDialog) {
        StyleChoicePicker(
            title = stringResource(id = R.string.settings_switch_style),
            options = SwitchStyle.entries.map { it.label to it },
            selected = settings.switchStyle,
            sheets = settings.effectiveStyle.sheets,
            onSelect = { viewModel.onSwitchStyleChanged(it); showSwitchDialog = false },
            onDismiss = { showSwitchDialog = false }
        )
    }
}

/** One choice picker that follows the sheets flag: sheet or dialog. */
@Composable
private fun <T> StyleChoicePicker(
    title: String,
    options: List<Pair<String, T>>,
    selected: T,
    sheets: Boolean,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    if (sheets) {
        SettingsChoiceSheet(
            title = title,
            options = options,
            selected = selected,
            onSelect = onSelect,
            onDismiss = onDismiss
        )
    } else {
        SettingsChoiceDialog(
            title = title,
            options = options,
            selected = selected,
            onSelect = onSelect,
            onDismiss = onDismiss
        )
    }
}

private fun cardCorner(card: CardStyle) = when (card) {
    CardStyle.ROUNDED -> RoundedCornerShape(24.dp)
    CardStyle.SQUARE -> RoundedCornerShape(4.dp)
    CardStyle.OUTLINED -> RoundedCornerShape(16.dp)
    CardStyle.INSET -> RoundedCornerShape(14.dp)
}

@Composable
private fun AppStyleTile(
    style: AppStyle,
    selected: Boolean,
    onClick: () -> Unit
) {
    val spacing = LocalSpacing.current
    val preset = if (style == AppStyle.CLASSIC) StylePreset.CLASSIC else StylePreset.CLEAN
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        animationSpec = tween(durationMillis = 280),
        label = "tileBorderColor"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 2.dp else 1.dp,
        animationSpec = tween(durationMillis = 280),
        label = "tileBorderWidth"
    )
    val tileShape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .clip(tileShape)
            .border(borderWidth, borderColor, tileShape)
            .clickable { onClick() }
            .testTag("app_style_tile_${style.name}")
    ) {
        StyleMock(
            preset = preset,
            modifier = Modifier.padding(spacing.medium)
        )
        Text(
            text = stringResource(
                id = if (style == AppStyle.CLASSIC) {
                    R.string.settings_app_style_classic
                } else {
                    R.string.settings_app_style_clean
                }
            ),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = spacing.medium)
        )
    }
}

/**
 * Mini mock of one look: a list row in the preset's card corner and
 * font, a keypad key in the preset's key style, and a switch in the
 * preset's switch style. Static shapes only, no real behavior.
 */
@Composable
private fun StyleMock(
    preset: StylePreset,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    val dark = isDarkGlassTheme()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        // Fake list row, in the preset's card corner.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardCorner(preset.card))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = spacing.small, vertical = spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            )
            Spacer(modifier = Modifier.width(spacing.small))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Name",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = fontFamilyFor(preset.font)
                    )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                )
            }
        }
        // Keypad key + switch side by side.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val keyBg = when (preset.keys) {
                KeyStyle.FLAT -> if (dark) Color(0xFF333336) else Color(0xFFE5E5EA)
                KeyStyle.GLASS -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                KeyStyle.SIMPLE -> MaterialTheme.colorScheme.surfaceVariant
            }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(keyBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "5",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = fontFamilyFor(preset.font)
                    )
                )
            }
            val trackColor = when (preset.switch) {
                SwitchStyle.GREEN -> Color(0xFF34C759)
                SwitchStyle.DEFAULT -> MaterialTheme.colorScheme.primary
            }
            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 28.dp)
                    .clip(CircleShape)
                    .background(trackColor),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .padding(end = 2.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
        }
    }
}
