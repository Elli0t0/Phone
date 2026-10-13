package io.github.elli0t0.phone.feature.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.elli0t0.phone.PhoneApplication
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalSpacing
import io.github.elli0t0.phone.core.permissions.SetupPermissions
import io.github.elli0t0.phone.core.telecom.rememberDefaultDialerState
import io.github.elli0t0.phone.feature.setup.SetupChecklist
import io.github.elli0t0.phone.data.settings.model.AccentPreset
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import io.github.elli0t0.phone.data.settings.model.CardStyle
import io.github.elli0t0.phone.data.settings.model.ContactNameFormat
import io.github.elli0t0.phone.data.settings.model.ContactSortOrder
import io.github.elli0t0.phone.data.settings.model.ListDensity
import io.github.elli0t0.phone.data.settings.model.StartTab
import io.github.elli0t0.phone.data.settings.model.ThemeMode
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val app = context.applicationContext as PhoneApplication
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.provideFactory(app.container.settingsRepository)
    )
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val spacing = LocalSpacing.current
    val dialerState = rememberDefaultDialerState()

    // Permissions & setup card visibility: hidden once every required permission
    // and the default dialer role are granted. Re-checked on every resume so the
    // card reappears if anything is revoked, and disappears once granted.
    val lifecycleOwner = LocalLifecycleOwner.current
    var requiredSetupComplete by remember {
        mutableStateOf(SetupPermissions.isRequiredSetupComplete(context))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                requiredSetupComplete = SetupPermissions.isRequiredSetupComplete(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showResetDialog by remember { mutableStateOf(false) }
    var showCustomColorDialog by remember { mutableStateOf(false) }
    var customColorHexInput by remember { mutableStateOf("") }
    var customColorError by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(text = stringResource(id = R.string.settings_reset_dialog_title)) },
            text = { Text(text = stringResource(id = R.string.settings_reset_dialog_message)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onResetToDefaults()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(text = stringResource(id = R.string.settings_reset_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        )
    }

    if (showCustomColorDialog) {
        AlertDialog(
            onDismissRequest = { showCustomColorDialog = false },
            title = { Text(text = stringResource(id = R.string.settings_custom_color_dialog_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = customColorHexInput,
                        onValueChange = {
                            customColorHexInput = it
                            customColorError = false
                        },
                        label = { Text("Hex code (e.g. #0072DE)") },
                        singleLine = true,
                        isError = customColorError
                    )
                    if (customColorError) {
                        Text(
                            text = "Invalid hex color format",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleaned = customColorHexInput.trim().removePrefix("#")
                        try {
                            val parsedColorLong = when (cleaned.length) {
                                6 -> ("FF$cleaned".toLong(16))
                                8 -> cleaned.toLong(16)
                                else -> throw IllegalArgumentException()
                            }
                            viewModel.onAccentColorSelected(parsedColorLong, isCustom = true)
                            showCustomColorDialog = false
                        } catch (e: Exception) {
                            customColorError = true
                        }
                    }
                ) {
                    Text(text = stringResource(id = R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomColorDialog = false }) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("settings_screen"),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = stringResource(id = R.string.cd_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = spacing.screenHorizontalPadding),
            verticalArrangement = Arrangement.spacedBy(spacing.medium)
        ) {
            item {
                Spacer(modifier = Modifier.height(spacing.extraSmall))
            }

            // Default Phone App Card
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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

                        if (dialerState.isDefault) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_check),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(id = R.string.settings_is_default),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
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
                        shape = MaterialTheme.shapes.large,
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
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(spacing.medium)) {
                        // Theme (System, Light, Dark)
                        Text(
                            text = stringResource(id = R.string.settings_theme),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = stringResource(id = R.string.settings_theme_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(spacing.small))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = settings.themeMode == mode,
                                    onClick = { viewModel.onThemeModeChanged(mode) },
                                    label = {
                                        Text(
                                            when (mode) {
                                                ThemeMode.SYSTEM -> stringResource(id = R.string.settings_theme_system)
                                                ThemeMode.LIGHT -> stringResource(id = R.string.settings_theme_light)
                                                ThemeMode.DARK -> stringResource(id = R.string.settings_theme_dark)
                                            }
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(spacing.medium))

                        // Dynamic Color (Android 12+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = spacing.small)) {
                                    Text(
                                        text = stringResource(id = R.string.settings_dynamic_color),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = stringResource(id = R.string.settings_dynamic_color_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = settings.dynamicColor,
                                    onCheckedChange = { viewModel.onDynamicColorChanged(it) }
                                )
                            }
                            Spacer(modifier = Modifier.height(spacing.medium))
                        }

                        // Accent Color
                        Text(
                            text = stringResource(id = R.string.settings_accent_color),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = stringResource(id = R.string.settings_accent_color_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(spacing.small))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AccentPreset.entries.forEach { preset ->
                                val isSelected = !settings.dynamicColor &&
                                    !settings.isCustomAccent &&
                                    settings.accentColor == preset.colorValue

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(preset.colorValue))
                                        .clickable {
                                            viewModel.onAccentColorSelected(preset.colorValue, isCustom = false)
                                        }
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
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
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            // Custom Color Button
                            OutlinedButton(
                                onClick = {
                                    customColorHexInput = String.format("#%06X", 0xFFFFFF and settings.accentColor.toInt())
                                    showCustomColorDialog = true
                                },
                                shape = RoundedCornerShape(19.dp),
                                modifier = Modifier.height(38.dp)
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

                        Spacer(modifier = Modifier.height(spacing.medium))

                        // Pure Black Dark Mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = spacing.small)) {
                                Text(
                                    text = stringResource(id = R.string.settings_pure_black),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = stringResource(id = R.string.settings_pure_black_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.pureBlackDarkMode,
                                onCheckedChange = { viewModel.onPureBlackDarkModeChanged(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(spacing.medium))

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
                        Slider(
                            value = settings.fontSizeScale,
                            onValueChange = { viewModel.onFontSizeScaleChanged((it * 20).roundToInt() / 20f) },
                            valueRange = 0.85f..1.30f,
                            steps = 8
                        )
                    }
                }
            }

            // Category 2: Layout & Density
            item {
                SettingsSectionHeader(title = stringResource(id = R.string.settings_cat_layout))
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(spacing.medium)) {
                        Text(
                            text = stringResource(id = R.string.settings_list_density),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(spacing.small))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ListDensity.entries.forEach { density ->
                                FilterChip(
                                    selected = settings.listDensity == density,
                                    onClick = { viewModel.onListDensityChanged(density) },
                                    label = { Text(density.label) },
                                    colors = FilterChipDefaults.filterChipColors()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(spacing.medium))

                        Text(
                            text = stringResource(id = R.string.settings_card_style),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(spacing.small))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CardStyle.entries.forEach { style ->
                                FilterChip(
                                    selected = settings.cardStyle == style,
                                    onClick = { viewModel.onCardStyleChanged(style) },
                                    label = { Text(style.label) }
                                )
                            }
                        }
                    }
                }
            }

            // Category 3: Navigation & Behavior
            item {
                SettingsSectionHeader(title = stringResource(id = R.string.settings_cat_navigation))
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(spacing.medium)) {
                        Text(
                            text = stringResource(id = R.string.settings_default_tab),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(spacing.small))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StartTab.entries.forEach { tab ->
                                FilterChip(
                                    selected = settings.defaultStartTab == tab,
                                    onClick = { viewModel.setDefaultStartTab(tab) },
                                    label = { Text(tab.label) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(spacing.medium))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = spacing.small)) {
                                Text(
                                    text = stringResource(id = R.string.settings_bottom_labels),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = stringResource(id = R.string.settings_bottom_labels_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.showBottomBarLabels,
                                onCheckedChange = { viewModel.onShowBottomBarLabelsChanged(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(spacing.medium))

                        Text(
                            text = stringResource(id = R.string.settings_animations),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(spacing.small))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AnimationOption.entries.forEach { anim ->
                                FilterChip(
                                    selected = settings.animationOption == anim,
                                    onClick = { viewModel.onAnimationOptionChanged(anim) },
                                    label = { Text(anim.label) }
                                )
                            }
                        }
                    }
                }
            }

            // Category 4: Contacts Display
            item {
                SettingsSectionHeader(title = stringResource(id = R.string.settings_cat_contacts))
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(spacing.medium)) {
                        Text(
                            text = stringResource(id = R.string.settings_sort_order),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(spacing.small))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ContactSortOrder.entries.forEach { order ->
                                FilterChip(
                                    selected = settings.contactSortOrder == order,
                                    onClick = { viewModel.onContactSortOrderChanged(order) },
                                    label = { Text(order.label) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(spacing.medium))

                        Text(
                            text = stringResource(id = R.string.settings_name_format),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(spacing.small))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ContactNameFormat.entries.forEach { format ->
                                FilterChip(
                                    selected = settings.contactNameFormat == format,
                                    onClick = { viewModel.onContactNameFormatChanged(format) },
                                    label = { Text(format.label) }
                                )
                            }
                        }
                    }
                }
            }

            // Category 5: Reset
            item {
                SettingsSectionHeader(title = stringResource(id = R.string.settings_cat_reset))
            }

            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(spacing.medium)) {
                        OutlinedButton(
                            onClick = { showResetDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = stringResource(id = R.string.settings_reset_button))
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(spacing.large))
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
