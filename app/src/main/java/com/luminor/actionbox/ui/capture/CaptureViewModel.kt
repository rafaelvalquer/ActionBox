package com.luminor.actionbox.ui.capture

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luminor.actionbox.ActionBoxApplication
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.data.local.ActionListEntity
import com.luminor.actionbox.data.local.ListItemEntity
import com.luminor.actionbox.data.local.ProjectEntity
import com.luminor.actionbox.data.local.RoutineRuleEntity
import com.luminor.actionbox.domain.ActionPriority
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.DetectActionUseCase
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

class CaptureViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ActionBoxApplication
    private val repository = app.repository
    private val detectAction = DetectActionUseCase()
    private val settings = app.settingsRepository.settings.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        UiSettings()
    )

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _detected = MutableStateFlow<DetectedAction?>(null)
    val detected: StateFlow<DetectedAction?> = _detected.asStateFlow()

    private val _navigateHome = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateHome = _navigateHome.asSharedFlow()

    fun setInput(value: String) {
        _input.value = value
    }

    fun showMessage(value: String) {
        app.uiEventBus.message(value)
    }

    fun clearInput() {
        _input.value = ""
        _detected.value = null
    }

    fun analyze() {
        val text = _input.value.trim()
        if (text.isBlank()) {
            showMessage("Digite ou cole algo primeiro.")
            return
        }
        _detected.value = detectAction(text)
    }

    fun processInput(text: String, fromShare: Boolean = false) {
        _input.value = text
        _detected.value = detectAction(text)
        if (fromShare) _navigateHome.tryEmit(Unit)
    }

    fun chooseType(type: ActionType) {
        val current = _detected.value
        if (current != null) {
            val inferred = if ((type == ActionType.LIST || type == ActionType.PROJECT) && current.items.isEmpty()) {
                detectAction.forceType(current.sourceText, type)
            } else {
                current
            }
            _detected.value = inferred.copy(type = type, confidence = 100)
        } else {
            val text = _input.value
            if (text.isNotBlank()) _detected.value = detectAction.forceType(text, type)
        }
    }

    fun updateDetectedTitle(value: String) = mutateDetected { it.copy(title = value) }

    fun updateDetectedDescription(value: String) = mutateDetected { it.copy(description = value) }

    fun setRecurrence(value: RecurrenceType) = mutateDetected { it.copy(recurrenceType = value) }

    fun setReminderMinutes(value: Int?) = mutateDetected { it.copy(reminderMinutes = value) }

    fun setPriority(value: ActionPriority) = mutateDetected { it.copy(priority = value) }

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
        val parsed = runCatching {
            LocalDate.parse(value, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        }.getOrNull() ?: return false
        setDetectedDate(parsed)
        return true
    }

    fun setDetectedTime(time: LocalTime?) {
        mutateDetected { action ->
            if (time == null) {
                action.copy(scheduledAt = action.scheduledAt?.toLocalDate()?.atStartOfDay())
            } else {
                action.copy(scheduledAt = (action.scheduledAt?.toLocalDate() ?: LocalDate.now()).atTime(time))
            }
        }
    }

    fun setDetectedTimeText(value: String): Boolean {
        val parsed = runCatching {
            LocalTime.parse(value, DateTimeFormatter.ofPattern("HH:mm"))
        }.getOrNull() ?: return false
        setDetectedTime(parsed)
        return true
    }

    fun addDetectedItem(value: String) {
        val item = value.trim()
        if (item.isNotBlank()) mutateDetected { it.copy(items = it.items + item) }
    }

    fun removeDetectedItem(index: Int) {
        mutateDetected { action ->
            action.copy(items = action.items.filterIndexed { i, _ -> i != index })
        }
    }

    fun saveDetected(context: Context) {
        val action = _detected.value ?: return
        viewModelScope.launch {
            when (action.type) {
                ActionType.LIST -> createList(context, action)
                ActionType.PROJECT -> createProject(action)
                ActionType.REPLY -> Unit
                else -> createStandardAction(context, action)
            }
            if (action.type != ActionType.REPLY) clearInput()
        }
    }

    fun saveDetectedAndOpenCalendar(context: Context) {
        val action = _detected.value ?: return
        if (action.type != ActionType.EVENT) return
        viewModelScope.launch {
            createStandardAction(context, action)
            ExternalActions.openCalendar(context, action)
            clearInput()
        }
    }

    fun execute(context: Context, action: DetectedAction) {
        _detected.value = action
        saveDetected(context)
    }

    fun replyOptions(message: String): List<ReplyOption> =
        ReplyEngine.generate(message, settings.value.replyTone)

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
            showMessage("Resposta copiada")
            clearInput()
        }
    }

    fun insertContact(context: Context, phone: String, name: String? = null) {
        ExternalActions.insertContact(context, name, phone)
    }

    private suspend fun createStandardAction(context: Context, action: DetectedAction) {
        val normalizedAction = if (action.type == ActionType.REMINDER && action.scheduledAt == null) {
            action.copy(scheduledAt = LocalDateTime.now().plusHours(1))
        } else {
            action
        }
        val status = when (normalizedAction.type) {
            ActionType.NOTE, ActionType.ADDRESS, ActionType.CONTACT -> ActionStatus.COMPLETED
            else -> ActionStatus.PENDING
        }
        val entity = normalizedAction.toEntity(status)
        val id = repository.insert(entity)
        val stored = entity.copy(id = id)
        if (normalizedAction.recurrenceType != RecurrenceType.NONE) {
            val effectiveFrom = startOfDayMillis(stored.createdAt.toLocalDate())
            repository.replaceRoutineRule(ruleFromAction(stored, effectiveFrom), effectiveFrom - 1)
        }
        scheduleIfNeeded(context, stored)

        when (normalizedAction.type) {
            ActionType.EVENT -> showMessage("Compromisso salvo na agenda")
            ActionType.TASK -> showMessage(
                if (normalizedAction.recurrenceType == RecurrenceType.NONE) "Tarefa criada" else "Rotina criada"
            )
            ActionType.REMINDER -> showMessage("Lembrete programado")
            ActionType.NOTE -> showMessage("Nota salva")
            ActionType.READ_LATER -> showMessage("Salvo para depois")
            ActionType.ADDRESS -> ExternalActions.openMaps(context, normalizedAction.content)
            ActionType.CONTACT -> ExternalActions.openDialer(
                context,
                normalizedAction.metadata ?: normalizedAction.content
            )
            else -> Unit
        }
    }

    private suspend fun createList(context: Context, action: DetectedAction) {
        val listId = repository.insertList(
            ActionListEntity(title = action.title.ifBlank { "Nova lista" })
        )
        action.items.forEachIndexed { index, item ->
            repository.insertListItem(
                ListItemEntity(listId = listId, title = item, position = index)
            )
        }
        if (action.scheduledAt != null || action.recurrenceType != RecurrenceType.NONE) {
            val entity = action.toEntity(ActionStatus.PENDING).copy(metadata = listId.toString())
            val actionId = repository.insert(entity)
            scheduleIfNeeded(context, entity.copy(id = actionId))
        }
        showMessage("Lista criada com ${action.items.size} itens")
    }

    private suspend fun createProject(action: DetectedAction) {
        val projectId = repository.insertProject(
            ProjectEntity(
                title = action.title.ifBlank { "Novo projeto" },
                description = action.description
            )
        )
        action.items.forEachIndexed { index, item ->
            val child = action.copy(
                type = ActionType.TASK,
                title = item,
                content = item,
                sourceText = action.sourceText,
                recurrenceType = RecurrenceType.NONE,
                recurrenceDays = emptySet(),
                reminderMinutes = null,
                items = emptyList()
            ).toEntity(ActionStatus.PENDING).copy(projectId = projectId, sortOrder = index)
            repository.insert(child)
        }
        showMessage(
            "Projeto criado${if (action.items.isNotEmpty()) " com ${action.items.size} tarefas" else ""}"
        )
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
        ReminderScheduler(context.applicationContext)
            .schedule(action.id, action.title, triggerMillis)
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

    private fun Long.toLocalDate(): LocalDate =
        Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

    private fun startOfDayMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
