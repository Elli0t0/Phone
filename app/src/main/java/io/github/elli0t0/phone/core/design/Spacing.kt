package io.github.elli0t0.phone.core.design

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class Spacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val screenHorizontalPadding: Dp = 20.dp,
    val screenTopPadding: Dp = 36.dp,
    val cardContentPadding: Dp = 20.dp,
    val bottomBarHeight: Dp = 68.dp
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
