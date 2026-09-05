package com.luminor.actionbox.ui.organize.notes

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

data class NotePaletteEntry(val key: String, val label: String, val light: Color, val dark: Color, val accent: Color)

val NotePalette = listOf(
    NotePaletteEntry("YELLOW", "Amarelo", Color(0xFFFFF1A8), Color(0xFF4A4120), Color(0xFFD0A800)),
    NotePaletteEntry("BLUE", "Azul", Color(0xFFD9E9FF), Color(0xFF1E334D), Color(0xFF4A8FE7)),
    NotePaletteEntry("ORANGE", "Laranja", Color(0xFFFFDCC2), Color(0xFF4B3020), Color(0xFFE48745)),
    NotePaletteEntry("GREEN", "Verde", Color(0xFFDDF3DF), Color(0xFF213D27), Color(0xFF4D9E62)),
    NotePaletteEntry("PURPLE", "Roxo", Color(0xFFEAE5FF), Color(0xFF322B4A), Color(0xFF7967D9)),
    NotePaletteEntry("PINK", "Rosa", Color(0xFFFFE1EC), Color(0xFF4A2835), Color(0xFFD46891)),
    NotePaletteEntry("GRAY", "Cinza", Color(0xFFF1F2F6), Color(0xFF2C2D35), Color(0xFF7B7E8A))
)

@Composable
fun noteBackground(key: String?): Color {
    val entry = NotePalette.firstOrNull { it.key == key } ?: NotePalette.first()
    return if (isSystemInDarkTheme()) entry.dark else entry.light
}

fun noteAccent(key: String?): Color = NotePalette.firstOrNull { it.key == key }?.accent ?: NotePalette.first().accent
