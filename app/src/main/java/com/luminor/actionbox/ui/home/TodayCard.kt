package com.luminor.actionbox.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.domain.RecurrenceCalculator
import com.luminor.actionbox.ui.designsystem.ActionBoxColors
import com.luminor.actionbox.ui.designsystem.ActionBoxIcons
import com.luminor.actionbox.ui.designsystem.actionTypeColor
import com.luminor.actionbox.ui.motion.AnimatedCheck
import com.luminor.actionbox.ui.motion.SharedKeys
import com.luminor.actionbox.ui.motion.actionSharedBounds
import com.luminor.actionbox.ui.motion.pressScale
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun TodayActionRow(
    action: ActionEntity,
    date: LocalDate,
    completed: Boolean,
    hapticsEnabled: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val color = if (completed) ActionBoxColors.Completed else actionTypeColor(action.type)
    val time = action.scheduledAt?.let {
        val localTime = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime()
        if (localTime.hour == 0 && localTime.minute == 0) "—" else localTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    } ?: "—"

    Surface(
        modifier = Modifier
            .actionSharedBounds(SharedKeys.action(action.id))
            .pressScale()
            .clickable(onClick = onOpen)
            .animateContentSize(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(time, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 12.dp))
            Surface(shape = MaterialTheme.shapes.medium, color = color.copy(alpha = 0.11f)) {
                Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                    Icon(ActionBoxIcons.forType(action.type), contentDescription = null, tint = color, modifier = Modifier.size(21.dp))
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    action.title,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (completed) TextDecoration.LineThrough else null
                )
                val meta = buildList {
                    if (RecurrenceCalculator.recurrenceType(action).name != "NONE") add("Recorrente")
                    if (action.projectId != null) add("Projeto")
                }.joinToString(" · ")
                if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = {
                if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            }) { AnimatedCheck(checked = completed) }
        }
    }
}
