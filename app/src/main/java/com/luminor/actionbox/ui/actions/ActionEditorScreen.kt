package com.luminor.actionbox.ui.actions

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luminor.actionbox.ActionViewModel
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.domain.ActionPriority
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.RecurrenceType
import com.luminor.actionbox.ui.actions.components.EditorDivider
import com.luminor.actionbox.ui.actions.components.EditorHeader
import com.luminor.actionbox.ui.actions.components.EditorRow
import com.luminor.actionbox.ui.actions.components.EditorTitle
import com.luminor.actionbox.ui.actions.components.EditorTypeChip
import com.luminor.actionbox.ui.actions.sheets.ActionTypeSheet
import com.luminor.actionbox.ui.actions.sheets.DateSheet
import com.luminor.actionbox.ui.actions.sheets.NotesSheet
import com.luminor.actionbox.ui.actions.sheets.PrioritySheet
import com.luminor.actionbox.ui.actions.sheets.RecurrenceSheet
import com.luminor.actionbox.ui.actions.sheets.TimeReminderSheet
import com.luminor.actionbox.ui.designsystem.ActionBoxIcons
import com.luminor.actionbox.ui.motion.SharedKeys
import com.luminor.actionbox.ui.motion.actionSharedBounds
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private enum class EditorSheet { TYPE, DATE, TIME, RECURRENCE, PRIORITY, NOTES }

@Composable
fun ActionEditorScreen(viewModel: ActionViewModel, action: ActionEntity, onBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val initial = remember(action.id) { ActionEditState.from(action) }
    var edit by remember(action.id) { mutableStateOf(initial) }
    var sheet by remember { mutableStateOf<EditorSheet?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dirty = edit != initial
    val convertible = initial.type in setOf(ActionType.TASK, ActionType.REMINDER, ActionType.EVENT)

    fun requestBack() {
        if (dirty) showDiscardDialog = true else onBack()
    }

    fun save() {
        if (edit.title.isBlank()) {
            viewModel.showMessage("Informe um título para a ação.")
            return
        }
        if (edit.type == ActionType.REMINDER && (edit.date == null || edit.time == null)) {
            viewModel.showMessage("Lembretes precisam de data e horário.")
            return
        }
        if (settings.hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.updateAction(context, action, edit.toEntity(action))
        onBack()
    }

    BackHandler(enabled = dirty) { showDiscardDialog = true }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .statusBarsPadding()
                .actionSharedBounds(SharedKeys.action(action.id)),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Box {
                    EditorHeader(
                        dirty = dirty,
                        onBack = ::requestBack,
                        onCancel = { edit = initial },
                        onSave = ::save,
                        onMore = { menuExpanded = true }
                    )
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }, modifier = Modifier.align(Alignment.TopEnd)) {
                        DropdownMenuItem(
                            text = { Text("Duplicar") },
                            onClick = { menuExpanded = false; viewModel.duplicateAction(context, action) }
                        )
                        DropdownMenuItem(
                            text = { Text("Arquivar") },
                            onClick = { menuExpanded = false; viewModel.archive(action.id); onBack() }
                        )
                        DropdownMenuItem(
                            text = { Text("Excluir") },
                            onClick = { menuExpanded = false; showDeleteDialog = true }
                        )
                    }
                }
            }
            item {
                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    EditorTypeChip(type = edit.type, locked = !convertible, onClick = { sheet = EditorSheet.TYPE })
                    EditorTitle(value = edit.title, onValueChange = { edit = edit.copy(title = it) }, modifier = Modifier.fillMaxWidth())
                }
            }
            item { Spacer(Modifier.height(22.dp)) }
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    EditorRow(ActionBoxIcons.Agenda, "Data", dateLabel(edit.date)) { sheet = EditorSheet.DATE }
                    EditorDivider()
                    EditorRow(ActionBoxIcons.Time, "Hora e lembrete", timeReminderLabel(edit.time, edit.reminderMinutes)) { sheet = EditorSheet.TIME }
                    EditorDivider()
                    EditorRow(ActionBoxIcons.Repeat, "Repetir", recurrenceLabel(edit.recurrenceType, edit.recurrenceDays)) { sheet = EditorSheet.RECURRENCE }
                    EditorDivider()
                    EditorRow(ActionBoxIcons.Tune, "Prioridade", priorityLabel(edit.priority)) { sheet = EditorSheet.PRIORITY }
                    EditorDivider()
                    EditorRow(ActionBoxIcons.forType(ActionType.NOTE.name), "Notas", if (edit.description.isNullOrBlank()) "Adicionar" else "Editado") { sheet = EditorSheet.NOTES }
                    EditorDivider()
                }
            }
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!dirty && action.type == ActionType.EVENT.name) {
                        TextButton(onClick = { viewModel.addToSystemCalendar(context, action) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Adicionar ao calendário do celular")
                        }
                    }
                    if (!dirty && action.status != ActionStatus.ARCHIVED.name) {
                        TextButton(
                            onClick = {
                                if (settings.hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.toggleOccurrence(action, LocalDate.now())
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (viewModel.isCompletedOn(action, LocalDate.now())) "Marcar como pendente" else "Concluir ação")
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }

    when (sheet) {
        EditorSheet.TYPE -> ActionTypeSheet(
            current = edit.type,
            canConvert = convertible,
            onDismiss = { sheet = null },
            onSelect = { newType ->
                edit = when (newType) {
                    ActionType.TASK -> edit.copy(type = newType, reminderMinutes = if (edit.type == ActionType.REMINDER) null else edit.reminderMinutes)
                    ActionType.REMINDER -> edit.copy(
                        type = newType,
                        date = edit.date ?: LocalDate.now(),
                        time = edit.time ?: LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0),
                        reminderMinutes = edit.reminderMinutes ?: 0
                    )
                    else -> edit.copy(type = newType)
                }
            }
        )
        EditorSheet.DATE -> DateSheet(edit.date, onDismiss = { sheet = null }) { selected ->
            edit = edit.copy(date = selected, time = if (selected == null) null else edit.time, reminderMinutes = if (selected == null) null else edit.reminderMinutes)
        }
        EditorSheet.TIME -> TimeReminderSheet(edit.time, edit.reminderMinutes, onDismiss = { sheet = null }) { time, reminder ->
            edit = edit.copy(
                date = if (time != null && edit.date == null) LocalDate.now() else edit.date,
                time = time,
                reminderMinutes = if (time == null) null else reminder
            )
        }
        EditorSheet.RECURRENCE -> RecurrenceSheet(
            initialType = edit.recurrenceType,
            initialDays = edit.recurrenceDays,
            defaultDay = (edit.date ?: LocalDate.now()).dayOfWeek.value,
            onDismiss = { sheet = null }
        ) { type, days -> edit = edit.copy(recurrenceType = type, recurrenceDays = days) }
        EditorSheet.PRIORITY -> PrioritySheet(edit.priority, onDismiss = { sheet = null }) { edit = edit.copy(priority = it) }
        EditorSheet.NOTES -> NotesSheet(edit.description, onDismiss = { sheet = null }) { edit = edit.copy(description = it) }
        null -> Unit
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Alterações não salvas") },
            text = { Text("Você possui alterações não salvas.") },
            dismissButton = { TextButton(onClick = { showDiscardDialog = false }) { Text("Continuar editando") } },
            confirmButton = { TextButton(onClick = { showDiscardDialog = false; onBack() }) { Text("Descartar") } }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir “${action.title}”?") },
            text = { Text("Esta ação será removida do ActionBox.") },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; viewModel.delete(action.id); onBack() }) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}

private fun dateLabel(date: LocalDate?): String = date?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "Sem data"

private fun timeReminderLabel(time: LocalTime?, reminder: Int?): String {
    if (time == null) return "Não"
    val timeText = time.format(DateTimeFormatter.ofPattern("HH:mm"))
    val reminderText = when (reminder) {
        null -> "sem lembrete"
        0 -> "na hora"
        10 -> "10 min antes"
        30 -> "30 min antes"
        60 -> "1 h antes"
        1440 -> "1 dia antes"
        else -> "$reminder min antes"
    }
    return "$timeText · $reminderText"
}

private fun recurrenceLabel(type: RecurrenceType, days: Set<Int>): String = when (type) {
    RecurrenceType.NONE -> "Não"
    RecurrenceType.DAILY -> "Todo dia"
    RecurrenceType.MONTHLY -> "Todo mês"
    RecurrenceType.WEEKLY -> {
        val names = mapOf(1 to "Seg", 2 to "Ter", 3 to "Qua", 4 to "Qui", 5 to "Sex", 6 to "Sáb", 7 to "Dom")
        if (days.isEmpty()) "Toda semana" else days.sorted().mapNotNull(names::get).joinToString(", ")
    }
}

private fun priorityLabel(priority: ActionPriority): String = when (priority) {
    ActionPriority.LOW -> "Baixa"
    ActionPriority.NORMAL -> "Normal"
    ActionPriority.HIGH -> "Alta"
}
