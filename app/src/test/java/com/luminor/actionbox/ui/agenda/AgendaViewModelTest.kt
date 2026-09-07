package com.luminor.actionbox.ui.agenda

import androidx.lifecycle.SavedStateHandle
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class AgendaViewModelTest {
    @Test
    fun restoresModeDateMonthAndIndependentScrollPositions() {
        val handle = SavedStateHandle()
        val first = AgendaSavedState(handle)

        assertEquals(AgendaMode.MONTH.name, first.modeName.value)
        first.setMode(AgendaMode.WEEK)
        first.setSelectedDate(LocalDate.of(2026, 1, 31))
        first.changeMonth(YearMonth.of(2026, 2))
        AgendaMode.entries.forEachIndexed { index, mode -> first.updateScroll(mode, index + 2, index * 10) }

        val restored = AgendaSavedState(handle)
        assertEquals(AgendaMode.WEEK.name, restored.modeName.value)
        assertEquals("2026-02-28", restored.selectedDateText.value)
        assertEquals("2026-02", restored.displayedMonthText.value)
        AgendaMode.entries.forEachIndexed { index, mode ->
            assertEquals(index + 2 to index * 10, restored.scrollPosition(mode))
        }
    }
}
