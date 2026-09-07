package com.luminor.actionbox.domain.reminder

import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.RecurrenceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderPlannerTest {
    private val zoneId = ZoneId.systemDefault()
    private val planner = ReminderPlanner()

    @Test
    fun `task without reminder does not create plan`() {
        val action = action(type = ActionType.TASK, scheduledAt = LocalDateTime.of(2026, 9, 7, 10, 0))
        assertNull(planner.plan(action, LocalDateTime.of(2026, 9, 6, 10, 0), zoneId))
    }

    @Test
    fun `task with reminder creates trigger before scheduled time`() {
        val scheduled = LocalDateTime.of(2026, 9, 7, 10, 0)
        val action = action(type = ActionType.TASK, scheduledAt = scheduled, reminderMinutes = 15)

        val plan = planner.plan(action, LocalDateTime.of(2026, 9, 6, 10, 0), zoneId)

        assertNotNull(plan)
        assertEquals(
            scheduled.minusMinutes(15).atZone(zoneId).toInstant().toEpochMilli(),
            plan!!.triggerAtMillis
        )
    }

    @Test
    fun `cancelled archived and deleted actions do not create plans`() {
        val scheduled = LocalDateTime.of(2026, 9, 7, 10, 0)
        val now = LocalDateTime.of(2026, 9, 6, 10, 0)

        assertNull(planner.plan(action(ActionType.REMINDER, scheduled, status = ActionStatus.CANCELLED), now, zoneId))
        assertNull(planner.plan(action(ActionType.REMINDER, scheduled, status = ActionStatus.ARCHIVED), now, zoneId))
        assertNull(planner.plan(action(ActionType.REMINDER, scheduled, deletedAt = 1L), now, zoneId))
    }

    @Test
    fun `daily recurrence uses next occurrence`() {
        val scheduled = LocalDateTime.of(2026, 9, 1, 9, 0)
        val now = LocalDateTime.of(2026, 9, 6, 8, 0)
        val action = action(
            type = ActionType.REMINDER,
            scheduledAt = scheduled,
            recurrenceType = RecurrenceType.DAILY
        )

        val plan = planner.plan(action, now, zoneId)

        assertEquals(
            LocalDateTime.of(2026, 9, 6, 9, 0).atZone(zoneId).toInstant().toEpochMilli(),
            plan!!.triggerAtMillis
        )
    }

    @Test
    fun `weekly recurrence uses configured weekday`() {
        val scheduled = LocalDateTime.of(2026, 9, 7, 9, 0)
        val now = LocalDateTime.of(2026, 9, 6, 12, 0)
        val action = action(
            type = ActionType.REMINDER,
            scheduledAt = scheduled,
            recurrenceType = RecurrenceType.WEEKLY,
            recurrenceDays = "1"
        )

        val plan = planner.plan(action, now, zoneId)

        assertEquals(
            LocalDateTime.of(2026, 9, 7, 9, 0).atZone(zoneId).toInstant().toEpochMilli(),
            plan!!.triggerAtMillis
        )
    }

    private fun action(
        type: ActionType,
        scheduledAt: LocalDateTime?,
        reminderMinutes: Int? = null,
        status: ActionStatus = ActionStatus.PENDING,
        recurrenceType: RecurrenceType = RecurrenceType.NONE,
        recurrenceDays: String? = null,
        deletedAt: Long? = null
    ) = ActionEntity(
        id = 7,
        type = type.name,
        title = "Teste",
        content = "Teste",
        sourceText = "Teste",
        scheduledAt = scheduledAt?.atZone(zoneId)?.toInstant()?.toEpochMilli(),
        status = status.name,
        recurrenceType = recurrenceType.name,
        recurrenceDays = recurrenceDays,
        reminderMinutes = reminderMinutes,
        deletedAt = deletedAt
    )
}
