package io.github.elli0t0.phone.core.design

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.elli0t0.phone.data.settings.model.AppSettings
import io.github.elli0t0.phone.data.settings.model.CardStyle
import io.github.elli0t0.phone.data.settings.model.ThemeMode

val LocalAppSettings = staticCompositionLocalOf { AppSettings() }

private val DefaultDarkColorScheme = darkColorScheme(
    primary = AccentBlueDark,
    onPrimary = DarkBackground,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = DarkOnBackground,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)

private val DefaultLightColorScheme = lightColorScheme(
    primary = AccentBlue,
    onPrimary = LightSurface,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = LightOnBackground,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant
)

@Composable
fun PhoneTheme(
    appSettings: AppSettings = AppSettings(),
    darkTheme: Boolean = when (appSettings.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    },
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val baseColorScheme = when {
        appSettings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> {
            val accent = Color(appSettings.accentColor)
            DefaultDarkColorScheme.copy(
                // Dark contrast: accent text reads in a lighter tone
                // (a lightened accent, still a filled accent for
                // buttons). Secondary keeps the unlightened accent.
                primary = accent.lighten(0.4f),
                secondary = accent
            )
        }
        else -> {
            val accent = Color(appSettings.accentColor)
            DefaultLightColorScheme.copy(
                primary = accent,
                secondary = accent
            )
        }
    }

    val colorScheme = if (darkTheme && appSettings.pureBlackDarkMode) {
        baseColorScheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF121212)
        )
    } else {
        baseColorScheme
    }

    val currentDensity = LocalDensity.current
    val customDensity = Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale * appSettings.fontSizeScale
    )

    val shapes = when (appSettings.cardStyle) {
        CardStyle.SQUARE -> Shapes(
            extraSmall = RoundedCornerShape(2.dp),
            small = RoundedCornerShape(4.dp),
            medium = RoundedCornerShape(6.dp),
            large = RoundedCornerShape(8.dp),
            extraLarge = RoundedCornerShape(12.dp)
        )
        CardStyle.OUTLINED -> Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(20.dp),
            extraLarge = RoundedCornerShape(24.dp)
        )
        CardStyle.ROUNDED -> PhoneShapes
        CardStyle.INSET -> PhoneShapes.copy(extraLarge = RoundedCornerShape(14.dp))
    }

    val dynamicSpacing = Spacing(
        extraSmall = (appSettings.listDensity.verticalPaddingDp / 2).coerceAtLeast(2).dp,
        small = appSettings.listDensity.verticalPaddingDp.dp,
        medium = (appSettings.listDensity.verticalPaddingDp * 2).coerceAtLeast(12).dp
    )

    CompositionLocalProvider(
        LocalDensity provides customDensity,
        LocalSpacing provides dynamicSpacing,
        LocalAppSettings provides appSettings
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PhoneTypography,
            shapes = shapes,
            content = content
        )
    }
}
