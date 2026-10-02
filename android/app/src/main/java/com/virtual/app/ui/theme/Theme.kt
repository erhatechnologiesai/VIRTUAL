package com.virtual.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = VirtualBluePrimary,
    onPrimary = VirtualWhite,
    primaryContainer = VirtualBlueSubtle,
    onPrimaryContainer = VirtualBluePrimary,
    secondary = VirtualBlueVibrant,
    onSecondary = VirtualWhite,
    background = VirtualBackground,
    onBackground = VirtualTextPrimary,
    surface = VirtualCardSurface,
    onSurface = VirtualTextPrimary,
    outline = VirtualBorder,
    error = VirtualOfflineRed,
    onError = VirtualWhite
)

@Composable
fun VirtualTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme // Keep clean Blue + White theme prominent

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = VirtualWhite.toArgb()
            window.navigationBarColor = VirtualWhite.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VirtualTypography,
        content = content
    )
}
