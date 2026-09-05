package com.luminor.actionbox.ui.organize

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luminor.actionbox.ActionViewModel
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.data.local.ActionListEntity
import com.luminor.actionbox.data.local.ListItemEntity
import com.luminor.actionbox.data.local.ProjectEntity
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.RecurrenceCalculator
import com.luminor.actionbox.domain.RecurrenceType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun OrganizeScreen(viewModel: ActionViewModel) {
    val all by viewModel.all.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val lists by viewModel.lists.collectAsStateWithLifecycle()
    val listItems by viewModel.listItems.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    var section by remember { mutableStateOf("PROJECTS") }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().widthIn(max = 920.dp).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(Modifier.padding(top = 18.dp)) {
                    Text("Organizar", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text("Projetos, listas, notas e suas rotinas em um só lugar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = section == "PROJECTS", onClick = { section = "PROJECTS" }, label = { Text("Projetos") })
                    FilterChip(selected = section == "LISTS", onClick = { section = "LISTS" }, label = { Text("Listas") })
                    FilterChip(selected = section == "ROUTINES", onClick = { section = "ROUTINES" }, label = { Text("Rotinas") })
                    FilterChip(selected = section == "NOTES", onClick = { section = "NOTES" }, label = { Text("Notas") })
                }
            }

            when (section) {
                "PROJECTS" -> {
                    if (projects.isEmpty()) item { EmptyOrganization("Nenhum projeto", "Crie algo como “Projeto viagem: passagem, hotel, seguro”.") }
                    items(projects.size, key = { projects[it].id }) { index -> ProjectCard(projects[index], all, viewModel) }
                }
                "LISTS" -> {
                    if (lists.isEmpty()) item { EmptyOrganization("Nenhuma lista", "Experimente “Ir ao mercado e comprar carne, pão e leite”.") }
                    items(lists.size, key = { lists[it].id }) { index -> ListCard(lists[index], listItems.filter { it.listId == lists[index].id }, viewModel) }
                }
                "ROUTINES" -> {
                    val routines = all.filter { RecurrenceCalculator.recurrenceType(it) != RecurrenceType.NONE && it.status != ActionStatus.ARCHIVED.name }
                    if (routines.isEmpty()) item { EmptyOrganization("Nenhuma rotina", "Crie uma tarefa recorrente como “Academia todos os dias às 19h”.") }
                    items(routines.size, key = { routines[it].id }) { index -> RoutineCard(routines[index], viewModel) }
                }
                else -> {
                    if (notes.isEmpty()) item { EmptyOrganization("Nenhuma nota", "Guarde ideias e informações que não exigem uma ação.") }
                    items(notes.size, key = { notes[it].id }) { index ->
                        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                Text(notes[index].title, fontWeight = FontWeight.SemiBold)
                                Text(notes[index].content, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.padding(bottom = 22.dp)) }
        }
    }
}

@Composable
private fun ProjectCard(project: ProjectEntity, all: List<ActionEntity>, viewModel: ActionViewModel) {
    val tasks = all.filter { it.projectId == project.id }
    val done = tasks.count { it.status == ActionStatus.COMPLETED.name }
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(project.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(if (tasks.isEmpty()) "Projeto sem tarefas" else "$done de ${tasks.size} concluídas", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("▣", style = MaterialTheme.typography.titleLarge)
            }
            tasks.take(6).forEach { task ->
                Row(Modifier.fillMaxWidth().clickable { viewModel.complete(task.id) }, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (task.status == ActionStatus.COMPLETED.name) "✓" else "○")
                    Text(task.title, modifier = Modifier.padding(start = 10.dp))
                }
            }
        }
    }
}

@Composable
private fun ListCard(list: ActionListEntity, items: List<ListItemEntity>, viewModel: ActionViewModel) {
    val done = items.count { it.completedAt != null }
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(list.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("$done de ${items.size} itens", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(if (items.isNotEmpty() && done == items.size) "✓" else "☑", style = MaterialTheme.typography.titleLarge)
            }
            items.forEach { item ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = item.completedAt != null, onCheckedChange = { viewModel.toggleListItem(item) })
                    Text(item.title)
                }
            }
        }
    }
}

@Composable
private fun RoutineCard(action: ActionEntity, viewModel: ActionViewModel) {
    val today = LocalDate.now()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val completed = days.count { viewModel.isCompletedOn(action, it) }
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(action.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("$completed de 7 dias concluídos", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("↻", style = MaterialTheme.typography.titleLarge)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                days.forEach { date ->
                    val done = viewModel.isCompletedOn(action, date)
                    Card(
                        modifier = Modifier.clickable { viewModel.toggleOccurrence(action, date) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = if (done) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(horizontal = 9.dp, vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(date.format(DateTimeFormatter.ofPattern("EE", Locale("pt", "BR"))).take(1).uppercase(), style = MaterialTheme.typography.labelSmall)
                            Text(if (done) "✓" else date.dayOfMonth.toString(), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyOrganization(title: String, subtitle: String) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
