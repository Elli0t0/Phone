package io.github.elli0t0.phone.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.elli0t0.phone.data.settings.model.AccentPreset
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import io.github.elli0t0.phone.data.settings.model.AppSettings
import io.github.elli0t0.phone.data.settings.model.CallBackground
import io.github.elli0t0.phone.data.settings.model.CallDisplay
import io.github.elli0t0.phone.data.settings.model.CardStyle
import io.github.elli0t0.phone.data.settings.model.ContactNameFormat
import io.github.elli0t0.phone.data.settings.model.ContactSortOrder
import io.github.elli0t0.phone.data.settings.model.ListDensity
import io.github.elli0t0.phone.data.settings.model.StartTab
import io.github.elli0t0.phone.data.settings.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "phone_settings")

interface SettingsRepository {
    val settingsFlow: Flow<AppSettings>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setAccentColor(colorValue: Long, isCustom: Boolean)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setPureBlackDarkMode(enabled: Boolean)
    suspend fun setListDensity(density: ListDensity)
    suspend fun setCardStyle(style: CardStyle)
    suspend fun setFontSizeScale(scale: Float)
    suspend fun setAnimationOption(option: AnimationOption)
    suspend fun setShowBottomBarLabels(show: Boolean)
    suspend fun setDefaultStartTab(tab: StartTab)
    suspend fun setContactSortOrder(order: ContactSortOrder)
    suspend fun setContactNameFormat(format: ContactNameFormat)
    suspend fun setCallDisplay(display: CallDisplay)
    suspend fun setCallBackground(background: CallBackground)
    suspend fun setQuickDeclineMessages(messages: List<String>)
    suspend fun resetToDefaults()
}

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR = longPreferencesKey("accent_color")
        val IS_CUSTOM_ACCENT = booleanPreferencesKey("is_custom_accent")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val PURE_BLACK_DARK_MODE = booleanPreferencesKey("pure_black_dark_mode")
        val LIST_DENSITY = stringPreferencesKey("list_density")
        val CARD_STYLE = stringPreferencesKey("card_style")
        val FONT_SIZE_SCALE = floatPreferencesKey("font_size_scale")
        val ANIMATION_OPTION = stringPreferencesKey("animation_option")
        val BOTTOM_BAR_LABELS = booleanPreferencesKey("bottom_bar_labels")
        val DEFAULT_START_TAB = stringPreferencesKey("default_start_tab")
        val CONTACT_SORT_ORDER = stringPreferencesKey("contact_sort_order")
        val CONTACT_NAME_FORMAT = stringPreferencesKey("contact_name_format")
        val CALL_DISPLAY = stringPreferencesKey("call_display")
        val CALL_BACKGROUND = stringPreferencesKey("call_background")
        val QUICK_DECLINE_MESSAGES = stringPreferencesKey("quick_decline_messages")
    }

    override val settingsFlow: Flow<AppSettings> = dataStore.data.map { prefs ->
        val themeModeStr = prefs[Keys.THEME_MODE]
        val themeMode = themeModeStr?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
        val accent = prefs[Keys.ACCENT_COLOR] ?: AccentPreset.ONE_UI_BLUE.colorValue
        val isCustom = prefs[Keys.IS_CUSTOM_ACCENT] ?: false
        val dynamic = prefs[Keys.DYNAMIC_COLOR] ?: true
        val pureBlack = prefs[Keys.PURE_BLACK_DARK_MODE] ?: false

        val densityStr = prefs[Keys.LIST_DENSITY]
        val density = densityStr?.let { runCatching { ListDensity.valueOf(it) }.getOrNull() } ?: ListDensity.DEFAULT

        val cardStyleStr = prefs[Keys.CARD_STYLE]
        val cardStyle = cardStyleStr?.let { runCatching { CardStyle.valueOf(it) }.getOrNull() } ?: CardStyle.ROUNDED

        val fontScale = prefs[Keys.FONT_SIZE_SCALE] ?: 1.0f

        val animStr = prefs[Keys.ANIMATION_OPTION]
        val anim = animStr?.let { runCatching { AnimationOption.valueOf(it) }.getOrNull() } ?: AnimationOption.ON

        val showLabels = prefs[Keys.BOTTOM_BAR_LABELS] ?: true

        val tabStr = prefs[Keys.DEFAULT_START_TAB]
        val startTab = tabStr?.let { runCatching { StartTab.valueOf(it) }.getOrNull() } ?: StartTab.KEYPAD

        val sortStr = prefs[Keys.CONTACT_SORT_ORDER]
        val sortOrder = sortStr?.let { runCatching { ContactSortOrder.valueOf(it) }.getOrNull() } ?: ContactSortOrder.FIRST_NAME

        val formatStr = prefs[Keys.CONTACT_NAME_FORMAT]
        val nameFormat = formatStr?.let { runCatching { ContactNameFormat.valueOf(it) }.getOrNull() } ?: ContactNameFormat.FIRST_NAME_FIRST

        val callDisplayStr = prefs[Keys.CALL_DISPLAY]
        val callDisplay = callDisplayStr?.let { runCatching { CallDisplay.valueOf(it) }.getOrNull() } ?: CallDisplay.FULL_SCREEN

        val callBackgroundStr = prefs[Keys.CALL_BACKGROUND]
        val callBackground = callBackgroundStr?.let { runCatching { CallBackground.valueOf(it) }.getOrNull() } ?: CallBackground.DEFAULT

        val quickDeclineStored = prefs[Keys.QUICK_DECLINE_MESSAGES] ?: ""
        val quickDeclineOverrides = (quickDeclineStored.split("\n") + listOf("", "", ""))
            .take(3)

        AppSettings(
            themeMode = themeMode,
            accentColor = accent,
            isCustomAccent = isCustom,
            dynamicColor = dynamic,
            pureBlackDarkMode = pureBlack,
            listDensity = density,
            cardStyle = cardStyle,
            fontSizeScale = fontScale,
            animationOption = anim,
            showBottomBarLabels = showLabels,
            defaultStartTab = startTab,
            contactSortOrder = sortOrder,
            contactNameFormat = nameFormat,
            callDisplay = callDisplay,
            callBackground = callBackground,
            quickDeclineOverrides = quickDeclineOverrides
        )
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    override suspend fun setAccentColor(colorValue: Long, isCustom: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.ACCENT_COLOR] = colorValue
            prefs[Keys.IS_CUSTOM_ACCENT] = isCustom
            prefs[Keys.DYNAMIC_COLOR] = false
        }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.DYNAMIC_COLOR] = enabled
        }
    }

    override suspend fun setPureBlackDarkMode(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.PURE_BLACK_DARK_MODE] = enabled
        }
    }

    override suspend fun setListDensity(density: ListDensity) {
        dataStore.edit { prefs ->
            prefs[Keys.LIST_DENSITY] = density.name
        }
    }

    override suspend fun setCardStyle(style: CardStyle) {
        dataStore.edit { prefs ->
            prefs[Keys.CARD_STYLE] = style.name
        }
    }

    override suspend fun setFontSizeScale(scale: Float) {
        dataStore.edit { prefs ->
            prefs[Keys.FONT_SIZE_SCALE] = scale
        }
    }

    override suspend fun setAnimationOption(option: AnimationOption) {
        dataStore.edit { prefs ->
            prefs[Keys.ANIMATION_OPTION] = option.name
        }
    }

    override suspend fun setShowBottomBarLabels(show: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.BOTTOM_BAR_LABELS] = show
        }
    }

    override suspend fun setDefaultStartTab(tab: StartTab) {
        dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_START_TAB] = tab.name
        }
    }

    override suspend fun setContactSortOrder(order: ContactSortOrder) {
        dataStore.edit { prefs ->
            prefs[Keys.CONTACT_SORT_ORDER] = order.name
        }
    }

    override suspend fun setContactNameFormat(format: ContactNameFormat) {
        dataStore.edit { prefs ->
            prefs[Keys.CONTACT_NAME_FORMAT] = format.name
        }
    }

    override suspend fun setCallDisplay(display: CallDisplay) {
        dataStore.edit { prefs ->
            prefs[Keys.CALL_DISPLAY] = display.name
        }
    }

    override suspend fun setCallBackground(background: CallBackground) {
        dataStore.edit { prefs ->
            prefs[Keys.CALL_BACKGROUND] = background.name
        }
    }

    override suspend fun setQuickDeclineMessages(messages: List<String>) {
        dataStore.edit { prefs ->
            prefs[Keys.QUICK_DECLINE_MESSAGES] = (messages.map { it.replace("\n", " ") } + listOf("", "", ""))
                .take(3)
                .joinToString("\n")
        }
    }

    override suspend fun resetToDefaults() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
