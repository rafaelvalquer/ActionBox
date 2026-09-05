package com.luminor.actionbox.domain

import com.luminor.actionbox.data.local.ActionEntity
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class HabitStreakCalculatorTest {
    private val action = ActionEntity(
        id = 10,
        type = ActionType.TASK.name,
        title = "Academia",
        content = "Academia",
        sourceText = "Academia segunda quarta e sexta",
        scheduledAt = LocalDateTime.of(2026, 8, 31, 19, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        status = ActionStatus.PENDING.name,
        recurrenceType = RecurrenceType.WEEKLY.name,
        recurrenceDays = "1,3,5"
    )

    @Test
    fun completedMondayWednesdayFridayHasStreakThree() {
        val completed = setOf(
            LocalDate.of(2026, 8, 31),
            LocalDate.of(2026, 9, 2),
            LocalDate.of(2026, 9, 4)
        )
        val streak = HabitStreakCalculator.currentStreak(action, LocalDate.of(2026, 9, 4)) { it in completed }
        assertEquals(3, streak)
    }

    @Test
    fun missingLatestScheduledOccurrenceBreaksStreak() {
        val completed = setOf(
            LocalDate.of(2026, 8, 31),
            LocalDate.of(2026, 9, 2),
            LocalDate.of(2026, 9, 4)
        )
        val streak = HabitStreakCalculator.currentStreak(action, LocalDate.of(2026, 9, 7)) { it in completed }
        assertEquals(0, streak)
    }

    @Test
    fun nonScheduledDaysDoNotBreakStreak() {
        val completed = setOf(
            LocalDate.of(2026, 8, 31),
            LocalDate.of(2026, 9, 2),
            LocalDate.of(2026, 9, 4)
        )
        val streak = HabitStreakCalculator.currentStreak(action, LocalDate.of(2026, 9, 6)) { it in completed }
        assertEquals(3, streak)
    }
}
