package com.luminor.actionbox

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luminor.actionbox.data.local.ActionCompletionEntity
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.data.local.ActionListEntity
import com.luminor.actionbox.data.local.ListItemEntity
import com.luminor.actionbox.data.local.ProjectEntity
import com.luminor.actionbox.data.local.RoutineRuleEntity
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
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ActionViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ActionBoxApplication
    private val repository = app.repository
    private val settingsRepository = app.settingsRepository

    val pending = repository.pending.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val saved = repository.saved.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val notes = repository.notes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val history = repository.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val all = repository.all.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val projects = repository.projects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val lists = repository.lists.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val listItems = repository.listItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val completions = repository.completions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val deletedActions = repository.deletedActions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val deletedProjects = repository.deletedProjects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val deletedLists = repository.deletedLists.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val tags = repository.tags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val tagRefs = repository.tagRefs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val contentLinks = repository.contentLinks.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val routineRules = repository.routineRules.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiSettings())
    val uiEvents = app.uiEventBus.events

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val message = _message.asSharedFlow()

    fun complete(id: Long) {
        viewModelScope.launch {
            val entity = repository.getById(id) ?: return@launch
            if (RecurrenceCalculator.recurrenceType(entity) != RecurrenceType.NONE) {
                markOccurrence(entity, LocalDate.now(), true)
            } else {
                repository.complete(id)
                ReminderScheduler(getApplication()).cancel(id)
            }
        }
    }

    fun toggleOccurrence(action: ActionEntity, date: LocalDate) {
        viewModelScope.launch {
            if (RecurrenceCalculator.recurrenceType(action) == RecurrenceType.NONE) {
                if (action.status == ActionStatus.COMPLETED.name) {
                    repository.reopen(action.id)
                    action.projectId?.let { repository.setProjectCompleted(it, null) }
                } else {
                    repository.complete(action.id)
                }
            } else {
                markOccurrence(action, date, !isCompletedOn(action, date))
            }
        }
    }

    private suspend fun markOccurrence(action: ActionEntity, date: LocalDate, completed: Boolean) {
        if (completed) repository.insertCompletion(ActionCompletionEntity(actionId = action.id, occurrenceDate = date.toString()))
        else repository.deleteCompletion(action.id, date.toString())
    }

    fun isCompletedOn(action: ActionEntity, date: LocalDate): Boolean {
        return if (RecurrenceCalculator.recurrenceType(action) == RecurrenceType.NONE) {
            action.status == ActionStatus.COMPLETED.name
        } else {
            completions.value.any { it.actionId == action.id && it.occurrenceDate == date.toString() }
        }
    }

    fun routineOccursOn(action: ActionEntity, date: LocalDate): Boolean {
        if (action.deletedAt != null) return false
        if (action.status == ActionStatus.CANCELLED.name && !date.isBefore(LocalDate.now())) return false
        val dateMillis = startOfDayMillis(date)
        val rule = routineRules.value
            .asSequence()
            .filter { it.actionId == action.id }
            .filter { it.effectiveFrom <= dateMillis && (it.effectiveUntil == null || it.effectiveUntil >= dateMillis) }
            .maxByOrNull { it.effectiveFrom }
            ?: return RecurrenceCalculator.occursOn(action, date)

        val startDate = rule.effectiveFrom.toLocalDate()
        if (date.isBefore(startDate)) return false
        return when (runCatching { RecurrenceType.valueOf(rule.recurrenceType) }.getOrDefault(RecurrenceType.NONE)) {
            RecurrenceType.NONE -> date == startDate
            RecurrenceType.DAILY -> true
            RecurrenceType.WEEKLY -> {
                val days = rule.recurrenceDays?.split(',')?.mapNotNull { it.toIntOrNull() }?.toSet().orEmpty()
                days.isEmpty() || date.dayOfWeek.value in days
            }
            RecurrenceType.MONTHLY -> date.dayOfMonth == startDate.dayOfMonth
        }
    }

    fun toggleListItem(item: ListItemEntity) {
        viewModelScope.launch {
            val completing = item.completedAt == null
            repository.setListItemCompleted(item.id, if (completing) System.currentTimeMillis() else null)
            if (!completing) {
                repository.setListCompleted(item.listId, null)
                setListAgendaCompleted(item.listId, false)
            }
        }
    }

    fun finishProject(id: Long) {
        viewModelScope.launch {
            repository.setProjectCompleted(id, System.currentTimeMillis())
            _message.emit("Projeto finalizado")
        }
    }

    fun reopenProject(id: Long) {
        viewModelScope.launch { repository.setProjectCompleted(id, null) }
    }

    fun saveProjectEdits(
        project: ProjectEntity,
        title: String,
        description: String,
        existingTaskTitles: Map<Long, String>,
        newTaskTitles: List<String>,
        deletedTaskIds: Set<Long>,
        orderedTaskIds: List<Long> = emptyList()
    ) {
        viewModelScope.launch {
            val normalizedTitle = title.trim()
            if (normalizedTitle.isBlank()) {
                _message.emit("O projeto precisa de um título")
                return@launch
            }

            repository.updateProject(
                project.copy(
                    title = normalizedTitle,
                    description = description.trim(),
                    updatedAt = System.currentTimeMillis()
                )
            )

            val projectTasks = all.value.filter { it.projectId == project.id }
            projectTasks.filter { it.id in deletedTaskIds }.forEach { task ->
                ReminderScheduler(getApplication()).cancel(task.id)
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

            _message.emit("Projeto atualizado")
        }
    }

    fun reorderProjectTasks(orderedTaskIds: List<Long>) {
        viewModelScope.launch { repository.setActionOrder(orderedTaskIds) }
    }

    fun finishList(id: Long) {
        viewModelScope.launch {
            repository.setListCompleted(id, System.currentTimeMillis())
            setListAgendaCompleted(id, true)
            _message.emit("Lista finalizada")
        }
    }

    fun reopenList(id: Long) {
        viewModelScope.launch {
            repository.setListCompleted(id, null)
            setListAgendaCompleted(id, false)
        }
    }

    fun saveListEdits(
        list: ActionListEntity,
        title: String,
        items: List<ListItemEntity>,
        deletedItemIds: Set<Long>
    ) {
        viewModelScope.launch {
            val normalizedTitle = title.trim()
            if (normalizedTitle.isBlank()) {
                _message.emit("A lista precisa de um título")
                return@launch
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
            _message.emit("Lista atualizada")
        }
    }

    private suspend fun setListAgendaCompleted(listId: Long, completed: Boolean) {
        all.value
            .filter { it.type == ActionType.LIST.name && it.metadata == listId.toString() }
            .forEach { action ->
                if (completed) repository.complete(action.id) else repository.reopen(action.id)
            }
    }

    fun saveRoutineEdits(
        context: Context,
        original: ActionEntity,
        title: String,
        recurrenceType: RecurrenceType,
        recurrenceDays: Set<Int>,
        time: LocalTime,
        reminderMinutes: Int?,
        priority: ActionPriority,
        paused: Boolean
    ) {
        viewModelScope.launch {
            val normalizedTitle = title.trim()
            if (normalizedTitle.isBlank()) {
                _message.emit("A rotina precisa de um nome")
                return@launch
            }
            val baseDate = original.scheduledAt?.toLocalDate() ?: LocalDate.now()
            val scheduledAt = baseDate.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val updated = original.copy(
                title = normalizedTitle,
                content = normalizedTitle,
                scheduledAt = scheduledAt,
                recurrenceType = recurrenceType.name,
                recurrenceDays = recurrenceDays.sorted().joinToString(",").ifBlank { null },
                reminderMinutes = reminderMinutes,
                priority = priority.name,
                status = if (paused) ActionStatus.CANCELLED.name else ActionStatus.PENDING.name,
                updatedAt = System.currentTimeMillis()
            )
            ReminderScheduler(context.applicationContext).cancel(original.id)
            repository.update(updated)

            if (routineConfigurationChanged(original, updated)) {
                val effectiveFrom = startOfDayMillis(LocalDate.now())
                repository.replaceRoutineRule(ruleFromAction(updated, effectiveFrom), effectiveFrom - 1)
            }
            if (!paused) scheduleIfNeeded(context, updated)
            _message.emit(if (paused) "Rotina pausada" else "Rotina atualizada")
        }
    }

    fun setRoutinePaused(context: Context, action: ActionEntity, paused: Boolean) {
        val recurrence = runCatching {
            RecurrenceType.valueOf(action.recurrenceType ?: RecurrenceType.WEEKLY.name)
        }.getOrDefault(RecurrenceType.WEEKLY)
        val time = action.scheduledAt?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime()
        } ?: LocalTime.of(9, 0)
        saveRoutineEdits(
            context = context,
            original = action,
            title = action.title,
            recurrenceType = recurrence,
            recurrenceDays = RecurrenceCalculator.recurrenceDays(action),
            time = time,
            reminderMinutes = action.reminderMinutes,
            priority = runCatching {
                ActionPriority.valueOf(action.priority ?: ActionPriority.NORMAL.name)
            }.getOrDefault(ActionPriority.NORMAL),
            paused = paused
        )
    }

    fun updateAction(context: Context, original: ActionEntity, updated: ActionEntity) {
        viewModelScope.launch {
            val scheduler = ReminderScheduler(context.applicationContext)
            scheduler.cancel(original.id)
            val normalized = updated.copy(id = original.id, updatedAt = System.currentTimeMillis())
            repository.update(normalized)
            if (routineConfigurationChanged(original, normalized) &&
                RecurrenceCalculator.recurrenceType(normalized) != RecurrenceType.NONE
            ) {
                val effectiveFrom = startOfDayMillis(LocalDate.now())
                repository.replaceRoutineRule(ruleFromAction(normalized, effectiveFrom), effectiveFrom - 1)
            }
            scheduleIfNeeded(context, normalized)
            _message.emit("Ação atualizada")
        }
    }

    fun duplicateAction(context: Context, original: ActionEntity) {
        viewModelScope.launch {
            val duplicate = original.copy(
                id = 0,
                title = "${original.title} (cópia)",
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
                repository.replaceRoutineRule(
                    ruleFromAction(stored, startOfDayMillis(LocalDate.now())),
                    startOfDayMillis(LocalDate.now()) - 1
                )
            }
            scheduleIfNeeded(context, stored)
            _message.emit("Ação duplicada")
        }
    }

    fun archive(id: Long) {
        viewModelScope.launch {
            ReminderScheduler(getApplication()).cancel(id)
            repository.archive(id)
            _message.emit("Item arquivado")
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            ReminderScheduler(getApplication()).cancel(id)
            repository.softDeleteAction(id)
            app.uiEventBus.undo("Item movido para a lixeira", UndoKind.ACTION, id)
        }
    }

    fun deleteProject(id: Long) {
        viewModelScope.launch {
            all.value.filter { it.projectId == id }.forEach {
                ReminderScheduler(getApplication()).cancel(it.id)
            }
            repository.softDeleteProjectCascade(id)
            app.uiEventBus.undo("Projeto movido para a lixeira", UndoKind.PROJECT, id)
        }
    }

    fun deleteList(id: Long) {
        viewModelScope.launch {
            all.value
                .filter { it.type == ActionType.LIST.name && it.metadata == id.toString() }
                .forEach { ReminderScheduler(getApplication()).cancel(it.id) }
            repository.softDeleteListCascade(id)
            app.uiEventBus.undo("Lista movida para a lixeira", UndoKind.LIST, id)
        }
    }

    fun undo(event: AppUiEvent.Undo) {
        viewModelScope.launch {
            when (event.kind) {
                UndoKind.ACTION -> repository.restoreAction(event.id)
                UndoKind.PROJECT -> repository.restoreProjectCascade(event.id)
                UndoKind.LIST -> repository.restoreListCascade(event.id)
            }
            _message.emit("Restaurado")
        }
    }

    fun restoreAction(id: Long) {
        viewModelScope.launch { repository.restoreAction(id) }
    }

    fun restoreProject(id: Long) {
        viewModelScope.launch { repository.restoreProjectCascade(id) }
    }

    fun restoreList(id: Long) {
        viewModelScope.launch { repository.restoreListCascade(id) }
    }

    fun permanentlyDeleteAction(id: Long) {
        viewModelScope.launch { repository.permanentlyDeleteAction(id) }
    }

    fun permanentlyDeleteProject(id: Long) {
        viewModelScope.launch { repository.permanentlyDeleteProject(id) }
    }

    fun permanentlyDeleteList(id: Long) {
        viewModelScope.launch { repository.permanentlyDeleteList(id) }
    }

    fun tagsFor(ownerType: String, ownerId: Long): List<TagEntity> {
        val ids = tagRefs.value
            .filter { it.ownerType == ownerType && it.ownerId == ownerId }
            .map { it.tagId }
            .toSet()
        return tags.value.filter { it.id in ids }
    }

    fun setTagsForOwner(ownerType: String, ownerId: Long, tagIds: Set<Long>) {
        viewModelScope.launch { repository.setTagsForOwner(ownerType, ownerId, tagIds) }
    }

    fun createAndAttachTag(ownerType: String, ownerId: Long, name: String) {
        viewModelScope.launch {
            val normalized = SearchNormalizer.normalize(name)
            if (normalized.isBlank()) return@launch
            val id = repository.createOrGetTag(name.trim(), normalized)
            if (id > 0) repository.addTagToOwner(ownerType, ownerId, id)
        }
    }

    fun removeTag(ownerType: String, ownerId: Long, tagId: Long) {
        viewModelScope.launch { repository.removeTagFromOwner(ownerType, ownerId, tagId) }
    }

    fun linkNote(noteId: Long, targetType: String, targetId: Long) {
        viewModelScope.launch {
            repository.addContentLink(OrganizationOwnerType.NOTE, noteId, targetType, targetId)
            _message.emit("Nota vinculada")
        }
    }

    fun unlinkContentLink(linkId: Long) {
        viewModelScope.launch { repository.deleteContentLink(linkId) }
    }

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
    }

    fun setTheme(value: String) = viewModelScope.launch {
        settingsRepository.setTheme(value)
    }

    fun setReplyTone(value: String) = viewModelScope.launch {
        settingsRepository.setReplyTone(value)
    }

    fun setHaptics(value: Boolean) = viewModelScope.launch {
        settingsRepository.setHaptics(value)
    }

    fun clearAllData() {
        viewModelScope.launch {
            all.value.forEach { ReminderScheduler(getApplication()).cancel(it.id) }
            repository.deleteAll()
            _message.emit("Dados locais apagados")
        }
    }

    private fun scheduleIfNeeded(context: Context, action: ActionEntity) {
        if (action.deletedAt != null ||
            action.status == ActionStatus.CANCELLED.name ||
            action.status == ActionStatus.ARCHIVED.name
        ) return

        val shouldNotify = action.type == ActionType.REMINDER.name || action.reminderMinutes != null
        if (!shouldNotify || action.scheduledAt == null) return

        val now = LocalDateTime.now()
        val base = if (RecurrenceCalculator.recurrenceType(action) == RecurrenceType.NONE) {
            Instant.ofEpochMilli(action.scheduledAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
        } else {
            RecurrenceCalculator.nextOccurrence(action, now.minusSeconds(1)) ?: return
        }
        val trigger = base.minusMinutes((action.reminderMinutes ?: 0).toLong())
        val triggerMillis = trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        ReminderScheduler(context.applicationContext).schedule(action.id, action.title, triggerMillis)
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

    private fun ruleFromAction(action: ActionEntity, effectiveFrom: Long): RoutineRuleEntity {
        val localTime = action.scheduledAt?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime()
        }
        return RoutineRuleEntity(
            actionId = action.id,
            effectiveFrom = effectiveFrom,
            recurrenceType = action.recurrenceType ?: RecurrenceType.NONE.name,
            recurrenceDays = action.recurrenceDays,
            scheduledTimeMinutes = localTime?.let { it.hour * 60 + it.minute },
            reminderMinutes = action.reminderMinutes
        )
    }

    private fun Long.toLocalDate(): LocalDate =
        Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

    private fun startOfDayMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
