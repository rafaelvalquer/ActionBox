package com.luminor.actionbox

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.domain.ActionDetector
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.DetectedAction
import com.luminor.actionbox.domain.ExternalActions
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
import java.time.LocalDateTime
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
    val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiSettings())

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _detected = MutableStateFlow<DetectedAction?>(null)
    val detected: StateFlow<DetectedAction?> = _detected.asStateFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 2)
    val message = _message.asSharedFlow()

    private val _navigateHome = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateHome = _navigateHome.asSharedFlow()

    fun setInput(value: String) {
        _input.value = value
    }

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
        val text = _input.value.ifBlank { _detected.value?.sourceText.orEmpty() }
        if (text.isNotBlank()) _detected.value = ActionDetector.forceType(text, type)
    }

    fun execute(context: Context, action: DetectedAction) {
        viewModelScope.launch {
            when (action.type) {
                ActionType.TASK -> {
                    repository.insert(action.toEntity(ActionStatus.PENDING))
                    _message.emit("✅ Tarefa criada")
                }
                ActionType.REMINDER -> {
                    val whenAt = action.scheduledAt ?: LocalDateTime.now().plusHours(1)
                    val entity = action.copy(scheduledAt = whenAt).toEntity(ActionStatus.PENDING)
                    val id = repository.insert(entity)
                    val millis = whenAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    ReminderScheduler(context.applicationContext).schedule(id, action.title, millis)
                    _message.emit("⏰ Lembrete programado")
                }
                ActionType.EVENT -> {
                    repository.insert(action.toEntity(ActionStatus.COMPLETED))
                    ExternalActions.openCalendar(context, action)
                    _message.emit("📅 Abrindo calendário")
                }
                ActionType.NOTE -> {
                    repository.insert(action.toEntity(ActionStatus.COMPLETED))
                    _message.emit("📝 Nota salva")
                }
                ActionType.READ_LATER -> {
                    repository.insert(action.toEntity(ActionStatus.PENDING))
                    _message.emit("🔖 Salvo para depois")
                }
                ActionType.ADDRESS -> {
                    repository.insert(action.toEntity(ActionStatus.COMPLETED))
                    ExternalActions.openMaps(context, action.content)
                }
                ActionType.CONTACT -> {
                    repository.insert(action.toEntity(ActionStatus.COMPLETED))
                    ExternalActions.openDialer(context, action.metadata ?: action.content)
                }
                ActionType.REPLY -> Unit
            }
            if (action.type != ActionType.REPLY) clearInput()
        }
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
            _message.emit("💬 Resposta copiada")
            clearInput()
        }
    }

    fun complete(id: Long) {
        viewModelScope.launch {
            val entity = repository.getById(id)
            repository.complete(id)
            if (entity?.type == ActionType.REMINDER.name) ReminderScheduler(getApplication()).cancel(id)
        }
    }

    fun archive(id: Long) {
        viewModelScope.launch { repository.archive(id) }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            ReminderScheduler(getApplication()).cancel(id)
            repository.delete(id)
        }
    }

    fun openSaved(context: Context, url: String?) {
        if (!url.isNullOrBlank()) ExternalActions.openUrl(context, url)
    }

    fun insertContact(context: Context, phone: String, name: String? = null) {
        ExternalActions.insertContact(context, name, phone)
    }

    fun setTheme(value: String) = viewModelScope.launch { settingsRepository.setTheme(value) }
    fun setReplyTone(value: String) = viewModelScope.launch { settingsRepository.setReplyTone(value) }
    fun setHaptics(value: Boolean) = viewModelScope.launch { settingsRepository.setHaptics(value) }

    fun clearAllData() {
        viewModelScope.launch {
            all.value.filter { it.type == ActionType.REMINDER.name }.forEach {
                ReminderScheduler(getApplication()).cancel(it.id)
            }
            repository.deleteAll()
            _message.emit("Dados locais apagados")
        }
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
        metadata = metadata
    )
}
