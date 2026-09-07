package com.luminor.actionbox.ui.capture

import com.luminor.actionbox.R
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luminor.actionbox.ActionBoxApplication
import com.luminor.actionbox.domain.ActionPriority
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.DetectActionUseCase
import com.luminor.actionbox.domain.DetectedAction
import com.luminor.actionbox.domain.ExternalActions
import com.luminor.actionbox.domain.RecurrenceType
import com.luminor.actionbox.domain.ReplyEngine
import com.luminor.actionbox.domain.ReplyOption
import com.luminor.actionbox.domain.UiSettings
import com.luminor.actionbox.domain.action.CreateActionUseCase
import com.luminor.actionbox.domain.action.CreateListUseCase
import com.luminor.actionbox.domain.action.CreateProjectUseCase
import com.luminor.actionbox.domain.action.SaveReplyUseCase
import com.luminor.actionbox.domain.reminder.ReminderPlanner
import com.luminor.actionbox.domain.reminder.ScheduleReminderUseCase
import com.luminor.actionbox.notification.ReminderScheduler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@dagger.hilt.android.lifecycle.HiltViewModel
class CaptureViewModel @javax.inject.Inject constructor(
    private val textResources: com.luminor.actionbox.ui.events.TextResources,
    private val detectAction: DetectActionUseCase,
    private val createAction: CreateActionUseCase,
    private val createList: CreateListUseCase,
    private val createProject: CreateProjectUseCase,
    private val saveReply: SaveReplyUseCase,
    private val settingsRepository: com.luminor.actionbox.data.preferences.SettingsRepository,
    private val uiEventBus: com.luminor.actionbox.ui.events.AppUiEventBus
) : androidx.lifecycle.ViewModel() {
    // Preferences are tiny and needed by share/capture callbacks even without a screen collector.
    private val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, UiSettings())

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _detected = MutableStateFlow<DetectedAction?>(null)
    val detected: StateFlow<DetectedAction?> = _detected.asStateFlow()

    private val _navigateHome = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateHome = _navigateHome.asSharedFlow()

    fun setInput(value: String) {
        _input.value = value
    }

    fun showMessage(@androidx.annotation.StringRes id: Int, vararg args: Any) = uiEventBus.message(id, *args)
    fun showMessage(value: String) {
        uiEventBus.message(value)
    }

    fun clearInput() {
        _input.value = ""
        _detected.value = null
    }

    fun analyze() {
        val text = _input.value.trim()
        if (text.isBlank()) {
            showMessage(R.string.text_digite_ou_cole_algo_primeiro)
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
                ActionType.LIST -> {
                    val result = createList(action)
                    showMessage(R.string.text_lista_criada_com_itens, result.itemCount)
                }
                ActionType.PROJECT -> {
                    val result = createProject(action)
                    showMessage(
                        R.string.text_projeto_criado, if (result.taskCount > 0) " com ${result.taskCount} tarefas" else ""
                    )
                }
                ActionType.REPLY -> Unit
                else -> saveStandardAction(context, action)
            }
            if (action.type != ActionType.REPLY) clearInput()
        }
    }

    fun saveDetectedAndOpenCalendar(context: Context) {
        val action = _detected.value ?: return
        if (action.type != ActionType.EVENT) return
        viewModelScope.launch {
            createAction(action)
            showMessage(R.string.text_compromisso_salvo_na_agenda)
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
        ExternalActions.copy(context, textResources.getString(R.string.text_resposta_actionbox), text)
        viewModelScope.launch {
            val source = _detected.value?.sourceText ?: _input.value
            saveReply(text = text, source = source)
            showMessage(R.string.text_resposta_copiada)
            clearInput()
        }
    }

    fun insertContact(context: Context, phone: String, name: String? = null) {
        ExternalActions.insertContact(context, name, phone)
    }

    private suspend fun saveStandardAction(context: Context, action: DetectedAction) {
        createAction(action)
        when (action.type) {
            ActionType.EVENT -> showMessage(R.string.text_compromisso_salvo_na_agenda)
            ActionType.TASK -> showMessage(
                if (action.recurrenceType == RecurrenceType.NONE) textResources.getString(R.string.text_tarefa_criada) else textResources.getString(R.string.text_rotina_criada)
            )
            ActionType.REMINDER -> showMessage(R.string.text_lembrete_programado)
            ActionType.NOTE -> showMessage(R.string.text_nota_salva)
            ActionType.READ_LATER -> showMessage(R.string.text_salvo_para_depois)
            ActionType.ADDRESS -> ExternalActions.openMaps(context, action.content)
            ActionType.CONTACT -> ExternalActions.openDialer(
                context,
                action.metadata ?: action.content
            )
            else -> Unit
        }
    }

    private fun mutateDetected(transform: (DetectedAction) -> DetectedAction) {
        _detected.value = _detected.value?.let(transform)
    }
}
