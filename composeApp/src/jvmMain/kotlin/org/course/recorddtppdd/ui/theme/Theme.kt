package org.course.recorddtppdd.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Светлая цветовая схема Material3 (адаптирована для Compose Desktop).
 * Убраны зависимости на android.os.Build, LocalContext и dynamic color.
 */
private val LightColorScheme = lightColorScheme(
    primary            = PrimaryBlue,
    onPrimary          = OnPrimary,
    primaryContainer   = PrimaryBlueLight,
    onPrimaryContainer = OnPrimary,
    secondary          = SecondaryGrey,
    onSecondary        = OnSecondary,
    secondaryContainer = SecondaryGreyLight,
    onSecondaryContainer = OnSecondary,
    tertiary           = AccentGold,
    onTertiary         = OnSurface,
    background         = SurfaceLight,
    onBackground       = OnSurface,
    surface            = CardLight,
    onSurface          = OnSurface,
    error              = AccentRed,
    onError            = OnError
)

/**
 * Тёмная цветовая схема Material3.
 */
private val DarkColorScheme = darkColorScheme(
    primary            = PrimaryBlueLight,
    onPrimary          = OnPrimary,
    primaryContainer   = PrimaryBlueDark,
    onPrimaryContainer = OnPrimary,
    secondary          = SecondaryGreyLight,
    onSecondary        = OnSecondary,
    secondaryContainer = SecondaryGreyDark,
    onSecondaryContainer = OnSecondary,
    tertiary           = AccentGold,
    onTertiary         = OnSurface,
    background         = SurfaceDark,
    onBackground       = OnSurfaceDark,
    surface            = CardDark,
    onSurface          = OnSurfaceDark,
    error              = AccentRed,
    onError            = OnError
)

@Composable
fun AppTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
