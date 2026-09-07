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

class ActionCommands @Inject constructor(
    private val textResources: com.luminor.actionbox.ui.events.TextResources,
    private val repository: ActionRepository,
    private val scheduleReminder: ScheduleReminderUseCase,
    private val routineRuleFactory: RoutineRuleFactory,
    private val uiEventBus: AppUiEventBus
) {
    suspend fun complete(id: Long) {
            val entity = repository.getById(id) ?: return
            if (RecurrenceCalculator.recurrenceType(entity) != RecurrenceType.NONE) {
                markOccurrence(entity, LocalDate.now(), true)
            } else {
                repository.complete(id)
                scheduleReminder.cancel(id)
            }
        }


    suspend fun toggleOccurrence(action: ActionEntity, date: LocalDate) = repository.transaction {
            val current = repository.getById(action.id) ?: return@transaction
            if (RecurrenceCalculator.recurrenceType(current) == RecurrenceType.NONE) {
                if (current.status == ActionStatus.COMPLETED.name) {
                    repository.reopen(action.id)
                    action.projectId?.let { repository.setProjectCompleted(it, null) }
                } else {
                    repository.complete(action.id)
                }
            } else {
                markOccurrence(action, date, !repository.isCompletedOn(action.id, date.toString()))
            }
        }


    private suspend fun markOccurrence(action: ActionEntity, date: LocalDate, completed: Boolean) {
        if (completed) repository.insertCompletion(ActionCompletionEntity(actionId = action.id, occurrenceDate = date.toString()))
        else repository.deleteCompletion(action.id, date.toString())
    }

    suspend fun updateAction(context: Context, original: ActionEntity, updated: ActionEntity) = repository.transaction {
            val normalized = updated.copy(id = original.id, updatedAt = System.currentTimeMillis())
            repository.update(normalized)
            if (routineConfigurationChanged(original, normalized) &&
                RecurrenceCalculator.recurrenceType(normalized) != RecurrenceType.NONE
            ) {
                val effectiveFrom = routineRuleFactory.startOfDayMillis(LocalDate.now())
                repository.replaceRoutineRule(
                    routineRuleFactory.create(normalized, effectiveFrom),
                    effectiveFrom - 1
                )
            }
            scheduleReminder.reschedule(normalized)
            uiEventBus.message(R.string.text_acao_atualizada)
        }


    suspend fun duplicateAction(context: Context, original: ActionEntity) {
            val duplicate = original.copy(
                id = 0,
                title = textResources.getString(R.string.text_copia , original.title),
                createdAt = System.currentTimeMillis(),
                completedAt = null,
                status = ActionStatus.PENDING.name,
                deletedAt = null,
                updatedAt = null,
                sortOrder = 0
            )
            val id = repository.insert(duplicate)
            val stored = duplicate.copy(id = id)
            if (RecurrenceCalculator.recurrenceType(stored) != RecurrenceType.NONE) {
                val effectiveFrom = routineRuleFactory.startOfDayMillis(LocalDate.now())
                repository.replaceRoutineRule(
                    routineRuleFactory.create(stored, effectiveFrom),
                    effectiveFrom - 1
                )
            }
            scheduleReminder(stored)
            uiEventBus.message(R.string.text_acao_duplicada)
        }


    suspend fun archive(id: Long) {
            scheduleReminder.cancel(id)
            repository.archive(id)
            uiEventBus.message(R.string.text_item_arquivado)
        }


    suspend fun delete(id: Long) {
            scheduleReminder.cancel(id)
            repository.softDeleteAction(id)
            uiEventBus.undo(R.string.text_item_movido_para_a_lixeira, UndoKind.ACTION, id)
        }

    private fun routineConfigurationChanged(original: ActionEntity, updated: ActionEntity): Boolean {
        val originalTime = original.scheduledAt?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime()
        }
        val updatedTime = updated.scheduledAt?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime()
        }
        return original.recurrenceType != updated.recurrenceType ||
            original.recurrenceDays != updated.recurrenceDays ||
            original.reminderMinutes != updated.reminderMinutes ||
            originalTime != updatedTime
    }

    private fun Long.toLocalDate(): LocalDate =
        Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
}
