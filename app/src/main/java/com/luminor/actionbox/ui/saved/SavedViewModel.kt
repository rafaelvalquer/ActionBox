package com.luminor.actionbox.ui.saved

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
class SavedViewModel @Inject constructor(
    private val repository: ActionRepository,
    settingsRepository: SettingsRepository,
    uiEventBus: AppUiEventBus,
    commandRunner: com.luminor.actionbox.ui.events.CommandRunner,
    private val actionCommands: ActionCommands
) : EventViewModel(uiEventBus, commandRunner) {
    fun addToSystemCalendar(context: Context, action: ActionEntity) {
        val detected = DetectedAction(
            type = ActionType.EVENT,
            title = action.title,
            content = action.content,
            sourceText = action.sourceText,
            scheduledAt = action.scheduledAt?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime()
            }
        )
        ExternalActions.openCalendar(context, detected)
    }
    fun openSaved(context: Context, url: String?) {
        if (!url.isNullOrBlank()) ExternalActions.openUrl(context, url)
    }
    fun insertContact(context: Context, phone: String, name: String? = null) {
        ExternalActions.insertContact(context, name, phone)
    }    val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiSettings())
    val saved = repository.saved.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun archive(id: Long) = execute { actionCommands.archive(id) }
    fun delete(id: Long) = execute { actionCommands.delete(id) }
}
