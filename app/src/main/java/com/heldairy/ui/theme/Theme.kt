package com.heldairy.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = DayPrimary,
    onPrimary = Color.White,
    secondary = DaySecondary,
    onSecondary = Color.White,
    tertiary = DayTertiary,
    onTertiary = Color.White,
    background = DayBackground,
    surface = DaySurface,
    surfaceVariant = DaySurfaceVariant,
    onSurface = DayOnSurface,
    onSurfaceVariant = DayOnSurfaceVariant,
    outline = DayOutline,
    outlineVariant = DayOutline,
    inverseOnSurface = Color.White,
    inverseSurface = DayOnSurface,
    tertiaryContainer = DayTrack,
    primaryContainer = DaySurfaceVariant,
    secondaryContainer = DaySurfaceVariant
)

private val DarkColors = darkColorScheme(
    primary = NightPrimary,
    onPrimary = Color.White,
    secondary = NightSecondary,
    onSecondary = Color.White,
    tertiary = NightTertiary,
    onTertiary = Color.White,
    background = NightBackground,
    surface = NightSurface,
    surfaceVariant = NightSurfaceVariant,
    onSurface = NightOnSurface,
    onSurfaceVariant = NightOnSurfaceVariant,
    outline = NightOutline,
    outlineVariant = NightOutline,
    inverseOnSurface = NightSurface,
    inverseSurface = NightOnSurface,
    tertiaryContainer = NightTrack,
    primaryContainer = NightSurfaceVariant,
    secondaryContainer = NightSurfaceVariant
)

@Composable
fun HElDairyTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme: ColorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
