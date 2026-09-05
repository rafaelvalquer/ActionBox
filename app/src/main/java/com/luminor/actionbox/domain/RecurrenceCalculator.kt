package com.luminor.actionbox.domain

import com.luminor.actionbox.data.local.ActionEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object RecurrenceCalculator {
    fun occursOn(action: ActionEntity, date: LocalDate): Boolean {
        if (action.status == ActionStatus.ARCHIVED.name || action.status == ActionStatus.CANCELLED.name) return false
        val scheduled = action.scheduledAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime() }
        val startDate = scheduled?.toLocalDate() ?: Instant.ofEpochMilli(action.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
        if (date.isBefore(startDate)) return false

        return when (recurrenceType(action)) {
            RecurrenceType.NONE -> scheduled?.toLocalDate() == date
            RecurrenceType.DAILY -> true
            RecurrenceType.WEEKLY -> {
                val days = recurrenceDays(action)
                days.isEmpty() || date.dayOfWeek.value in days
            }
            RecurrenceType.MONTHLY -> date.dayOfMonth == startDate.dayOfMonth
        }
    }

    fun nextOccurrence(action: ActionEntity, after: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
        val scheduled = action.scheduledAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime() } ?: return null
        if (recurrenceType(action) == RecurrenceType.NONE) return scheduled.takeIf { it.isAfter(after) }

        val time = scheduled.toLocalTime()
        var date = if (after.toLocalDate().isBefore(scheduled.toLocalDate())) scheduled.toLocalDate() else after.toLocalDate()
        repeat(740) {
            val candidate = date.atTime(time)
            if (occursOn(action, date) && candidate.isAfter(after)) return candidate
            date = date.plusDays(1)
        }
        return null
    }

    fun recurrenceType(action: ActionEntity): RecurrenceType =
        runCatching { RecurrenceType.valueOf(action.recurrenceType ?: RecurrenceType.NONE.name) }.getOrDefault(RecurrenceType.NONE)

    fun recurrenceDays(action: ActionEntity): Set<Int> = action.recurrenceDays
        ?.split(',')
        ?.mapNotNull { it.toIntOrNull() }
        ?.toSet()
        .orEmpty()
}
