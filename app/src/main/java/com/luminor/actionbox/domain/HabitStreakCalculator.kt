package com.luminor.actionbox.domain

import com.luminor.actionbox.data.local.ActionEntity
import java.time.LocalDate

object HabitStreakCalculator {
    fun currentStreak(
        action: ActionEntity,
        today: LocalDate,
        isCompleted: (LocalDate) -> Boolean
    ): Int = currentStreak(
        today = today,
        occursOn = { RecurrenceCalculator.occursOn(action, it) },
        isCompleted = isCompleted
    )

    fun currentStreak(
        today: LocalDate,
        occursOn: (LocalDate) -> Boolean,
        isCompleted: (LocalDate) -> Boolean
    ): Int {
        var cursor = today
        var latestOccurrenceFound = false
        var streak = 0

        repeat(366) {
            if (occursOn(cursor)) {
                if (!latestOccurrenceFound) {
                    latestOccurrenceFound = true
                    if (!isCompleted(cursor)) return 0
                    streak = 1
                } else if (isCompleted(cursor)) {
                    streak++
                } else {
                    return streak
                }
            }
            cursor = cursor.minusDays(1)
        }
        return streak
    }
}
