package io.github.elli0t0.phone.feature.settings
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalSpacing

/**
 * The Settings LazyColumn sections (Appearance, Liquid glass, Layout,
 * Navigation, Contacts, Calls, Privacy, About, Reset). Extracted from
 * SettingsScreen to keep that file under 400 lines. Each section opens
 * its dialogs via [dialogs]; the dialogs themselves live in the main
 * SettingsScreen function.
 */
fun androidx.compose.foundation.lazy.LazyListScope.settingsSectionsLayoutNav(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    dialogs: SettingsDialogState,
    dialerState: io.github.elli0t0.phone.core.telecom.DefaultDialerState,
    showSupplementaryRow: Boolean
) {
    val spacing = LocalSpacing.current
    val context = LocalContext.current
// Category 2: Layout & Density
item {
    SettingsSectionHeader(title = stringResource(id = R.string.settings_cat_layout))
}

item {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(spacing.medium)) {
            SettingsRow(
                title = stringResource(id = R.string.settings_list_density),
                subtitle = settings.listDensity.label,
                onClick = { dialogs.showDensityDialog = true }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsRow(
                title = stringResource(id = R.string.settings_card_style),
                subtitle = settings.cardStyle.label,
                onClick = { dialogs.showCardStyleDialog = true }
            )
        }
    }
}

// Category 3: Navigation & Behavior
item {
    SettingsSectionHeader(title = stringResource(id = R.string.settings_cat_navigation))
}

item {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(spacing.medium)) {
            SettingsRow(
                title = stringResource(id = R.string.settings_default_tab),
                subtitle = settings.defaultStartTab.label,
                onClick = { dialogs.showTabDialog = true }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsSwitchRow(
                title = stringResource(id = R.string.settings_bottom_labels),
                subtitle = stringResource(id = R.string.settings_bottom_labels_desc),
                checked = settings.showBottomBarLabels,
                onCheckedChange = { viewModel.onShowBottomBarLabelsChanged(it) }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsRow(
                title = stringResource(id = R.string.settings_bottom_bar_style),
                subtitle = settings.bottomBarStyle.label,
                onClick = { dialogs.showBottomBarStyleDialog = true }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsRow(
                title = stringResource(id = R.string.settings_bottom_bar_size),
                subtitle = settings.bottomBarSize.label,
                onClick = { dialogs.showBottomBarSizeDialog = true }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsSwitchRow(
                title = stringResource(id = R.string.settings_bottom_bar_shrink),
                subtitle = stringResource(id = R.string.settings_bottom_bar_shrink_desc),
                checked = settings.shrinkBottomBarOnScroll,
                onCheckedChange = { viewModel.onShrinkBottomBarOnScrollChanged(it) }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsRow(
                title = stringResource(id = R.string.settings_animations),
                subtitle = settings.animationOption.label,
                onClick = { dialogs.showAnimDialog = true }
            )
        }
    }
}

}
