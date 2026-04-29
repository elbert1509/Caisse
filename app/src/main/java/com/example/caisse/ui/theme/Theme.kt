package com.example.caisse.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary              = Brand700,
    onPrimary            = Color.White,
    primaryContainer     = Brand100,
    onPrimaryContainer   = Brand900,
    secondary            = Accent500,
    onSecondary          = Color.White,
    secondaryContainer   = AccentContainer,
    onSecondaryContainer = Color(0xFF3730A3),
    tertiary             = SemanticAmber,
    onTertiary           = Color.White,
    background           = Slate50,
    onBackground         = Slate900,
    surface              = Color.White,
    onSurface            = Slate900,
    surfaceVariant       = Slate100,
    onSurfaceVariant     = Slate700,
    error                = SemanticRed,
    onError              = Color.White,
    outline              = Slate300,
)

private val DarkColorScheme = darkColorScheme(
    primary              = Brand400,
    onPrimary            = Brand900,
    primaryContainer     = Brand800,
    onPrimaryContainer   = Brand300,
    secondary            = Accent400,
    onSecondary          = Color(0xFF1E1B4B),
    background           = Color(0xFF0A0F1E),
    onBackground         = Slate100,
    surface              = Color(0xFF111827),
    onSurface            = Slate100,
    surfaceVariant       = Color(0xFF1E293B),
    onSurfaceVariant     = Slate300,
    error                = SemanticRed,
    onError              = Color.White,
)

@Composable
fun CaisseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
