package com.luminor.actionbox.ui.organize

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luminor.actionbox.ActionViewModel
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.OrganizationOwnerType
import com.luminor.actionbox.domain.RecurrenceCalculator
import com.luminor.actionbox.domain.RecurrenceType
import com.luminor.actionbox.ui.designsystem.components.ActionEmptyState
import com.luminor.actionbox.ui.designsystem.components.ActionSegmentedControl
import com.luminor.actionbox.ui.organize.notes.NotesBoard
import com.luminor.actionbox.ui.tags.TagFilterBar
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun OrganizeScreen(
    actionViewModel: ActionViewModel,
    onProjectOpen: (Long) -> Unit,
    onListOpen: (Long) -> Unit,
    onRoutineOpen: (Long) -> Unit,
    onNoteOpen: (Long) -> Unit,
    onSearch: () -> Unit,
    organizeViewModel: OrganizeViewModel = viewModel()
) {
    val all by organizeViewModel.actions.collectAsStateWithLifecycle()
    val projects by organizeViewModel.projects.collectAsStateWithLifecycle()
    val lists by organizeViewModel.lists.collectAsStateWithLifecycle()
    val listItems by organizeViewModel.listItems.collectAsStateWithLifecycle()
    val notes by organizeViewModel.notes.collectAsStateWithLifecycle()
    val completions by organizeViewModel.completions.collectAsStateWithLifecycle()
    val tags by organizeViewModel.tags.collectAsStateWithLifecycle()
    val tagRefs by organizeViewModel.tagRefs.collectAsStateWithLifecycle()
    val sectionName by organizeViewModel.selectedSection.collectAsStateWithLifecycle()
    val selectedTagId by organizeViewModel.selectedTagId.collectAsStateWithLifecycle()
    val section = runCatching { OrganizeSection.valueOf(sectionName) }.getOrDefault(OrganizeSection.PROJECTS)
    val scrollPosition = remember(section) { organizeViewModel.scrollPosition(section) }
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = scrollPosition.first,
        initialFirstVisibleItemScrollOffset = scrollPosition.second
    )

    LaunchedEffect(section, listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (index, offset) -> organizeViewModel.updateScroll(section, index, offset) }
    }

    fun ownerHasSelectedTag(ownerType: String, ownerId: Long): Boolean =
        selectedTagId == null || tagRefs.any {
            it.tagId == selectedTagId && it.ownerType == ownerType && it.ownerId == ownerId
        }

    val visibleProjects = projects.filter { ownerHasSelectedTag(OrganizationOwnerType.PROJECT, it.id) }
    val visibleLists = lists.filter { ownerHasSelectedTag(OrganizationOwnerType.LIST, it.id) }
    val visibleRoutines = all
        .filter { RecurrenceCalculator.recurrenceType(it) != RecurrenceType.NONE && it.status != ActionStatus.ARCHIVED.name }
        .filter { ownerHasSelectedTag(OrganizationOwnerType.ACTION, it.id) }
    val visibleNotes = notes.filter { ownerHasSelectedTag(OrganizationOwnerType.ACTION, it.id) }

    Column(
        modifier = Modifier.fillMaxSize().widthIn(max = 920.dp).statusBarsPadding().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Organizar", style = MaterialTheme.typography.headlineLarge)
                    Text("Projetos, listas, rotinas e notas em um só lugar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onSearch) { Text("🔍 Buscar") }
            }
            ActionSegmentedControl(
                listOf("Projetos", "Listas", "Rotinas", "Notas"),
                section.ordinal,
                onSelected = organizeViewModel::selectSection
            )
            if (tags.isNotEmpty()) {
                TagFilterBar(tags = tags, selectedTagId = selectedTagId, onSelected = organizeViewModel::selectTag)
            }
        }

        if (section == OrganizeSection.NOTES) {
            NotesBoard(notes = visibleNotes, viewModel = actionViewModel, onOpen = onNoteOpen)
        } else {
            Box(Modifier.weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (section) {
                        OrganizeSection.PROJECTS -> {
                            if (visibleProjects.isEmpty()) item { ActionEmptyState("📁", "Nenhum projeto", if (selectedTagId == null) "Experimente: Projeto viagem: passagem, hotel e seguro." else "Nenhum projeto usa esta tag.") }
                            items(visibleProjects, key = { it.id }) { project ->
                                ProjectRichCard(
                                    project,
                                    all.filter { it.projectId == project.id }.sortedWith(compareBy({ it.sortOrder }, { it.createdAt })),
                                    onOpen = { onProjectOpen(project.id) }
                                )
                            }
                        }
                        OrganizeSection.LISTS -> {
                            if (visibleLists.isEmpty()) item { ActionEmptyState("☑️", "Nenhuma lista", if (selectedTagId == null) "Experimente: Ir ao mercado e comprar carne, pão e leite." else "Nenhuma lista usa esta tag.") }
                            items(visibleLists, key = { it.id }) { list ->
                                ListRichCard(
                                    list = list,
                                    items = listItems.filter { it.listId == list.id },
                                    viewModel = actionViewModel,
                                    onOpen = { onListOpen(list.id) }
                                )
                            }
                        }
                        OrganizeSection.ROUTINES -> {
                            if (visibleRoutines.isEmpty()) item { ActionEmptyState("🔁", "Nenhuma rotina", if (selectedTagId == null) "Crie algo recorrente como Ler todos os dias às 21h." else "Nenhuma rotina usa esta tag.") }
                            items(visibleRoutines, key = { "routine-${it.id}-${completions.size}" }) { action ->
                                HabitRichCard(action, actionViewModel, onOpen = { onRoutineOpen(action.id) })
                            }
                        }
                        OrganizeSection.NOTES -> Unit
                    }
                    item { Spacer(Modifier.height(28.dp)) }
                }
            }
        }
    }
}
