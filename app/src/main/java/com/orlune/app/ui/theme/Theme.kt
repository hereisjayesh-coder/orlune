package com.orlune.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ColorOutlineDark = Color(0xFF3A3935)

private val DarkColorScheme = darkColorScheme(
    primary = OrluneAccent,
    onPrimary = OrluneBlack,
    secondary = OrluneMutedText,
    background = OrluneBlack,
    onBackground = OrluneText,
    surface = OrluneSurface,
    onSurface = OrluneText,
    surfaceVariant = OrluneSurfaceVariant,
    onSurfaceVariant = OrluneMutedText,
    outline = ColorOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = OrluneAccentDark,
    onPrimary = OrluneText,
    secondary = OrluneLightMutedText,
    background = OrluneLightBackground,
    onBackground = OrluneLightText,
    surface = OrluneLightSurface,
    onSurface = OrluneLightText,
    surfaceVariant = Color(0xFFEAE8E1),
    onSurfaceVariant = OrluneLightMutedText
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
        content = content
    )
}
