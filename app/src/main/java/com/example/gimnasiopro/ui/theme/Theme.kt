package com.example.gimnasiopro.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val AppColorScheme = darkColorScheme(
    primary          = AccentPurple,
    primaryContainer = CardDark,
    secondary        = AccentGreen,
    secondaryContainer = CardDarkSecondary,
    tertiary         = AccentBlue,
    background       = DarkBackground,
    surface          = DarkBackgroundSecondary,
    surfaceVariant   = CardDark,
    onPrimary        = TextPrimary,
    onPrimaryContainer = TextPrimary,
    onSecondary      = TextOnButton,
    onSecondaryContainer = TextPrimary,
    onTertiary       = TextPrimary,
    onBackground     = TextPrimary,
    onSurface        = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline          = DividerColor,
    error            = RedDelete
)

// Alias for backwards compat inside this file
private val DarkColorScheme  = AppColorScheme
private val LightColorScheme = AppColorScheme

@Composable
fun GimnasioProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled to use our custom colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}