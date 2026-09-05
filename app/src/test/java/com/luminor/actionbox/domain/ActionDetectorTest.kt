package com.luminor.actionbox.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ActionDetectorTest {
    private val now = LocalDateTime.of(2026, 9, 4, 13, 0)

    @Test fun detectsReminder() {
        assertEquals(ActionType.REMINDER, ActionDetector.detect("Me lembra amanhã às 10h de ligar para João", now).type)
    }

    @Test fun detectsEvent() {
        assertEquals(ActionType.EVENT, ActionDetector.detect("Reunião amanhã às 14h com o time", now).type)
    }

    @Test fun detectsTask() {
        assertEquals(ActionType.TASK, ActionDetector.detect("Preciso enviar o relatório até sexta", now).type)
    }

    @Test fun detectsUrl() {
        assertEquals(ActionType.READ_LATER, ActionDetector.detect("https://developer.android.com", now).type)
    }

    @Test fun detectsPhone() {
        assertEquals(ActionType.CONTACT, ActionDetector.detect("11 99999-8888", now).type)
    }

    @Test fun detectsAddress() {
        assertEquals(ActionType.ADDRESS, ActionDetector.detect("Avenida Paulista, 1000, São Paulo", now).type)
    }

    @Test fun detectsShoppingListAndItems() {
        val result = ActionDetector.detect("Ir ao mercado e comprar carne, pão e leite", now)
        assertEquals(ActionType.LIST, result.type)
        assertEquals(listOf("Carne", "Pão", "Leite"), result.items)
    }

    @Test fun detectsDailyRoutine() {
        val result = ActionDetector.detect("Academia todos os dias às 19h", now)
        assertEquals(ActionType.TASK, result.type)
        assertEquals(RecurrenceType.DAILY, result.recurrenceType)
        assertEquals(19, result.scheduledAt?.hour)
    }

    @Test fun detectsWeeklyRoutineDays() {
        val result = ActionDetector.detect("Treinar segunda, quarta e sexta às 7h", now)
        assertEquals(RecurrenceType.WEEKLY, result.recurrenceType)
        assertTrue(result.recurrenceDays.containsAll(setOf(1, 3, 5)))
    }
}
