package com.luminor.actionbox.ui.organize

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import com.luminor.actionbox.domain.routine.RoutineEvaluation

@dagger.hilt.android.lifecycle.HiltViewModel
class OrganizeViewModel @javax.inject.Inject constructor(
    private val repository: com.luminor.actionbox.data.repository.ActionRepository,
    private val actionCommands: com.luminor.actionbox.domain.commands.ActionCommands,
    private val listCommands: com.luminor.actionbox.domain.commands.ListCommands,
    private val settingsRepository: com.luminor.actionbox.data.preferences.SettingsRepository,
    commandRunner: com.luminor.actionbox.ui.events.CommandRunner,
    uiEventBus: com.luminor.actionbox.ui.events.AppUiEventBus,
    private val savedStateHandle: SavedStateHandle
) : com.luminor.actionbox.ui.events.EventViewModel(uiEventBus, commandRunner) {

    private companion object {
        const val KEY_EXPANDED_PROJECTS = "organize_expanded_project_ids"
        const val KEY_EXPANDED_ROUTINES = "organize_expanded_routine_ids"
        const val KEY_SELECTED_SECTION = "organize_selected_section"
    }

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

    val expandedProjectIds = savedStateHandle
        .getStateFlow(KEY_EXPANDED_PROJECTS, emptyList<Long>())
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
    val expandedRoutineIds = savedStateHandle
        .getStateFlow(KEY_EXPANDED_ROUTINES, emptyList<Long>())
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
    val selectedSection = savedStateHandle.getStateFlow(KEY_SELECTED_SECTION, 0)

    fun setSelectedSection(section: Int) {
        savedStateHandle[KEY_SELECTED_SECTION] = section
    }

    fun toggleProjectExpanded(projectId: Long) {
        val current = savedStateHandle.get<List<Long>>(KEY_EXPANDED_PROJECTS).orEmpty().toSet()
        savedStateHandle[KEY_EXPANDED_PROJECTS] = if (projectId in current) {
            current - projectId
        } else {
            current + projectId
        }.toList()
    }

    fun toggleRoutineExpanded(routineId: Long) {
        val current = savedStateHandle.get<List<Long>>(KEY_EXPANDED_ROUTINES).orEmpty().toSet()
        savedStateHandle[KEY_EXPANDED_ROUTINES] = if (routineId in current) current - routineId else current + routineId
    }
}
