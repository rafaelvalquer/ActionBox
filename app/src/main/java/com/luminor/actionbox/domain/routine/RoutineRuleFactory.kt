package com.luminor.actionbox.domain.routine

import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.data.local.RoutineRuleEntity
import com.luminor.actionbox.domain.RecurrenceType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class RoutineRuleFactory {
    fun create(
        action: ActionEntity,
        effectiveFrom: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): RoutineRuleEntity {
        val localTime = action.scheduledAt?.let {
            Instant.ofEpochMilli(it).atZone(zoneId).toLocalTime()
        }
        return RoutineRuleEntity(
            actionId = action.id,
            effectiveFrom = effectiveFrom,
            recurrenceType = action.recurrenceType ?: RecurrenceType.NONE.name,
            recurrenceDays = action.recurrenceDays,
            scheduledTimeMinutes = localTime?.let { it.hour * 60 + it.minute },
            reminderMinutes = action.reminderMinutes
        )
    }

    fun startOfDayMillis(
        date: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
}
