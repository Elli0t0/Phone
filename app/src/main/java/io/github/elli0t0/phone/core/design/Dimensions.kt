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
    // Space kept clear above the bar: largest bar height (68dp, the
    // Large size) + gap (12dp) + 16dp, plus the navigation-bar inset
    // added at the call site.
    val bottomBarReservedHeight = 96.dp
    // Bottom scrim: page color fades from 0% to this alpha over the
    // fade zone above the bar, then stays at this alpha under the bar.
    val bottomScrimMaxAlpha = 0.55f
    // Real glass blur (Haze): radius and tint alpha by intensity.
    val glassBlurRadiusSubtle = 20.dp
    val glassBlurRadiusStandard = 28.dp
    val glassBlurRadiusStrong = 36.dp
    val glassBlurTintAlphaDark = 0.22f
    val glassBlurTintAlphaLight = 0.30f
    // Container fill over a live blur: thin enough that the blurred
    // content shows through (the haze tint carries the dimming).
    val glassBlurContainerAlpha = 0.30f
    // Overflow-menu (glass) popup surface.
    val menuCornerRadius = 28.dp
    val menuElevation = 16.dp
    // Collapsing page header (Recents, Contacts): expanded header is
    // ~38% of the screen (min 260dp), title centered at ~40% of its
    // height, action row at the bottom 12dp above the first list item.
    val headerHeightFraction = 0.38f
    val headerHeightFractionCompact = 0.22f
    val headerHeightFractionStandard = 0.30f
    val headerMinHeight = 220.dp
    val headerTitleCenterFraction = 0.40f
    val headerTitleExpandedSize = 34.sp
    val headerActionsBottomPaddingExpanded = 12.dp
    val keypadDigitSize = 40.sp
    val keypadLetterSize = 11.sp
    val keypadTouchSize = 64.dp
    // Keypad no-match chips: vertical (icon on top, label below),
    // two lines max, sized to fit at font scale 1.3 on 360 dp.
    val noMatchChipHeight = 64.dp
    val noMatchChipIconSize = 20.dp
    val noMatchChipLabelSize = 12.sp
    val noMatchChipLabelLineHeight = 14.sp
    val keypadNumberDisplaySize = 40.sp
    val keypadNumberDisplayMinSize = 24.sp
    val keypadNumberDisplayShrinkFactor = 0.92f
    val callButtonSize = 70.dp
    // Incoming call screen: avatar, title, answer/decline buttons.
    val incomingHeaderTopPadding = 48.dp
    val incomingAvatarSize = 120.dp
    val incomingTitleSize = 40.sp
    val incomingTitleMinSize = 24.sp
    val incomingChipSpacing = 8.dp
    val incomingActionsBottomPadding = 48.dp
    val incomingActionButtonSize = 76.dp
    val incomingActionIconSize = 36.dp
    // Answer styles: classic label gap, slider, swipe buttons, liquid pill.
    val answerLabelGap = 12.dp
    val sliderTrackHeight = 72.dp
    val sliderKnobSize = 64.dp
    val sliderTrackPadding = 4.dp
    val sliderDeclineSize = 72.dp
    val swipeCommitDistance = 96.dp
    val liquidPillHeight = 76.dp
    val liquidPillMaxWidth = 300.dp
    val liquidKnobSize = 68.dp
    val liquidKnobEndPadding = 6.dp
    // Answer style settings page: preview tiles and the Try-it area.
    val answerStylePreviewHeight = 116.dp
    val answerStyleTryItHeight = 380.dp
    // Ongoing call screen: caller info, controls, end-call, DTMF keypad.
    val ongoingAvatarSize = 96.dp
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
    // In-call keypad: gap between the timer and the keypad block,
    // and the typed-digits display above the keys.
    val ongoingKeypadTopGap = 12.dp
    val ongoingTypedDigitsSize = 28.sp
    val ongoingTypedDigitsMinSize = 16.sp
    val ongoingTypedDigitsRowHeight = 48.dp
    val ongoingTypedDigitsClearWidth = 64.dp
    val inCallEqualizerBarWidth = 3.dp
    val inCallEqualizerBarMaxHeight = 14.dp
    val inCallEqualizerBarSpacing = 2.dp
    val inCallSwipeTintMaxAlpha = 0.35f
    val inCallSwipeShimmerAlpha = 0.18f
    val incomingChipHorizontalPadding = 12.dp
    val incomingChipVerticalPadding = 6.dp
    // Call Studio preview: the call screen is laid out at this reference
    // phone size, then scaled proportionally to fit the preview box.
    val studioPreviewReferenceWidth = 360.dp
    val studioPreviewReferenceHeight = 640.dp
    // Studio preview card: phone-shaped (9:19.5), 60% of the screen
    // tall, capped at this height, with a glass frame around it.
    val studioPreviewMaxHeight = 520.dp
    val studioPreviewCornerRadius = 28.dp
    val studioPreviewFramePadding = 4.dp
    // Floating Preview pill thumbnail (phone-shaped) and its corner radius.
    val studioPreviewThumbnailHeight = 70.dp
    val studioPreviewThumbnailCornerRadius = 12.dp
    // Bottom clearance so the floating Preview pill never covers the
    // last settings row, and the glass segmented control's height.
    val studioPreviewPillClearance = 110.dp
    val studioSegmentedControlHeight = 48.dp
    val studioSegmentedPillPadding = 4.dp
    // Full-screen glass icon button in the preview corner.
    val studioPreviewFullscreenIconSize = 20.dp
    // Studio settings group cards.
    val studioGroupCardCornerRadius = 24.dp
    val studioReorderButtonSize = 40.dp
    val studioReorderIconSize = 24.dp
    val studioReorderDisabledAlpha = 0.38f
    // Contacts: gap between the duplicates banner and the favorites card.
    val duplicatesCardFavoritesGap = 12.dp
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
    // Contacts top-bar pills.
    val contactsTopBarPillHorizontalPadding = 12.dp
    val contactsTopBarPillVerticalPadding = 8.dp
    val contactsTopBarPillGap = 8.dp
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
    // Contact details header card: avatar at the right, name size,
    // content top gap under the status bar, and the History card's
    // clearance above the floating bar (gap + bottom scrim height).
    val detailHeaderAvatarSize = 72.dp
    val detailHeaderNameSize = 28.sp
    val detailContentTopGap = 8.dp
    val detailHistoryGap = 24.dp
    val bottomScrimHeight = 56.dp
    // Contact edit: floating Cancel/Save pill and the form's bottom
    // padding beneath it (pill height + 24 dp).
    val editSavePillHeight = 60.dp
    val editSavePillInnerPadding = 6.dp
    val editSavePillButtonHeight = 48.dp
    val editSavePillWidthFraction = 0.75f
    val editFormBottomPadding = 84.dp
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
