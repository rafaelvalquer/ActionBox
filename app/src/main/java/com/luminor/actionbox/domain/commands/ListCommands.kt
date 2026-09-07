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

class ListCommands @Inject constructor(
    private val repository: ActionRepository,
    private val scheduleReminder: ScheduleReminderUseCase,
    private val uiEventBus: AppUiEventBus
) {
    suspend fun toggleListItem(item: ListItemEntity) = repository.transaction {
            val completing = item.completedAt == null
            repository.setListItemCompleted(item.id, if (completing) System.currentTimeMillis() else null)
            if (!completing) {
                repository.setListCompleted(item.listId, null)
                setListAgendaCompleted(item.listId, false)
            }
        }


    suspend fun finishList(id: Long) = repository.transaction {
            repository.setListCompleted(id, System.currentTimeMillis())
            setListAgendaCompleted(id, true)
            uiEventBus.message(R.string.text_lista_finalizada)
        }


    suspend fun reopenList(id: Long) = repository.transaction {
            repository.setListCompleted(id, null)
            setListAgendaCompleted(id, false)
        }


    suspend fun saveListEdits(
        list: ActionListEntity,
        title: String,
        items: List<ListItemEntity>,
        deletedItemIds: Set<Long>
    ) = repository.transaction {
            val normalizedTitle = title.trim()
            if (normalizedTitle.isBlank()) {
                uiEventBus.message(R.string.text_a_lista_precisa_de_um_titulo)
                return@transaction
            }
            val normalizedItems = items.mapIndexedNotNull { index, item ->
                val itemTitle = item.title.trim()
                itemTitle.takeIf { it.isNotBlank() }?.let { item.copy(title = itemTitle, position = index, listId = list.id) }
            }
            repository.saveListSnapshot(
                list = list.copy(title = normalizedTitle, updatedAt = System.currentTimeMillis()),
                items = normalizedItems,
                deletedItemIds = deletedItemIds
            )
            if (list.completedAt != null && normalizedItems.any { it.completedAt == null }) {
                repository.setListCompleted(list.id, null)
                setListAgendaCompleted(list.id, false)
            }
            uiEventBus.message(R.string.text_lista_atualizada)
        }


    private suspend fun setListAgendaCompleted(listId: Long, completed: Boolean) {
        repository.listAgendaActions(listId)
            .forEach { action ->
                if (completed) repository.complete(action.id) else repository.reopen(action.id)
            }
    }

    suspend fun deleteList(id: Long) {
            repository.listAgendaActions(id)
                .forEach { scheduleReminder.cancel(it.id) }
            repository.softDeleteListCascade(id)
            uiEventBus.undo(R.string.text_lista_movida_para_a_lixeira, UndoKind.LIST, id)
        }

}
