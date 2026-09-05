package com.luminor.actionbox.ui.organize

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luminor.actionbox.ActionViewModel
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.RecurrenceCalculator
import com.luminor.actionbox.domain.RecurrenceType
import com.luminor.actionbox.ui.designsystem.components.ActionEmptyState
import com.luminor.actionbox.ui.designsystem.components.ActionSegmentedControl
import com.luminor.actionbox.ui.organize.notes.NotesBoard

@Composable
fun OrganizeScreen(viewModel: ActionViewModel, onProjectOpen: (Long) -> Unit, onNoteOpen: (Long) -> Unit) {
    val all by viewModel.all.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val lists by viewModel.lists.collectAsStateWithLifecycle()
    val listItems by viewModel.listItems.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val completions by viewModel.completions.collectAsStateWithLifecycle()
    var section by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize().widthIn(max = 920.dp).statusBarsPadding().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Organizar", style = MaterialTheme.typography.headlineLarge)
            Text("Projetos, listas, rotinas e notas em um só lugar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            ActionSegmentedControl(listOf("Projetos", "Listas", "Rotinas", "Notas"), section, onSelected = { section = it })
        }

        if (section == 3) {
            NotesBoard(notes = notes, viewModel = viewModel, onOpen = onNoteOpen)
        } else {
            Box(Modifier.weight(1f)) {
                LazyColumn(modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (section) {
                        0 -> {
                            if (projects.isEmpty()) item { ActionEmptyState("📁", "Nenhum projeto", "Experimente: Projeto viagem: passagem, hotel e seguro.") }
                            items(projects, key = { it.id }) { project -> ProjectRichCard(project, all.filter { it.projectId == project.id }, onOpen = { onProjectOpen(project.id) }) }
                        }
                        1 -> {
                            if (lists.isEmpty()) item { ActionEmptyState("☑️", "Nenhuma lista", "Experimente: Ir ao mercado e comprar carne, pão e leite.") }
                            items(lists, key = { it.id }) { list -> ListRichCard(list, listItems.filter { it.listId == list.id }, viewModel) }
                        }
                        2 -> {
                            val routines = all.filter { RecurrenceCalculator.recurrenceType(it) != RecurrenceType.NONE && it.status != ActionStatus.ARCHIVED.name }
                            if (routines.isEmpty()) item { ActionEmptyState("🏋️", "Nenhuma rotina", "Crie algo recorrente como Academia segunda, quarta e sexta às 19h.") }
                            items(routines, key = { "routine-${it.id}-${completions.size}" }) { action -> HabitRichCard(action, viewModel) }
                        }
                    }
                    item { Spacer(Modifier.height(28.dp)) }
                }
            }
        }
    }
}
