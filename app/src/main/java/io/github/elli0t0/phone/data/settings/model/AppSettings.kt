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
    OUTLINED("Outlined")
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
    CUSTOM("Custom")
}

enum class AnswerGesture(val label: String) {
    TAP("Tap buttons"),
    SWIPE("Swipe")
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
    HOLD("Hold")
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

enum class BottomBarSize(val label: String) {
    SMALL("Small"),
    MEDIUM("Medium"),
    LARGE("Large")
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
    val animationOption: AnimationOption = AnimationOption.ON,
    val showBottomBarLabels: Boolean = true,
    val bottomBarStyle: BottomBarStyle = BottomBarStyle.GLASS,
    val bottomBarSize: BottomBarSize = BottomBarSize.MEDIUM,
    val shrinkBottomBarOnScroll: Boolean = false,
    // Liquid glass per section: each area with glass surfaces can fall
    // back to its flat rendering. All default to glass on.
    val glassBottomBar: Boolean = true,
    val glassDialpad: Boolean = true,
    val glassHeaders: Boolean = true,
    val glassContactDetail: Boolean = true,
    val glassCallScreens: Boolean = true,
    val defaultStartTab: StartTab = StartTab.KEYPAD,
    val contactSortOrder: ContactSortOrder = ContactSortOrder.FIRST_NAME,
    val contactNameFormat: ContactNameFormat = ContactNameFormat.FIRST_NAME_FIRST,
    val callDisplay: CallDisplay = CallDisplay.FULL_SCREEN,
    val callBackground: CallBackground = CallBackground.DEFAULT,
    val callBackgroundDim: Float = 0f,
    val callBackgroundCustomColor: Long = AccentPreset.ONE_UI_BLUE.colorValue,
    val answerGesture: AnswerGesture = AnswerGesture.TAP,
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
    val incomingAvatarSize: IncomingAvatarSize = IncomingAvatarSize.MEDIUM,
    val incomingAvatarShape: IncomingAvatarShape = IncomingAvatarShape.CIRCLE,
    val ongoingCallButtons: List<OngoingCallButton> = OngoingCallButton.entries,
    val showNumberOnCallScreen: Boolean = true,
    val defaultSim: DefaultSim = DefaultSim.ASK,
    // Quick-decline message overrides, one per reply chip. A blank entry
    // falls back to the built-in default message for that slot, so an
    // untouched setting keeps the localized defaults.
    val quickDeclineOverrides: List<String> = emptyList()
)
