package com.luminor.actionbox.ui.agenda

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luminor.actionbox.data.local.ActionCompletionEntity
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.data.local.ActionListEntity
import com.luminor.actionbox.data.local.ListItemEntity
import com.luminor.actionbox.data.local.ProjectEntity
import com.luminor.actionbox.data.local.TagEntity
import com.luminor.actionbox.domain.ActionPriority
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.DetectedAction
import com.luminor.actionbox.domain.ExternalActions
import com.luminor.actionbox.domain.OrganizationOwnerType
import com.luminor.actionbox.domain.RecurrenceCalculator
import com.luminor.actionbox.domain.RecurrenceType
import com.luminor.actionbox.domain.UiSettings
import com.luminor.actionbox.domain.reminder.ReminderPlanner
import com.luminor.actionbox.domain.reminder.ScheduleReminderUseCase
import com.luminor.actionbox.domain.routine.RoutineRuleFactory
import com.luminor.actionbox.domain.search.SearchNormalizer
import com.luminor.actionbox.notification.ReminderScheduler
import com.luminor.actionbox.ui.events.AppUiEvent
import com.luminor.actionbox.ui.events.UndoKind
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

import javax.inject.Inject
import com.luminor.actionbox.data.repository.ActionRepository
import com.luminor.actionbox.data.preferences.SettingsRepository
import com.luminor.actionbox.ui.events.AppUiEventBus
import kotlinx.coroutines.flow.first
import androidx.lifecycle.SavedStateHandle
import com.luminor.actionbox.domain.commands.*
import com.luminor.actionbox.ui.events.EventViewModel
import com.luminor.actionbox.domain.routine.RoutineEvaluation
import kotlinx.coroutines.flow.*
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class AgendaViewModel @Inject constructor(
    private val repository: ActionRepository,
    settingsRepository: SettingsRepository,
    uiEventBus: AppUiEventBus,
    private val actionCommands: ActionCommands
) : EventViewModel(uiEventBus) {
    private val period = MutableStateFlow(LocalDate.now() to LocalDate.now())
    fun setPeriod(start: LocalDate, end: LocalDate) { period.value = start to end }
    private val actionsFlow = period.flatMapLatest { (start, end) -> repository.observeAgenda(start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(), end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()) }
    private val completionFlow = period.flatMapLatest { (start, end) -> repository.observePeriodCompletions(start.toString(), end.toString()) }
    val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiSettings())
    val all = actionsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val completions = completionFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val routineRules = repository.routineRules.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun isCompletedOn(action: ActionEntity, date: LocalDate) = RoutineEvaluation.isCompletedOn(action, date, completions.value)
    fun routineOccursOn(action: ActionEntity, date: LocalDate) = RoutineEvaluation.routineOccursOn(action, date, routineRules.value)
    fun toggleOccurrence(action: ActionEntity, date: LocalDate) = execute { actionCommands.toggleOccurrence(action, date) }
}
