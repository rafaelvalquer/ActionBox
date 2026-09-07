package com.luminor.actionbox.domain.reminder

import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.ActionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ScheduleReminderUseCaseTest {
    private val zoneId = ZoneId.systemDefault()

    @Test
    fun `reschedule cancels old alarm before scheduling new one`() {
        val events = mutableListOf<String>()
        val useCase = ScheduleReminderUseCase(
            planner = ReminderPlanner(),
            scheduleAlarm = { id, _, _ -> events += "schedule:$id" },
            cancelAlarm = { id -> events += "cancel:$id" }
        )
        val action = reminder(5, LocalDateTime.now().plusHours(1))

        useCase.reschedule(action)

        assertEquals(listOf("cancel:5", "schedule:5"), events)
    }

    @Test
    fun `schedule future ignores expired reminder after boot`() {
        val scheduled = mutableListOf<Long>()
        val useCase = ScheduleReminderUseCase(
            planner = ReminderPlanner(),
            scheduleAlarm = { id, _, _ -> scheduled += id },
            cancelAlarm = {}
        )
        val action = reminder(8, LocalDateTime.now().minusHours(1))

        useCase.scheduleFuture(action, nowMillis = System.currentTimeMillis())

        assertEquals(emptyList<Long>(), scheduled)
    }

    private fun reminder(id: Long, dateTime: LocalDateTime) = ActionEntity(
        id = id,
        type = ActionType.REMINDER.name,
        title = "Lembrete",
        content = "Lembrete",
        sourceText = "Lembrete",
        scheduledAt = dateTime.atZone(zoneId).toInstant().toEpochMilli(),
        status = ActionStatus.PENDING.name
    )
}
