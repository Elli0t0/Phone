package io.github.elli0t0.phone.core.design

import androidx.compose.ui.graphics.Color

// App color palette
val AccentBlue = Color(0xFF2473FE)
val AccentBlueDark = Color(0xFF3E89FF)
val OnAccentBlue = Color(0xFFFFFFFF)

// Bottom bar island: solid pill, follows the theme
val BottomBarDark = Color(0xFF2C2C2E)
val BottomBarDarkSelected = Color(0xFF48484A)
val BottomBarDarkSelectedContent = Color(0xFFFFFFFF)
val BottomBarDarkUnselected = Color(0xFFB0B0B5)
val BottomBarLight = Color(0xFFEBEBED)
val BottomBarLightSelected = Color(0xFFD9D9DD)
val BottomBarLightSelectedContent = Color(0xFF1C1C1E)
val BottomBarLightUnselected = Color(0xFF6E6E73)

// Favorite contact card gradients (reference-style colorful cards)
val FavoriteCardGradients = listOf(
    Color(0xFFFFB75E) to Color(0xFFFF8C42),
    Color(0xFFFF9EBB) to Color(0xFFFF6B9D),
    Color(0xFF8EC5FF) to Color(0xFF5B8DEF),
    Color(0xFF9BE7C4) to Color(0xFF4ECB9B),
    Color(0xFFD5A6FF) to Color(0xFF9B5DE5),
    Color(0xFFFFD166) to Color(0xFFFFB703)
)
val InCallGradientTop = Color(0xFF6E1B0C)
val InCallGradientBottom = Color(0xFF000000)
val CallDeclineRed = Color(0xFFE53935)
val CallAnswerGreen = Color(0xFF43A047)
// Keypad: page background (dark gray, not pure black) and call button green.
// Light keypad background is the same #F7F8FA as every other light screen.
val KeypadBackgroundDark = Color(0xFF161618)
val KeypadBackgroundLight = Color(0xFFF7F8FA)
val KeypadCallButtonGreenDark = Color(0xFF4AA564)
val KeypadCallButtonGreenLight = Color(0xFF3DAA5C)
val FavoriteHintOrange = Color(0xFFFFA726)
// Contact avatar fallback: soft two-tone pastel gradients,
// picked deterministically from the contact name.
val ContactAvatarGradients = listOf(
    Color(0xFFFFD6E0) to Color(0xFFFF9EBB),
    Color(0xFFC9E8FF) to Color(0xFF8EC5FF),
    Color(0xFFD3F5D3) to Color(0xFF9BE7C4),
    Color(0xFFFFE9C4) to Color(0xFFFFD166),
    Color(0xFFE3D3FF) to Color(0xFFD5A6FF),
    Color(0xFFFFD9B8) to Color(0xFFFFB75E),
    Color(0xFFC4F1F0) to Color(0xFF7FD8D4),
    Color(0xFFFFD1DC) to Color(0xFFFF8FAB)
)
val InCallControlBackground = Color(0x1FFFFFFF)
val InCallControlActive = Color(0xFFFFFFFF)
val InCallControlActiveContent = Color(0xFF000000)

// Light theme colors
val LightBackground = Color(0xFFF7F8FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFEFF1F4)
val LightSurfaceContainerLow = Color(0xFFF4F6F8)
val LightSurfaceContainer = Color(0xFFECEEF1)
val LightSurfaceContainerHigh = Color(0xFFE2E5EA)
val LightSurfaceContainerHighest = Color(0xFFD8DCE1)
val LightOnBackground = Color(0xFF191C1E)
val LightOnSurface = Color(0xFF191C1E)
val LightOnSurfaceVariant = Color(0xFF74777F)
val LightOutline = Color(0xFFC4C7C5)
val LightOutlineVariant = Color(0xFFE0E2EC)

// Dark theme colors
val DarkBackground = Color(0xFF000000)
val DarkSurface = Color(0xFF1C1C1E)
val DarkSurfaceVariant = Color(0xFF1E2024)
val DarkSurfaceContainerLow = Color(0xFF17181C)
val DarkSurfaceContainer = Color(0xFF1E2024)
val DarkSurfaceContainerHigh = Color(0xFF27292F)
val DarkSurfaceContainerHighest = Color(0xFF32343B)
val DarkOnBackground = Color(0xFFE2E2E6)
val DarkOnSurface = Color(0xFFE2E2E6)
val DarkOnSurfaceVariant = Color(0xFF8E9099)
val DarkOutline = Color(0xFF44474E)
val DarkOutlineVariant = Color(0xFF2C2E33)
