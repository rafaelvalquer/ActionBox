package com.luminor.actionbox.ui.capture

import com.luminor.actionbox.R
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
fun CaptureEditor(viewModel: CaptureViewModel, action: DetectedAction) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources

    var sheet by remember { mutableStateOf<EditorSheet?>(null) }
    val dateLabel = action.scheduledAt?.toLocalDate()?.let {
        when (it) {
            LocalDate.now() -> textResources.getString(R.string.text_hoje)
            LocalDate.now().plusDays(1) -> textResources.getString(R.string.text_amanha)
            else -> it.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        }
    } ?: textResources.getString(R.string.text_sem_data)
    val timeLabel = action.scheduledAt?.toLocalTime()?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: textResources.getString(R.string.text_sem_hora)
    val recurrenceLabel = when (action.recurrenceType) {
        RecurrenceType.NONE -> textResources.getString(R.string.text_nao_repetir)
        RecurrenceType.DAILY -> textResources.getString(R.string.text_todo_dia)
        RecurrenceType.WEEKLY -> textResources.getString(R.string.text_semanal)
        RecurrenceType.MONTHLY -> textResources.getString(R.string.text_mensal)
    }
    val reminderLabel = when (action.reminderMinutes) {
        null -> textResources.getString(R.string.text_sem_aviso)
        0 -> textResources.getString(R.string.text_na_hora)
        10 -> textResources.getString(R.string.text_10_min_antes)
        30 -> textResources.getString(R.string.text_30_min_antes)
        60 -> textResources.getString(R.string.text_1_hora_antes)
        else -> textResources.getString(R.string.text_min_antes , action.reminderMinutes)
    }

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        DetailRow(textResources.getString(R.string.text_tipo), action.type.label) { sheet = EditorSheet.TYPE }
        if (action.type in listOf(ActionType.TASK, ActionType.REMINDER, ActionType.EVENT, ActionType.LIST)) {
            DetailRow(textResources.getString(R.string.text_data), dateLabel) { sheet = EditorSheet.DATE }
            DetailRow(textResources.getString(R.string.text_hora), timeLabel) { sheet = EditorSheet.TIME }
            DetailRow(textResources.getString(R.string.text_aviso), reminderLabel) { sheet = EditorSheet.REMINDER }
            DetailRow(textResources.getString(R.string.text_repetir), recurrenceLabel) { sheet = EditorSheet.RECURRENCE }
        }
        if (action.type == ActionType.TASK || action.type == ActionType.PROJECT) {
            DetailRow(textResources.getString(R.string.text_prioridade), priorityLabel(action.priority)) { sheet = EditorSheet.PRIORITY }
        }
        if (action.type == ActionType.LIST || action.type == ActionType.PROJECT) {
            DetailRow(textResources.getString(R.string.text_itens), "${action.items.size}") { sheet = EditorSheet.ITEMS }
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
                ActionButton(textResources.getString(R.string.text_concluir), onClick = { sheet = null })
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
private fun TypeEditor(viewModel: CaptureViewModel, action: DetectedAction) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources

    Text(textResources.getString(R.string.text_tipo_de_acao), style = MaterialTheme.typography.titleLarge)
    val types = listOf(ActionType.TASK, ActionType.REMINDER, ActionType.EVENT, ActionType.NOTE, ActionType.LIST, ActionType.PROJECT)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(types) { type -> ActionChip(type.label, action.type == type) { viewModel.chooseType(type) } }
    }
}

@Composable
private fun DateEditor(viewModel: CaptureViewModel, action: DetectedAction) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources

    var custom by remember(action.scheduledAt?.toLocalDate()) { mutableStateOf(action.scheduledAt?.toLocalDate()?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")).orEmpty()) }
    Text(textResources.getString(R.string.text_data), style = MaterialTheme.typography.titleLarge)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { ActionChip(textResources.getString(R.string.text_hoje), action.scheduledAt?.toLocalDate() == LocalDate.now()) { viewModel.setDetectedDate(LocalDate.now()) } }
        item { ActionChip(textResources.getString(R.string.text_amanha), action.scheduledAt?.toLocalDate() == LocalDate.now().plusDays(1)) { viewModel.setDetectedDate(LocalDate.now().plusDays(1)) } }
        item { ActionChip(textResources.getString(R.string.text_sem_data), action.scheduledAt == null) { viewModel.setDetectedDate(null) } }
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
private fun TimeEditor(viewModel: CaptureViewModel, action: DetectedAction) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources

    var custom by remember(action.scheduledAt?.toLocalTime()) { mutableStateOf(action.scheduledAt?.toLocalTime()?.format(DateTimeFormatter.ofPattern("HH:mm")).orEmpty()) }
    Text(textResources.getString(R.string.text_hora), style = MaterialTheme.typography.titleLarge)
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
private fun ReminderEditor(viewModel: CaptureViewModel, action: DetectedAction) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources

    Text(textResources.getString(R.string.text_aviso), style = MaterialTheme.typography.titleLarge)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val values = listOf(null to textResources.getString(R.string.text_sem_aviso), 0 to textResources.getString(R.string.text_na_hora), 10 to textResources.getString(R.string.text_10_min), 30 to textResources.getString(R.string.text_30_min), 60 to textResources.getString(R.string.text_1_hora))
        items(values) { (value, label) -> ActionChip(label, action.reminderMinutes == value) { viewModel.setReminderMinutes(value) } }
    }
}

@Composable
private fun RecurrenceEditor(viewModel: CaptureViewModel, action: DetectedAction) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources

    Text(textResources.getString(R.string.text_repetir), style = MaterialTheme.typography.titleLarge)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val values = listOf(RecurrenceType.NONE to textResources.getString(R.string.text_nao), RecurrenceType.DAILY to textResources.getString(R.string.text_todo_dia), RecurrenceType.WEEKLY to textResources.getString(R.string.text_semanal), RecurrenceType.MONTHLY to textResources.getString(R.string.text_mensal))
        items(values) { (value, label) -> ActionChip(label, action.recurrenceType == value) { viewModel.setRecurrence(value) } }
    }
    if (action.recurrenceType == RecurrenceType.WEEKLY) {
        Text(textResources.getString(R.string.text_dias_da_semana), style = MaterialTheme.typography.labelLarge)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val days = listOf(1 to "S", 2 to "T", 3 to "Q", 4 to "Q", 5 to "S", 6 to "S", 7 to "D")
            items(days) { (day, label) -> ActionChip(label, day in action.recurrenceDays) { viewModel.toggleRecurrenceDay(day) } }
        }
    }
}

@Composable
private fun PriorityEditor(viewModel: CaptureViewModel, action: DetectedAction) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources

    Text(textResources.getString(R.string.text_prioridade), style = MaterialTheme.typography.titleLarge)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(ActionPriority.entries) { priority -> ActionChip(priorityLabel(priority), action.priority == priority) { viewModel.setPriority(priority) } }
    }
}

@Composable
private fun ItemsEditor(viewModel: CaptureViewModel, action: DetectedAction) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources

    var newItem by remember { mutableStateOf("") }
    Text(textResources.getString(R.string.text_itens), style = MaterialTheme.typography.titleLarge)
    action.items.forEachIndexed { index, item ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("• $item", modifier = Modifier.weight(1f))
            TextButton(onClick = { viewModel.removeDetectedItem(index) }) { Text(textResources.getString(R.string.text_remover)) }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = newItem,
            onValueChange = { newItem = it },
            modifier = Modifier.weight(1f),
            label = { Text(textResources.getString(R.string.text_novo_item)) },
            singleLine = true,
            shape = MaterialTheme.shapes.medium
        )
        TextButton(onClick = { viewModel.addDetectedItem(newItem); newItem = "" }) { Text(textResources.getString(R.string.text_adicionar)) }
    }
}

@Composable
private fun priorityLabel(priority: ActionPriority) = when (priority) {
    ActionPriority.LOW -> androidx.compose.ui.res.stringResource(R.string.text_baixa)
    ActionPriority.NORMAL -> androidx.compose.ui.res.stringResource(R.string.text_normal)
    ActionPriority.HIGH -> androidx.compose.ui.res.stringResource(R.string.text_alta)
}
