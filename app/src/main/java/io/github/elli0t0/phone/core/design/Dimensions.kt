package io.github.elli0t0.phone.core.design

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.elli0t0.phone.data.settings.model.BottomBarSize

/** Bottom bar island and keypad dimensions. */
object PhoneDimens {
    // Bottom bar: floating pill, 60% of screen width, 60dp tall,
    // 3 equal-width items. The selected pill fills its item with a
    // 4dp inset. Very soft shadow, alpha <= 10%.
    val bottomBarWidthFraction = 0.6f
    // Contact details bottom action bar: same pill, 70% of screen width.
    val detailBottomBarWidthFraction = 0.7f
    val bottomBarHeight = 60.dp
    val bottomBarBottomMargin = 12.dp
    val bottomBarCornerPercent = 50
    val bottomBarItemInset = 4.dp
    // Extra width the shared selection pill gains mid-flight.
    val bottomBarPillStretch = 14.dp
    // Height lost when the bar compacts on scroll (icons only).
    val bottomBarShrinkDelta = 12.dp
    val bottomBarElevation = 2.dp
    val bottomBarShadowAlpha = 0.08f
    val bottomBarIconSize = 24.dp
    val bottomBarLabelSize = 12.sp
    val bottomBarLabelIconGap = 2.dp
    // Space kept clear above the bar: bar height (60dp) + gap (12dp)
    // + 16dp, plus the navigation-bar inset added at the call site.
    val bottomBarReservedHeight = 88.dp
    // Collapsing page header (Recents, Contacts): expanded header is
    // ~38% of the screen (min 260dp), title centered at ~42% of its
    // height, action row at the bottom 12dp above the first list item.
    val headerHeightFraction = 0.38f
    val headerMinHeight = 260.dp
    val headerTitleCenterFraction = 0.42f
    val headerTitleExpandedSize = 34.sp
    val headerActionsBottomPaddingExpanded = 12.dp
    val keypadDigitSize = 40.sp
    val keypadLetterSize = 11.sp
    val keypadTouchSize = 64.dp
    val keypadNumberDisplaySize = 40.sp
    val keypadNumberDisplayMinSize = 20.sp
    val keypadNumberDisplayShrinkFactor = 0.92f
    val callButtonSize = 70.dp
    // Incoming call screen: avatar, title, answer/decline buttons.
    val incomingHeaderTopPadding = 48.dp
    val incomingAvatarSize = 120.dp
    val incomingTitleSize = 40.sp
    val incomingChipSpacing = 8.dp
    val incomingActionsBottomPadding = 48.dp
    val incomingActionButtonSize = 76.dp
    val incomingActionIconSize = 36.dp
    // Ongoing call screen: caller info, controls, end-call, DTMF keypad.
    val ongoingHeaderTopPadding = 40.dp
    val ongoingTitleSize = 36.sp
    val ongoingNumberSize = 17.sp
    val ongoingStateSize = 15.sp
    val ongoingControlGridSpacing = 24.dp
    val ongoingEndCallBottomPadding = 16.dp
    val ongoingEndCallButtonSize = 72.dp
    val ongoingEndCallIconSize = 32.dp
    val ongoingControlLabelGap = 8.dp
    val ongoingControlButtonSize = 64.dp
    val ongoingControlIconSize = 28.dp
    val ongoingControlLabelSize = 12.sp
    val ongoingKeypadRowSpacing = 16.dp
    val ongoingKeypadColumnSpacing = 32.dp
    val ongoingDtmfKeySize = 64.dp
    val ongoingDtmfDigitSize = 26.sp
    val ongoingDtmfLettersSize = 10.sp
    val ongoingKeypadBottomRowGap = 8.dp
    // Dual-SIM picker dialog rows.
    val simPickerRowMinHeight = 64.dp
    val simPickerRowCornerRadius = 12.dp
    val simPickerIconSize = 24.dp
    // Space below the call-button row: 16dp + 88dp reserved − 72dp bar
    // (60dp bar + 12dp gap) = ~32dp clear space above the bottom bar.
    val keypadCallButtonBottomPadding = 16.dp
    val keypadAsteriskSize = 28.dp
    // Recents: one rounded card per day, swipe reveal, row metrics.
    val recentsDayCardCornerRadius = 28.dp
    val recentsFilterPopupCornerRadius = 22.dp
    val recentsRowHeight = 64.dp
    val recentsDividerAlpha = 0.08f
    val recentsRowIconSize = 20.dp
    val recentsRowVerticalPadding = 3.dp
    val recentsSwipeRevealIconSize = 24.dp
    // Contacts list: name-only rows, gradient avatars, pill cards.
    val contactsRowAvatarSize = 40.dp
    val contactsRowVerticalPadding = 8.dp
    val contactsDuplicatesCardWidthFraction = 0.7f
    val contactsDuplicatesDismissSize = 24.dp
    val contactsDuplicatesDismissIconSize = 14.dp
    val contactsFavoritesHintIconSize = 22.dp
    val contactsGroupIconSize = 24.dp
    val contactsFavoriteAvatarSize = 56.dp
    val contactsFavoriteCardPadding = 12.dp
    val contactsFavoriteAvatarNameGap = 8.dp
    val contactsPillCornerPercent = 50
    // Contacts top-bar pills and fast scroller.
    val contactsTopBarPillHorizontalPadding = 12.dp
    val contactsTopBarPillVerticalPadding = 8.dp
    val contactsTopBarPillGap = 8.dp
    val contactsScrubberWidth = 28.dp
    val contactsScrubberTopPadding = 72.dp
    val contactsScrubberEndPadding = 4.dp
    val contactsScrubberGap = 8.dp
    val contactsScrubberLetterSize = 11.sp
    val contactsScrubberPreviewSize = 72.dp
    val contactsScrubberPreviewLetterSize = 32.sp
    // Settings: cards, slider, switches.
    val settingsSwitchWidth = 52.dp
    val settingsSwitchHeight = 28.dp
    val settingsSwitchThumbSize = 24.dp
    val settingsSwitchThumbPadding = 2.dp
    val settingsSwitchCornerPercent = 50
    // Settings rows, dialogs, accent picker.
    val settingsSectionHeaderVerticalPadding = 4.dp
    val settingsRowVerticalPadding = 12.dp
    val settingsSwitchRowVerticalPadding = 4.dp
    val settingsSwitchRowTextEndPadding = 8.dp
    val settingsChoiceOptionGap = 8.dp
    val settingsErrorTextTopPadding = 4.dp
    val settingsAccentSwatchSpacing = 10.dp
    val settingsAccentSwatchSize = 38.dp
    val settingsAccentSwatchBorderSelected = 3.dp
    val settingsAccentSwatchBorderUnselected = 1.dp
    val settingsAccentSwatchCheckSize = 20.dp
    val settingsCustomColorButtonCornerRadius = 19.dp
    val settingsSimRowCornerRadius = 12.dp
    val settingsSimRowIconSize = 24.dp
    val settingsSliderTrackHeight = 4.dp
    val settingsSliderThumbWidth = 4.dp
    val settingsSliderThumbHeight = 24.dp
    val settingsSliderTickSize = 4.dp
    val settingsSliderCornerRadius = 2.dp
    // Common dialog shape: rounded card, regardless of the card-style
    // setting, for dialogs on the detail / edit / duplicates screens.
    val dialogCornerRadius = 28.dp
    // Contact detail / edit: large avatar with camera badge.
    val detailAvatarSize = 112.dp
    val detailPhotoBadgeSize = 36.dp
    val detailPhotoBadgeIconSize = 18.dp
    val detailPhotoBadgeElevation = 2.dp
    val detailTopBarHeight = 56.dp
    // Contact detail: action row (call / message / favorite / more).
    val detailActionRowHorizontalPadding = 16.dp
    val detailActionRowVerticalPadding = 8.dp
    val detailActionButtonPadding = 8.dp
    val detailActionButtonSize = 52.dp
    val detailActionIconSize = 24.dp
    // Contact detail: row icons and trailing action buttons.
    val detailRowIconSize = 20.dp
    val detailRowActionButtonSize = 36.dp
    // Contact detail dialogs: photo preview sizes.
    val detailPhotoCropPreviewSize = 160.dp
    val detailPhotoFullPreviewSize = 260.dp
    val detailPhotoFullPreviewCornerRadius = 16.dp
    // Contact edit: input rows and bottom spacing.
    val editFieldGap = 8.dp
    val editTypeDropdownWidth = 135.dp
    val editLoadingSize = 24.dp
    val editBottomSpacer = 40.dp
    // Permissions setup: checklist spacing and item icons.
    val setupItemSpacing = 12.dp
    val setupItemIconSize = 24.dp
    val setupCardTextPaddingStart = 12.dp
    val setupCardTextPaddingEnd = 8.dp
}

/**
 * Width, height, icon and label sizes for one bottom-bar size setting.
 * Medium reuses the default bottom-bar tokens above.
 */
data class BottomBarSizeSpec(
    val widthFraction: Float,
    val height: Dp,
    val iconSize: Dp,
    val labelSize: TextUnit
)

fun bottomBarSizeSpec(size: BottomBarSize): BottomBarSizeSpec = when (size) {
    BottomBarSize.SMALL -> BottomBarSizeSpec(0.5f, 52.dp, 20.dp, 11.sp)
    BottomBarSize.MEDIUM -> BottomBarSizeSpec(
        PhoneDimens.bottomBarWidthFraction,
        PhoneDimens.bottomBarHeight,
        PhoneDimens.bottomBarIconSize,
        PhoneDimens.bottomBarLabelSize
    )
    BottomBarSize.LARGE -> BottomBarSizeSpec(0.7f, 68.dp, 28.dp, 13.sp)
}
