package io.github.elli0t0.phone.core.design

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    val bottomBarElevation = 2.dp
    val bottomBarShadowAlpha = 0.08f
    val bottomBarIconSize = 24.dp
    val bottomBarLabelSize = 12.sp
    val bottomBarLabelIconGap = 2.dp
    // Space kept clear above the bar: bar height (60dp) + gap (12dp)
    // + 16dp, plus the navigation-bar inset added at the call site.
    val bottomBarReservedHeight = 88.dp
    // Collapsing page header (Recents, Contacts): tall expanded area
    // that shrinks to a small top bar as the list scrolls.
    val headerExpandedFraction = 0.3f
    val headerCollapsedHeight = 56.dp
    val headerTitleExpandedSize = 34.sp
    val headerTitleCollapsedSize = 20.sp
    val headerActionsBottomPaddingExpanded = 12.dp
    val headerActionsBottomPaddingCollapsed = 4.dp
    val keypadDigitSize = 40.sp
    val keypadLetterSize = 11.sp
    val keypadTouchSize = 64.dp
    val keypadNumberDisplaySize = 36.sp
    val callButtonSize = 70.dp
    // Space below the call-button row: 16dp + 88dp reserved − 72dp bar
    // (60dp bar + 12dp gap) = ~32dp clear space above the bottom bar.
    val keypadCallButtonBottomPadding = 16.dp
    val keypadAsteriskSize = 28.dp
    val keypadVoicemailIconSize = 20.dp
    // Recents: one rounded card per day, swipe reveal, row metrics.
    val recentsDayCardCornerRadius = 28.dp
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
    val contactsPillCornerPercent = 50
    // Settings: cards, slider, switches.
    val settingsSwitchWidth = 52.dp
    val settingsSwitchHeight = 28.dp
    val settingsSwitchThumbSize = 24.dp
    val settingsSwitchThumbPadding = 2.dp
    val settingsSwitchCornerPercent = 50
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
