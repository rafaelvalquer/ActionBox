package com.luminor.actionbox.ui.organize.notes

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
import com.luminor.actionbox.ui.detailState
import androidx.lifecycle.SavedStateHandle
import com.luminor.actionbox.domain.commands.*
import com.luminor.actionbox.ui.events.EventViewModel
import com.luminor.actionbox.domain.routine.RoutineEvaluation
import kotlinx.coroutines.flow.*
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val repository: ActionRepository,
    settingsRepository: SettingsRepository,
    uiEventBus: AppUiEventBus,
    private val actionCommands: ActionCommands,
    private val organizationCommands: OrganizationCommands,
    savedStateHandle: SavedStateHandle
) : EventViewModel(uiEventBus) {
    private val id = savedStateHandle.get<String>("id")?.toLongOrNull() ?: -1L
    private val retry = MutableStateFlow(0)
    fun retry() { retry.value++ }
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val detail = retry.flatMapLatest { repository.observeAction(id).detailState() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.luminor.actionbox.ui.DetailState.Loading)
    val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiSettings())
    val notes = repository.observeAction(id).map { listOfNotNull(it) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val projects = repository.projects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val all = repository.all.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val contentLinks = repository.observeOwnerLinks("NOTE", id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val tags = repository.tags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val tagRefs = repository.observeOwnerTagRefs("ACTION", id).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun updateAction(context: Context, original: ActionEntity, updated: ActionEntity) = execute { actionCommands.updateAction(context, original, updated) }
    fun archive(id: Long) = execute { actionCommands.archive(id) }
    fun delete(id: Long) = execute { actionCommands.delete(id) }
    fun setTagsForOwner(ownerType: String, ownerId: Long, tagIds: Set<Long>) = execute { organizationCommands.setTagsForOwner(ownerType, ownerId, tagIds) }
    fun createAndAttachTag(ownerType: String, ownerId: Long, name: String) = execute { organizationCommands.createAndAttachTag(ownerType, ownerId, name) }
    fun removeTag(ownerType: String, ownerId: Long, tagId: Long) = execute { organizationCommands.removeTag(ownerType, ownerId, tagId) }
    fun linkNote(noteId: Long, targetType: String, targetId: Long) = execute { organizationCommands.linkNote(noteId, targetType, targetId) }
    fun unlinkContentLink(linkId: Long) = execute { organizationCommands.unlinkContentLink(linkId) }
}
