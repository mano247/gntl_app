package com.gentlemanstore.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Gold500,
    onPrimary = Navy900,
    primaryContainer = Navy800,
    onPrimaryContainer = Gold300,

    secondary = Cream100,
    onSecondary = Navy900,
    secondaryContainer = Navy700,
    onSecondaryContainer = Cream100,

    tertiary = Gold300,
    onTertiary = Navy900,

    background = Navy900,
    onBackground = Cream100,

    surface = Navy800,
    onSurface = Cream100,
    surfaceVariant = Navy700,
    onSurfaceVariant = Stone400,

    outline = Navy600,
    outlineVariant = Navy700,

    error = ErrorRed,
    onError = White
)

private val LightColorScheme = lightColorScheme(
    primary = Navy800,
    onPrimary = White,
    primaryContainer = Gold100,
    onPrimaryContainer = Navy800,

    secondary = Gold500,
    onSecondary = White,
    secondaryContainer = Gold100,
    onSecondaryContainer = Navy800,

    tertiary = Stone400,
    onTertiary = White,

    background = White,
    onBackground = Navy800,

    surface = White,
    onSurface = Navy800,
    surfaceVariant = Cream100,
    onSurfaceVariant = Stone600,

    outline = Cream200,
    outlineVariant = Cream100,

    error = ErrorRed,
    onError = White
)

@Composable
fun GentlemanStoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}