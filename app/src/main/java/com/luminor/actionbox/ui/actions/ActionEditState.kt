package com.luminor.actionbox.ui.actions

import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.domain.ActionPriority
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.RecurrenceCalculator
import com.luminor.actionbox.domain.RecurrenceType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Draft used by the editor. The database is only changed when the user explicitly saves. */
data class ActionEditState(
    val id: Long,
    val type: ActionType,
    val title: String,
    val description: String?,
    val date: LocalDate?,
    val time: LocalTime?,
    val priority: ActionPriority,
    val recurrenceType: RecurrenceType,
    val recurrenceDays: Set<Int>,
    val reminderMinutes: Int?
) {
    fun toEntity(original: ActionEntity): ActionEntity {
        val zone = ZoneId.systemDefault()
        val newScheduledAt = date?.let { selectedDate ->
            val localDateTime = time?.let(selectedDate::atTime) ?: selectedDate.atStartOfDay()
            localDateTime.atZone(zone).toInstant().toEpochMilli()
        }
        val originalDuration = if (original.scheduledAt != null && original.endAt != null && original.endAt >= original.scheduledAt) {
            original.endAt - original.scheduledAt
        } else null
        val newEndAt = if (newScheduledAt != null && originalDuration != null) newScheduledAt + originalDuration else null
        val trimmedTitle = title.trim()
        val updatedContent = if (original.content.isBlank() || original.content == original.title) trimmedTitle else original.content

        return original.copy(
            type = type.name,
            title = trimmedTitle,
            content = updatedContent,
            description = description?.trim()?.ifBlank { null },
            scheduledAt = newScheduledAt,
            endAt = newEndAt,
            priority = priority.name,
            recurrenceType = recurrenceType.name,
            recurrenceDays = if (recurrenceType == RecurrenceType.WEEKLY) recurrenceDays.sorted().joinToString(",").ifBlank { null } else null,
            reminderMinutes = reminderMinutes
        )
    }

    companion object {
        fun from(action: ActionEntity): ActionEditState {
            val scheduled = action.scheduledAt?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime()
            }
            val parsedType = runCatching { ActionType.valueOf(action.type) }.getOrDefault(ActionType.TASK)
            val parsedPriority = runCatching { ActionPriority.valueOf(action.priority ?: ActionPriority.NORMAL.name) }
                .getOrDefault(ActionPriority.NORMAL)
            return ActionEditState(
                id = action.id,
                type = parsedType,
                title = action.title,
                description = action.description,
                date = scheduled?.toLocalDate(),
                time = scheduled?.toLocalTime()?.takeUnless { it == LocalTime.MIDNIGHT },
                priority = parsedPriority,
                recurrenceType = RecurrenceCalculator.recurrenceType(action),
                recurrenceDays = RecurrenceCalculator.recurrenceDays(action),
                reminderMinutes = action.reminderMinutes
            )
        }
    }
}
