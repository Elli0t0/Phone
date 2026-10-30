package io.github.elli0t0.phone.data.settings.model

enum class AccentPreset(val colorValue: Long, val label: String) {
    ONE_UI_BLUE(0xFF0072DEL, "Blue"),
    EMERALD_GREEN(0xFF00875AL, "Green"),
    ROYAL_PURPLE(0xFF6554C0L, "Purple"),
    SUNSET_ORANGE(0xFFFF5630L, "Orange"),
    CRIMSON_RED(0xFFDE350BL, "Red"),
    TEAL_CYAN(0xFF00B8D9L, "Cyan"),
    ROSE_PINK(0xFFE03997L, "Pink"),
    AMBER_GOLD(0xFFFFAB00L, "Amber")
}

enum class ListDensity(val verticalPaddingDp: Int, val label: String) {
    COMPACT(4, "Compact"),
    DEFAULT(8, "Default"),
    COMFORTABLE(14, "Comfortable")
}

enum class CardStyle(val label: String) {
    ROUNDED("Rounded"),
    SQUARE("Square"),
    OUTLINED("Outlined"),
    INSET("Inset")
}

enum class KeyStyle(val label: String) {
    GLASS("Glass"),
    SIMPLE("Simple"),
    FLAT("Flat")
}

/**
 * Dialpad digit-key look, independent of the app-style preset:
 * GLASS is the current unified key, CLASSIC restores the
 * pre-redesign glass key, MINIMAL is bare digits with a ripple,
 * SQUIRCLE is a rounded-square tonal tile.
 */
enum class KeypadStyle(val label: String) {
    GLASS("Glass"),
    CLASSIC("Classic"),
    MINIMAL("Minimal"),
    SQUIRCLE("Squircle")
}

enum class SwitchStyle(val label: String) {
    DEFAULT("Default"),
    GREEN("Green")
}

/**
 * Master look preset. Classic is the default: nothing changes for
 * existing users. Clean is the newer look. Custom means at least one
 * individual option was changed away from the selected preset (see
 * [StylePreset]); the stored individual values are the look then.
 */
enum class AppStyle(val label: String) {
    CLASSIC("Classic"),
    CLEAN("Clean"),
    CUSTOM("Custom")
}

/** Typeface family for the app typography (see Type.kt). */
enum class FontStyle(val label: String) {
    SYSTEM("System"),
    INTER("Inter")
}

/** Push-screen transition: a plain fade, or the sliding push. */
enum class TransitionStyle(val label: String) {
    FADE("Fade"),
    PUSH("Push")
}

/**
 * One look, as seven independent options. The presets below are the
 * two built-in looks; [AppSettings.effectiveStyle] resolves CUSTOM
 * to the individually stored values.
 */
data class StylePreset(
    val font: FontStyle,
    val card: CardStyle,
    val keys: KeyStyle,
    val switch: SwitchStyle,
    val transitions: TransitionStyle,
    val sheets: Boolean,
    val swipeActions: Boolean
) {
    companion object {
        val CLASSIC = StylePreset(
            font = FontStyle.SYSTEM,
            card = CardStyle.ROUNDED,
            keys = KeyStyle.GLASS,
            switch = SwitchStyle.DEFAULT,
            transitions = TransitionStyle.FADE,
            sheets = false,
            swipeActions = false
        )
        val CLEAN = StylePreset(
            font = FontStyle.INTER,
            card = CardStyle.INSET,
            keys = KeyStyle.FLAT,
            switch = SwitchStyle.GREEN,
            transitions = TransitionStyle.PUSH,
            sheets = true,
            swipeActions = true
        )
    }
}

enum class AnimationOption(val label: String) {    ON("On"),
    REDUCED("Reduced"),
    OFF("Off")
}

enum class StartTab(val route: String, val label: String) {
    KEYPAD("dialpad", "Keypad"),
    RECENTS("recents", "Recents"),
    CONTACTS("contacts", "Contacts")
}

enum class ContactSortOrder(val label: String) {
    FIRST_NAME("First name"),
    LAST_NAME("Last name")
}

enum class ContactNameFormat(val label: String) {
    FIRST_NAME_FIRST("First name first"),
    LAST_NAME_FIRST("Last name first")
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class CallDisplay(val label: String) {
    FULL_SCREEN("Full screen"),
    POPUP("Pop-up")
}

enum class CallBackground(val label: String) {
    DEFAULT("Default"),
    DARK("Dark"),
    ACCENT("Accent"),
    SUNSET("Sunset"),
    OCEAN("Ocean"),
    FOREST("Forest"),
    LAVENDER("Lavender"),
    ROSE("Rose"),
    MIDNIGHT("Midnight"),
    AMBER("Amber"),
    AURORA("Aurora"),
    CUSTOM("Custom")
}

enum class AnswerStyle(val label: String) {
    CLASSIC("Classic"),
    SLIDER("Slider"),
    SWIPE_BUTTONS("Swipe buttons"),
    LIQUID("Liquid")
}

enum class AnswerSide(val label: String) {
    RIGHT("Answer on right"),
    LEFT("Answer on left")
}

enum class EndCallAlignment(val label: String) {
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right")
}

enum class CallerInfoPosition(val label: String) {
    TOP("Top"),
    CENTER("Center")
}

enum class ControlGridAlignment(val label: String) {
    SPREAD("Spread"),
    CENTER("Center"),
    START("Start"),
    END("End")
}

enum class EndCallButtonSize(val label: String, val scale: Float) {
    SMALL("Small", 0.8f),
    MEDIUM("Medium", 1f),
    LARGE("Large", 1.25f)
}

enum class AnswerButtonSize(val label: String, val scale: Float) {
    SMALL("Small", 0.8f),
    MEDIUM("Medium", 1f),
    LARGE("Large", 1.25f)
}

enum class ControlButtonSize(val label: String, val scale: Float) {
    SMALL("Small", 0.8f),
    MEDIUM("Medium", 1f),
    LARGE("Large", 1.25f)
}

enum class DtmfKeySize(val label: String, val scale: Float) {
    SMALL("Small", 0.8f),
    MEDIUM("Medium", 1f),
    LARGE("Large", 1.25f)
}

enum class IncomingAvatarSize(val label: String, val scale: Float) {
    SMALL("Small", 0.8f),
    MEDIUM("Medium", 1f),
    LARGE("Large", 1.25f)
}

enum class IncomingAvatarShape(val label: String) {
    CIRCLE("Circle"),
    ROUNDED("Rounded")
}

enum class OngoingCallButton(val label: String) {
    MUTE("Mute"),
    SPEAKER("Speaker"),
    BLUETOOTH("Bluetooth"),
    KEYPAD("Keypad"),
    HOLD("Hold"),
    RECORD("Record")
}

enum class DefaultSim(val label: String) {
    ASK("Ask every time"),
    SIM_1("SIM 1"),
    SIM_2("SIM 2")
}

enum class BottomBarStyle(val label: String) {
    GLASS("Glass"),
    SOLID("Solid")
}

/**
 * The single bottom-bar picker (0.17.7): three choices derived from
 * the overlapping bottomBarStyle / glassBottomBar / lensBar fields.
 * SOLID reads from the style alone; LENS needs GLASS + lens on.
 */
enum class BottomBarChoice(val label: String) {
    LENS("Lens glass"),
    GLASS("Glass"),
    SOLID("Solid")
}

val AppSettings.bottomBarChoice: BottomBarChoice
    get() = when {
        bottomBarStyle == BottomBarStyle.SOLID -> BottomBarChoice.SOLID
        lensBar -> BottomBarChoice.LENS
        else -> BottomBarChoice.GLASS
    }

/**
 * The single accent picker (0.17.7): Dynamic / Presets / Custom,
 * derived from the dynamicColor / isCustomAccent flags.
 */
enum class AccentChoice(val label: String) {
    DYNAMIC("Dynamic"),
    PRESETS("Presets"),
    CUSTOM("Custom")
}

val AppSettings.accentChoice: AccentChoice
    get() = when {
        dynamicColor -> AccentChoice.DYNAMIC
        isCustomAccent -> AccentChoice.CUSTOM
        else -> AccentChoice.PRESETS
    }

enum class BottomBarSize(val label: String) {
    SMALL("Small"),
    MEDIUM("Medium"),
    LARGE("Large")
}

enum class HeaderSize(val label: String) {
    COMPACT("Compact"),
    STANDARD("Standard"),
    LARGE("Large")
}

enum class GlassIntensity(val label: String) {
    SUBTLE("Subtle"),
    STANDARD("Standard"),
    STRONG("Strong")
}

/**
 * Glass edge light: scales every glass rim alpha. Soft reproduces the
 * current rims; Off removes them; Bright strengthens them.
 */
enum class EdgeLight(val label: String, val factor: Float) {
    OFF("Off", 0f),
    SOFT("Soft", 1f),
    BRIGHT("Bright", 1.6f)
}

/** Bottom-bar droplet stretch: scales the bar's stretchPx. */
enum class DropletStretch(val label: String, val factor: Float) {
    OFF("Off", 0f),
    NORMAL("Normal", 1f),
    EXTRA("Extra", 1.5f)
}

/** Haptic intensity for glass interactions (bottom-bar tabs). */
enum class GlassHapticFeedback(val label: String) {
    OFF("Off"),
    LIGHT("Light"),
    MEDIUM("Medium")
}

/**
 * Ambient backdrop behind the tab content: three large soft
 * accent-hue blobs drifting slowly, giving glass something to
 * blur. Strength is the blob center alpha.
 */
enum class Backdrop(val label: String, val strength: Float) {
    OFF("Off", 0f),
    SUBTLE("Subtle", 0.20f),
    VIVID("Vivid", 0.40f)
}

/**
 * App-wide background behind the tab content. AURORA (the drifting
 * accent blobs) is the historical look and the default, so existing
 * users see no change; the other entries reuse the call-background
 * gradients, PLAIN is the bare theme background, PHOTO is a
 * user-chosen image from filesDir.
 */
enum class AppBackground(val label: String, val gradient: CallBackground?) {
    AURORA("Aurora blobs", null),
    ACCENT("Accent", CallBackground.ACCENT),
    SUNSET("Sunset", CallBackground.SUNSET),
    OCEAN("Ocean", CallBackground.OCEAN),
    FOREST("Forest", CallBackground.FOREST),
    LAVENDER("Lavender", CallBackground.LAVENDER),
    ROSE("Rose", CallBackground.ROSE),
    MIDNIGHT("Midnight", CallBackground.MIDNIGHT),
    AMBER("Amber", CallBackground.AMBER),
    PLAIN("Plain", null),
    PHOTO("Photo", null)
}

/** Which calls auto-record captures; ALL is the historical behavior. */
enum class AutoRecordMode(val label: String) {
    ALL("All calls"),
    CONTACTS_ONLY("Only contacts"),
    UNKNOWN_ONLY("Only unknown numbers"),
    EXCEPT_LIST("All except selected numbers")
}

/** Recordings library sort order. */
enum class RecordingSort(val label: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    LONGEST("Longest"),
    LARGEST("Largest"),
    STARRED_FIRST("Starred first")
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: Long = AccentPreset.ONE_UI_BLUE.colorValue,
    val isCustomAccent: Boolean = false,
    val dynamicColor: Boolean = true,
    val pureBlackDarkMode: Boolean = false,
    val listDensity: ListDensity = ListDensity.DEFAULT,
    val cardStyle: CardStyle = CardStyle.ROUNDED,
    val fontSizeScale: Float = 1.0f,
    val headerSize: HeaderSize = HeaderSize.COMPACT,
    val animationOption: AnimationOption = AnimationOption.ON,
    val showBottomBarLabels: Boolean = true,
    val bottomBarStyle: BottomBarStyle = BottomBarStyle.GLASS,
    val bottomBarSize: BottomBarSize = BottomBarSize.MEDIUM,
    val shrinkBottomBarOnScroll: Boolean = false,
    // Liquid glass per section: each area with glass surfaces can fall
    // back to its flat rendering. All default to glass on.
    val glassBottomBar: Boolean = true,
    // Lens-glass bottom bar (Backdrop library): default on, but only
    // active on API 33+ without Reduce transparency; otherwise the
    // current Haze bar is used unchanged.
    val lensBar: Boolean = true,
    val keyStyle: KeyStyle = KeyStyle.GLASS,
    /** Dialpad digit-key look, independent of the app-style preset. */
    val keypadStyle: KeypadStyle = KeypadStyle.GLASS,
    val switchStyle: SwitchStyle = SwitchStyle.DEFAULT,
    // Master look preset (see StylePreset). The individual values
    // below are the look when appStyle is CUSTOM, and are copied from
    // the preset whenever a preset is picked, so switching to Custom
    // later starts from the preset that was showing.
    val appStyle: AppStyle = AppStyle.CLASSIC,
    val fontStyle: FontStyle = FontStyle.SYSTEM,
    val transitionStyle: TransitionStyle = TransitionStyle.FADE,
    val bottomSheets: Boolean = false,
    val swipeActions: Boolean = false,
    val glassHeaders: Boolean = true,
    val glassContactDetail: Boolean = true,
    val glassCallScreens: Boolean = true,
    val glassIntensity: GlassIntensity = GlassIntensity.STANDARD,
    val reduceTransparency: Boolean = false,
    val glassCards: Boolean = false,
    // Glass Studio fine-tuning. Defaults reproduce the current
    // behavior exactly: full blur radius and tint, soft rims,
    // 1.7x color richness, normal droplet stretch, light haptics.
    val glassBlurAmount: Float = 1.0f,
    val glassTintAmount: Float = 1.0f,
    val edgeLight: EdgeLight = EdgeLight.SOFT,
    val glassColorRichness: Float = 1.7f,
    val dropletStretch: DropletStretch = DropletStretch.NORMAL,
    val glassHapticFeedback: GlassHapticFeedback = GlassHapticFeedback.LIGHT,
    // Edge refraction on the floating bottom bar's blur layer.
    // Default on; forced off by Reduce transparency (no blur, so
    // nothing to refract) and by API < 33 (no RuntimeShader).
    val edgeRefraction: Boolean = true,
    // Dynamic highlights: the premium glass specular border shifts
    // with device tilt. Default on; off when Animations is Off.
    val dynamicHighlights: Boolean = true,
    // Ambient backdrop behind the tab content. Forced Off when
    // Pure black dark mode is on (see AmbientBackdrop).
    val backdrop: Backdrop = Backdrop.SUBTLE,
    val appBackground: AppBackground = AppBackground.AURORA,
    val appBackgroundDim: Float = 0.35f,
    val appBackgroundPhotoVersion: Long = 0L,
    val defaultStartTab: StartTab = StartTab.KEYPAD,
    val contactSortOrder: ContactSortOrder = ContactSortOrder.FIRST_NAME,
    val contactNameFormat: ContactNameFormat = ContactNameFormat.FIRST_NAME_FIRST,
    val callDisplay: CallDisplay = CallDisplay.FULL_SCREEN,
    val callBackground: CallBackground = CallBackground.DEFAULT,
    val callBackgroundDim: Float = 0f,
    val callBackgroundCustomColor: Long = AccentPreset.ONE_UI_BLUE.colorValue,
    val answerStyle: AnswerStyle = AnswerStyle.CLASSIC,
    val sliderOnlyWhenLocked: Boolean = true,
    val answerSide: AnswerSide = AnswerSide.RIGHT,
    val endCallAlignment: EndCallAlignment = EndCallAlignment.CENTER,
    val callerInfoPosition: CallerInfoPosition = CallerInfoPosition.TOP,
    val controlGridAlignment: ControlGridAlignment = ControlGridAlignment.SPREAD,
    val endCallButtonSize: EndCallButtonSize = EndCallButtonSize.MEDIUM,
    val answerButtonSize: AnswerButtonSize = AnswerButtonSize.MEDIUM,
    val showQuickReplies: Boolean = true,
    val quickRepliesBelowButtons: Boolean = false,
    val controlButtonSize: ControlButtonSize = ControlButtonSize.MEDIUM,
    val dtmfKeySize: DtmfKeySize = DtmfKeySize.MEDIUM,
    val dialpadTones: Boolean = false,
    val incomingAvatarSize: IncomingAvatarSize = IncomingAvatarSize.MEDIUM,
    val incomingAvatarShape: IncomingAvatarShape = IncomingAvatarShape.CIRCLE,
    val ongoingCallButtons: List<OngoingCallButton> = OngoingCallButton.entries,
    /** Manual ADB connect port: always tried before NSD discovery. */
    val adbManualPort: Int? = null,
    /** Last ADB connect port that worked; tried after NSD results. */
    val adbLastKnownPort: Int? = null,
    /** Offline ADB recording: prefer a local debugging port (5555) over Wi-Fi. */
    val adbOfflineMode: Boolean = false,
    /** Auto-record every call once it connects (0.17.7). */
    val autoRecordCalls: Boolean = false,
    val autoRecordMode: AutoRecordMode = AutoRecordMode.ALL,
    val autoRecordExcluded: Set<String> = emptySet(),
    /** 0 = never; otherwise auto-delete after this many days. */
    val recordingRetentionDays: Int = 0,
    val recordingsPrivate: Boolean = false,
    val recordingsSort: RecordingSort = RecordingSort.NEWEST,
    /** Loopback recorder-helper port/token: an already running helper
     * can be reused after an app restart instead of relaunching. */
    val adbHelperPort: Int? = null,
    val adbHelperToken: String? = null,
    val showNumberOnCallScreen: Boolean = true,
    val defaultSim: DefaultSim = DefaultSim.ASK,
    // Quick-decline message overrides, one per reply chip. A blank entry
    // falls back to the built-in default message for that slot, so an
    // untouched setting keeps the localized defaults.
    val quickDeclineOverrides: List<String> = emptyList(),
    // Speed dial: digit ('2'-'9') to number, assigned by long-pressing
    // a digit with a number typed. '1' is always voicemail.
    val speedDial: Map<Char, String> = emptyMap(),
    // App-side blocked prefixes: numbers starting with one of these
    // are rejected on arrival (the platform provider only matches
    // exact numbers, so prefixes live here, not there).
    val blockedPrefixes: Set<String> = emptySet(),
    // Restructured Settings (0.16.9): the main page shows eight
    // section rows; advanced rows hide unless this is on.
    val showAdvancedSettings: Boolean = false,
    // Dismissed once, the duplicates banner moves into the Contacts
    // overflow menu; the favorites hint hides for good.
    val duplicatesDismissed: Boolean = false,
    val favoritesHintDismissed: Boolean = false
) {
    /**
     * The look screens actually render: the preset for CLASSIC/CLEAN,
     * the individually stored values for CUSTOM. Screens read this,
     * never the raw fields, so presets and custom values always agree.
     */
    val effectiveStyle: StylePreset
        get() = when (appStyle) {
            AppStyle.CLASSIC -> StylePreset.CLASSIC
            AppStyle.CLEAN -> StylePreset.CLEAN
            AppStyle.CUSTOM -> StylePreset(
                font = fontStyle,
                card = cardStyle,
                keys = keyStyle,
                switch = switchStyle,
                transitions = transitionStyle,
                sheets = bottomSheets,
                swipeActions = swipeActions
            )
        }
}
