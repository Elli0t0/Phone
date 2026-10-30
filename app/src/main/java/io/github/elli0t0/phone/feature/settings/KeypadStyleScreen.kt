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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.common.bidiWrap
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.data.settings.model.AppSettings
import io.github.elli0t0.phone.data.settings.model.KeypadStyle
import io.github.elli0t0.phone.feature.dialpad.DialpadKeypadGrid

/**
 * The Keypad style page, mirroring the App style page: one
 * preview tile per style, then a "Try it" area with the real
 * keypad grid in the selected style — tapping its keys builds
 * a number live. Picking a style applies it immediately
 * (settings apply live) and the saved style drives the real
 * keypad. Entry: Settings -> Layout & navigation -> Keypad style.
 */
@Composable
fun KeypadStyleScreen(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    var tryNumber by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("keypad_style_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
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
                    text = bidiWrap(stringResource(id = R.string.settings_keypad_style)),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
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
                            KeypadStyleTile(
                                style = KeypadStyle.GLASS,
                                selected = settings.keypadStyle == KeypadStyle.GLASS,
                                onClick = { viewModel.onKeypadStyleChanged(KeypadStyle.GLASS) }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            KeypadStyleTile(
                                style = KeypadStyle.CLASSIC,
                                selected = settings.keypadStyle == KeypadStyle.CLASSIC,
                                onClick = { viewModel.onKeypadStyleChanged(KeypadStyle.CLASSIC) }
                            )
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.medium)) {
                        Box(modifier = Modifier.weight(1f)) {
                            KeypadStyleTile(
                                style = KeypadStyle.MINIMAL,
                                selected = settings.keypadStyle == KeypadStyle.MINIMAL,
                                onClick = { viewModel.onKeypadStyleChanged(KeypadStyle.MINIMAL) }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            KeypadStyleTile(
                                style = KeypadStyle.SQUIRCLE,
                                selected = settings.keypadStyle == KeypadStyle.SQUIRCLE,
                                onClick = { viewModel.onKeypadStyleChanged(KeypadStyle.SQUIRCLE) }
                            )
                        }
                    }
                }
                item {
                    SettingsSectionHeader(title = stringResource(id = R.string.settings_keypad_style_try_it))
                }
                item {
                    Card(
                        shape = settingsListCardShape(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(spacing.medium)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tryNumber.ifBlank {
                                        stringResource(id = R.string.settings_keypad_style_try_hint)
                                    },
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Light,
                                    color = if (tryNumber.isBlank()) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { tryNumber = "" }) {
                                    Text(text = stringResource(id = R.string.settings_keypad_style_clear))
                                }
                            }
                            DialpadKeypadGrid(
                                onKeyPressed = { digit -> tryNumber += digit },
                                onLongPressDigit = { digit ->
                                    // Mirror the real keypad: long-press
                                    // 0 types +, other long-presses type
                                    // the digit (no speed dial here).
                                    tryNumber += if (digit == '0') "+" else digit.toString()
                                },
                                keypadStyle = settings.keypadStyle
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadStyleTile(
    style: KeypadStyle,
    selected: Boolean,
    onClick: () -> Unit
) {
    val spacing = LocalSpacing.current
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
            .testTag("keypad_style_tile_${style.name}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Static preview only (like the App style screen's mocks):
        // the live keys live in the Try it area below, so the
        // dial_key_<tag> testTags stay unique to it.
        Row(
            modifier = Modifier.padding(top = spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf('1', '2', '3').forEach { digit ->
                KeyMock(style = style, digit = digit)
            }
        }
        Text(
            text = stringResource(id = keypadStyleNameRes(style)),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = spacing.small)
        )
    }
}

private fun keypadStyleNameRes(style: KeypadStyle): Int = when (style) {
    KeypadStyle.GLASS -> R.string.settings_keypad_style_glass
    KeypadStyle.CLASSIC -> R.string.settings_keypad_style_classic
    KeypadStyle.MINIMAL -> R.string.settings_keypad_style_minimal
    KeypadStyle.SQUIRCLE -> R.string.settings_keypad_style_squircle
}

/** One static mini key in the tile preview's style. */
@Composable
private fun KeyMock(style: KeypadStyle, digit: Char) {
    val size = 48.dp
    val onSurface = MaterialTheme.colorScheme.onSurface
    when (style) {
        KeypadStyle.GLASS -> Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(onSurface.copy(alpha = 0.08f))
                .border(1.dp, onSurface.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = digit.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                color = onSurface
            )
        }
        KeypadStyle.CLASSIC -> Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            onSurface.copy(alpha = 0.14f),
                            onSurface.copy(alpha = 0.04f)
                        )
                    )
                )
                .border(1.dp, onSurface.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = digit.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Normal,
                color = onSurface
            )
        }
        KeypadStyle.MINIMAL -> Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = digit.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                color = onSurface
            )
        }
        KeypadStyle.SQUIRCLE -> Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = digit.toString(),
                fontSize = 20.sp,
                fontWeight = FontWeight.Light,
                color = onSurface
            )
        }
    }
}
