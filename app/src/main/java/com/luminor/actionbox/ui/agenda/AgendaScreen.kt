package com.luminor.actionbox.ui.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luminor.actionbox.ActionViewModel
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.RecurrenceCalculator
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun AgendaScreen(viewModel: ActionViewModel) {
    val all by viewModel.all.collectAsStateWithLifecycle()
    var mode by rememberSaveable { mutableStateOf("MONTH") }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf(LocalDate.now()) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().widthIn(max = 980.dp).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Agenda", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text("Tudo que tem dia, horário ou recorrência.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = mode == "MONTH", onClick = { mode = "MONTH" }, label = { Text("Mês") })
                        FilterChip(selected = mode == "LIST", onClick = { mode = "LIST" }, label = { Text("Lista") })
                    }
                }
            }

            if (mode == "MONTH") {
                item { MonthView(month, selected, all, viewModel, onMonth = { month = it }, onSelect = { selected = it }) }
            } else {
                val today = LocalDate.now()
                var rendered = 0
                repeat(31) { offset ->
                    val date = today.plusDays(offset.toLong())
                    val entries = entriesFor(date, all)
                    if (entries.isNotEmpty()) {
                        rendered++
                        item(key = "agenda-$date") { DayDetails(date, entries, viewModel) }
                    }
                }
                val undated = all.filter { it.type == ActionType.TASK.name && it.scheduledAt == null && it.status == ActionStatus.PENDING.name }
                if (undated.isNotEmpty()) {
                    item {
                        Text("Sem data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Tarefas que ainda não foram colocadas no calendário.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    undated.forEach { action ->
                        item(key = "undated-${action.id}") { AgendaActionRow(action, today, viewModel) }
                    }
                }
                if (rendered == 0 && undated.isEmpty()) {
                    item { Text("Sua agenda está livre nos próximos 30 dias.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }

            item { Spacer(Modifier.padding(bottom = 24.dp)) }
        }
    }
}

@Composable
private fun MonthView(
    month: YearMonth,
    selected: LocalDate,
    all: List<ActionEntity>,
    viewModel: ActionViewModel,
    onMonth: (YearMonth) -> Unit,
    onSelect: (LocalDate) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { onMonth(month.minusMonths(1)) }) { Text("‹") }
                    Text(
                        "${month.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-BR")).replaceFirstChar { it.uppercase() }} ${month.year}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = { onMonth(month.plusMonths(1)) }) { Text("›") }
                }
                Row(Modifier.fillMaxWidth()) {
                    listOf("S", "T", "Q", "Q", "S", "S", "D").forEach { day ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(day, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                val first = month.atDay(1)
                val cells = MutableList<LocalDate?>(first.dayOfWeek.value - 1) { null }
                repeat(month.lengthOfMonth()) { cells += month.atDay(it + 1) }
                while (cells.size % 7 != 0) cells += null

                cells.chunked(7).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            if (date == null) Spacer(Modifier.weight(1f).aspectRatio(1f))
                            else DayCell(date, selected, entriesFor(date, all), viewModel, Modifier.weight(1f)) { onSelect(date) }
                        }
                    }
                }
            }
        }

        val selectedEntries = entriesFor(selected, all)
        DayDetails(selected, selectedEntries, viewModel)
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    selected: LocalDate,
    entries: List<ActionEntity>,
    viewModel: ActionViewModel,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val isToday = date == LocalDate.now()
    val done = entries.isNotEmpty() && entries.all { viewModel.isCompletedOn(it, date) }
    Box(modifier.aspectRatio(1f).padding(3.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(if (date == selected) MaterialTheme.colorScheme.primary else if (isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(date.dayOfMonth.toString(), color = if (date == selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
            }
            if (entries.isNotEmpty()) {
                Text(if (done) "✓" else "• ${entries.size}", color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun DayDetails(date: LocalDate, entries: List<ActionEntity>, viewModel: ActionViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            date.format(DateTimeFormatter.ofPattern("EEEE, dd/MM", Locale.forLanguageTag("pt-BR"))).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (entries.isEmpty()) Text("Nada planejado para este dia.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        entries.forEach { AgendaActionRow(it, date, viewModel) }
    }
}

@Composable
private fun AgendaActionRow(action: ActionEntity, date: LocalDate, viewModel: ActionViewModel) {
    val completed = viewModel.isCompletedOn(action, date)
    Card(
        modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleOccurrence(action, date) },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (completed) "✓" else when (action.type) { ActionType.EVENT.name -> "◷"; ActionType.REMINDER.name -> "⏰"; ActionType.LIST.name -> "☑"; else -> "○" })
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(action.title, fontWeight = FontWeight.Medium, textDecoration = if (completed) TextDecoration.LineThrough else null)
                action.scheduledAt?.let {
                    val time = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
                    Text(time, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (RecurrenceCalculator.recurrenceType(action).name != "NONE") Text("↻", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun entriesFor(date: LocalDate, all: List<ActionEntity>): List<ActionEntity> = all
    .filter { it.type in setOf(ActionType.TASK.name, ActionType.REMINDER.name, ActionType.EVENT.name, ActionType.LIST.name) }
    .filter { RecurrenceCalculator.occursOn(it, date) }
    .sortedBy { it.scheduledAt ?: Long.MAX_VALUE }
