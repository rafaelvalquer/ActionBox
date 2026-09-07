package com.luminor.actionbox.domain.action

import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.data.local.ActionListEntity
import com.luminor.actionbox.data.local.ListItemEntity
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.DetectedAction
import com.luminor.actionbox.domain.reminder.ReminderPlanner
import com.luminor.actionbox.domain.reminder.ScheduleReminderUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class CreateListUseCaseTest {
    private val zoneId = ZoneId.systemDefault()

    @Test
    fun `empty list is valid and does not create agenda action`() = runBlocking {
        val fixture = Fixture()

        val result = fixture.useCase(
            DetectedAction(ActionType.LIST, "Compras", "Compras", "Compras"),
            now = LocalDateTime.of(2026, 9, 6, 12, 0)
        )

        assertEquals(11L, result.listId)
        assertEquals(0, result.itemCount)
        assertNull(result.actionId)
        assertEquals(0, fixture.items.size)
        assertEquals(0, fixture.actions.size)
    }

    @Test
    fun `list items preserve original order`() = runBlocking {
        val fixture = Fixture()

        fixture.useCase(
            DetectedAction(
                type = ActionType.LIST,
                title = "Mercado",
                content = "Mercado",
                sourceText = "Mercado",
                items = listOf("Leite", "Pão", "Café")
            ),
            now = LocalDateTime.of(2026, 9, 6, 12, 0)
        )

        assertEquals(listOf("Leite", "Pão", "Café"), fixture.items.map { it.title })
        assertEquals(listOf(0, 1, 2), fixture.items.map { it.position })
        assertEquals(setOf(11L), fixture.items.map { it.listId }.toSet())
    }

    @Test
    fun `dated list creates auxiliary list action`() = runBlocking {
        val fixture = Fixture()

        val result = fixture.useCase(
            DetectedAction(
                type = ActionType.LIST,
                title = "Viagem",
                content = "Viagem",
                sourceText = "Viagem",
                scheduledAt = LocalDateTime.of(2026, 9, 8, 9, 0)
            ),
            now = LocalDateTime.of(2026, 9, 6, 12, 0)
        )

        assertNotNull(result.actionId)
        assertEquals(ActionType.LIST.name, fixture.actions.single().type)
        assertEquals("11", fixture.actions.single().metadata)
    }

    @Test
    fun `list with reminder schedules alarm`() = runBlocking {
        val fixture = Fixture()

        fixture.useCase(
            DetectedAction(
                type = ActionType.LIST,
                title = "Mercado",
                content = "Mercado",
                sourceText = "Mercado",
                scheduledAt = LocalDateTime.of(2026, 9, 8, 9, 0),
                reminderMinutes = 15
            ),
            now = LocalDateTime.of(2026, 9, 6, 12, 0)
        )

        assertEquals(1, fixture.alarmIds.size)
        assertEquals(99L, fixture.alarmIds.single())
    }

    private inner class Fixture {
        val lists = mutableListOf<ActionListEntity>()
        val items = mutableListOf<ListItemEntity>()
        val actions = mutableListOf<ActionEntity>()
        val alarmIds = mutableListOf<Long>()
        private val scheduleUseCase = ScheduleReminderUseCase(
            planner = ReminderPlanner(),
            scheduleAlarm = { id, _, _ -> alarmIds += id },
            cancelAlarm = {}
        )

        val useCase = CreateListUseCase(
            insertList = { list -> lists += list.copy(id = 11); 11L },
            insertListItem = { item -> items += item; items.size.toLong() },
            insertAction = { action -> actions += action.copy(id = 99); 99L },
            scheduleReminder = scheduleUseCase::invoke,
            zoneId = zoneId
        )
    }
}
