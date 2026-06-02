package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = PolishPrimary,
    primaryContainer = PolishPrimaryContainer,
    onPrimaryContainer = PolishOnPrimaryContainer,
    secondaryContainer = PolishSecondaryContainer,
    onSecondaryContainer = PolishOnSecondaryContainer,
    background = PolishBackground,
    onBackground = PolishOnBackground,
    surface = PolishSurface,
    onSurface = PolishOnSurface,
    onSurfaceVariant = PolishOnSurfaceVariant,
    outlineVariant = PolishOutlineVariant,
    errorContainer = PolishAlertBackground,
    error = PolishAlertAction
)

private val DarkColorScheme = darkColorScheme(
    primary = PolishPrimary,
    primaryContainer = PolishPrimaryContainer,
    onPrimaryContainer = PolishOnPrimaryContainer,
    secondaryContainer = PolishSecondaryContainer,
    onSecondaryContainer = PolishOnSecondaryContainer,
    background = PolishBackground,
    onBackground = PolishOnBackground,
    surface = PolishSurface,
    onSurface = PolishOnSurface,
    onSurfaceVariant = PolishOnSurfaceVariant,
    outlineVariant = PolishOutlineVariant,
    errorContainer = PolishAlertBackground,
    error = PolishAlertAction
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Set default dynamic color to false so our brand-driven "Professional Polish" colours shine
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
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
