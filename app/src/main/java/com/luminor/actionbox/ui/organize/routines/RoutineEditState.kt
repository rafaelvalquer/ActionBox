package com.luminor.actionbox.ui.organize.routines

import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.domain.ActionPriority
import com.luminor.actionbox.domain.RecurrenceCalculator
import com.luminor.actionbox.domain.RecurrenceType
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

data class RoutineEditState(
    val title: String,
    val recurrenceType: RecurrenceType,
    val recurrenceDays: Set<Int>,
    val time: LocalTime,
    val reminderMinutes: Int?,
    val priority: ActionPriority,
    val paused: Boolean
) {
    companion object {
        fun from(action: ActionEntity): RoutineEditState {
            val time = action.scheduledAt
                ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime() }
                ?: LocalTime.of(9, 0)
            return RoutineEditState(
                title = action.title,
                recurrenceType = RecurrenceCalculator.recurrenceType(action).takeIf { it != RecurrenceType.NONE } ?: RecurrenceType.WEEKLY,
                recurrenceDays = RecurrenceCalculator.recurrenceDays(action),
                time = time,
                reminderMinutes = action.reminderMinutes,
                priority = runCatching { ActionPriority.valueOf(action.priority ?: ActionPriority.NORMAL.name) }.getOrDefault(ActionPriority.NORMAL),
                paused = action.status == "CANCELLED"
            )
        }
    }
}
