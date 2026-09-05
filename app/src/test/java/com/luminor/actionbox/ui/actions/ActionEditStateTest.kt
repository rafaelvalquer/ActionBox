package com.luminor.actionbox.ui.actions

import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.domain.ActionPriority
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.RecurrenceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ActionEditStateTest {
    private fun entity(at: LocalDateTime = LocalDateTime.of(2026, 9, 6, 14, 0)) = ActionEntity(
        id = 7,
        type = ActionType.EVENT.name,
        title = "Reunião Solar",
        content = "Reunião Solar",
        sourceText = "Reunião Solar amanhã às 14h",
        scheduledAt = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        status = ActionStatus.PENDING.name,
        priority = ActionPriority.NORMAL.name,
        recurrenceType = RecurrenceType.NONE.name
    )

    @Test
    fun mapsEntityIntoDraft() {
        val state = ActionEditState.from(entity())
        assertEquals(ActionType.EVENT, state.type)
        assertEquals(LocalDate.of(2026, 9, 6), state.date)
        assertEquals(LocalTime.of(14, 0), state.time)
        assertEquals(ActionPriority.NORMAL, state.priority)
    }

    @Test
    fun savesChangedDateTimeAndReminder() {
        val original = entity()
        val updated = ActionEditState.from(original).copy(
            date = LocalDate.of(2026, 9, 10),
            time = LocalTime.of(15, 30),
            reminderMinutes = 10,
            priority = ActionPriority.HIGH
        ).toEntity(original)
        val local = Instant.ofEpochMilli(updated.scheduledAt!!).atZone(ZoneId.systemDefault()).toLocalDateTime()
        assertEquals(LocalDateTime.of(2026, 9, 10, 15, 30), local)
        assertEquals(10, updated.reminderMinutes)
        assertEquals(ActionPriority.HIGH.name, updated.priority)
    }

    @Test
    fun weeklyDaysArePersistedSorted() {
        val original = entity()
        val updated = ActionEditState.from(original).copy(
            recurrenceType = RecurrenceType.WEEKLY,
            recurrenceDays = setOf(5, 1, 3)
        ).toEntity(original)
        assertEquals(RecurrenceType.WEEKLY.name, updated.recurrenceType)
        assertEquals("1,3,5", updated.recurrenceDays)
    }

    @Test
    fun dateWithoutTimeUsesMidnightAndReloadsAsNoTime() {
        val original = entity()
        val updated = ActionEditState.from(original).copy(date = LocalDate.of(2026, 9, 10), time = null).toEntity(original)
        val reloaded = ActionEditState.from(updated)
        assertEquals(LocalDate.of(2026, 9, 10), reloaded.date)
        assertNull(reloaded.time)
    }
}
