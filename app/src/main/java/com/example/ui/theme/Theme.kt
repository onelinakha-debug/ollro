package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val TerminalColorScheme = darkColorScheme(
    primary = TerminalCyan,
    onPrimary = TerminalBackground,
    primaryContainer = TerminalSurfaceElevated,
    onPrimaryContainer = TerminalCyan,
    secondary = TerminalBullishGreen,
    onSecondary = TerminalBackground,
    secondaryContainer = TerminalBullishGreenBg,
    onSecondaryContainer = TerminalBullishGreen,
    tertiary = TerminalWarningAmber,
    onTertiary = TerminalBackground,
    background = TerminalBackground,
    onBackground = TerminalTextPrimary,
    surface = TerminalSurface,
    onSurface = TerminalTextPrimary,
    surfaceVariant = TerminalSurfaceElevated,
    onSurfaceVariant = TerminalTextSecondary,
    outline = TerminalSurfaceBorder,
    error = TerminalBearishRed,
    onError = TerminalTextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Enforce dark trading terminal aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = TerminalColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = TerminalBackground.toArgb()
                window.navigationBarColor = TerminalBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
