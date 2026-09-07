package com.luminor.actionbox.domain.commands

import com.luminor.actionbox.R
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

class TrashCommands @Inject constructor(
    private val repository: ActionRepository,
    private val uiEventBus: AppUiEventBus
) {
    suspend fun undo(event: AppUiEvent.Undo) {
            when (event.kind) {
                UndoKind.ACTION -> repository.restoreAction(event.id)
                UndoKind.PROJECT -> repository.restoreProjectCascade(event.id)
                UndoKind.LIST -> repository.restoreListCascade(event.id)
            }
            uiEventBus.message(R.string.text_restaurado)
        }


    suspend fun restoreAction(id: Long) {
        repository.restoreAction(id)
    }


    suspend fun restoreProject(id: Long) {
        repository.restoreProjectCascade(id)
    }


    suspend fun restoreList(id: Long) {
        repository.restoreListCascade(id)
    }


    suspend fun permanentlyDeleteAction(id: Long) {
        repository.permanentlyDeleteAction(id)
    }


    suspend fun permanentlyDeleteProject(id: Long) {
        repository.permanentlyDeleteProject(id)
    }


    suspend fun permanentlyDeleteList(id: Long) {
        repository.permanentlyDeleteList(id)
    }

}
