package com.luminor.actionbox.ui.agenda

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.YearMonth

enum class AgendaMode { DAY, WEEK, MONTH, LIST }

class AgendaViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    val modeName: StateFlow<String> = savedStateHandle.getStateFlow(KEY_MODE, AgendaMode.MONTH.name)
    val selectedDate: StateFlow<String> = savedStateHandle.getStateFlow(KEY_SELECTED_DATE, LocalDate.now().toString())
    val monthName: StateFlow<String> = savedStateHandle.getStateFlow(KEY_MONTH, YearMonth.now().toString())

    fun selectMode(index: Int) {
        val mode = AgendaMode.entries.getOrNull(index) ?: AgendaMode.MONTH
        savedStateHandle[KEY_MODE] = mode.name
    }

    fun selectDate(date: LocalDate) {
        savedStateHandle[KEY_SELECTED_DATE] = date.toString()
        savedStateHandle[KEY_MONTH] = YearMonth.from(date).toString()
    }

    fun changeMonth(next: YearMonth) {
        val selected = currentSelectedDate()
        val day = selected.dayOfMonth.coerceAtMost(next.lengthOfMonth())
        savedStateHandle[KEY_MONTH] = next.toString()
        savedStateHandle[KEY_SELECTED_DATE] = next.atDay(day).toString()
    }

    fun scrollPosition(mode: AgendaMode): Pair<Int, Int> =
        (savedStateHandle.get<Int>(scrollIndexKey(mode)) ?: 0) to
            (savedStateHandle.get<Int>(scrollOffsetKey(mode)) ?: 0)

    fun updateScroll(mode: AgendaMode, index: Int, offset: Int) {
        savedStateHandle[scrollIndexKey(mode)] = index
        savedStateHandle[scrollOffsetKey(mode)] = offset
    }

    private fun currentSelectedDate(): LocalDate = runCatching {
        LocalDate.parse(savedStateHandle[KEY_SELECTED_DATE] ?: LocalDate.now().toString())
    }.getOrDefault(LocalDate.now())

    private fun scrollIndexKey(mode: AgendaMode) = "agenda_${mode.name}_scroll_index"
    private fun scrollOffsetKey(mode: AgendaMode) = "agenda_${mode.name}_scroll_offset"

    private companion object {
        const val KEY_MODE = "agenda_mode"
        const val KEY_SELECTED_DATE = "agenda_selected_date"
        const val KEY_MONTH = "agenda_month"
    }
}
