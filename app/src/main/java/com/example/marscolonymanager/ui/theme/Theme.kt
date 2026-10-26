package com.example.marscolonymanager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MarsDarkColorScheme = darkColorScheme(
    primary = MarsOrange, onPrimary = SpaceBackground,
    primaryContainer = SpaceSurfaceVariant, onPrimaryContainer = MarsOrange,
    secondary = OxygenColor, onSecondary = SpaceBackground,
    background = SpaceBackground, onBackground = TextPrimary,
    surface = SpaceSurface, onSurface = TextPrimary,
    surfaceVariant = SpaceSurfaceVariant, onSurfaceVariant = TextSecondary,
    outline = SpaceOutline, error = CriticalColor, onError = SpaceBackground
)

@Composable
fun MarsColonyManagerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MarsDarkColorScheme, typography = Typography, content = content)
}
