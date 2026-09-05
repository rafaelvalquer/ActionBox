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
import com.luminor.actionbox.domain.ActionDetector
import com.luminor.actionbox.domain.ActionPriority
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.DetectedAction
import com.luminor.actionbox.domain.ExternalActions
import com.luminor.actionbox.domain.RecurrenceCalculator
import com.luminor.actionbox.domain.RecurrenceType
import com.luminor.actionbox.domain.ReplyEngine
import com.luminor.actionbox.domain.ReplyOption
import com.luminor.actionbox.domain.UiSettings
import com.luminor.actionbox.notification.ReminderScheduler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
    val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiSettings())

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _detected = MutableStateFlow<DetectedAction?>(null)
    val detected: StateFlow<DetectedAction?> = _detected.asStateFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val message = _message.asSharedFlow()

    private val _navigateHome = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateHome = _navigateHome.asSharedFlow()

    fun setInput(value: String) { _input.value = value }

    fun clearInput() {
        _input.value = ""
        _detected.value = null
    }

    fun analyze() {
        val text = _input.value.trim()
        if (text.isBlank()) {
            _message.tryEmit("Digite ou cole algo primeiro.")
            return
        }
        _detected.value = ActionDetector.detect(text)
    }

    fun processInput(text: String, fromShare: Boolean = false) {
        _input.value = text
        _detected.value = ActionDetector.detect(text)
        if (fromShare) _navigateHome.tryEmit(Unit)
    }

    fun chooseType(type: ActionType) {
        val current = _detected.value
        if (current != null) {
            val inferred = if ((type == ActionType.LIST || type == ActionType.PROJECT) && current.items.isEmpty()) {
                ActionDetector.forceType(current.sourceText, type)
            } else current
            _detected.value = inferred.copy(type = type, confidence = 100)
        } else {
            val text = _input.value
            if (text.isNotBlank()) _detected.value = ActionDetector.forceType(text, type)
        }
    }

    fun updateDetectedTitle(value: String) { mutateDetected { it.copy(title = value) } }
    fun updateDetectedDescription(value: String) { mutateDetected { it.copy(description = value) } }
    fun setRecurrence(value: RecurrenceType) { mutateDetected { it.copy(recurrenceType = value) } }
    fun setReminderMinutes(value: Int?) { mutateDetected { it.copy(reminderMinutes = value) } }
    fun setPriority(value: ActionPriority) { mutateDetected { it.copy(priority = value) } }

    fun toggleRecurrenceDay(day: Int) {
        mutateDetected { action ->
            val days = action.recurrenceDays.toMutableSet()
            if (!days.add(day)) days.remove(day)
            action.copy(recurrenceType = RecurrenceType.WEEKLY, recurrenceDays = days)
        }
    }

    fun setDetectedDate(date: LocalDate?) {
        mutateDetected { action ->
            val time = action.scheduledAt?.toLocalTime() ?: LocalTime.of(9, 0)
            action.copy(scheduledAt = date?.atTime(time))
        }
    }

    fun setDetectedDateText(value: String): Boolean {
        val parsed = runCatching { LocalDate.parse(value, DateTimeFormatter.ofPattern("dd/MM/yyyy")) }.getOrNull() ?: return false
        setDetectedDate(parsed)
        return true
    }

    fun setDetectedTime(time: LocalTime?) {
        mutateDetected { action ->
            if (time == null) action.copy(scheduledAt = action.scheduledAt?.toLocalDate()?.atStartOfDay())
            else action.copy(scheduledAt = (action.scheduledAt?.toLocalDate() ?: LocalDate.now()).atTime(time))
        }
    }

    fun setDetectedTimeText(value: String): Boolean {
        val parsed = runCatching { LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm")) }.getOrNull() ?: return false
        setDetectedTime(parsed)
        return true
    }

    fun addDetectedItem(value: String) {
        val item = value.trim()
        if (item.isNotBlank()) mutateDetected { it.copy(items = it.items + item) }
    }

    fun removeDetectedItem(index: Int) {
        mutateDetected { action -> action.copy(items = action.items.filterIndexed { i, _ -> i != index }) }
    }

    fun saveDetected(context: Context) {
        val action = _detected.value ?: return
        viewModelScope.launch {
            when (action.type) {
                ActionType.LIST -> createList(context, action)
                ActionType.PROJECT -> createProject(context, action)
                ActionType.REPLY -> Unit
                else -> createStandardAction(context, action)
            }
            if (action.type != ActionType.REPLY) clearInput()
        }
    }

    fun execute(context: Context, action: DetectedAction) {
        _detected.value = action
        saveDetected(context)
    }

    private suspend fun createStandardAction(context: Context, action: DetectedAction) {
        val normalizedAction = if (action.type == ActionType.REMINDER && action.scheduledAt == null) {
            action.copy(scheduledAt = LocalDateTime.now().plusHours(1))
        } else action
        val status = when (normalizedAction.type) {
            ActionType.NOTE, ActionType.ADDRESS, ActionType.CONTACT -> ActionStatus.COMPLETED
            else -> ActionStatus.PENDING
        }
        val entity = normalizedAction.toEntity(status)
        val id = repository.insert(entity)
        val stored = entity.copy(id = id)
        scheduleIfNeeded(context, stored)

        when (normalizedAction.type) {
            ActionType.EVENT -> _message.emit("Compromisso salvo na agenda")
            ActionType.TASK -> _message.emit(if (normalizedAction.recurrenceType == RecurrenceType.NONE) "Tarefa criada" else "Rotina criada")
            ActionType.REMINDER -> _message.emit("Lembrete programado")
            ActionType.NOTE -> _message.emit("Nota salva")
            ActionType.READ_LATER -> _message.emit("Salvo para depois")
            ActionType.ADDRESS -> ExternalActions.openMaps(context, normalizedAction.content)
            ActionType.CONTACT -> ExternalActions.openDialer(context, normalizedAction.metadata ?: normalizedAction.content)
            else -> Unit
        }
    }

    private suspend fun createList(context: Context, action: DetectedAction) {
        val listId = repository.insertList(ActionListEntity(title = action.title.ifBlank { "Nova lista" }))
        action.items.forEachIndexed { index, item ->
            repository.insertListItem(ListItemEntity(listId = listId, title = item, position = index))
        }
        if (action.scheduledAt != null || action.recurrenceType != RecurrenceType.NONE) {
            val entity = action.toEntity(ActionStatus.PENDING).copy(metadata = listId.toString())
            val actionId = repository.insert(entity)
            scheduleIfNeeded(context, entity.copy(id = actionId))
        }
        _message.emit("Lista criada com ${action.items.size} itens")
    }

    private suspend fun createProject(context: Context, action: DetectedAction) {
        val projectId = repository.insertProject(ProjectEntity(title = action.title.ifBlank { "Novo projeto" }, description = action.description))
        action.items.forEach { item ->
            val child = action.copy(
                type = ActionType.TASK,
                title = item,
                content = item,
                sourceText = action.sourceText,
                recurrenceType = RecurrenceType.NONE,
                recurrenceDays = emptySet(),
                reminderMinutes = null,
                items = emptyList()
            ).toEntity(ActionStatus.PENDING).copy(projectId = projectId)
            repository.insert(child)
        }
        _message.emit("Projeto criado${if (action.items.isNotEmpty()) " com ${action.items.size} tarefas" else ""}")
    }

    private fun scheduleIfNeeded(context: Context, action: ActionEntity) {
        val shouldNotify = action.type == ActionType.REMINDER.name || action.reminderMinutes != null
        if (!shouldNotify || action.scheduledAt == null) return

        val now = LocalDateTime.now()
        val base = if (RecurrenceCalculator.recurrenceType(action) == RecurrenceType.NONE) {
            Instant.ofEpochMilli(action.scheduledAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
        } else {
            RecurrenceCalculator.nextOccurrence(action, now.minusSeconds(1)) ?: return
        }
        val trigger = base.minusMinutes((action.reminderMinutes ?: 0).toLong())
        val triggerMillis = trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        ReminderScheduler(context.applicationContext).schedule(action.id, action.title, triggerMillis)
    }

    fun replyOptions(message: String): List<ReplyOption> = ReplyEngine.generate(message, settings.value.replyTone)

    fun copyReply(context: Context, text: String) {
        ExternalActions.copy(context, "Resposta ActionBox", text)
        viewModelScope.launch {
            val source = _detected.value?.sourceText ?: _input.value
            repository.insert(
                ActionEntity(
                    type = ActionType.REPLY.name,
                    title = "Resposta copiada",
                    content = text,
                    sourceText = source,
                    status = ActionStatus.COMPLETED.name,
                    completedAt = System.currentTimeMillis()
                )
            )
            _message.emit("Resposta copiada")
            clearInput()
        }
    }

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
                if (action.status == ActionStatus.COMPLETED.name) repository.reopen(action.id) else repository.complete(action.id)
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

    fun toggleListItem(item: ListItemEntity) {
        viewModelScope.launch {
            repository.setListItemCompleted(item.id, if (item.completedAt == null) System.currentTimeMillis() else null)
        }
    }

    fun archive(id: Long) { viewModelScope.launch { repository.archive(id) } }

    fun delete(id: Long) {
        viewModelScope.launch {
            ReminderScheduler(getApplication()).cancel(id)
            repository.delete(id)
        }
    }

    fun deleteProject(id: Long) { viewModelScope.launch { repository.deleteProject(id) } }
    fun deleteList(id: Long) { viewModelScope.launch { repository.deleteList(id) } }

    fun addToSystemCalendar(context: Context, action: ActionEntity) {
        val detected = DetectedAction(
            type = ActionType.EVENT,
            title = action.title,
            content = action.content,
            sourceText = action.sourceText,
            scheduledAt = action.scheduledAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime() }
        )
        ExternalActions.openCalendar(context, detected)
    }

    fun openSaved(context: Context, url: String?) { if (!url.isNullOrBlank()) ExternalActions.openUrl(context, url) }
    fun insertContact(context: Context, phone: String, name: String? = null) { ExternalActions.insertContact(context, name, phone) }

    fun setTheme(value: String) = viewModelScope.launch { settingsRepository.setTheme(value) }
    fun setReplyTone(value: String) = viewModelScope.launch { settingsRepository.setReplyTone(value) }
    fun setHaptics(value: Boolean) = viewModelScope.launch { settingsRepository.setHaptics(value) }

    fun clearAllData() {
        viewModelScope.launch {
            all.value.forEach { ReminderScheduler(getApplication()).cancel(it.id) }
            repository.deleteAll()
            _message.emit("Dados locais apagados")
        }
    }

    private fun mutateDetected(transform: (DetectedAction) -> DetectedAction) {
        _detected.value = _detected.value?.let(transform)
    }

    private fun DetectedAction.toEntity(status: ActionStatus): ActionEntity = ActionEntity(
        type = type.name,
        title = title,
        content = content,
        sourceText = sourceText,
        sourceUrl = sourceUrl,
        scheduledAt = scheduledAt?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
        completedAt = if (status == ActionStatus.COMPLETED) System.currentTimeMillis() else null,
        status = status.name,
        metadata = metadata,
        description = description.ifBlank { null },
        priority = priority.name,
        recurrenceType = recurrenceType.name,
        recurrenceDays = recurrenceDays.sorted().joinToString(",").ifBlank { null },
        reminderMinutes = reminderMinutes
    )
}
