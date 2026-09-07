package com.luminor.actionbox.ui.agenda

import androidx.lifecycle.SavedStateHandle
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class AgendaViewModelTest {
    @Test
    fun preservesModeDateMonthAndScrollInSavedState() {
        val handle = SavedStateHandle()
        val viewModel = AgendaViewModel(handle)
        val selected = LocalDate.of(2027, 3, 15)

        viewModel.selectMode(AgendaMode.WEEK.ordinal)
        viewModel.selectDate(selected)
        viewModel.updateScroll(AgendaMode.WEEK, index = 6, offset = 48)

        assertEquals(AgendaMode.WEEK.name, viewModel.modeName.value)
        assertEquals(selected.toString(), viewModel.selectedDate.value)
        assertEquals(YearMonth.of(2027, 3).toString(), viewModel.monthName.value)
        assertEquals(6 to 48, viewModel.scrollPosition(AgendaMode.WEEK))

        val restored = AgendaViewModel(handle)
        assertEquals(AgendaMode.WEEK.name, restored.modeName.value)
        assertEquals(selected.toString(), restored.selectedDate.value)
        assertEquals(YearMonth.of(2027, 3).toString(), restored.monthName.value)
        assertEquals(6 to 48, restored.scrollPosition(AgendaMode.WEEK))
    }

    @Test
    fun changingMonthKeepsValidDayOfMonth() {
        val viewModel = AgendaViewModel(SavedStateHandle())
        viewModel.selectDate(LocalDate.of(2027, 1, 31))

        viewModel.changeMonth(YearMonth.of(2027, 2))

        assertEquals("2027-02-28", viewModel.selectedDate.value)
        assertEquals("2027-02", viewModel.monthName.value)
    }
}
