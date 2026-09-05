package com.luminor.actionbox.domain

import com.luminor.actionbox.data.local.ActionEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class RecurrenceCalculatorTest {
    private fun action(recurrence: RecurrenceType, days: String? = null): ActionEntity {
        val start = LocalDateTime.of(2026, 9, 1, 19, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return ActionEntity(
            id = 1,
            type = ActionType.TASK.name,
            title = "Academia",
            content = "Academia",
            sourceText = "Academia",
            scheduledAt = start,
            status = ActionStatus.PENDING.name,
            recurrenceType = recurrence.name,
            recurrenceDays = days
        )
    }

    @Test fun dailyOccursEveryDayAfterStart() {
        val action = action(RecurrenceType.DAILY)
        assertTrue(RecurrenceCalculator.occursOn(action, LocalDate.of(2026, 9, 5)))
        assertFalse(RecurrenceCalculator.occursOn(action, LocalDate.of(2026, 8, 31)))
    }

    @Test fun weeklyRespectsSelectedDays() {
        val action = action(RecurrenceType.WEEKLY, "1,3,5")
        assertTrue(RecurrenceCalculator.occursOn(action, LocalDate.of(2026, 9, 2)))
        assertFalse(RecurrenceCalculator.occursOn(action, LocalDate.of(2026, 9, 3)))
    }
}
