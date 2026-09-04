package com.luminor.actionbox.ui.theme

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

private val LightColors = lightColorScheme(
    primary = Brand,
    secondary = Brand,
    background = LightBackground,
    surface = LightSurface,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onBackground = LightText,
    onSurface = LightText,
    onSurfaceVariant = LightSubtle
)

private val DarkColors = darkColorScheme(
    primary = BrandDark,
    secondary = BrandDark,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceHigh,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onBackground = DarkText,
    onSurface = DarkText
)

@Composable
fun ActionBoxTheme(themeMode: String = "SYSTEM", content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }
    val colors = if (dark) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(colorScheme = colors, typography = ActionBoxTypography, content = content)
}
