package com.luminor.actionbox.domain.action

import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.data.local.RoutineRuleEntity
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.DetectedAction
import com.luminor.actionbox.domain.RecurrenceType
import com.luminor.actionbox.domain.reminder.ReminderPlanner
import com.luminor.actionbox.domain.reminder.ScheduleReminderUseCase
import com.luminor.actionbox.domain.routine.RoutineRuleFactory
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class CreateActionUseCaseTest {
    private val zoneId = ZoneId.systemDefault()

    @Test
    fun `task is persisted as pending`() = runBlocking {
        val fixture = Fixture()

        val stored = fixture.useCase(
            DetectedAction(
                type = ActionType.TASK,
                title = "Comprar leite",
                content = "Comprar leite",
                sourceText = "Comprar leite"
            ),
            now = LocalDateTime.of(2026, 9, 6, 12, 0)
        )

        assertEquals(101L, stored.id)
        assertEquals(ActionStatus.PENDING.name, stored.status)
        assertNull(stored.completedAt)
    }

    @Test
    fun `note contact and address are persisted as completed`() = runBlocking {
        listOf(ActionType.NOTE, ActionType.CONTACT, ActionType.ADDRESS).forEach { type ->
            val fixture = Fixture()
            val stored = fixture.useCase(
                DetectedAction(type, "Item", "Item", "Item"),
                now = LocalDateTime.of(2026, 9, 6, 12, 0)
            )

            assertEquals(ActionStatus.COMPLETED.name, stored.status)
            assertNotNull(stored.completedAt)
        }
    }

    @Test
    fun `reminder without time is normalized to one hour later and scheduled`() = runBlocking {
        val fixture = Fixture()
        val now = LocalDateTime.of(2026, 9, 6, 12, 0)

        val stored = fixture.useCase(
            DetectedAction(
                type = ActionType.REMINDER,
                title = "Ligar",
                content = "Ligar",
                sourceText = "Ligar"
            ),
            now = now
        )

        assertEquals(
            now.plusHours(1).atZone(zoneId).toInstant().toEpochMilli(),
            stored.scheduledAt
        )
        assertEquals(1, fixture.scheduled.size)
        assertEquals(stored.id, fixture.scheduled.single().id)
    }

    @Test
    fun `recurring action creates routine rule`() = runBlocking {
        val fixture = Fixture()
        val scheduled = LocalDateTime.of(2026, 9, 7, 18, 0)

        fixture.useCase(
            DetectedAction(
                type = ActionType.TASK,
                title = "Academia",
                content = "Academia",
                sourceText = "Academia",
                scheduledAt = scheduled,
                recurrenceType = RecurrenceType.WEEKLY,
                recurrenceDays = setOf(1, 3, 5)
            ),
            now = LocalDateTime.of(2026, 9, 6, 12, 0)
        )

        assertEquals(1, fixture.rules.size)
        assertEquals(RecurrenceType.WEEKLY.name, fixture.rules.single().first.recurrenceType)
        assertEquals("1,3,5", fixture.rules.single().first.recurrenceDays)
    }

    @Test
    fun `action without reminder does not schedule alarm`() = runBlocking {
        val fixture = Fixture()

        fixture.useCase(
            DetectedAction(
                type = ActionType.TASK,
                title = "Sem aviso",
                content = "Sem aviso",
                sourceText = "Sem aviso",
                scheduledAt = LocalDateTime.of(2026, 9, 7, 10, 0)
            ),
            now = LocalDateTime.of(2026, 9, 6, 12, 0)
        )

        assertEquals(0, fixture.scheduled.size)
    }

    private inner class Fixture {
        val inserted = mutableListOf<ActionEntity>()
        val rules = mutableListOf<Pair<RoutineRuleEntity, Long>>()
        val scheduled = mutableListOf<ActionEntity>()
        private val scheduleUseCase = ScheduleReminderUseCase(
            planner = ReminderPlanner(),
            scheduleAlarm = { id, _, _ ->
                inserted.lastOrNull { it.id == id }?.let(scheduled::add)
            },
            cancelAlarm = {}
        )

        val useCase = CreateActionUseCase(
            insertAction = { entity ->
                val stored = entity.copy(id = 101)
                inserted += stored
                101L
            },
            replaceRoutineRule = { rule, closeAt -> rules += rule to closeAt },
            scheduleReminder = scheduleUseCase::invoke,
            routineRuleFactory = RoutineRuleFactory(),
            zoneId = zoneId
        )
    }
}
