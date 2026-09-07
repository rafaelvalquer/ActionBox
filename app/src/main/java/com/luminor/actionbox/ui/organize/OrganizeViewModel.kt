package com.luminor.actionbox.ui.organize

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.luminor.actionbox.ActionBoxApplication
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

enum class OrganizeSection { PROJECTS, LISTS, ROUTINES, NOTES }

class OrganizeViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val repository = (application as ActionBoxApplication).repository

    val actions = repository.all.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val projects = repository.projects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val lists = repository.lists.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val listItems = repository.listItems.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val notes = repository.notes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val completions = repository.completions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val tags = repository.tags.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val tagRefs = repository.tagRefs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedSection: StateFlow<String> = savedStateHandle.getStateFlow(
        KEY_SECTION,
        OrganizeSection.PROJECTS.name
    )
    val selectedTagId: StateFlow<Long?> = savedStateHandle.getStateFlow(KEY_TAG_ID, null)

    fun selectSection(index: Int) {
        val section = OrganizeSection.entries.getOrNull(index) ?: OrganizeSection.PROJECTS
        savedStateHandle[KEY_SECTION] = section.name
    }

    fun selectTag(tagId: Long?) {
        savedStateHandle[KEY_TAG_ID] = tagId
    }

    fun scrollPosition(section: OrganizeSection): Pair<Int, Int> =
        (savedStateHandle.get<Int>(scrollIndexKey(section)) ?: 0) to
            (savedStateHandle.get<Int>(scrollOffsetKey(section)) ?: 0)

    fun updateScroll(section: OrganizeSection, index: Int, offset: Int) {
        savedStateHandle[scrollIndexKey(section)] = index
        savedStateHandle[scrollOffsetKey(section)] = offset
    }

    private fun scrollIndexKey(section: OrganizeSection) = "organize_${section.name}_scroll_index"
    private fun scrollOffsetKey(section: OrganizeSection) = "organize_${section.name}_scroll_offset"

    private companion object {
        const val KEY_SECTION = "organize_selected_section"
        const val KEY_TAG_ID = "organize_selected_tag_id"
    }
}
