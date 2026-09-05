package com.luminor.actionbox.ui.organize

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
import java.time.YearMonth
import java.time.format.TextStyle
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
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = section == "PROJECTS", onClick = { section = "PROJECTS" }, label = { Text("Projetos") }) }
                    item { FilterChip(selected = section == "LISTS", onClick = { section = "LISTS" }, label = { Text("Listas") }) }
                    item { FilterChip(selected = section == "ROUTINES", onClick = { section = "ROUTINES" }, label = { Text("Rotinas") }) }
                    item { FilterChip(selected = section == "NOTES", onClick = { section = "NOTES" }, label = { Text("Notas") }) }
                }
            }

            when (section) {
                "PROJECTS" -> {
                    if (projects.isEmpty()) item { EmptyOrganization("Nenhum projeto", "Crie algo como “Projeto viagem: passagem, hotel, seguro”.") }
                    items(projects, key = { it.id }) { project -> ProjectCard(project, all, viewModel) }
                }
                "LISTS" -> {
                    if (lists.isEmpty()) item { EmptyOrganization("Nenhuma lista", "Experimente “Ir ao mercado e comprar carne, pão e leite”.") }
                    items(lists, key = { it.id }) { list -> ListCard(list, listItems.filter { it.listId == list.id }, viewModel) }
                }
                "ROUTINES" -> {
                    val routines = all.filter { RecurrenceCalculator.recurrenceType(it) != RecurrenceType.NONE && it.status != ActionStatus.ARCHIVED.name }
                    if (routines.isEmpty()) item { EmptyOrganization("Nenhuma rotina", "Crie uma tarefa recorrente como “Academia todos os dias às 19h”.") }
                    items(routines, key = { it.id }) { action -> RoutineCard(action, viewModel) }
                }
                else -> {
                    if (notes.isEmpty()) item { EmptyOrganization("Nenhuma nota", "Guarde ideias e informações que não exigem uma ação.") }
                    items(notes, key = { it.id }) { note ->
                        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                Text(note.title, fontWeight = FontWeight.SemiBold)
                                Text(note.content, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(22.dp)) }
        }
    }
}

@Composable
private fun ProjectCard(project: ProjectEntity, all: List<ActionEntity>, viewModel: ActionViewModel) {
    val tasks = all.filter { it.projectId == project.id }
    val done = tasks.count { it.status == ActionStatus.COMPLETED.name }
    val progress = if (tasks.isEmpty()) 0f else done.toFloat() / tasks.size
    val readyToFinish = tasks.isNotEmpty() && done == tasks.size && project.completedAt == null
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(project.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textDecoration = if (project.completedAt != null) TextDecoration.LineThrough else null)
                    Text(
                        when {
                            project.completedAt != null -> "Projeto finalizado"
                            tasks.isEmpty() -> "Projeto sem tarefas"
                            else -> "$done de ${tasks.size} concluídas"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(if (project.completedAt != null) "✓" else "▣", style = MaterialTheme.typography.titleLarge)
            }
            if (tasks.isNotEmpty()) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            tasks.take(8).forEach { task ->
                Row(Modifier.fillMaxWidth().clickable { viewModel.toggleOccurrence(task, LocalDate.now()) }, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (task.status == ActionStatus.COMPLETED.name) "✓" else "○")
                    Text(task.title, modifier = Modifier.padding(start = 10.dp), textDecoration = if (task.status == ActionStatus.COMPLETED.name) TextDecoration.LineThrough else null)
                }
            }
            if (readyToFinish) {
                FilledTonalButton(onClick = { viewModel.finishProject(project.id) }, modifier = Modifier.fillMaxWidth()) { Text("Finalizar projeto") }
            } else if (project.completedAt != null) {
                TextButton(onClick = { viewModel.reopenProject(project.id) }) { Text("Reabrir projeto") }
            }
        }
    }
}

@Composable
private fun ListCard(list: ActionListEntity, items: List<ListItemEntity>, viewModel: ActionViewModel) {
    val done = items.count { it.completedAt != null }
    val progress = if (items.isEmpty()) 0f else done.toFloat() / items.size
    val readyToFinish = items.isNotEmpty() && done == items.size && list.completedAt == null
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(list.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textDecoration = if (list.completedAt != null) TextDecoration.LineThrough else null)
                    Text(if (list.completedAt != null) "Lista finalizada" else "$done de ${items.size} itens", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(if (list.completedAt != null) "✓" else "☑", style = MaterialTheme.typography.titleLarge)
            }
            if (items.isNotEmpty()) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            items.forEach { item ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = item.completedAt != null, onCheckedChange = { viewModel.toggleListItem(item) })
                    Text(item.title, textDecoration = if (item.completedAt != null) TextDecoration.LineThrough else null)
                }
            }
            if (readyToFinish) {
                FilledTonalButton(onClick = { viewModel.finishList(list.id) }, modifier = Modifier.fillMaxWidth()) { Text("Finalizar lista") }
            } else if (list.completedAt != null) {
                TextButton(onClick = { viewModel.reopenList(list.id) }) { Text("Reabrir lista") }
            }
        }
    }
}

@Composable
private fun RoutineCard(action: ActionEntity, viewModel: ActionViewModel) {
    val today = LocalDate.now()
    val month = YearMonth.from(today)
    val occurrenceDates = (1..month.lengthOfMonth()).map { month.atDay(it) }.filter { RecurrenceCalculator.occursOn(action, it) && !it.isAfter(today) }
    val completed = occurrenceDates.count { viewModel.isCompletedOn(action, it) }
    val progress = if (occurrenceDates.isEmpty()) 0f else completed.toFloat() / occurrenceDates.size

    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(action.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("$completed de ${occurrenceDates.size} concluídos neste mês", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("↻", style = MaterialTheme.typography.titleLarge)
            }
            if (occurrenceDates.isNotEmpty()) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Text(
                month.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-BR")).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelLarge
            )
            HabitMonthGrid(action, month, today, viewModel)
        }
    }
}

@Composable
private fun HabitMonthGrid(action: ActionEntity, month: YearMonth, today: LocalDate, viewModel: ActionViewModel) {
    Row(Modifier.fillMaxWidth()) {
        listOf("S", "T", "Q", "Q", "S", "S", "D").forEach { label ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    val cells = MutableList<LocalDate?>(month.atDay(1).dayOfWeek.value - 1) { null }
    repeat(month.lengthOfMonth()) { cells += month.atDay(it + 1) }
    while (cells.size % 7 != 0) cells += null

    cells.chunked(7).forEach { week ->
        Row(Modifier.fillMaxWidth()) {
            week.forEach { date ->
                if (date == null) {
                    Spacer(Modifier.weight(1f).height(42.dp))
                } else {
                    val occurs = RecurrenceCalculator.occursOn(action, date)
                    val done = occurs && viewModel.isCompletedOn(action, date)
                    val future = date.isAfter(today)
                    var cellModifier = Modifier.weight(1f).height(42.dp).padding(2.dp)
                    if (occurs && !future) cellModifier = cellModifier.clickable { viewModel.toggleOccurrence(action, date) }
                    Card(
                        modifier = cellModifier,
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (done) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(if (done) "✓" else if (!occurs) "·" else " ", style = MaterialTheme.typography.labelMedium)
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
