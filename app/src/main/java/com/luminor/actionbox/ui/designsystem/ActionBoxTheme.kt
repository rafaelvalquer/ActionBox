package com.luminor.actionbox.ui.designsystem

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = ActionBoxColors.ActionPurple,
    secondary = ActionBoxColors.List,
    tertiary = ActionBoxColors.Saved,
    background = ActionBoxColors.Background,
    surface = ActionBoxColors.Surface,
    surfaceVariant = Color(0xFFF0F1F7),
    primaryContainer = ActionBoxColors.PurpleSoft,
    secondaryContainer = Color(0xFFE1F5F2),
    onPrimary = Color.White,
    onBackground = ActionBoxColors.Text,
    onSurface = ActionBoxColors.Text,
    onSurfaceVariant = ActionBoxColors.Muted,
    error = ActionBoxColors.Danger
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFAAA5FF),
    secondary = Color(0xFF5CD4CA),
    tertiary = Color(0xFFD79AF2),
    background = ActionBoxColors.DarkBackground,
    surface = ActionBoxColors.DarkSurface,
    surfaceVariant = ActionBoxColors.DarkSurfaceHigh,
    primaryContainer = Color(0xFF343064),
    secondaryContainer = Color(0xFF123E3A),
    onPrimary = Color(0xFF1C1746),
    onBackground = ActionBoxColors.DarkText,
    onSurface = ActionBoxColors.DarkText,
    onSurfaceVariant = Color(0xFFB9BAC5),
    error = Color(0xFFFFB4AB)
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
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = ActionBoxTypography,
        shapes = ActionBoxShapes,
        content = content
    )
}
