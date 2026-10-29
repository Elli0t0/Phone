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
import io.github.elli0t0.phone.data.settings.model.AppSettings
import androidx.compose.runtime.Composable
import io.github.elli0t0.phone.core.design.Spacing
import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.ExperimentalLayoutApi

/**
 * The Settings section cards (Layout, Navigation). Each card is one
 * part of the Layout & navigation page of the restructured Settings.
 * The rows open their dialogs via [dialogs]; the dialogs themselves
 * live in the main SettingsScreen function.
 */
@Composable
fun LayoutCard(
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
            SettingsRow(
                title = stringResource(id = R.string.settings_list_density),
                subtitle = settings.listDensity.label,
                onClick = { dialogs.showDensityDialog = true }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsRow(
                title = stringResource(id = R.string.settings_card_style),
                subtitle = settings.cardStyle.label,
                onClick = { dialogs.showCardStyleDialog = true }
            )
        }
    }
}

@Composable
fun NavigationCard(
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
            SettingsRow(
                title = stringResource(id = R.string.settings_default_tab),
                subtitle = settings.defaultStartTab.label,
                onClick = { dialogs.showTabDialog = true }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsSwitchRow(
                title = stringResource(id = R.string.settings_bottom_labels),
                subtitle = stringResource(id = R.string.settings_bottom_labels_desc),
                checked = settings.showBottomBarLabels,
                onCheckedChange = { viewModel.onShowBottomBarLabelsChanged(it) }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsRow(
                title = stringResource(id = R.string.settings_bottom_bar_style),
                subtitle = settings.bottomBarStyle.label,
                onClick = { dialogs.showBottomBarStyleDialog = true }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsRow(
                title = stringResource(id = R.string.settings_bottom_bar_size),
                subtitle = settings.bottomBarSize.label,
                onClick = { dialogs.showBottomBarSizeDialog = true }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsSwitchRow(
                title = stringResource(id = R.string.settings_bottom_bar_shrink),
                subtitle = stringResource(id = R.string.settings_bottom_bar_shrink_desc),
                checked = settings.shrinkBottomBarOnScroll,
                onCheckedChange = { viewModel.onShrinkBottomBarOnScrollChanged(it) }
            )

            Spacer(modifier = Modifier.height(spacing.small))
            SettingsListDivider()
            Spacer(modifier = Modifier.height(spacing.small))

            SettingsRow(
                title = stringResource(id = R.string.settings_animations),
                subtitle = settings.animationOption.label,
                onClick = { dialogs.showAnimDialog = true }
            )
        }
    }
}
