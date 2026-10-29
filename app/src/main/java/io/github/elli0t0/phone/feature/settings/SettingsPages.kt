package io.github.elli0t0.phone.feature.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.Spacing
import io.github.elli0t0.phone.core.design.bottomBarListBottomPadding
import io.github.elli0t0.phone.data.settings.model.AppSettings
import io.github.elli0t0.phone.navigation.InnerTopBar

/**
 * One full-screen page per Settings section (0.16.9): the main page
 * shows a navigation row per section, and each page renders that
 * section's card. Same pattern as the other settings pages
 * (Scaffold + InnerTopBar + LazyColumn); the rows open their
 * dialogs via [dialogs], which SettingsScreen renders.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSectionPage(
    title: String,
    backTestTag: String,
    pageTestTag: String,
    spacing: Spacing,
    onNavigateBack: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize().testTag(pageTestTag)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            InnerTopBar(
                onBack = onNavigateBack,
                title = title,
                backTestTag = backTestTag
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = spacing.screenHorizontalPadding),
                verticalArrangement = Arrangement.spacedBy(spacing.medium),
                contentPadding = PaddingValues(bottom = bottomBarListBottomPadding())
            ) {
                item { content() }
            }
        }
    }
}

@Composable
fun AppearancePage(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    dialogs: SettingsDialogState,
    spacing: Spacing,
    onNavigateBack: () -> Unit
) {
    SettingsSectionPage(
        title = stringResource(id = R.string.settings_cat_appearance),
        backTestTag = "btn_appearance_back",
        pageTestTag = "appearance_page",
        spacing = spacing,
        onNavigateBack = onNavigateBack
    ) {
        AppearanceCard(settings, viewModel, dialogs, spacing)
    }
}

@Composable
fun GlassPage(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    dialogs: SettingsDialogState,
    spacing: Spacing,
    onNavigateBack: () -> Unit
) {
    SettingsSectionPage(
        title = stringResource(id = R.string.settings_cat_liquid_glass),
        backTestTag = "btn_glass_back",
        pageTestTag = "glass_page",
        spacing = spacing,
        onNavigateBack = onNavigateBack
    ) {
        GlassCard(settings, viewModel, dialogs, spacing)
    }
}

@Composable
fun LayoutNavPage(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    dialogs: SettingsDialogState,
    spacing: Spacing,
    onNavigateBack: () -> Unit
) {
    SettingsSectionPage(
        title = stringResource(id = R.string.settings_cat_layout_nav),
        backTestTag = "btn_layout_nav_back",
        pageTestTag = "layout_nav_page",
        spacing = spacing,
        onNavigateBack = onNavigateBack
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            SettingsSectionHeader(title = stringResource(id = R.string.settings_cat_layout))
            LayoutCard(settings, viewModel, dialogs, spacing)
            SettingsSectionHeader(title = stringResource(id = R.string.settings_cat_navigation))
            NavigationCard(settings, viewModel, dialogs, spacing)
        }
    }
}

@Composable
fun CallsPage(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    dialogs: SettingsDialogState,
    showSupplementaryRow: Boolean,
    spacing: Spacing,
    context: Context,
    onNavigateBack: () -> Unit
) {
    SettingsSectionPage(
        title = stringResource(id = R.string.settings_cat_calls),
        backTestTag = "btn_calls_back",
        pageTestTag = "calls_page",
        spacing = spacing,
        onNavigateBack = onNavigateBack
    ) {
        CallsCard(settings, viewModel, dialogs, spacing, showSupplementaryRow, context)
    }
}

@Composable
fun ContactsPage(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    dialogs: SettingsDialogState,
    spacing: Spacing,
    onNavigateBack: () -> Unit
) {
    SettingsSectionPage(
        title = stringResource(id = R.string.settings_cat_contacts),
        backTestTag = "btn_contacts_back",
        pageTestTag = "contacts_page",
        spacing = spacing,
        onNavigateBack = onNavigateBack
    ) {
        ContactsCard(settings, viewModel, dialogs, spacing)
    }
}

@Composable
fun PrivacyPage(
    dialogs: SettingsDialogState,
    spacing: Spacing,
    context: Context,
    onNavigateBack: () -> Unit
) {
    SettingsSectionPage(
        title = stringResource(id = R.string.settings_cat_privacy),
        backTestTag = "btn_privacy_back",
        pageTestTag = "privacy_page",
        spacing = spacing,
        onNavigateBack = onNavigateBack
    ) {
        PrivacyCard(dialogs, spacing, context)
    }
}

@Composable
fun AboutPage(
    settings: AppSettings,
    viewModel: SettingsViewModel,
    dialogs: SettingsDialogState,
    spacing: Spacing,
    runUpdateCheck: () -> Unit,
    onNavigateBack: () -> Unit
) {
    SettingsSectionPage(
        title = stringResource(id = R.string.settings_cat_about),
        backTestTag = "btn_about_back",
        pageTestTag = "about_page",
        spacing = spacing,
        onNavigateBack = onNavigateBack
    ) {
        AboutCard(settings, viewModel, dialogs, spacing, runUpdateCheck)
    }
}
