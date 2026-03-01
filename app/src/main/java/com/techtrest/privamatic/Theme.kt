package com.techtrest.privamatic

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BritishRacingGreen,
    secondary = Cream,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = Cream,
    onSecondary = BritishRacingGreen,
    onBackground = BritishRacingGreen,
    onSurface = BritishRacingGreen
)

private val DarkColorScheme = darkColorScheme(
    primary = BritishRacingGreenDark,
    secondary = CreamDark,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = CreamDark,
    onSecondary = BritishRacingGreenDark,
    onBackground = Cream,
    onSurface = Cream
)

@Composable
fun PrivacyWidgetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Make status bar transparent for edge-to-edge display
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            // Always use light status bar icons (cream/white) for visibility on green top bar
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}