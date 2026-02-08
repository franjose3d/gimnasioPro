package com.example.gimnasiopro.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ElectricGreen,
    secondary = ElectricGreenDark,
    tertiary = ElectricGreen,
    background = DarkGreenBackground,
    surface = DarkGreenBackground,
    onPrimary = TextOnButton,
    onSecondary = TextOnButton,
    onTertiary = TextOnButton,
    onBackground = TextOnBackground,
    onSurface = TextOnBackground
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricGreen,
    secondary = ElectricGreenDark,
    tertiary = ElectricGreen,
    background = DarkGreenBackground,
    surface = DarkGreenBackground,
    onPrimary = TextOnButton,
    onSecondary = TextOnButton,
    onTertiary = TextOnButton,
    onBackground = TextOnBackground,
    onSurface = TextOnBackground
)

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