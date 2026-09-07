package com.luminor.actionbox.domain.routine

import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.RecurrenceType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class RoutineRuleFactoryTest {
    private val zoneId = ZoneId.systemDefault()
    private val factory = RoutineRuleFactory()

    @Test
    fun `factory preserves recurrence data and scheduled minutes`() {
        val scheduled = LocalDateTime.of(2026, 9, 7, 18, 30)
        val effectiveFrom = factory.startOfDayMillis(LocalDate.of(2026, 9, 7), zoneId)
        val action = ActionEntity(
            id = 42,
            type = ActionType.TASK.name,
            title = "Academia",
            content = "Academia",
            sourceText = "Academia",
            scheduledAt = scheduled.atZone(zoneId).toInstant().toEpochMilli(),
            status = ActionStatus.PENDING.name,
            recurrenceType = RecurrenceType.WEEKLY.name,
            recurrenceDays = "1,3,5",
            reminderMinutes = 20
        )

        val rule = factory.create(action, effectiveFrom, zoneId)

        assertEquals(42L, rule.actionId)
        assertEquals(effectiveFrom, rule.effectiveFrom)
        assertEquals(RecurrenceType.WEEKLY.name, rule.recurrenceType)
        assertEquals("1,3,5", rule.recurrenceDays)
        assertEquals(18 * 60 + 30, rule.scheduledTimeMinutes)
        assertEquals(20, rule.reminderMinutes)
    }
}
