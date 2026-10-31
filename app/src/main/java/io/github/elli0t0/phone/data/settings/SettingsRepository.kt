package io.github.elli0t0.phone.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.elli0t0.phone.data.settings.model.AccentPreset
import io.github.elli0t0.phone.data.settings.model.AnimationOption
import io.github.elli0t0.phone.data.settings.model.AnswerStyle
import io.github.elli0t0.phone.data.settings.model.AnswerSide
import io.github.elli0t0.phone.data.settings.model.CallerInfoPosition
import io.github.elli0t0.phone.data.settings.model.AnswerButtonSize
import io.github.elli0t0.phone.data.settings.model.ControlButtonSize
import io.github.elli0t0.phone.data.settings.model.ControlGridAlignment
import io.github.elli0t0.phone.data.settings.model.DtmfKeySize
import io.github.elli0t0.phone.data.settings.model.IncomingAvatarShape
import io.github.elli0t0.phone.data.settings.model.IncomingAvatarSize
import io.github.elli0t0.phone.data.settings.model.EndCallButtonSize
import io.github.elli0t0.phone.data.settings.model.EndCallAlignment
import io.github.elli0t0.phone.data.settings.model.OngoingCallButton
import io.github.elli0t0.phone.data.settings.model.AppBackground
import io.github.elli0t0.phone.data.settings.model.AppSettings
import io.github.elli0t0.phone.data.settings.model.BottomBarSize
import io.github.elli0t0.phone.data.settings.model.AutoRecordMode
import io.github.elli0t0.phone.data.settings.model.Backdrop
import io.github.elli0t0.phone.data.settings.model.RecordingSort
import io.github.elli0t0.phone.data.settings.model.DropletStretch
import io.github.elli0t0.phone.data.settings.model.EdgeLight
import io.github.elli0t0.phone.data.settings.model.GlassHapticFeedback
import io.github.elli0t0.phone.data.settings.model.GlassIntensity
import io.github.elli0t0.phone.data.settings.model.HeaderSize
import io.github.elli0t0.phone.data.settings.model.BottomBarStyle
import io.github.elli0t0.phone.data.settings.model.CallBackground
import io.github.elli0t0.phone.data.settings.model.CallDisplay
import io.github.elli0t0.phone.data.settings.model.DefaultSim
import io.github.elli0t0.phone.data.settings.model.CardStyle
import io.github.elli0t0.phone.data.settings.model.KeyStyle
import io.github.elli0t0.phone.data.settings.model.KeypadStyle
import io.github.elli0t0.phone.data.settings.model.SwitchStyle
import io.github.elli0t0.phone.data.settings.model.AppStyle
import io.github.elli0t0.phone.data.settings.model.FontStyle
import io.github.elli0t0.phone.data.settings.model.TransitionStyle
import io.github.elli0t0.phone.data.settings.model.StylePreset
import io.github.elli0t0.phone.data.settings.model.ContactNameFormat
import io.github.elli0t0.phone.data.settings.model.ContactSortOrder
import io.github.elli0t0.phone.data.settings.model.ListDensity
import io.github.elli0t0.phone.data.settings.model.StartTab
import io.github.elli0t0.phone.data.settings.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
    suspend fun setBottomBarStyle(style: BottomBarStyle)
    suspend fun setBottomBarSize(size: BottomBarSize)
    suspend fun setHeaderSize(size: HeaderSize)
    suspend fun setShrinkBottomBarOnScroll(shrink: Boolean)
    suspend fun setGlassBottomBar(enabled: Boolean)
    suspend fun setKeyStyle(style: KeyStyle)
    suspend fun setKeypadStyle(style: KeypadStyle)
    suspend fun setSwitchStyle(style: SwitchStyle)
    suspend fun setAppStyle(style: AppStyle)
    suspend fun setFontStyle(style: FontStyle)
    suspend fun setTransitionStyle(style: TransitionStyle)
    suspend fun setBottomSheets(enabled: Boolean)
    suspend fun setSwipeActionsEnabled(enabled: Boolean)
    suspend fun setGlassHeaders(enabled: Boolean)
    suspend fun setGlassContactDetail(enabled: Boolean)
    suspend fun setGlassCallScreens(enabled: Boolean)
    suspend fun setGlassIntensity(intensity: GlassIntensity)
    suspend fun setGlassBlurAmount(amount: Float)
    suspend fun setGlassTintAmount(amount: Float)
    suspend fun setEdgeLight(edgeLight: EdgeLight)
    suspend fun setGlassColorRichness(richness: Float)
    suspend fun setDropletStretch(stretch: DropletStretch)
    suspend fun setGlassHapticFeedback(feedback: GlassHapticFeedback)
    suspend fun setEdgeRefraction(enabled: Boolean)
    suspend fun setLensBar(enabled: Boolean)
    suspend fun setReduceTransparency(enabled: Boolean)
    suspend fun setGlassCards(enabled: Boolean)
    suspend fun setDynamicHighlights(enabled: Boolean)
    suspend fun setBackdrop(backdrop: Backdrop)
    suspend fun setAppBackground(background: AppBackground)
    suspend fun setAppBackgroundDim(dim: Float)
    suspend fun setAppBackgroundPhotoVersion(version: Long)
    suspend fun setDefaultStartTab(tab: StartTab)
    suspend fun setContactSortOrder(order: ContactSortOrder)
    suspend fun setContactNameFormat(format: ContactNameFormat)
    suspend fun setCallDisplay(display: CallDisplay)
    suspend fun setCallBackground(background: CallBackground)
    suspend fun setCallBackgroundDim(dim: Float)
    suspend fun setCallBackgroundCustomColor(color: Long)
    suspend fun setAnswerStyle(style: AnswerStyle)
    suspend fun setSliderOnlyWhenLocked(enabled: Boolean)
    suspend fun setAnswerSide(side: AnswerSide)
    suspend fun setEndCallAlignment(alignment: EndCallAlignment)
    suspend fun setCallerInfoPosition(position: CallerInfoPosition)
    suspend fun setControlGridAlignment(alignment: ControlGridAlignment)
    suspend fun setEndCallButtonSize(size: EndCallButtonSize)
    suspend fun setAnswerButtonSize(size: AnswerButtonSize)
    suspend fun setShowQuickReplies(show: Boolean)
    suspend fun setQuickRepliesBelowButtons(below: Boolean)
    suspend fun setControlButtonSize(size: ControlButtonSize)
    suspend fun setDtmfKeySize(size: DtmfKeySize)
    suspend fun setDialpadTones(enabled: Boolean)
    suspend fun setIncomingAvatarSize(size: IncomingAvatarSize)
    suspend fun setIncomingAvatarShape(shape: IncomingAvatarShape)
    suspend fun setOngoingCallButtons(buttons: List<OngoingCallButton>)
    suspend fun setAdbManualPort(port: Int?)
    suspend fun setAdbLastKnownPort(port: Int?)
    suspend fun setAdbOfflineMode(enabled: Boolean)
    suspend fun setAutoRecordCalls(enabled: Boolean)
    suspend fun setAutoRecordMode(mode: AutoRecordMode)
    suspend fun setAutoRecordExcluded(numbers: Set<String>)
    suspend fun setRecordingRetentionDays(days: Int)
    /** Last auto-cleanup run, epoch ms; 0 when never. Internal bookkeeping, not a user setting. */
    suspend fun getLastCleanupAt(): Long = 0
    suspend fun setLastCleanupAt(time: Long) {}
    suspend fun setRecordingsPrivate(private: Boolean)
    suspend fun setRecordingsSort(sort: RecordingSort)
    suspend fun setAdbHelperPort(port: Int?)
    suspend fun setAdbHelperToken(token: String?)
    suspend fun setShowNumberOnCallScreen(show: Boolean)
    suspend fun setDefaultSim(defaultSim: DefaultSim)
    suspend fun setConfirmBeforeCall(enabled: Boolean)
    suspend fun setVibrateOnAnswer(enabled: Boolean)
    suspend fun setQuickDeclineMessages(messages: List<String>)
    suspend fun setSpeedDial(speedDial: Map<Char, String>)
    suspend fun setBlockedPrefixes(prefixes: Set<String>)
    suspend fun setShowAdvancedSettings(show: Boolean)
    suspend fun setMinimalMode(enabled: Boolean)
    suspend fun setDuplicatesDismissed(dismissed: Boolean)
    suspend fun setDuplicatesDismissedHash(hash: String)
    suspend fun setFavoritesHintDismissed(dismissed: Boolean)
    suspend fun resetToDefaults()

    /** JSON backup; default no-ops so other implementations keep compiling. */
    suspend fun exportToJson(): String = SettingsBackup.encode(emptyMap())
    suspend fun importFromJson(json: String): Int = 0
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
        val BOTTOM_BAR_STYLE = stringPreferencesKey("bottom_bar_style")
        val BOTTOM_BAR_SIZE = stringPreferencesKey("bottom_bar_size")
        val HEADER_SIZE = stringPreferencesKey("header_size")
        val BOTTOM_BAR_SHRINK = booleanPreferencesKey("bottom_bar_shrink")
        val GLASS_BOTTOM_BAR = booleanPreferencesKey("glass_bottom_bar")
        val KEY_STYLE = stringPreferencesKey("key_style")
        val KEYPAD_STYLE = stringPreferencesKey("keypad_style")
        val SWITCH_STYLE = stringPreferencesKey("switch_style")
        val APP_STYLE = stringPreferencesKey("app_style")
        val FONT_STYLE = stringPreferencesKey("font_style")
        val TRANSITION_STYLE = stringPreferencesKey("transition_style")
        val BOTTOM_SHEETS = booleanPreferencesKey("bottom_sheets")
        val SWIPE_ACTIONS_ENABLED = booleanPreferencesKey("swipe_actions_enabled")
        // Legacy key style flag, read only: migrates to KEY_STYLE when
        // the new key is absent (true -> Glass, false -> Simple).
        val GLASS_DIALPAD = booleanPreferencesKey("glass_dialpad")
        val GLASS_HEADERS = booleanPreferencesKey("glass_headers")
        val GLASS_CONTACT_DETAIL = booleanPreferencesKey("glass_contact_detail")
        val GLASS_CALL_SCREENS = booleanPreferencesKey("glass_call_screens")
        val GLASS_INTENSITY = stringPreferencesKey("glass_intensity")
        val GLASS_BLUR_AMOUNT = floatPreferencesKey("glass_blur_amount")
        val GLASS_TINT_AMOUNT = floatPreferencesKey("glass_tint_amount")
        val EDGE_LIGHT = stringPreferencesKey("edge_light")
        val GLASS_COLOR_RICHNESS = floatPreferencesKey("glass_color_richness")
        val DROPLET_STRETCH = stringPreferencesKey("droplet_stretch")
        val GLASS_HAPTIC_FEEDBACK = stringPreferencesKey("glass_haptic_feedback")
        val EDGE_REFRACTION = booleanPreferencesKey("edge_refraction")
        val LENS_BAR = booleanPreferencesKey("lens_bar")
        val REDUCE_TRANSPARENCY = booleanPreferencesKey("reduce_transparency")
        val GLASS_CARDS = booleanPreferencesKey("glass_cards")
        val DYNAMIC_HIGHLIGHTS = booleanPreferencesKey("dynamic_highlights")
        val BACKDROP = stringPreferencesKey("backdrop")
        val APP_BACKGROUND = stringPreferencesKey("app_background")
        val APP_BACKGROUND_DIM = floatPreferencesKey("app_background_dim")
        val APP_BACKGROUND_PHOTO_VERSION = longPreferencesKey("app_background_photo_version")
        val DEFAULT_START_TAB = stringPreferencesKey("default_start_tab")
        val CONTACT_SORT_ORDER = stringPreferencesKey("contact_sort_order")
        val CONTACT_NAME_FORMAT = stringPreferencesKey("contact_name_format")
        val CALL_DISPLAY = stringPreferencesKey("call_display")
        val CALL_BACKGROUND = stringPreferencesKey("call_background")
        val CALL_BACKGROUND_DIM = floatPreferencesKey("call_background_dim")
        val CALL_BACKGROUND_CUSTOM_COLOR = longPreferencesKey("call_background_custom_color")
        // Legacy only: the answer-gesture setting was removed
        // (no screen ever applied it); this key survives solely so
        // the answer-style migration above can still read it.
        val ANSWER_GESTURE = stringPreferencesKey("answer_gesture")
        val ANSWER_STYLE = stringPreferencesKey("call_answer_style")
        val SLIDER_ONLY_WHEN_LOCKED = booleanPreferencesKey("call_slider_only_when_locked")
        val ANSWER_SIDE = stringPreferencesKey("answer_side")
        val END_CALL_ALIGNMENT = stringPreferencesKey("end_call_alignment")
        val CALLER_INFO_POSITION = stringPreferencesKey("caller_info_position")
        val CONTROL_GRID_ALIGNMENT = stringPreferencesKey("control_grid_alignment")
        val END_CALL_BUTTON_SIZE = stringPreferencesKey("end_call_button_size")
        val ANSWER_BUTTON_SIZE = stringPreferencesKey("answer_button_size")
        val SHOW_QUICK_REPLIES = booleanPreferencesKey("show_quick_replies")
        val QUICK_REPLIES_BELOW_BUTTONS = booleanPreferencesKey("quick_replies_below_buttons")
        val CONTROL_BUTTON_SIZE = stringPreferencesKey("control_button_size")
        val DTMF_KEY_SIZE = stringPreferencesKey("dtmf_key_size")
        val DIALPAD_TONES = booleanPreferencesKey("dialpad_tones")
        val INCOMING_AVATAR_SIZE = stringPreferencesKey("incoming_avatar_size")
        val INCOMING_AVATAR_SHAPE = stringPreferencesKey("incoming_avatar_shape")
        val ONGOING_CALL_BUTTONS = stringPreferencesKey("ongoing_call_buttons")
        val ADB_MANUAL_PORT = intPreferencesKey("adb_manual_port")
        val ADB_LAST_KNOWN_PORT = intPreferencesKey("adb_last_known_port")
        val ADB_OFFLINE_MODE = booleanPreferencesKey("adb_offline_mode")
        val AUTO_RECORD_CALLS = booleanPreferencesKey("auto_record_calls")
        val AUTO_RECORD_MODE = stringPreferencesKey("auto_record_mode")
        val AUTO_RECORD_EXCLUDED = stringSetPreferencesKey("auto_record_excluded")
        val RECORDING_RETENTION_DAYS = intPreferencesKey("recording_retention_days")
        val LAST_CLEANUP_AT = longPreferencesKey("last_cleanup_at")
        val RECORDINGS_PRIVATE = booleanPreferencesKey("recordings_private")
        val RECORDINGS_SORT = stringPreferencesKey("recordings_sort")
        val ADB_HELPER_PORT = intPreferencesKey("adb_helper_port")
        val ADB_HELPER_TOKEN = stringPreferencesKey("adb_helper_token")
        val SHOW_NUMBER_ON_CALL_SCREEN = booleanPreferencesKey("show_number_on_call_screen")
        val DEFAULT_SIM = stringPreferencesKey("default_sim")
        val QUICK_DECLINE_MESSAGES = stringPreferencesKey("quick_decline_messages")
        val SPEED_DIAL = stringPreferencesKey("speed_dial")
        val BLOCKED_PREFIXES = stringPreferencesKey("blocked_prefixes")
        val SHOW_ADVANCED_SETTINGS = booleanPreferencesKey("show_advanced_settings")
        val MINIMAL_MODE = booleanPreferencesKey("minimal_mode")
        val CONFIRM_BEFORE_CALL = booleanPreferencesKey("confirm_before_call")
        val VIBRATE_ON_ANSWER = booleanPreferencesKey("vibrate_on_answer")
        val DUPLICATES_DISMISSED = booleanPreferencesKey("duplicates_dismissed")
        val DUPLICATES_DISMISSED_HASH = stringPreferencesKey("duplicates_dismissed_hash")
        val FAVORITES_HINT_DISMISSED = booleanPreferencesKey("favorites_hint_dismissed")
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

        val barStyleStr = prefs[Keys.BOTTOM_BAR_STYLE]
        val barStyle = barStyleStr?.let { runCatching { BottomBarStyle.valueOf(it) }.getOrNull() } ?: BottomBarStyle.GLASS

        val barSizeStr = prefs[Keys.BOTTOM_BAR_SIZE]
        val barSize = barSizeStr?.let { runCatching { BottomBarSize.valueOf(it) }.getOrNull() } ?: BottomBarSize.MEDIUM

        val headerSizeStr = prefs[Keys.HEADER_SIZE]
        val headerSize = headerSizeStr?.let { runCatching { HeaderSize.valueOf(it) }.getOrNull() } ?: HeaderSize.COMPACT

        val barShrink = prefs[Keys.BOTTOM_BAR_SHRINK] ?: false

        val glassBottomBar = prefs[Keys.GLASS_BOTTOM_BAR] ?: true
        val keyStyleStr = prefs[Keys.KEY_STYLE]
        val keyStyle = keyStyleStr?.let { runCatching { KeyStyle.valueOf(it) }.getOrNull() }
            ?: if (prefs[Keys.GLASS_DIALPAD] ?: true) KeyStyle.GLASS else KeyStyle.SIMPLE
        val keypadStyleStr = prefs[Keys.KEYPAD_STYLE]
        val keypadStyle = keypadStyleStr?.let { runCatching { KeypadStyle.valueOf(it) }.getOrNull() }
            ?: KeypadStyle.GLASS
        val switchStyleStr = prefs[Keys.SWITCH_STYLE]
        val switchStyle = switchStyleStr?.let { runCatching { SwitchStyle.valueOf(it) }.getOrNull() } ?: SwitchStyle.DEFAULT
        val appStyleStr = prefs[Keys.APP_STYLE]
        val appStyle = appStyleStr?.let { runCatching { AppStyle.valueOf(it) }.getOrNull() } ?: AppStyle.CLASSIC
        val fontStyleStr = prefs[Keys.FONT_STYLE]
        val fontStyle = fontStyleStr?.let { runCatching { FontStyle.valueOf(it) }.getOrNull() } ?: FontStyle.SYSTEM
        val transitionStyleStr = prefs[Keys.TRANSITION_STYLE]
        val transitionStyle = transitionStyleStr?.let { runCatching { TransitionStyle.valueOf(it) }.getOrNull() } ?: TransitionStyle.FADE
        val bottomSheets = prefs[Keys.BOTTOM_SHEETS] ?: false
        val swipeActionsEnabled = prefs[Keys.SWIPE_ACTIONS_ENABLED] ?: true
        val glassHeaders = prefs[Keys.GLASS_HEADERS] ?: true
        val glassContactDetail = prefs[Keys.GLASS_CONTACT_DETAIL] ?: true
        val glassCallScreens = prefs[Keys.GLASS_CALL_SCREENS] ?: true

        val intensityStr = prefs[Keys.GLASS_INTENSITY]
        val glassIntensity = intensityStr?.let { runCatching { GlassIntensity.valueOf(it) }.getOrNull() } ?: GlassIntensity.STANDARD

        val glassBlurAmount = prefs[Keys.GLASS_BLUR_AMOUNT] ?: 1.0f
        val glassTintAmount = prefs[Keys.GLASS_TINT_AMOUNT] ?: 1.0f
        val edgeLightStr = prefs[Keys.EDGE_LIGHT]
        val edgeLight = edgeLightStr?.let { runCatching { EdgeLight.valueOf(it) }.getOrNull() } ?: EdgeLight.SOFT
        val glassColorRichness = prefs[Keys.GLASS_COLOR_RICHNESS] ?: 1.7f
        val dropletStretchStr = prefs[Keys.DROPLET_STRETCH]
        val dropletStretch = dropletStretchStr?.let { runCatching { DropletStretch.valueOf(it) }.getOrNull() } ?: DropletStretch.NORMAL
        val glassHapticStr = prefs[Keys.GLASS_HAPTIC_FEEDBACK]
        val glassHapticFeedback = glassHapticStr?.let { runCatching { GlassHapticFeedback.valueOf(it) }.getOrNull() } ?: GlassHapticFeedback.LIGHT
        val edgeRefraction = prefs[Keys.EDGE_REFRACTION] ?: true
        val lensBar = prefs[Keys.LENS_BAR] ?: true

        val reduceTransparency = prefs[Keys.REDUCE_TRANSPARENCY] ?: false
        val glassCards = prefs[Keys.GLASS_CARDS] ?: false
        val dynamicHighlights = prefs[Keys.DYNAMIC_HIGHLIGHTS] ?: true
        val backdropStr = prefs[Keys.BACKDROP]
        val backdrop = backdropStr?.let { runCatching { Backdrop.valueOf(it) }.getOrNull() } ?: Backdrop.SUBTLE
        val appBackgroundStr = prefs[Keys.APP_BACKGROUND]
        val appBackground = appBackgroundStr?.let { runCatching { AppBackground.valueOf(it) }.getOrNull() } ?: AppBackground.AURORA
        val appBackgroundDim = (prefs[Keys.APP_BACKGROUND_DIM] ?: 0.35f).coerceIn(0f, 0.9f)
        val appBackgroundPhotoVersion = prefs[Keys.APP_BACKGROUND_PHOTO_VERSION] ?: 0L

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

        val callBackgroundDim = prefs[Keys.CALL_BACKGROUND_DIM] ?: 0f

        val callBackgroundCustomColor = prefs[Keys.CALL_BACKGROUND_CUSTOM_COLOR]
            ?: AccentPreset.ONE_UI_BLUE.colorValue

        // Answer style: the stored style wins; before it was ever set,
        // migrate the old answer gesture (TAP -> CLASSIC,
        // SWIPE -> SWIPE_BUTTONS). The gesture setting itself is
        // gone (no screen ever applied it); only this legacy read
        // of its stored value remains, so old users keep their style.
        val answerStyleStr = prefs[Keys.ANSWER_STYLE]
        val answerStyle = answerStyleStr?.let { runCatching { AnswerStyle.valueOf(it) }.getOrNull() }
            ?: when (prefs[Keys.ANSWER_GESTURE]) {
                "SWIPE" -> AnswerStyle.SWIPE_BUTTONS
                else -> AnswerStyle.CLASSIC
            }

        val sliderOnlyWhenLocked = prefs[Keys.SLIDER_ONLY_WHEN_LOCKED] ?: true

        val answerSideStr = prefs[Keys.ANSWER_SIDE]
        val answerSide = answerSideStr?.let { runCatching { AnswerSide.valueOf(it) }.getOrNull() }
            ?: AnswerSide.RIGHT

        val endCallAlignmentStr = prefs[Keys.END_CALL_ALIGNMENT]
        val endCallAlignment = endCallAlignmentStr?.let { runCatching { EndCallAlignment.valueOf(it) }.getOrNull() }
            ?: EndCallAlignment.CENTER

        val callerInfoPositionStr = prefs[Keys.CALLER_INFO_POSITION]
        val callerInfoPosition = callerInfoPositionStr?.let { runCatching { CallerInfoPosition.valueOf(it) }.getOrNull() }
            ?: CallerInfoPosition.TOP

        val controlGridAlignmentStr = prefs[Keys.CONTROL_GRID_ALIGNMENT]
        val controlGridAlignment = controlGridAlignmentStr?.let { runCatching { ControlGridAlignment.valueOf(it) }.getOrNull() }
            ?: ControlGridAlignment.SPREAD

        val endCallButtonSizeStr = prefs[Keys.END_CALL_BUTTON_SIZE]
        val endCallButtonSize = endCallButtonSizeStr?.let { runCatching { EndCallButtonSize.valueOf(it) }.getOrNull() }
            ?: EndCallButtonSize.MEDIUM

        val answerButtonSizeStr = prefs[Keys.ANSWER_BUTTON_SIZE]
        val answerButtonSize = answerButtonSizeStr?.let { runCatching { AnswerButtonSize.valueOf(it) }.getOrNull() }
            ?: AnswerButtonSize.MEDIUM

        val showQuickReplies = prefs[Keys.SHOW_QUICK_REPLIES] ?: true
        val quickRepliesBelowButtons = prefs[Keys.QUICK_REPLIES_BELOW_BUTTONS] ?: false

        val controlButtonSizeStr = prefs[Keys.CONTROL_BUTTON_SIZE]
        val controlButtonSize = controlButtonSizeStr?.let { runCatching { ControlButtonSize.valueOf(it) }.getOrNull() }
            ?: ControlButtonSize.MEDIUM

        val dtmfKeySizeStr = prefs[Keys.DTMF_KEY_SIZE]
        val dtmfKeySize = dtmfKeySizeStr?.let { runCatching { DtmfKeySize.valueOf(it) }.getOrNull() }
            ?: DtmfKeySize.MEDIUM

        val dialpadTones = prefs[Keys.DIALPAD_TONES] ?: false

        val incomingAvatarSizeStr = prefs[Keys.INCOMING_AVATAR_SIZE]
        val incomingAvatarSize = incomingAvatarSizeStr?.let { runCatching { IncomingAvatarSize.valueOf(it) }.getOrNull() }
            ?: IncomingAvatarSize.MEDIUM

        val incomingAvatarShapeStr = prefs[Keys.INCOMING_AVATAR_SHAPE]
        val incomingAvatarShape = incomingAvatarShapeStr?.let { runCatching { IncomingAvatarShape.valueOf(it) }.getOrNull() }
            ?: IncomingAvatarShape.CIRCLE

        // Shown ongoing-call buttons, in display order, as comma-separated
        // names. Missing key = all buttons; empty = none (user hid them all).
        val ongoingCallButtons = prefs[Keys.ONGOING_CALL_BUTTONS]
            ?.split(",")
            ?.mapNotNull { runCatching { OngoingCallButton.valueOf(it) }.getOrNull() }
            ?: OngoingCallButton.entries

        val adbManualPort = prefs[Keys.ADB_MANUAL_PORT]?.takeIf { it in 1..65535 }
        val adbLastKnownPort = prefs[Keys.ADB_LAST_KNOWN_PORT]?.takeIf { it in 1..65535 }
        val adbOfflineMode = prefs[Keys.ADB_OFFLINE_MODE] ?: false
        val autoRecordCalls = prefs[Keys.AUTO_RECORD_CALLS] ?: false
        val autoRecordMode = prefs[Keys.AUTO_RECORD_MODE]
            ?.let { runCatching { AutoRecordMode.valueOf(it) }.getOrNull() } ?: AutoRecordMode.ALL
        val autoRecordExcluded = prefs[Keys.AUTO_RECORD_EXCLUDED] ?: emptySet()
        val recordingRetentionDays = prefs[Keys.RECORDING_RETENTION_DAYS]
            ?.takeIf { it == 0 || it == 7 || it == 30 || it == 90 } ?: 0
        val recordingsPrivate = prefs[Keys.RECORDINGS_PRIVATE] ?: false
        val recordingsSort = prefs[Keys.RECORDINGS_SORT]
            ?.let { runCatching { RecordingSort.valueOf(it) }.getOrNull() } ?: RecordingSort.NEWEST

        val adbHelperPort = prefs[Keys.ADB_HELPER_PORT]?.takeIf { it in 20000..60000 }
        val adbHelperToken = prefs[Keys.ADB_HELPER_TOKEN]?.takeIf { it.isNotBlank() }

        val showNumberOnCallScreen = prefs[Keys.SHOW_NUMBER_ON_CALL_SCREEN] ?: true

        val defaultSimStr = prefs[Keys.DEFAULT_SIM]
        val defaultSim = defaultSimStr?.let { runCatching { DefaultSim.valueOf(it) }.getOrNull() } ?: DefaultSim.ASK

        val quickDeclineStored = prefs[Keys.QUICK_DECLINE_MESSAGES] ?: ""
        val quickDeclineOverrides = (quickDeclineStored.split("\n") + listOf("", "", "", "", ""))
            .take(5)

        // "2=+98912...;3=021..." — digits and '+' never collide
        // with the ';' and '=' separators.
        val speedDial = (prefs[Keys.SPEED_DIAL] ?: "")
            .split(";")
            .mapNotNull { part ->
                val keyValue = part.split("=", limit = 2)
                val digit = keyValue.getOrNull(0)?.singleOrNull()
                val number = keyValue.getOrNull(1)?.takeIf { it.isNotBlank() }
                if (digit != null && number != null) digit to number else null
            }
            .toMap()
        val blockedPrefixes = (prefs[Keys.BLOCKED_PREFIXES] ?: "")
            .split(";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
        val showAdvancedSettings = prefs[Keys.SHOW_ADVANCED_SETTINGS] ?: false
        val minimalMode = prefs[Keys.MINIMAL_MODE] ?: false
        val confirmBeforeCall = prefs[Keys.CONFIRM_BEFORE_CALL] ?: false
        val vibrateOnAnswer = prefs[Keys.VIBRATE_ON_ANSWER] ?: true
        val duplicatesDismissed = prefs[Keys.DUPLICATES_DISMISSED] ?: false
        val duplicatesDismissedHash = prefs[Keys.DUPLICATES_DISMISSED_HASH]
        val favoritesHintDismissed = prefs[Keys.FAVORITES_HINT_DISMISSED] ?: false

        AppSettings(
            themeMode = themeMode,
            accentColor = accent,
            isCustomAccent = isCustom,
            dynamicColor = dynamic,
            pureBlackDarkMode = pureBlack,
            listDensity = density,
            cardStyle = cardStyle,
            fontSizeScale = fontScale,
            headerSize = headerSize,
            animationOption = anim,
            showBottomBarLabels = showLabels,
            bottomBarStyle = barStyle,
            bottomBarSize = barSize,
            shrinkBottomBarOnScroll = barShrink,
            glassBottomBar = glassBottomBar,
            keyStyle = keyStyle,
            keypadStyle = keypadStyle,
            switchStyle = switchStyle,
            appStyle = appStyle,
            fontStyle = fontStyle,
            transitionStyle = transitionStyle,
            bottomSheets = bottomSheets,
            swipeActionsEnabled = swipeActionsEnabled,
            glassHeaders = glassHeaders,
            glassContactDetail = glassContactDetail,
            glassCallScreens = glassCallScreens,
            glassIntensity = glassIntensity,
            glassBlurAmount = glassBlurAmount,
            glassTintAmount = glassTintAmount,
            edgeLight = edgeLight,
            glassColorRichness = glassColorRichness,
            dropletStretch = dropletStretch,
            glassHapticFeedback = glassHapticFeedback,
            edgeRefraction = edgeRefraction,
            lensBar = lensBar,
            reduceTransparency = reduceTransparency,
            glassCards = glassCards,
            dynamicHighlights = dynamicHighlights,
            backdrop = backdrop,
            appBackground = appBackground,
            appBackgroundDim = appBackgroundDim,
            appBackgroundPhotoVersion = appBackgroundPhotoVersion,
            defaultStartTab = startTab,
            contactSortOrder = sortOrder,
            contactNameFormat = nameFormat,
            callDisplay = callDisplay,
            callBackground = callBackground,
            callBackgroundDim = callBackgroundDim,
            callBackgroundCustomColor = callBackgroundCustomColor,
            answerStyle = answerStyle,
            sliderOnlyWhenLocked = sliderOnlyWhenLocked,
            answerSide = answerSide,
            endCallAlignment = endCallAlignment,
            callerInfoPosition = callerInfoPosition,
            controlGridAlignment = controlGridAlignment,
            endCallButtonSize = endCallButtonSize,
            answerButtonSize = answerButtonSize,
            showQuickReplies = showQuickReplies,
            quickRepliesBelowButtons = quickRepliesBelowButtons,
            controlButtonSize = controlButtonSize,
            dtmfKeySize = dtmfKeySize,
            dialpadTones = dialpadTones,
            incomingAvatarSize = incomingAvatarSize,
            incomingAvatarShape = incomingAvatarShape,
            ongoingCallButtons = ongoingCallButtons,
            adbManualPort = adbManualPort,
            adbLastKnownPort = adbLastKnownPort,
            adbOfflineMode = adbOfflineMode,
            autoRecordCalls = autoRecordCalls,
            autoRecordMode = autoRecordMode,
            autoRecordExcluded = autoRecordExcluded,
            recordingRetentionDays = recordingRetentionDays,
            recordingsPrivate = recordingsPrivate,
            recordingsSort = recordingsSort,
            adbHelperPort = adbHelperPort,
            adbHelperToken = adbHelperToken,
            showNumberOnCallScreen = showNumberOnCallScreen,
            defaultSim = defaultSim,
            confirmBeforeCall = confirmBeforeCall,
            vibrateOnAnswer = vibrateOnAnswer,
            quickDeclineOverrides = quickDeclineOverrides,
            speedDial = speedDial,
            blockedPrefixes = blockedPrefixes,
            showAdvancedSettings = showAdvancedSettings,
            minimalMode = minimalMode,
            duplicatesDismissed = duplicatesDismissed,
            duplicatesDismissedHash = duplicatesDismissedHash,
            favoritesHintDismissed = favoritesHintDismissed
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
            prefs[Keys.APP_STYLE] = AppStyle.CUSTOM.name
        }
    }

    // Picking a preset also copies it into the individual values, so
    // switching to Custom later starts from the preset that showed.
    // CUSTOM itself only flips the marker; the values stay as stored.
    override suspend fun setAppStyle(style: AppStyle) {
        dataStore.edit { prefs ->
            prefs[Keys.APP_STYLE] = style.name
            val preset = when (style) {
                AppStyle.CLASSIC -> StylePreset.CLASSIC
                AppStyle.CLEAN -> StylePreset.CLEAN
                AppStyle.CUSTOM -> null
            }
            if (preset != null) {
                prefs[Keys.FONT_STYLE] = preset.font.name
                prefs[Keys.CARD_STYLE] = preset.card.name
                prefs[Keys.KEY_STYLE] = preset.keys.name
                prefs[Keys.SWITCH_STYLE] = preset.switch.name
                prefs[Keys.TRANSITION_STYLE] = preset.transitions.name
                prefs[Keys.BOTTOM_SHEETS] = preset.sheets
            }
        }
    }

    override suspend fun setFontStyle(style: FontStyle) {
        dataStore.edit { prefs ->
            prefs[Keys.FONT_STYLE] = style.name
            prefs[Keys.APP_STYLE] = AppStyle.CUSTOM.name
        }
    }

    override suspend fun setTransitionStyle(style: TransitionStyle) {
        dataStore.edit { prefs ->
            prefs[Keys.TRANSITION_STYLE] = style.name
            prefs[Keys.APP_STYLE] = AppStyle.CUSTOM.name
        }
    }

    override suspend fun setBottomSheets(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.BOTTOM_SHEETS] = enabled
            prefs[Keys.APP_STYLE] = AppStyle.CUSTOM.name
        }
    }

    override suspend fun setSwipeActionsEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.SWIPE_ACTIONS_ENABLED] = enabled
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

    override suspend fun setBottomBarStyle(style: BottomBarStyle) {
        dataStore.edit { prefs ->
            prefs[Keys.BOTTOM_BAR_STYLE] = style.name
        }
    }

    override suspend fun setBottomBarSize(size: BottomBarSize) {
        dataStore.edit { prefs ->
            prefs[Keys.BOTTOM_BAR_SIZE] = size.name
        }
    }

    override suspend fun setHeaderSize(size: HeaderSize) {
        dataStore.edit { prefs ->
            prefs[Keys.HEADER_SIZE] = size.name
        }
    }

    override suspend fun setShrinkBottomBarOnScroll(shrink: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.BOTTOM_BAR_SHRINK] = shrink
        }
    }

    override suspend fun setGlassBottomBar(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.GLASS_BOTTOM_BAR] = enabled
        }
    }

    override suspend fun setKeyStyle(style: KeyStyle) {
        dataStore.edit { prefs ->
            prefs[Keys.KEY_STYLE] = style.name
            prefs[Keys.APP_STYLE] = AppStyle.CUSTOM.name
        }
    }

    // Standalone like the app background: picking a keypad style
    // does not flip the app style to Custom.
    override suspend fun setKeypadStyle(style: KeypadStyle) {
        dataStore.edit { prefs ->
            prefs[Keys.KEYPAD_STYLE] = style.name
        }
    }

    override suspend fun setSwitchStyle(style: SwitchStyle) {
        dataStore.edit { prefs ->
            prefs[Keys.SWITCH_STYLE] = style.name
            prefs[Keys.APP_STYLE] = AppStyle.CUSTOM.name
        }
    }

    override suspend fun setGlassHeaders(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.GLASS_HEADERS] = enabled
        }
    }

    override suspend fun setGlassContactDetail(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.GLASS_CONTACT_DETAIL] = enabled
        }
    }

    override suspend fun setGlassCallScreens(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.GLASS_CALL_SCREENS] = enabled
        }
    }

    override suspend fun setGlassIntensity(intensity: GlassIntensity) {
        dataStore.edit { prefs ->
            prefs[Keys.GLASS_INTENSITY] = intensity.name
        }
    }

    override suspend fun setGlassBlurAmount(amount: Float) {
        dataStore.edit { prefs ->
            prefs[Keys.GLASS_BLUR_AMOUNT] = amount
        }
    }

    override suspend fun setGlassTintAmount(amount: Float) {
        dataStore.edit { prefs ->
            prefs[Keys.GLASS_TINT_AMOUNT] = amount
        }
    }

    override suspend fun setEdgeLight(edgeLight: EdgeLight) {
        dataStore.edit { prefs ->
            prefs[Keys.EDGE_LIGHT] = edgeLight.name
        }
    }

    override suspend fun setGlassColorRichness(richness: Float) {
        dataStore.edit { prefs ->
            prefs[Keys.GLASS_COLOR_RICHNESS] = richness
        }
    }

    override suspend fun setDropletStretch(stretch: DropletStretch) {
        dataStore.edit { prefs ->
            prefs[Keys.DROPLET_STRETCH] = stretch.name
        }
    }

    override suspend fun setGlassHapticFeedback(feedback: GlassHapticFeedback) {
        dataStore.edit { prefs ->
            prefs[Keys.GLASS_HAPTIC_FEEDBACK] = feedback.name
        }
    }

    override suspend fun setEdgeRefraction(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.EDGE_REFRACTION] = enabled
        }
    }

    override suspend fun setLensBar(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.LENS_BAR] = enabled
        }
    }

    override suspend fun setReduceTransparency(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.REDUCE_TRANSPARENCY] = enabled
        }
    }

    override suspend fun setGlassCards(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.GLASS_CARDS] = enabled
        }
    }

    override suspend fun setDynamicHighlights(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.DYNAMIC_HIGHLIGHTS] = enabled
        }
    }

    override suspend fun setBackdrop(backdrop: Backdrop) {
        dataStore.edit { prefs ->
            prefs[Keys.BACKDROP] = backdrop.name
        }
    }

    override suspend fun setAppBackground(background: AppBackground) {
        dataStore.edit { prefs ->
            prefs[Keys.APP_BACKGROUND] = background.name
        }
    }

    override suspend fun setAppBackgroundDim(dim: Float) {
        dataStore.edit { prefs ->
            prefs[Keys.APP_BACKGROUND_DIM] = dim.coerceIn(0f, 0.9f)
        }
    }

    override suspend fun setAppBackgroundPhotoVersion(version: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.APP_BACKGROUND_PHOTO_VERSION] = version
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

    override suspend fun setCallBackgroundDim(dim: Float) {
        dataStore.edit { prefs ->
            prefs[Keys.CALL_BACKGROUND_DIM] = dim.coerceIn(0f, 1f)
        }
    }

    override suspend fun setCallBackgroundCustomColor(color: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.CALL_BACKGROUND_CUSTOM_COLOR] = color
        }
    }

    override suspend fun setAnswerStyle(style: AnswerStyle) {
        dataStore.edit { prefs ->
            prefs[Keys.ANSWER_STYLE] = style.name
        }
    }

    override suspend fun setSliderOnlyWhenLocked(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.SLIDER_ONLY_WHEN_LOCKED] = enabled
        }
    }

    override suspend fun setAnswerSide(side: AnswerSide) {
        dataStore.edit { prefs ->
            prefs[Keys.ANSWER_SIDE] = side.name
        }
    }

    override suspend fun setEndCallAlignment(alignment: EndCallAlignment) {
        dataStore.edit { prefs ->
            prefs[Keys.END_CALL_ALIGNMENT] = alignment.name
        }
    }

    override suspend fun setCallerInfoPosition(position: CallerInfoPosition) {
        dataStore.edit { prefs ->
            prefs[Keys.CALLER_INFO_POSITION] = position.name
        }
    }

    override suspend fun setControlGridAlignment(alignment: ControlGridAlignment) {
        dataStore.edit { prefs ->
            prefs[Keys.CONTROL_GRID_ALIGNMENT] = alignment.name
        }
    }

    override suspend fun setEndCallButtonSize(size: EndCallButtonSize) {
        dataStore.edit { prefs ->
            prefs[Keys.END_CALL_BUTTON_SIZE] = size.name
        }
    }

    override suspend fun setAnswerButtonSize(size: AnswerButtonSize) {
        dataStore.edit { prefs ->
            prefs[Keys.ANSWER_BUTTON_SIZE] = size.name
        }
    }

    override suspend fun setShowQuickReplies(show: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.SHOW_QUICK_REPLIES] = show
        }
    }

    override suspend fun setQuickRepliesBelowButtons(below: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.QUICK_REPLIES_BELOW_BUTTONS] = below
        }
    }

    override suspend fun setControlButtonSize(size: ControlButtonSize) {
        dataStore.edit { prefs ->
            prefs[Keys.CONTROL_BUTTON_SIZE] = size.name
        }
    }

    override suspend fun setDtmfKeySize(size: DtmfKeySize) {
        dataStore.edit { prefs ->
            prefs[Keys.DTMF_KEY_SIZE] = size.name
        }
    }

    override suspend fun setDialpadTones(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.DIALPAD_TONES] = enabled
        }
    }

    override suspend fun setIncomingAvatarSize(size: IncomingAvatarSize) {
        dataStore.edit { prefs ->
            prefs[Keys.INCOMING_AVATAR_SIZE] = size.name
        }
    }

    override suspend fun setIncomingAvatarShape(shape: IncomingAvatarShape) {
        dataStore.edit { prefs ->
            prefs[Keys.INCOMING_AVATAR_SHAPE] = shape.name
        }
    }

    override suspend fun setOngoingCallButtons(buttons: List<OngoingCallButton>) {
        dataStore.edit { prefs ->
            prefs[Keys.ONGOING_CALL_BUTTONS] = buttons.joinToString(",") { it.name }
        }
    }

    override suspend fun setAdbManualPort(port: Int?) {
        dataStore.edit { prefs ->
            if (port == null) {
                prefs.remove(Keys.ADB_MANUAL_PORT)
            } else {
                prefs[Keys.ADB_MANUAL_PORT] = port
            }
        }
    }

    override suspend fun setAdbLastKnownPort(port: Int?) {
        dataStore.edit { prefs ->
            if (port == null) {
                prefs.remove(Keys.ADB_LAST_KNOWN_PORT)
            } else {
                prefs[Keys.ADB_LAST_KNOWN_PORT] = port
            }
        }
    }

    override suspend fun setAdbOfflineMode(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.ADB_OFFLINE_MODE] = enabled }
    }

    override suspend fun setAutoRecordCalls(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.AUTO_RECORD_CALLS] = enabled }
    }

    override suspend fun setAutoRecordMode(mode: AutoRecordMode) {
        dataStore.edit { prefs -> prefs[Keys.AUTO_RECORD_MODE] = mode.name }
    }

    override suspend fun setAutoRecordExcluded(numbers: Set<String>) {
        dataStore.edit { prefs -> prefs[Keys.AUTO_RECORD_EXCLUDED] = numbers }
    }

    override suspend fun setRecordingRetentionDays(days: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.RECORDING_RETENTION_DAYS] =
                if (days == 0 || days == 7 || days == 30 || days == 90) days else 0
        }
    }

    override suspend fun getLastCleanupAt(): Long {
        return dataStore.data.first()[Keys.LAST_CLEANUP_AT] ?: 0L
    }

    override suspend fun setLastCleanupAt(time: Long) {
        dataStore.edit { prefs -> prefs[Keys.LAST_CLEANUP_AT] = time }
    }

    override suspend fun setRecordingsPrivate(private: Boolean) {
        dataStore.edit { prefs -> prefs[Keys.RECORDINGS_PRIVATE] = private }
    }

    override suspend fun setRecordingsSort(sort: RecordingSort) {
        dataStore.edit { prefs -> prefs[Keys.RECORDINGS_SORT] = sort.name }
    }

    override suspend fun setAdbHelperPort(port: Int?) {
        dataStore.edit { prefs ->
            if (port == null) {
                prefs.remove(Keys.ADB_HELPER_PORT)
            } else {
                prefs[Keys.ADB_HELPER_PORT] = port
            }
        }
    }

    override suspend fun setAdbHelperToken(token: String?) {
        dataStore.edit { prefs ->
            if (token == null) {
                prefs.remove(Keys.ADB_HELPER_TOKEN)
            } else {
                prefs[Keys.ADB_HELPER_TOKEN] = token
            }
        }
    }

    override suspend fun setShowNumberOnCallScreen(show: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.SHOW_NUMBER_ON_CALL_SCREEN] = show
        }
    }

    override suspend fun setDefaultSim(defaultSim: DefaultSim) {
        dataStore.edit { prefs ->
            prefs[Keys.DEFAULT_SIM] = defaultSim.name
        }
    }

    override suspend fun setQuickDeclineMessages(messages: List<String>) {
        dataStore.edit { prefs ->
            prefs[Keys.QUICK_DECLINE_MESSAGES] = (messages.map { it.replace("\n", " ") } + listOf("", "", "", "", ""))
                .take(5)
                .joinToString("\n")
        }
    }

    override suspend fun exportToJson(): String {
        val prefs = dataStore.data.first()
        return SettingsBackup.encode(prefs.asMap().mapKeys { it.key.name })
    }

    override suspend fun importFromJson(json: String): Int {
        val values = SettingsBackup.decode(json)
        dataStore.edit { prefs ->
            for ((name, value) in values) {
                when (value) {
                    is Boolean -> prefs[booleanPreferencesKey(name)] = value
                    is Int -> prefs[intPreferencesKey(name)] = value
                    is Long -> prefs[longPreferencesKey(name)] = value
                    is Float -> prefs[floatPreferencesKey(name)] = value
                    is Double -> prefs[doublePreferencesKey(name)] = value
                    is String -> prefs[stringPreferencesKey(name)] = value
                    is Set<*> -> prefs[stringSetPreferencesKey(name)] = value.map { it.toString() }.toSet()
                }
            }
        }
        return values.size
    }

    override suspend fun setSpeedDial(speedDial: Map<Char, String>) {
        dataStore.edit { prefs ->
            prefs[Keys.SPEED_DIAL] = speedDial.entries
                .filter { it.key in '2'..'9' && it.value.isNotBlank() }
                .joinToString(";") { "${it.key}=${it.value.trim()}" }
        }
    }

    override suspend fun setBlockedPrefixes(prefixes: Set<String>) {
        dataStore.edit { prefs ->
            prefs[Keys.BLOCKED_PREFIXES] = prefixes
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString(";")
        }
    }

    override suspend fun setShowAdvancedSettings(show: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.SHOW_ADVANCED_SETTINGS] = show
        }
    }

    override suspend fun setMinimalMode(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.MINIMAL_MODE] = enabled
        }
    }

    override suspend fun setConfirmBeforeCall(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.CONFIRM_BEFORE_CALL] = enabled
        }
    }

    override suspend fun setVibrateOnAnswer(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.VIBRATE_ON_ANSWER] = enabled
        }
    }

    override suspend fun setDuplicatesDismissed(dismissed: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.DUPLICATES_DISMISSED] = dismissed
        }
    }

    override suspend fun setDuplicatesDismissedHash(hash: String) {
        dataStore.edit { prefs ->
            prefs[Keys.DUPLICATES_DISMISSED_HASH] = hash
        }
    }

    override suspend fun setFavoritesHintDismissed(dismissed: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.FAVORITES_HINT_DISMISSED] = dismissed
        }
    }

    override suspend fun resetToDefaults() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
