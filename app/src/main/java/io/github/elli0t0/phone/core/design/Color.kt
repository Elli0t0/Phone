package io.github.elli0t0.phone.core.design

import androidx.compose.ui.graphics.Color

// App color palette

/** Mixes this color toward white by [fraction] (0 = unchanged, 1 = white). */
fun Color.lighten(fraction: Float): Color {
    return Color(
        red = red + (1f - red) * fraction,
        green = green + (1f - green) * fraction,
        blue = blue + (1f - blue) * fraction,
        alpha = alpha
    )
}

val AccentBlue = Color(0xFF2473FE)
val AccentBlueDark = Color(0xFF3E89FF)
val OnAccentBlue = Color(0xFFFFFFFF)

// Switch styles: the green style's track colors (thumb stays white,
// no borders in either style).
val SwitchGreenTrack = Color(0xFF34C759)
val SwitchGreenUncheckedTrack = Color(0xFF39393D)

// Bottom bar island: solid pill, follows the theme
val BottomBarDark = Color(0xFF2C2C2E)
val BottomBarDarkSelected = Color(0xFF48484A)
val BottomBarDarkSelectedContent = Color(0xFFFFFFFF)
val BottomBarDarkUnselected = Color.White.copy(alpha = 0.80f)
val BottomBarLight = Color(0xFFEBEBED)
val BottomBarLightSelected = Color(0xFFD9D9DD)
val BottomBarLightSelectedContent = Color(0xFF1C1C1E)
val BottomBarLightUnselected = Color.Black.copy(alpha = 0.60f)
// Selected droplet: neutral translucent fill, no primary tint.
val BottomBarDarkDroplet = Color.White.copy(alpha = 0.16f)
val BottomBarLightDroplet = Color.Black.copy(alpha = 0.08f)

// Favorite contact card gradients: calm muted pastels,
// same family as the avatar palette, white name text on top.
val FavoriteCardGradients = listOf(
    Color(0xFFFFD6D6) to Color(0xFFFFA8A8),
    Color(0xFFFFE3C4) to Color(0xFFFFBE8A),
    Color(0xFFC9E8FF) to Color(0xFF8EC5FF),
    Color(0xFFD3F5D3) to Color(0xFF9BE7C4),
    Color(0xFFE3D3FF) to Color(0xFFC4A6F5),
    Color(0xFFFFEFC4) to Color(0xFFFFD166)
)
val InCallGradientTop = Color(0xFF6E1B0C)
val InCallGradientBottom = Color(0xFF000000)
val CallDeclineRed = Color(0xFFE53935)
val CallAnswerGreen = Color(0xFF43A047)
// Recents swipe actions: call back green, delete red.
val SwipeCallGreen = Color(0xFF34C759)
val SwipeDeleteRed = Color(0xFFFF3B30)
// Real-blur tint overlay: dark glass over #1C1C1E, light glass over
// white (alphas live in PhoneDimens, scaled by GlassIntensity).
val GlassBlurTintDark = Color(0xFF1C1C1E)
val GlassBlurTintLight = Color.White
val CallRejectedOrange = Color(0xFFFB8C00)
// Keypad call button green.
val KeypadCallButtonGreenDark = Color(0xFF34A853)
val KeypadCallButtonGreenLight = Color(0xFF34A853)
val KeypadKeyLightRim = Color.Black.copy(alpha = 0.12f)
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
val InCallControlBackgroundFlat = Color(0xFF2C2C2E)
val InCallSwipeTrackBackground = Color(0x26FFFFFF)
val InCallControlActive = Color(0xFFFFFFFF)
val InCallControlActiveContent = Color(0xFF000000)
// Aurora background: dark base gradient + drifting accent/teal/violet blobs.
val AuroraBaseTop = Color(0xFF0B1026)
val AuroraBaseBottom = Color(0xFF1E1B4B)
val AuroraTeal = Color(0xFF2DD4BF)
val AuroraViolet = Color(0xFF8B5CF6)
// Incoming avatar pulse rings: soft white.
val InCallAvatarRing = Color(0x66FFFFFF)
// Ongoing-call equalizer bars: mostly-opaque white.
val InCallEqualizerBar = Color(0xCCFFFFFF)

// Light theme colors
val LightBackground = Color(0xFFF6F6F8)
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
