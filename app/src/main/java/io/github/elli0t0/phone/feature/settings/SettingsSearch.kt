package io.github.elli0t0.phone.feature.settings

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.R
import io.github.elli0t0.phone.core.design.LocalAppSettings
import io.github.elli0t0.phone.data.settings.model.AnimationOption

/**
 * Settings search (0.17.3): one registry, no duplicated strings. A
 * result opens its page and briefly highlights its row. Pages are
 * single-card pages, not list items, so there is no row to scroll to:
 * the highlight pulse is the locator, and the page is the navigation.
 */

/** Every page search can open. MAIN means "already on the main page". */
enum class SettingsPage {
    MAIN,
    APPEARANCE,
    GLASS,
    LAYOUT_NAV,
    KEYPAD,
    CALLS,
    CONTACTS,
    PRIVACY,
    ABOUT,
    APP_STYLE,
    KEYPAD_STYLE,
    GLASS_STUDIO,
    CALL_BACKGROUND,
    BLOCKED_NUMBERS,
    RECORDING,
    ANSWER_STYLE
}

fun openSettingsPage(dialogs: SettingsDialogState, page: SettingsPage) {
    when (page) {
        SettingsPage.MAIN -> Unit
        SettingsPage.APPEARANCE -> dialogs.showAppearancePage = true
        SettingsPage.GLASS -> dialogs.showGlassPage = true
        SettingsPage.LAYOUT_NAV -> dialogs.showLayoutNavPage = true
        SettingsPage.KEYPAD -> dialogs.showKeypadPage = true
        SettingsPage.CALLS -> dialogs.showCallsPage = true
        SettingsPage.CONTACTS -> dialogs.showContactsPage = true
        SettingsPage.PRIVACY -> dialogs.showPrivacyPage = true
        SettingsPage.ABOUT -> dialogs.showAboutPage = true
        SettingsPage.APP_STYLE -> dialogs.showAppStylePage = true
        SettingsPage.KEYPAD_STYLE -> dialogs.showKeypadStylePage = true
        SettingsPage.GLASS_STUDIO -> dialogs.showGlassStudioPage = true
        SettingsPage.CALL_BACKGROUND -> dialogs.showCallBackgroundPage = true
        SettingsPage.BLOCKED_NUMBERS -> dialogs.showBlockedNumbers = true
        SettingsPage.RECORDING -> dialogs.showRecordingPage = true
        SettingsPage.ANSWER_STYLE -> dialogs.showAnswerStylePage = true
    }
}

fun settingsPageIconRes(page: SettingsPage): Int = when (page) {
    SettingsPage.MAIN,
    SettingsPage.APPEARANCE,
    SettingsPage.APP_STYLE,
    SettingsPage.KEYPAD_STYLE -> R.drawable.ic_settings
    SettingsPage.GLASS,
    SettingsPage.GLASS_STUDIO -> R.drawable.ic_filter
    SettingsPage.LAYOUT_NAV -> R.drawable.ic_fullscreen
    SettingsPage.KEYPAD -> R.drawable.ic_dialpad
    SettingsPage.CALLS,
    SettingsPage.CALL_BACKGROUND,
    SettingsPage.ANSWER_STYLE -> R.drawable.ic_phone
    SettingsPage.CONTACTS -> R.drawable.ic_contacts
    SettingsPage.PRIVACY,
    SettingsPage.BLOCKED_NUMBERS -> R.drawable.ic_block
    SettingsPage.ABOUT -> R.drawable.ic_info
    SettingsPage.RECORDING -> R.drawable.ic_record
}

/**
 * One searchable row. [anchor] is the row's highlight id, or null when
 * tapping the result only opens the page (section rows, picker pages).
 * [keywordsRes] is 0 when the title + section words suffice; otherwise
 * a space-separated synonym list.
 */
data class SettingEntry(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val sectionRes: Int,
    val page: SettingsPage,
    val anchor: String? = null,
    @StringRes val keywordsRes: Int = 0
)

/** A registry entry with its strings resolved. */
data class ResolvedSettingEntry(
    val entry: SettingEntry,
    val title: String,
    val keywords: String,
    val section: String
)

/** Normalize for matching: lowercase, Persian/Arabic yeh + kaf unified, tashkeel and joiners stripped, Arabic-Indic and Persian digits mapped to ASCII. All invisible code points are explicit escapes so the mapping is reviewable. */
fun normalizeSearchText(text: String): String {
    val out = StringBuilder()
    for (ch in text.lowercase()) {
        when (ch) {
            // Arabic yeh -> Persian yeh, Arabic kaf -> Persian kaf.
            '\u064A' -> out.append('\u06CC')
            '\u0643' -> out.append('\u06A9')
            // ZWNJ + ZWJ stripped.
            '\u200C', '\u200D' -> Unit
            // Tashkeel (U+064B..U+0652) + superscript alef (U+0670) stripped.
            in '\u064B'..'\u0652', '\u0670' -> Unit
            // Arabic-Indic digits -> ASCII.
            in '\u0660'..'\u0669' -> out.append('0' + (ch - '\u0660'))
            // Persian digits -> ASCII.
            in '\u06F0'..'\u06F9' -> out.append('0' + (ch - '\u06F0'))
            else -> out.append(ch)
        }
    }
    return out.toString()
}

/**
 * All query tokens must appear in title + keywords + section.
 * Title-prefix matches come first, registry order otherwise (stable).
 */
fun searchSettings(
    query: String,
    entries: List<ResolvedSettingEntry>
): List<ResolvedSettingEntry> {
    val tokens = normalizeSearchText(query)
        .split(' ', '،', '؛')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return emptyList()
    return entries.filter { e ->
        val haystack = normalizeSearchText("${e.title} ${e.keywords} ${e.section}")
        tokens.all { haystack.contains(it) }
    }.sortedWith(compareBy { e ->
        val title = normalizeSearchText(e.title)
        if (tokens.any { title.startsWith(it) }) 0 else 1
    })
}

/** Row id currently highlighted by a search result, or null. Provided at the Settings screen root. */
val LocalSearchHighlight = compositionLocalOf<String?> { null }


/**
 * Pulse background for a row with the given highlight id. No-op for any
 * other id (or a null id). The pulse fades accent 0.18 -> 0 over 1.2s;
 * with animations off it stays static until the highlight clears.
 */
@Composable
fun Modifier.searchHighlight(id: String?): Modifier {
    if (id == null) return this
    if (LocalSearchHighlight.current != id) return this
    val animations = LocalAppSettings.current.animationOption
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(id) { started = true }
    val alpha by animateFloatAsState(
        targetValue = if (animations == AnimationOption.OFF || !started) 0.18f else 0f,
        animationSpec = tween(1200),
        label = "searchHighlight"
    )
    return this.background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
}

/** 48dp pill search field for the top of the main Settings page. */
@Composable
fun SettingsSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        placeholder = { Text(text = stringResource(id = R.string.settings_search_hint)) },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_search),
                contentDescription = null
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = stringResource(id = R.string.cd_dismiss)
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(50),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
    )
}

/** One card of results: small section icon, title, section breadcrumb. */
@Composable
fun SettingsSearchResults(
    results: List<ResolvedSettingEntry>,
    onResultClick: (SettingEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            results.forEachIndexed { index, result ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onResultClick(result.entry) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = settingsPageIconRes(result.entry.page)),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = result.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = result.section,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (index < results.lastIndex) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

val allSettingEntries: List<SettingEntry> = listOf(
    // Main page rows.
    SettingEntry("nav_appearance", R.string.settings_cat_appearance, R.string.settings_cat_appearance, SettingsPage.APPEARANCE, anchor = "nav_appearance"),
    SettingEntry("nav_glass", R.string.settings_cat_liquid_glass, R.string.settings_cat_liquid_glass, SettingsPage.GLASS, anchor = "nav_glass"),
    SettingEntry("nav_layout_nav", R.string.settings_cat_layout_nav, R.string.settings_cat_layout_nav, SettingsPage.LAYOUT_NAV, anchor = "nav_layout_nav"),
    SettingEntry("nav_keypad", R.string.settings_cat_keypad, R.string.settings_cat_keypad, SettingsPage.KEYPAD, anchor = "nav_keypad"),
    SettingEntry("nav_calls", R.string.settings_cat_calls, R.string.settings_cat_calls, SettingsPage.CALLS, anchor = "nav_calls"),
    SettingEntry("nav_contacts", R.string.settings_cat_contacts, R.string.settings_cat_contacts, SettingsPage.CONTACTS, anchor = "nav_contacts"),
    SettingEntry("nav_recording", R.string.settings_call_recording, R.string.settings_call_recording, SettingsPage.RECORDING, anchor = "nav_recording"),
    SettingEntry("nav_privacy", R.string.settings_cat_privacy, R.string.settings_cat_privacy, SettingsPage.PRIVACY, anchor = "nav_privacy"),
    SettingEntry("nav_about", R.string.settings_cat_about, R.string.settings_cat_about, SettingsPage.ABOUT, anchor = "nav_about"),
    SettingEntry("main_reset", R.string.settings_reset_button, R.string.settings_cat_about, SettingsPage.ABOUT, anchor = "main_reset"),
    // Appearance page.
    SettingEntry("minimal_mode", R.string.settings_minimal_mode, R.string.settings_cat_appearance, SettingsPage.APPEARANCE, anchor = "minimal_mode"),
    SettingEntry("app_style", R.string.settings_app_style, R.string.settings_cat_appearance, SettingsPage.APP_STYLE),
    SettingEntry("theme", R.string.settings_theme, R.string.settings_cat_appearance, SettingsPage.APPEARANCE, anchor = "theme"),
    SettingEntry("accent", R.string.settings_accent_color, R.string.settings_cat_appearance, SettingsPage.APPEARANCE, anchor = "accent", keywordsRes = R.string.search_keywords_dynamic_color),
    SettingEntry("pure_black", R.string.settings_pure_black, R.string.settings_cat_appearance, SettingsPage.APPEARANCE, anchor = "pure_black", keywordsRes = R.string.search_keywords_pure_black),
    SettingEntry("font_scale", R.string.settings_font_scale, R.string.settings_cat_appearance, SettingsPage.APPEARANCE, anchor = "font_scale"),
    SettingEntry("header_size", R.string.settings_header_size, R.string.settings_cat_appearance, SettingsPage.APPEARANCE, anchor = "header_size"),
    SettingEntry("app_background", R.string.settings_app_background, R.string.settings_cat_appearance, SettingsPage.APPEARANCE, anchor = "app_background"),
    // Liquid glass page.
    SettingEntry("glass_master", R.string.settings_cat_liquid_glass, R.string.settings_cat_liquid_glass, SettingsPage.GLASS, anchor = "glass_master"),
    SettingEntry("glass_intensity", R.string.settings_glass_intensity, R.string.settings_cat_liquid_glass, SettingsPage.GLASS, anchor = "glass_intensity"),
    SettingEntry("reduce_transparency", R.string.settings_reduce_transparency, R.string.settings_cat_liquid_glass, SettingsPage.GLASS, anchor = "reduce_transparency"),
    SettingEntry("glass_dynamic_highlights", R.string.settings_dynamic_highlights, R.string.settings_cat_liquid_glass, SettingsPage.GLASS, anchor = "glass_dynamic_highlights"),
    SettingEntry("glass_studio", R.string.settings_glass_studio, R.string.settings_cat_liquid_glass, SettingsPage.GLASS_STUDIO),
    // Layout & navigation page.
    SettingEntry("list_density", R.string.settings_list_density, R.string.settings_cat_layout_nav, SettingsPage.LAYOUT_NAV, anchor = "list_density"),
    SettingEntry("keypad_style", R.string.settings_keypad_style, R.string.settings_cat_keypad, SettingsPage.KEYPAD, anchor = "keypad_style"),
    SettingEntry("key_style", R.string.settings_key_style, R.string.settings_cat_keypad, SettingsPage.KEYPAD, anchor = "key_style"),
    SettingEntry("dialpad_tones", R.string.settings_dialpad_tones, R.string.settings_cat_keypad, SettingsPage.KEYPAD, anchor = "dialpad_tones"),
    SettingEntry("speed_dial", R.string.settings_speed_dial, R.string.settings_cat_keypad, SettingsPage.KEYPAD, anchor = "speed_dial"),
    SettingEntry("swipe_actions", R.string.settings_swipe_actions, R.string.settings_cat_layout_nav, SettingsPage.LAYOUT_NAV, anchor = "swipe_actions", keywordsRes = R.string.search_keywords_swipe_actions),
    SettingEntry("default_tab", R.string.settings_default_tab, R.string.settings_cat_layout_nav, SettingsPage.LAYOUT_NAV, anchor = "default_tab"),
    SettingEntry("bottom_labels", R.string.settings_bottom_labels, R.string.settings_cat_layout_nav, SettingsPage.LAYOUT_NAV, anchor = "bottom_labels"),
    SettingEntry("bottom_bar_style", R.string.settings_bottom_bar_style, R.string.settings_cat_layout_nav, SettingsPage.LAYOUT_NAV, anchor = "bottom_bar_style", keywordsRes = R.string.search_keywords_bottom_bar),
    SettingEntry("bottom_bar_size", R.string.settings_bottom_bar_size, R.string.settings_cat_layout_nav, SettingsPage.LAYOUT_NAV, anchor = "bottom_bar_size"),
    SettingEntry("bottom_bar_shrink", R.string.settings_bottom_bar_shrink, R.string.settings_cat_layout_nav, SettingsPage.LAYOUT_NAV, anchor = "bottom_bar_shrink"),
    SettingEntry("animations", R.string.settings_animations, R.string.settings_cat_layout_nav, SettingsPage.LAYOUT_NAV, anchor = "animations", keywordsRes = R.string.search_keywords_animations),
    // Calls page.
    SettingEntry("block_numbers", R.string.settings_block_numbers, R.string.settings_cat_calls, SettingsPage.BLOCKED_NUMBERS, keywordsRes = R.string.search_keywords_block_numbers),
    SettingEntry("supplementary", R.string.settings_supplementary, R.string.settings_cat_calls, SettingsPage.CALLS, anchor = "supplementary"),
    SettingEntry("call_waiting", R.string.settings_call_waiting, R.string.settings_cat_calls, SettingsPage.CALLS, anchor = "call_waiting", keywordsRes = R.string.search_keywords_call_waiting),
    SettingEntry("call_display", R.string.settings_call_display, R.string.settings_cat_calls, SettingsPage.CALLS, anchor = "call_display"),
    SettingEntry("answer_style", R.string.settings_answer_style, R.string.settings_cat_calls, SettingsPage.ANSWER_STYLE),
    SettingEntry("call_background", R.string.settings_call_background, R.string.settings_cat_calls, SettingsPage.CALL_BACKGROUND),
    SettingEntry("default_sim", R.string.settings_default_sim, R.string.settings_cat_calls, SettingsPage.CALLS, anchor = "default_sim", keywordsRes = R.string.search_keywords_default_sim),
    SettingEntry("quick_decline", R.string.settings_quick_decline, R.string.settings_cat_calls, SettingsPage.CALLS, anchor = "quick_decline", keywordsRes = R.string.search_keywords_quick_decline),
    SettingEntry("confirm_call", R.string.settings_confirm_call, R.string.settings_cat_calls, SettingsPage.CALLS, anchor = "confirm_call"),
    SettingEntry("vibrate_answer", R.string.settings_vibrate_answer, R.string.settings_cat_calls, SettingsPage.CALLS, anchor = "vibrate_answer"),
    // Contacts page.
    SettingEntry("sort_order", R.string.settings_sort_order, R.string.settings_cat_contacts, SettingsPage.CONTACTS, anchor = "sort_order"),
    SettingEntry("name_format", R.string.settings_name_format, R.string.settings_cat_contacts, SettingsPage.CONTACTS, anchor = "name_format"),
    // Privacy page.
    SettingEntry("permissions", R.string.settings_permissions, R.string.settings_cat_privacy, SettingsPage.PRIVACY, anchor = "permissions"),
    // About page.
    SettingEntry("about_phone", R.string.settings_about_phone, R.string.settings_cat_about, SettingsPage.ABOUT, anchor = "about_phone"),
    SettingEntry("update_check", R.string.update_check_title, R.string.settings_cat_about, SettingsPage.ABOUT, anchor = "update_check"),
    // App style screen.
    SettingEntry("style_font", R.string.settings_font_style, R.string.settings_cat_appearance, SettingsPage.APP_STYLE, anchor = "style_font"),
    SettingEntry("style_card", R.string.settings_card_style, R.string.settings_cat_appearance, SettingsPage.APP_STYLE, anchor = "style_card"),
    SettingEntry("style_switch", R.string.settings_switch_style, R.string.settings_cat_appearance, SettingsPage.APP_STYLE, anchor = "style_switch"),
    // Glass Studio screen.
    SettingEntry("studio_blur", R.string.settings_glass_blur_amount, R.string.settings_cat_liquid_glass, SettingsPage.GLASS_STUDIO, anchor = "studio_blur"),
    SettingEntry("studio_tint", R.string.settings_glass_tint, R.string.settings_cat_liquid_glass, SettingsPage.GLASS_STUDIO, anchor = "studio_tint"),
    SettingEntry("studio_richness", R.string.settings_glass_richness, R.string.settings_cat_liquid_glass, SettingsPage.GLASS_STUDIO, anchor = "studio_richness"),
    SettingEntry("studio_edge_light", R.string.settings_edge_light, R.string.settings_cat_liquid_glass, SettingsPage.GLASS_STUDIO, anchor = "studio_edge_light"),
    SettingEntry("studio_dynamic_highlights", R.string.settings_dynamic_highlights, R.string.settings_cat_liquid_glass, SettingsPage.GLASS_STUDIO, anchor = "studio_dynamic_highlights"),
    SettingEntry("studio_edge_refraction", R.string.settings_edge_refraction, R.string.settings_cat_liquid_glass, SettingsPage.GLASS_STUDIO, anchor = "studio_edge_refraction"),
    SettingEntry("studio_droplet_stretch", R.string.settings_droplet_stretch, R.string.settings_cat_liquid_glass, SettingsPage.GLASS_STUDIO, anchor = "studio_droplet_stretch"),
    SettingEntry("studio_haptic", R.string.settings_glass_haptic, R.string.settings_cat_liquid_glass, SettingsPage.GLASS_STUDIO, anchor = "studio_haptic"),
    SettingEntry("studio_backdrop", R.string.settings_backdrop, R.string.settings_cat_liquid_glass, SettingsPage.GLASS_STUDIO, anchor = "studio_backdrop", keywordsRes = R.string.search_keywords_backdrop),
    SettingEntry("studio_switch_style", R.string.settings_switch_style, R.string.settings_cat_liquid_glass, SettingsPage.GLASS_STUDIO, anchor = "studio_switch_style"),
    SettingEntry("studio_reset", R.string.settings_glass_reset, R.string.settings_cat_liquid_glass, SettingsPage.GLASS_STUDIO, anchor = "studio_reset"),
    // Call background screen (studio groups).
    SettingEntry("studio_layout_preset", R.string.studio_layout_preset, R.string.settings_cat_calls, SettingsPage.CALL_BACKGROUND, anchor = "studio_layout_preset"),
    SettingEntry("studio_answer_side", R.string.studio_answer_side, R.string.settings_cat_calls, SettingsPage.CALL_BACKGROUND, anchor = "studio_answer_side"),
    SettingEntry("studio_answer_button_size", R.string.studio_answer_button_size, R.string.settings_cat_calls, SettingsPage.CALL_BACKGROUND, anchor = "studio_answer_button_size"),
    SettingEntry("studio_quick_replies", R.string.studio_quick_replies, R.string.settings_cat_calls, SettingsPage.CALL_BACKGROUND, anchor = "studio_quick_replies"),
    SettingEntry("studio_call_controls", R.string.studio_call_controls, R.string.settings_cat_calls, SettingsPage.CALL_BACKGROUND, anchor = "studio_call_controls"),
    SettingEntry("studio_grid_alignment", R.string.studio_control_grid_alignment, R.string.settings_cat_calls, SettingsPage.CALL_BACKGROUND, anchor = "studio_grid_alignment"),
    SettingEntry("studio_end_call_position", R.string.studio_end_call_position, R.string.settings_cat_calls, SettingsPage.CALL_BACKGROUND, anchor = "studio_end_call_position"),
    SettingEntry("studio_end_call_size", R.string.studio_end_call_size, R.string.settings_cat_calls, SettingsPage.CALL_BACKGROUND, anchor = "studio_end_call_size"),
    SettingEntry("call_button_size", R.string.call_button_size, R.string.settings_cat_calls, SettingsPage.CALL_BACKGROUND, anchor = "call_button_size"),
    SettingEntry("control_button_size", R.string.call_bg_control_button_size, R.string.settings_cat_calls, SettingsPage.CALL_BACKGROUND, anchor = "control_button_size"),
    SettingEntry("dtmf_key_size", R.string.call_bg_keypad_key_size, R.string.settings_cat_calls, SettingsPage.CALL_BACKGROUND, anchor = "dtmf_key_size"),
    SettingEntry("studio_caller_info_position", R.string.studio_caller_info_position, R.string.settings_cat_calls, SettingsPage.CALL_BACKGROUND, anchor = "studio_caller_info_position"),
    SettingEntry("studio_background_style", R.string.settings_call_background_style, R.string.settings_cat_calls, SettingsPage.CALL_BACKGROUND, anchor = "studio_background_style"),
    SettingEntry("studio_dim", R.string.settings_call_background_dim, R.string.settings_cat_calls, SettingsPage.CALL_BACKGROUND, anchor = "studio_dim"),
    SettingEntry("studio_show_number", R.string.settings_call_show_number, R.string.settings_cat_calls, SettingsPage.CALL_BACKGROUND, anchor = "studio_show_number"),
    // Call recording page.
    SettingEntry("recording_pair", R.string.settings_call_recording, R.string.settings_call_recording, SettingsPage.RECORDING, anchor = "recording_status", keywordsRes = R.string.search_keywords_recording_pair),
    SettingEntry("recording_port", R.string.recording_connection_port, R.string.settings_call_recording, SettingsPage.RECORDING, anchor = "recording_port", keywordsRes = R.string.search_keywords_recording_port),
    SettingEntry("recording_detect", R.string.recording_action_detect_port, R.string.settings_call_recording, SettingsPage.RECORDING, anchor = "recording_detect"),
    SettingEntry("recording_reset_pairing", R.string.recording_action_reset_pairing, R.string.settings_call_recording, SettingsPage.RECORDING, anchor = "recording_reset_pairing"),
    SettingEntry("recording_offline", R.string.recording_offline_mode_title, R.string.settings_call_recording, SettingsPage.RECORDING, anchor = "recording_offline", keywordsRes = R.string.search_keywords_recording_offline),
    SettingEntry("recording_test", R.string.recording_test, R.string.settings_call_recording, SettingsPage.RECORDING, anchor = "recording_test"),
    SettingEntry("recording_open", R.string.recordings_open, R.string.settings_call_recording, SettingsPage.RECORDING, anchor = "recording_open"),
    SettingEntry("recording_auto_mode", R.string.recording_auto_record, R.string.settings_call_recording, SettingsPage.RECORDING, anchor = "recording_auto"),
    SettingEntry("recording_excluded", R.string.recording_excluded_title, R.string.settings_call_recording, SettingsPage.RECORDING, anchor = "recording_excluded")
)
