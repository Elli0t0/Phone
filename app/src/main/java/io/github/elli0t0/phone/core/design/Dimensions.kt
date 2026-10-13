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
    val keypadDigitSize = 40.sp
    val keypadLetterSize = 11.sp
    val keypadTouchSize = 64.dp
    val keypadNumberDisplaySize = 36.sp
    val callButtonSize = 76.dp
}
