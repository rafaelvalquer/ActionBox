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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var section by remember { mutableIntStateOf(0) }
    var selectedTagId by remember { mutableStateOf<Long?>(null) }

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
            ActionSegmentedControl(listOf("Projetos", "Listas", "Rotinas", "Notas"), section, onSelected = { section = it })
            if (tags.isNotEmpty()) {
                TagFilterBar(tags = tags, selectedTagId = selectedTagId, onSelected = { selectedTagId = it })
            }
        }

        if (section == 3) {
            NotesBoard(notes = visibleNotes, viewModel = actionViewModel, onOpen = onNoteOpen)
        } else {
            Box(Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (section) {
                        0 -> {
                            if (visibleProjects.isEmpty()) item { ActionEmptyState("📁", "Nenhum projeto", if (selectedTagId == null) "Experimente: Projeto viagem: passagem, hotel e seguro." else "Nenhum projeto usa esta tag.") }
                            items(visibleProjects, key = { it.id }) { project ->
                                ProjectRichCard(
                                    project,
                                    all.filter { it.projectId == project.id }.sortedWith(compareBy({ it.sortOrder }, { it.createdAt })),
                                    onOpen = { onProjectOpen(project.id) }
                                )
                            }
                        }
                        1 -> {
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
                        2 -> {
                            if (visibleRoutines.isEmpty()) item { ActionEmptyState("🏋️", "Nenhuma rotina", if (selectedTagId == null) "Crie algo recorrente como Academia segunda, quarta e sexta às 19h." else "Nenhuma rotina usa esta tag.") }
                            items(visibleRoutines, key = { "routine-${it.id}-${completions.size}" }) { action ->
                                HabitRichCard(action, actionViewModel, onOpen = { onRoutineOpen(action.id) })
                            }
                        }
                    }
                    item { Spacer(Modifier.height(28.dp)) }
                }
            }
        }
    }
}
