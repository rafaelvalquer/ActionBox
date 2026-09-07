package com.luminor.actionbox.ui.organize

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.map

/** Serializable UI state kept independently from Room observers. */
internal class OrganizeSavedState(private val handle: SavedStateHandle) {
    val expandedProjectIds = handle.getStateFlow(KEY_EXPANDED_PROJECTS, emptyList<Long>()).map { it.toSet() }
    val expandedRoutineIds = handle.getStateFlow(KEY_EXPANDED_ROUTINES, emptyList<Long>()).map { it.toSet() }
    val selectedSection = handle.getStateFlow(KEY_SELECTED_SECTION, 0)
    val selectedTagId = handle.getStateFlow<Long?>(KEY_SELECTED_TAG, null)

    fun setSelectedSection(section: Int) { handle[KEY_SELECTED_SECTION] = section }
    fun selectTag(tagId: Long?) { handle[KEY_SELECTED_TAG] = tagId }

    fun scrollPosition(section: OrganizeSection): Pair<Int, Int> =
        handle.get<Int>("organize_${section.name}_scroll_index").orZero() to
            handle.get<Int>("organize_${section.name}_scroll_offset").orZero()

    fun updateScroll(section: OrganizeSection, index: Int, offset: Int) {
        handle["organize_${section.name}_scroll_index"] = index
        handle["organize_${section.name}_scroll_offset"] = offset
    }

    fun toggleProjectExpanded(projectId: Long) {
        handle[KEY_EXPANDED_PROJECTS] = handle.get<List<Long>>(KEY_EXPANDED_PROJECTS).orEmpty().toSet().toggle(projectId).toList()
    }

    fun toggleRoutineExpanded(routineId: Long) {
        handle[KEY_EXPANDED_ROUTINES] = handle.get<List<Long>>(KEY_EXPANDED_ROUTINES).orEmpty().toSet().toggle(routineId).toList()
    }

    private companion object {
        const val KEY_EXPANDED_PROJECTS = "organize_expanded_project_ids"
        const val KEY_EXPANDED_ROUTINES = "organize_expanded_routine_ids"
        const val KEY_SELECTED_SECTION = "organize_selected_section"
        const val KEY_SELECTED_TAG = "organize_selected_tag_id"
    }
}

private fun Set<Long>.toggle(id: Long): Set<Long> = if (id in this) this - id else this + id
private fun Int?.orZero() = this ?: 0
