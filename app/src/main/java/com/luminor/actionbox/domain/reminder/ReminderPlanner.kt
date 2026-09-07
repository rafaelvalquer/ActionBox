package com.luminor.actionbox.domain.reminder

import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.RecurrenceCalculator
import com.luminor.actionbox.domain.RecurrenceType
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderPlanner {
    fun plan(
        action: ActionEntity,
        now: LocalDateTime = LocalDateTime.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): ReminderPlan? {
        if (action.deletedAt != null ||
            action.status == ActionStatus.CANCELLED.name ||
            action.status == ActionStatus.ARCHIVED.name
        ) return null

        val shouldNotify = action.type == ActionType.REMINDER.name || action.reminderMinutes != null
        if (!shouldNotify || action.scheduledAt == null) return null

        val base = if (RecurrenceCalculator.recurrenceType(action) == RecurrenceType.NONE) {
            Instant.ofEpochMilli(action.scheduledAt)
                .atZone(zoneId)
                .toLocalDateTime()
        } else {
            RecurrenceCalculator.nextOccurrence(action, now.minusSeconds(1)) ?: return null
        }

        val trigger = base.minusMinutes((action.reminderMinutes ?: 0).toLong())
        return ReminderPlan(
            actionId = action.id,
            title = action.title,
            triggerAtMillis = trigger.atZone(zoneId).toInstant().toEpochMilli()
        )
    }
}
