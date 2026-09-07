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

class ProjectCommands @Inject constructor(
    private val repository: ActionRepository,
    private val scheduleReminder: ScheduleReminderUseCase,
    private val uiEventBus: AppUiEventBus
) {
    suspend fun finishProject(id: Long) {
            repository.setProjectCompleted(id, System.currentTimeMillis())
            uiEventBus.message(R.string.text_projeto_finalizado)
        }


    suspend fun reopenProject(id: Long) {
        repository.setProjectCompleted(id, null)
    }


    suspend fun saveProjectEdits(
        project: ProjectEntity,
        title: String,
        description: String,
        existingTaskTitles: Map<Long, String>,
        newTaskTitles: List<String>,
        deletedTaskIds: Set<Long>,
        orderedTaskIds: List<Long> = emptyList()
    ) = repository.transaction {
            val normalizedTitle = title.trim()
            if (normalizedTitle.isBlank()) {
                uiEventBus.message(R.string.text_o_projeto_precisa_de_um_titulo)
                return@transaction
            }

            repository.updateProject(
                project.copy(
                    title = normalizedTitle,
                    description = description.trim(),
                    updatedAt = System.currentTimeMillis()
                )
            )

            val projectTasks = repository.projectActions(project.id)
            projectTasks.filter { it.id in deletedTaskIds }.forEach { task ->
                scheduleReminder.cancel(task.id)
                repository.softDeleteAction(task.id)
            }

            val orderMap = orderedTaskIds.withIndex().associate { it.value to it.index }
            projectTasks.filterNot { it.id in deletedTaskIds }.forEach { task ->
                val updatedTitle = existingTaskTitles[task.id]?.trim().orEmpty()
                val updatedOrder = orderMap[task.id] ?: task.sortOrder
                if ((updatedTitle.isNotBlank() && updatedTitle != task.title) || updatedOrder != task.sortOrder) {
                    repository.update(
                        task.copy(
                            title = if (updatedTitle.isBlank()) task.title else updatedTitle,
                            content = if (updatedTitle.isBlank()) task.content else updatedTitle,
                            sortOrder = updatedOrder,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }

            val baseOrder = (orderedTaskIds.size + projectTasks.size).coerceAtLeast(projectTasks.size)
            val addedTasks = newTaskTitles.map { it.trim() }.filter { it.isNotBlank() }
            addedTasks.forEachIndexed { index, taskTitle ->
                repository.insert(
                    ActionEntity(
                        type = ActionType.TASK.name,
                        title = taskTitle,
                        content = taskTitle,
                        sourceText = taskTitle,
                        status = ActionStatus.PENDING.name,
                        priority = ActionPriority.NORMAL.name,
                        recurrenceType = RecurrenceType.NONE.name,
                        projectId = project.id,
                        sortOrder = baseOrder + index
                    )
                )
            }

            if (addedTasks.isNotEmpty() && project.completedAt != null) {
                repository.setProjectCompleted(project.id, null)
            }

            uiEventBus.message(R.string.text_projeto_atualizado)
        }


    suspend fun reorderProjectTasks(orderedTaskIds: List<Long>) {
        repository.setActionOrder(orderedTaskIds)
    }


    suspend fun deleteProject(id: Long) {
            repository.projectActions(id).forEach {
                scheduleReminder.cancel(it.id)
            }
            repository.softDeleteProjectCascade(id)
            uiEventBus.undo(R.string.text_projeto_movido_para_a_lixeira, UndoKind.PROJECT, id)
        }

}
