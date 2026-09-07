package com.luminor.actionbox.ui.organize

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import com.luminor.actionbox.domain.routine.RoutineEvaluation

@dagger.hilt.android.lifecycle.HiltViewModel
class OrganizeViewModel @javax.inject.Inject constructor(
    private val repository: com.luminor.actionbox.data.repository.ActionRepository,
    private val actionCommands: com.luminor.actionbox.domain.commands.ActionCommands,
    private val listCommands: com.luminor.actionbox.domain.commands.ListCommands,
    private val settingsRepository: com.luminor.actionbox.data.preferences.SettingsRepository,
    uiEventBus: com.luminor.actionbox.ui.events.AppUiEventBus,
    private val savedStateHandle: SavedStateHandle
) : com.luminor.actionbox.ui.events.EventViewModel(uiEventBus) {
    private val navigationState = OrganizeSavedState(savedStateHandle)

    val projectActions = repository.observeAllProjectActions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val actions = repository.observeRoutines().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val routineRules = repository.routineRules.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.luminor.actionbox.domain.UiSettings())
    fun toggleListItem(item: com.luminor.actionbox.data.local.ListItemEntity) = execute { listCommands.toggleListItem(item) }
    fun finishList(id: Long) = execute { listCommands.finishList(id) }
    fun reopenList(id: Long) = execute { listCommands.reopenList(id) }
    fun toggleOccurrence(action: com.luminor.actionbox.data.local.ActionEntity, date: java.time.LocalDate) = execute {
        if (!date.isAfter(LocalDate.now()) && RoutineEvaluation.routineOccursOn(action, date, routineRules.value)) {
            actionCommands.toggleOccurrence(action, date)
        }
    }
    fun toggleProjectTask(action: com.luminor.actionbox.data.local.ActionEntity) = execute {
        actionCommands.toggleOccurrence(action, LocalDate.now())
    }
    fun updateAction(context: android.content.Context, original: com.luminor.actionbox.data.local.ActionEntity, updated: com.luminor.actionbox.data.local.ActionEntity) = execute { actionCommands.updateAction(context, original, updated) }
    fun archive(id: Long) = execute { actionCommands.archive(id) }
    fun delete(id: Long) = execute { actionCommands.delete(id) }

    val projects = repository.projects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val lists = repository.lists.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val listItems = repository.listItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val notes = repository.notes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val completions = repository.completions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val tags = repository.tags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val tagRefs = repository.tagRefs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val expandedProjectIds = navigationState.expandedProjectIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
    val expandedRoutineIds = navigationState.expandedRoutineIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
    val selectedSection = navigationState.selectedSection
    val selectedTagId = navigationState.selectedTagId

    fun setSelectedSection(section: Int) {
        navigationState.setSelectedSection(section)
    }
    fun selectTag(tagId: Long?) = navigationState.selectTag(tagId)

    fun scrollPosition(section: OrganizeSection): Pair<Int, Int> =
        navigationState.scrollPosition(section)

    fun updateScroll(section: OrganizeSection, index: Int, offset: Int) {
        navigationState.updateScroll(section, index, offset)
    }

    fun toggleProjectExpanded(projectId: Long) {
        navigationState.toggleProjectExpanded(projectId)
    }

    fun toggleRoutineExpanded(routineId: Long) {
        navigationState.toggleRoutineExpanded(routineId)
    }
}
