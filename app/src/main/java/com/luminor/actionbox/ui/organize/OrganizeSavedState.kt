package com.luminor.actionbox.ui.organize

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.map

/** Serializable UI state kept independently from Room observers. */
internal class OrganizeSavedState(private val handle: SavedStateHandle) {
    val expandedProjectIds = idsFlow(KEY_EXPANDED_PROJECTS)
    val expandedRoutineIds = idsFlow(KEY_EXPANDED_ROUTINES)
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
        handle[KEY_EXPANDED_PROJECTS] = handle.get<Any?>(KEY_EXPANDED_PROJECTS).toIdSet().toggle(projectId).toList()
    }

    fun toggleRoutineExpanded(routineId: Long) {
        handle[KEY_EXPANDED_ROUTINES] = handle.get<Any?>(KEY_EXPANDED_ROUTINES).toIdSet().toggle(routineId).toList()
    }

    private fun idsFlow(key: String) = handle
        .getStateFlow<Any?>(key, emptyList<Long>())
        .map { it.toIdSet() }

    private companion object {
        const val KEY_EXPANDED_PROJECTS = "organize_expanded_project_ids"
        const val KEY_EXPANDED_ROUTINES = "organize_expanded_routine_ids"
        const val KEY_SELECTED_SECTION = "organize_selected_section"
        const val KEY_SELECTED_TAG = "organize_selected_tag_id"
    }
}

private fun Set<Long>.toggle(id: Long): Set<Long> = if (id in this) this - id else this + id
private fun Any?.toIdSet(): Set<Long> = (this as? Collection<*>)
    ?.mapNotNull { (it as? Number)?.toLong() }
    ?.toSet()
    .orEmpty()
private fun Int?.orZero() = this ?: 0
