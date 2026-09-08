package com.luminor.actionbox.ui.organize

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class OrganizeViewModelTest {
    @Test
    fun acceptsLegacySetStateAndWritesBackSerializableLists() = runBlocking {
        val handle = SavedStateHandle(mapOf("organize_expanded_routine_ids" to setOf(12L)))
        val state = OrganizeSavedState(handle)

        assertEquals(setOf(12L), state.expandedRoutineIds.first())
        state.toggleRoutineExpanded(13L)

        assertEquals(setOf(12L, 13L), handle.get<List<Long>>("organize_expanded_routine_ids")?.toSet())
    }

    @Test
    fun restoresSectionTagAccordionsAndIndependentScrollPositions() = runBlocking {
        val handle = SavedStateHandle()
        val first = OrganizeSavedState(handle)

        first.setSelectedSection(OrganizeSection.ROUTINES.ordinal)
        first.selectTag(9L)
        first.toggleProjectExpanded(4L)
        first.toggleRoutineExpanded(12L)
        OrganizeSection.entries.forEachIndexed { index, section -> first.updateScroll(section, index + 1, index * 25) }

        val restored = OrganizeSavedState(handle)
        assertEquals(OrganizeSection.ROUTINES.ordinal, restored.selectedSection.value)
        assertEquals(9L, restored.selectedTagId.value)
        assertEquals(setOf(4L), restored.expandedProjectIds.first())
        assertEquals(setOf(12L), restored.expandedRoutineIds.first())
        OrganizeSection.entries.forEachIndexed { index, section ->
            assertEquals(index + 1 to index * 25, restored.scrollPosition(section))
        }
    }
}
