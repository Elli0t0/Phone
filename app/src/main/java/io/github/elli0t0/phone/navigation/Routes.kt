package io.github.elli0t0.phone.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.elli0t0.phone.R

sealed class Screen(
    val route: String,
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
    @DrawableRes val iconResSelected: Int,
    @StringRes val contentDescriptionRes: Int
) {
    data object Recents : Screen(
        route = "recents",
        titleRes = R.string.tab_recents,
        iconRes = R.drawable.ic_nav_call_log_outline,
        iconResSelected = R.drawable.ic_nav_call_log,
        contentDescriptionRes = R.string.cd_tab_recents
    )

    data object Contacts : Screen(
        route = "contacts",
        titleRes = R.string.tab_contacts,
        iconRes = R.drawable.ic_nav_contacts,
        iconResSelected = R.drawable.ic_nav_contacts_filled,
        contentDescriptionRes = R.string.cd_tab_contacts
    )

    data object Dialpad : Screen(
        route = "dialpad",
        titleRes = R.string.tab_dialpad,
        iconRes = R.drawable.ic_nav_dialpad_outline,
        iconResSelected = R.drawable.ic_nav_dialpad,
        contentDescriptionRes = R.string.cd_tab_dialpad
    )

    data object Settings : Screen(
        route = "settings",
        titleRes = R.string.settings_title,
        iconRes = R.drawable.ic_settings,
        iconResSelected = R.drawable.ic_settings,
        contentDescriptionRes = R.string.cd_settings
    )

    data object Setup : Screen(
        route = "setup",
        titleRes = R.string.setup_title,
        iconRes = R.drawable.ic_settings,
        iconResSelected = R.drawable.ic_settings,
        contentDescriptionRes = R.string.cd_setup
    )

    companion object {
        val bottomNavItems = listOf(Dialpad, Recents, Contacts)
    }
}
