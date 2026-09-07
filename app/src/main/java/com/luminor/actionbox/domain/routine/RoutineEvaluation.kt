package com.luminor.actionbox.domain.routine
import com.luminor.actionbox.data.local.*
import com.luminor.actionbox.domain.*
import java.time.*

object RoutineEvaluation {
    fun isCompletedOn(action: ActionEntity, date: LocalDate, completions: List<ActionCompletionEntity>): Boolean {
        return if (RecurrenceCalculator.recurrenceType(action) == RecurrenceType.NONE) {
            action.status == ActionStatus.COMPLETED.name
        } else {
            completions.any { it.actionId == action.id && it.occurrenceDate == date.toString() }
        }
    }
    fun routineOccursOn(action: ActionEntity, date: LocalDate, routineRules: List<RoutineRuleEntity>): Boolean {
        if (action.deletedAt != null) return false
        if (action.status == ActionStatus.CANCELLED.name && !date.isBefore(LocalDate.now())) return false
        val dateMillis = RoutineRuleFactory().startOfDayMillis(date)
        val rule = routineRules
            .asSequence()
            .filter { it.actionId == action.id }
            .filter { it.effectiveFrom <= dateMillis && (it.effectiveUntil == null || it.effectiveUntil >= dateMillis) }
            .maxByOrNull { it.effectiveFrom }
            ?: return RecurrenceCalculator.occursOn(action, date)

        val startDate = rule.effectiveFrom.toLocalDate()
        if (date.isBefore(startDate)) return false
        return when (runCatching { RecurrenceType.valueOf(rule.recurrenceType) }.getOrDefault(RecurrenceType.NONE)) {
            RecurrenceType.NONE -> date == startDate
            RecurrenceType.DAILY -> true
            RecurrenceType.WEEKLY -> {
                val days = rule.recurrenceDays?.split(',')?.mapNotNull { it.toIntOrNull() }?.toSet().orEmpty()
                days.isEmpty() || date.dayOfWeek.value in days
            }
            RecurrenceType.MONTHLY -> date.dayOfMonth == startDate.dayOfMonth
        }
    }
    private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
}
