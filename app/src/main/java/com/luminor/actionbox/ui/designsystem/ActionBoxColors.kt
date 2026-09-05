package com.luminor.actionbox.ui.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.luminor.actionbox.domain.ActionType

object ActionBoxColors {
    val ActionPurple = Color(0xFF6157F5)
    val PurpleSoft = Color(0xFFEAE8FF)
    val Background = Color(0xFFF7F7FB)
    val Surface = Color(0xFFFFFFFF)
    val Text = Color(0xFF171822)
    val Muted = Color(0xFF727583)

    val DarkBackground = Color(0xFF0D0E13)
    val DarkSurface = Color(0xFF171820)
    val DarkSurfaceHigh = Color(0xFF22232D)
    val DarkText = Color(0xFFF6F6FA)

    val Task = ActionPurple
    val Reminder = Color(0xFFF2A93B)
    val Event = Color(0xFF3D7BF2)
    val Note = Color(0xFFE7B93E)
    val List = Color(0xFF00A99D)
    val Project = Color(0xFF4F46C8)
    val Saved = Color(0xFFB04BE0)
    val Completed = Color(0xFF2FA36B)
    val Danger = Color(0xFFE0565B)
}

@Composable
fun actionTypeColor(type: String): Color = when (type) {
    ActionType.REMINDER.name -> ActionBoxColors.Reminder
    ActionType.EVENT.name -> ActionBoxColors.Event
    ActionType.NOTE.name -> ActionBoxColors.Note
    ActionType.LIST.name -> ActionBoxColors.List
    ActionType.PROJECT.name -> ActionBoxColors.Project
    ActionType.READ_LATER.name -> ActionBoxColors.Saved
    else -> MaterialTheme.colorScheme.primary
}
