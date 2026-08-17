package com.orlune.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = OrluneAccent,
    onPrimary = OrluneBlack,
    primaryContainer = OrlunePrimaryContainer,
    onPrimaryContainer = OrluneOnPrimaryContainer,
    secondary = OrluneMutedText,
    background = OrluneBlack,
    onBackground = OrluneText,
    surface = OrluneSurface,
    onSurface = OrluneText,
    surfaceVariant = OrluneSurfaceVariant,
    onSurfaceVariant = OrluneMutedText,
    outline = OrluneOutline,
    outlineVariant = OrluneOutlineVariant,
    error = OrluneError,
    onError = OrluneOnError,
    errorContainer = OrluneErrorContainer,
    onErrorContainer = OrluneOnErrorContainer
)

private val LightColorScheme = lightColorScheme(
    primary = OrluneAccentDark,
    onPrimary = OrluneText,
    primaryContainer = OrluneLightPrimaryContainer,
    onPrimaryContainer = OrluneLightOnPrimaryContainer,
    secondary = OrluneLightMutedText,
    background = OrluneLightBackground,
    onBackground = OrluneLightText,
    surface = OrluneLightSurface,
    onSurface = OrluneLightText,
    surfaceVariant = OrluneLightSurfaceVariant,
    onSurfaceVariant = OrluneLightMutedText,
    outline = OrluneLightOutline,
    outlineVariant = OrluneLightOutlineVariant,
    error = OrluneLightError,
    onError = OrluneLightOnError,
    errorContainer = OrluneLightErrorContainer,
    onErrorContainer = OrluneLightOnErrorContainer
)

@Composable
fun OrluneTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = OrluneShapes,
        content = content
    )
}
