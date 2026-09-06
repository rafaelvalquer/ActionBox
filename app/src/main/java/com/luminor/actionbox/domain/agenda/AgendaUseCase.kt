package com.luminor.actionbox.domain.agenda

import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.RecurrenceCalculator
import java.time.LocalDate

object AgendaUseCase {
    private val agendaTypes = setOf(
        ActionType.TASK.name,
        ActionType.REMINDER.name,
        ActionType.EVENT.name,
        ActionType.LIST.name
    )

    fun entriesForDay(
        date: LocalDate,
        all: List<ActionEntity>,
        occursOn: (ActionEntity, LocalDate) -> Boolean = RecurrenceCalculator::occursOn
    ): List<ActionEntity> = all
        .asSequence()
        .filter { it.deletedAt == null }
        .filter { it.type in agendaTypes }
        .filter { it.status != ActionStatus.ARCHIVED.name }
        .filter { occursOn(it, date) }
        .sortedWith(compareBy<ActionEntity> { it.scheduledAt ?: Long.MAX_VALUE }.thenBy { it.sortOrder }.thenBy { it.createdAt })
        .toList()

    fun entriesForRange(
        start: LocalDate,
        endInclusive: LocalDate,
        all: List<ActionEntity>,
        occursOn: (ActionEntity, LocalDate) -> Boolean = RecurrenceCalculator::occursOn
    ): Map<LocalDate, List<ActionEntity>> {
        if (endInclusive.isBefore(start)) return emptyMap()
        val result = linkedMapOf<LocalDate, List<ActionEntity>>()
        var cursor = start
        while (!cursor.isAfter(endInclusive)) {
            val entries = entriesForDay(cursor, all, occursOn)
            if (entries.isNotEmpty()) result[cursor] = entries
            cursor = cursor.plusDays(1)
        }
        return result
    }
}
