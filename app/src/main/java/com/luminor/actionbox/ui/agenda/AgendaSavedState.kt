package com.luminor.actionbox.ui.agenda

import androidx.lifecycle.SavedStateHandle
import java.time.LocalDate
import java.time.YearMonth

/** Owns the serializable navigation state for Agenda and is deliberately free of data dependencies. */
internal class AgendaSavedState(private val handle: SavedStateHandle) {
    val modeName = handle.getStateFlow(KEY_MODE, AgendaMode.MONTH.name)
    val selectedDateText = handle.getStateFlow(KEY_DATE, LocalDate.now().toString())
    val displayedMonthText = handle.getStateFlow(KEY_MONTH, YearMonth.now().toString())

    fun setMode(mode: AgendaMode) {
        handle[KEY_MODE] = mode.name
    }

    fun setSelectedDate(date: LocalDate) {
        handle[KEY_DATE] = date.toString()
        handle[KEY_MONTH] = YearMonth.from(date).toString()
    }

    fun changeMonth(next: YearMonth) {
        val selected = runCatching { LocalDate.parse(selectedDateText.value) }.getOrDefault(LocalDate.now())
        setSelectedDate(next.atDay(selected.dayOfMonth.coerceAtMost(next.lengthOfMonth())))
    }

    fun scrollPosition(mode: AgendaMode): Pair<Int, Int> =
        handle.get<Int>("agenda_${mode.name}_scroll_index").orZero() to
            handle.get<Int>("agenda_${mode.name}_scroll_offset").orZero()

    fun updateScroll(mode: AgendaMode, index: Int, offset: Int) {
        handle["agenda_${mode.name}_scroll_index"] = index
        handle["agenda_${mode.name}_scroll_offset"] = offset
    }

    private companion object {
        const val KEY_MODE = "agenda_mode"
        const val KEY_DATE = "agenda_selected_date"
        const val KEY_MONTH = "agenda_displayed_month"
    }
}

private fun Int?.orZero() = this ?: 0
