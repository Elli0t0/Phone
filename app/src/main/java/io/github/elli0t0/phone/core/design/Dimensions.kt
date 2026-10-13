package io.github.elli0t0.phone.core.design

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Bottom bar island and keypad dimensions. */
object PhoneDimens {
    // Bottom bar: floating pill, 3 items of 80dp, min height 60dp.
    val bottomBarItemWidth = 80.dp
    val bottomBarTotalWidth = bottomBarItemWidth * 3
    val bottomBarMinHeight = 60.dp
    val bottomBarBottomMargin = 8.dp
    val bottomBarCornerPercent = 50
    val bottomBarItemInset = 4.dp
    val bottomBarElevation = 4.dp
    val bottomBarIconSize = 24.dp
    val bottomBarLabelSize = 12.sp
    val bottomBarLabelIconGap = 2.dp
    val bottomBarItemPaddingVertical = 8.dp
    // Space kept clear above the bar: min bar height (60dp) + 16dp gap.
    val bottomBarReservedHeight = 76.dp
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
    // Space below the call-button row: 16dp + 76dp reserved − 68dp bar
    // (60dp bar + 8dp margin) = ~24dp clear space above the bottom bar.
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
}
