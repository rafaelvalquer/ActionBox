package com.luminor.actionbox.ui.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luminor.actionbox.ActionViewModel
import com.luminor.actionbox.domain.ActionPriority
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.DetectedAction
import com.luminor.actionbox.domain.RecurrenceType
import com.luminor.actionbox.ui.designsystem.ActionBoxIcons
import com.luminor.actionbox.ui.designsystem.components.ActionButton
import com.luminor.actionbox.ui.designsystem.components.ActionChip
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private enum class EditorSheet { TYPE, DATE, TIME, REMINDER, RECURRENCE, PRIORITY, ITEMS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureEditor(viewModel: ActionViewModel, action: DetectedAction) {
    var sheet by remember { mutableStateOf<EditorSheet?>(null) }
    val dateLabel = action.scheduledAt?.toLocalDate()?.let {
        when (it) {
            LocalDate.now() -> "Hoje"
            LocalDate.now().plusDays(1) -> "Amanhã"
            else -> it.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        }
    } ?: "Sem data"
    val timeLabel = action.scheduledAt?.toLocalTime()?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "Sem hora"
    val recurrenceLabel = when (action.recurrenceType) {
        RecurrenceType.NONE -> "Não repetir"
        RecurrenceType.DAILY -> "Todo dia"
        RecurrenceType.WEEKLY -> "Semanal"
        RecurrenceType.MONTHLY -> "Mensal"
    }
    val reminderLabel = when (action.reminderMinutes) {
        null -> "Sem aviso"
        0 -> "Na hora"
        10 -> "10 min antes"
        30 -> "30 min antes"
        60 -> "1 hora antes"
        else -> "${action.reminderMinutes} min antes"
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        DetailRow("Tipo", action.type.label) { sheet = EditorSheet.TYPE }
        if (action.type in listOf(ActionType.TASK, ActionType.REMINDER, ActionType.EVENT, ActionType.LIST)) {
            DetailRow("Data", dateLabel) { sheet = EditorSheet.DATE }
            DetailRow("Hora", timeLabel) { sheet = EditorSheet.TIME }
            DetailRow("Aviso", reminderLabel) { sheet = EditorSheet.REMINDER }
            DetailRow("Repetir", recurrenceLabel) { sheet = EditorSheet.RECURRENCE }
        }
        if (action.type == ActionType.TASK || action.type == ActionType.PROJECT) {
            DetailRow("Prioridade", priorityLabel(action.priority)) { sheet = EditorSheet.PRIORITY }
        }
        if (action.type == ActionType.LIST || action.type == ActionType.PROJECT) {
            DetailRow("Itens", "${action.items.size}") { sheet = EditorSheet.ITEMS }
        }
    }

    if (sheet != null) {
        ModalBottomSheet(onDismissRequest = { sheet = null }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                when (sheet) {
                    EditorSheet.TYPE -> TypeEditor(viewModel, action)
                    EditorSheet.DATE -> DateEditor(viewModel, action)
                    EditorSheet.TIME -> TimeEditor(viewModel, action)
                    EditorSheet.REMINDER -> ReminderEditor(viewModel, action)
                    EditorSheet.RECURRENCE -> RecurrenceEditor(viewModel, action)
                    EditorSheet.PRIORITY -> PriorityEditor(viewModel, action)
                    EditorSheet.ITEMS -> ItemsEditor(viewModel, action)
                    null -> Unit
                }
                ActionButton("Concluir", onClick = { sheet = null })
                androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 10.dp))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(ActionBoxIcons.Chevron, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TypeEditor(viewModel: ActionViewModel, action: DetectedAction) {
    Text("Tipo de ação", style = MaterialTheme.typography.titleLarge)
    val types = listOf(ActionType.TASK, ActionType.REMINDER, ActionType.EVENT, ActionType.NOTE, ActionType.LIST, ActionType.PROJECT)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(types) { type -> ActionChip(type.label, action.type == type) { viewModel.chooseType(type) } }
    }
}

@Composable
private fun DateEditor(viewModel: ActionViewModel, action: DetectedAction) {
    var custom by remember(action.scheduledAt?.toLocalDate()) { mutableStateOf(action.scheduledAt?.toLocalDate()?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")).orEmpty()) }
    Text("Data", style = MaterialTheme.typography.titleLarge)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { ActionChip("Hoje", action.scheduledAt?.toLocalDate() == LocalDate.now()) { viewModel.setDetectedDate(LocalDate.now()) } }
        item { ActionChip("Amanhã", action.scheduledAt?.toLocalDate() == LocalDate.now().plusDays(1)) { viewModel.setDetectedDate(LocalDate.now().plusDays(1)) } }
        item { ActionChip("Sem data", action.scheduledAt == null) { viewModel.setDetectedDate(null) } }
    }
    OutlinedTextField(
        value = custom,
        onValueChange = { value -> custom = value; if (value.length == 10) viewModel.setDetectedDateText(value) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("dd/mm/aaaa") },
        singleLine = true,
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
private fun TimeEditor(viewModel: ActionViewModel, action: DetectedAction) {
    var custom by remember(action.scheduledAt?.toLocalTime()) { mutableStateOf(action.scheduledAt?.toLocalTime()?.format(DateTimeFormatter.ofPattern("HH:mm")).orEmpty()) }
    Text("Hora", style = MaterialTheme.typography.titleLarge)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("07:00", "09:00", "14:00", "19:00").forEach { value ->
            item { ActionChip(value, custom == value) { custom = value; viewModel.setDetectedTimeText(value) } }
        }
    }
    OutlinedTextField(
        value = custom,
        onValueChange = { value -> custom = value; if (value.length == 5) viewModel.setDetectedTimeText(value) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("HH:mm") },
        singleLine = true,
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
private fun ReminderEditor(viewModel: ActionViewModel, action: DetectedAction) {
    Text("Aviso", style = MaterialTheme.typography.titleLarge)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val values = listOf(null to "Sem aviso", 0 to "Na hora", 10 to "10 min", 30 to "30 min", 60 to "1 hora")
        items(values) { (value, label) -> ActionChip(label, action.reminderMinutes == value) { viewModel.setReminderMinutes(value) } }
    }
}

@Composable
private fun RecurrenceEditor(viewModel: ActionViewModel, action: DetectedAction) {
    Text("Repetir", style = MaterialTheme.typography.titleLarge)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val values = listOf(RecurrenceType.NONE to "Não", RecurrenceType.DAILY to "Todo dia", RecurrenceType.WEEKLY to "Semanal", RecurrenceType.MONTHLY to "Mensal")
        items(values) { (value, label) -> ActionChip(label, action.recurrenceType == value) { viewModel.setRecurrence(value) } }
    }
    if (action.recurrenceType == RecurrenceType.WEEKLY) {
        Text("Dias da semana", style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val days = listOf(1 to "S", 2 to "T", 3 to "Q", 4 to "Q", 5 to "S", 6 to "S", 7 to "D")
            items(days) { (day, label) -> ActionChip(label, day in action.recurrenceDays) { viewModel.toggleRecurrenceDay(day) } }
        }
    }
}

@Composable
private fun PriorityEditor(viewModel: ActionViewModel, action: DetectedAction) {
    Text("Prioridade", style = MaterialTheme.typography.titleLarge)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(ActionPriority.entries) { priority -> ActionChip(priorityLabel(priority), action.priority == priority) { viewModel.setPriority(priority) } }
    }
}

@Composable
private fun ItemsEditor(viewModel: ActionViewModel, action: DetectedAction) {
    var newItem by remember { mutableStateOf("") }
    Text("Itens", style = MaterialTheme.typography.titleLarge)
    action.items.forEachIndexed { index, item ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("• $item", modifier = Modifier.weight(1f))
            TextButton(onClick = { viewModel.removeDetectedItem(index) }) { Text("Remover") }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = newItem,
            onValueChange = { newItem = it },
            modifier = Modifier.weight(1f),
            label = { Text("Novo item") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )
        TextButton(onClick = { viewModel.addDetectedItem(newItem); newItem = "" }) { Text("Adicionar") }
    }
}

private fun priorityLabel(priority: ActionPriority) = when (priority) {
    ActionPriority.LOW -> "Baixa"
    ActionPriority.NORMAL -> "Normal"
    ActionPriority.HIGH -> "Alta"
}
