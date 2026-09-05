package com.luminor.actionbox.ui.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luminor.actionbox.ActionViewModel
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.RecurrenceCalculator
import com.luminor.actionbox.ui.components.EmptyState
import com.luminor.actionbox.ui.components.SmartCapture
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(viewModel: ActionViewModel, onSettings: () -> Unit) {
    val all by viewModel.all.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val scheduledToday = all
        .filter { it.type in setOf(ActionType.TASK.name, ActionType.REMINDER.name, ActionType.EVENT.name, ActionType.LIST.name) }
        .filter { RecurrenceCalculator.occursOn(it, today) }
        .sortedBy { it.scheduledAt ?: Long.MAX_VALUE }
    val inbox = all
        .filter { it.type == ActionType.TASK.name && it.scheduledAt == null && it.status == ActionStatus.PENDING.name }
        .take(4)

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = 920.dp)
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Hoje", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text(
                            today.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("pt", "BR"))).replaceFirstChar { it.uppercase() },
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = onSettings) { Text("Ajustes") }
                }
            }

            item { SmartCapture(viewModel = viewModel) }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryCard("Hoje", scheduledToday.size.toString(), Modifier.weight(1f))
                    SummaryCard("Concluídas", scheduledToday.count { viewModel.isCompletedOn(it, today) }.toString(), Modifier.weight(1f))
                    SummaryCard("Sem data", inbox.size.toString(), Modifier.weight(1f))
                }
            }

            item {
                Text("Seu dia", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            }

            if (scheduledToday.isEmpty()) {
                item { EmptyState("✓", "Dia livre", "Quando algo tiver data, aparecerá aqui.") }
            } else {
                items(scheduledToday.size, key = { scheduledToday[it].id }) { index ->
                    TodayActionCard(scheduledToday[index], today, viewModel)
                }
            }

            if (inbox.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text("Sem data", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("Coisas que você quer fazer, mas ainda não colocou na agenda.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(inbox.size, key = { "inbox-${inbox[it].id}" }) { index ->
                    TodayActionCard(inbox[index], today, viewModel)
                }
            }

            item { Spacer(Modifier.height(28.dp)) }
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TodayActionCard(action: ActionEntity, date: LocalDate, viewModel: ActionViewModel) {
    val completed = viewModel.isCompletedOn(action, date)
    val time = action.scheduledAt?.let {
        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
    }
    val recurring = RecurrenceCalculator.recurrenceType(action).name != "NONE"

    Card(
        modifier = Modifier.fillMaxWidth().clickable { viewModel.toggleOccurrence(action, date) },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(if (completed) "✓" else actionTypeEmoji(action.type), modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    action.title,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (completed) TextDecoration.LineThrough else null
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (time != null) Text(time, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                    if (recurring) Text("Recorrente", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    if (action.projectId != null) Text("Projeto", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text(if (completed) "Feito" else "Marcar", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun actionTypeEmoji(type: String): String = when (type) {
    ActionType.REMINDER.name -> "⏰"
    ActionType.EVENT.name -> "◷"
    ActionType.LIST.name -> "☑"
    else -> "○"
}
