package com.luminor.actionbox.ui.actions

import com.luminor.actionbox.R
import androidx.activity.compose.BackHandler
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
import com.luminor.actionbox.ui.actions.ActionEditorViewModel
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.domain.ActionPriority
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.OrganizationOwnerType
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
import com.luminor.actionbox.ui.relations.RelatedNotesSection
import com.luminor.actionbox.ui.tags.TagPickerBottomSheet
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private enum class EditorSheet { TYPE, DATE, TIME, RECURRENCE, PRIORITY, NOTES }

@Composable
fun ActionEditorScreen(
    viewModel: ActionEditorViewModel,
    action: ActionEntity,
    onBack: () -> Unit,
    onNoteOpen: (Long) -> Unit = {}
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val links by viewModel.contentLinks.collectAsStateWithLifecycle()
    val allTags by viewModel.tags.collectAsStateWithLifecycle()
    val tagRefs by viewModel.tagRefs.collectAsStateWithLifecycle()
    val initial = remember(action.id, action.updatedAt) { ActionEditState.from(action) }
    var edit by remember(action.id, action.updatedAt) { mutableStateOf(initial) }
    var sheet by remember { mutableStateOf<EditorSheet?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var tagsOpen by remember { mutableStateOf(false) }
    val dirty = edit != initial
    val convertible = initial.type in setOf(ActionType.TASK, ActionType.REMINDER, ActionType.EVENT)
    val selectedTagIds = tagRefs
        .filter { it.ownerType == OrganizationOwnerType.ACTION && it.ownerId == action.id }
        .map { it.tagId }
        .toSet()

    fun requestBack() {
        if (dirty) showDiscardDialog = true else onBack()
    }

    fun save() {
        if (edit.title.isBlank()) {
            viewModel.showMessage(textResources.getString(R.string.text_informe_um_titulo_para_a_acao))
            return
        }
        if (edit.type == ActionType.REMINDER && (edit.date == null || edit.time == null)) {
            viewModel.showMessage(textResources.getString(R.string.text_lembretes_precisam_de_data_e_horario))
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
                            text = { Text(textResources.getString(R.string.text_duplicar)) },
                            onClick = { menuExpanded = false; viewModel.duplicateAction(context, action) }
                        )
                        DropdownMenuItem(
                            text = { Text(textResources.getString(R.string.text_arquivar)) },
                            onClick = { menuExpanded = false; viewModel.archive(action.id); onBack() }
                        )
                        DropdownMenuItem(
                            text = { Text(textResources.getString(R.string.text_mover_para_lixeira)) },
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
                    EditorRow(ActionBoxIcons.Agenda, textResources.getString(R.string.text_data), dateLabel(edit.date)) { sheet = EditorSheet.DATE }
                    EditorDivider()
                    EditorRow(ActionBoxIcons.Time, textResources.getString(R.string.text_hora_e_lembrete), timeReminderLabel(edit.time, edit.reminderMinutes)) { sheet = EditorSheet.TIME }
                    EditorDivider()
                    EditorRow(ActionBoxIcons.Repeat, textResources.getString(R.string.text_repetir), recurrenceLabel(edit.recurrenceType, edit.recurrenceDays)) { sheet = EditorSheet.RECURRENCE }
                    EditorDivider()
                    EditorRow(ActionBoxIcons.Tune, textResources.getString(R.string.text_prioridade), priorityLabel(edit.priority)) { sheet = EditorSheet.PRIORITY }
                    EditorDivider()
                    EditorRow(ActionBoxIcons.forType(ActionType.NOTE.name), textResources.getString(R.string.text_notas), if (edit.description.isNullOrBlank()) textResources.getString(R.string.text_adicionar) else textResources.getString(R.string.text_editado)) { sheet = EditorSheet.NOTES }
                    EditorDivider()
                }
            }
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(textResources.getString(R.string.text_tags), style = MaterialTheme.typography.titleMedium)
                    if (selectedTagIds.isEmpty()) {
                        Text(textResources.getString(R.string.text_sem_tags), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            allTags.filter { it.id in selectedTagIds }.forEach { tag ->
                                Text("#${tag.name}", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    TextButton(onClick = { tagsOpen = true }) { Text(if (selectedTagIds.isEmpty()) textResources.getString(R.string.text_adicionar_tags) else textResources.getString(R.string.text_editar_tags)) }
                }
            }
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    RelatedNotesSection(
                        ownerType = OrganizationOwnerType.ACTION,
                        ownerId = action.id,
                        notes = notes,
                        links = links,
                        onLink = { viewModel.linkNote(it, OrganizationOwnerType.ACTION, action.id) },
                        onUnlink = viewModel::unlinkContentLink,
                        onOpenNote = onNoteOpen
                    )
                }
            }
            item {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!dirty && action.type == ActionType.EVENT.name) {
                        TextButton(onClick = { viewModel.addToSystemCalendar(context, action) }, modifier = Modifier.fillMaxWidth()) {
                            Text(textResources.getString(R.string.text_adicionar_ao_calendario_do_celular))
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
                            Text(if (viewModel.isCompletedOn(action, LocalDate.now())) textResources.getString(R.string.text_marcar_como_pendente) else textResources.getString(R.string.text_concluir_acao))
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

    if (tagsOpen) {
        TagPickerBottomSheet(
            tags = allTags,
            selectedIds = selectedTagIds,
            onToggle = { tag ->
                viewModel.setTagsForOwner(
                    OrganizationOwnerType.ACTION,
                    action.id,
                    if (tag.id in selectedTagIds) selectedTagIds - tag.id else selectedTagIds + tag.id
                )
            },
            onCreate = { viewModel.createAndAttachTag(OrganizationOwnerType.ACTION, action.id, it) },
            onDismiss = { tagsOpen = false }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(textResources.getString(R.string.text_alteracoes_nao_salvas)) },
            text = { Text(textResources.getString(R.string.text_voce_possui_alteracoes_nao_salvas)) },
            dismissButton = { TextButton(onClick = { showDiscardDialog = false }) { Text(textResources.getString(R.string.text_continuar_editando)) } },
            confirmButton = { TextButton(onClick = { showDiscardDialog = false; onBack() }) { Text(textResources.getString(R.string.text_descartar)) } }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(textResources.getString(R.string.text_mover_para_a_lixeira , action.title)) },
            text = { Text(textResources.getString(R.string.text_este_item_podera_ser_restaurado_durante_30_dias)) },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(textResources.getString(R.string.text_cancelar)) } },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; viewModel.delete(action.id); onBack() }) {
                    Text(textResources.getString(R.string.text_mover), color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}

@Composable
private fun dateLabel(date: LocalDate?): String = date?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: androidx.compose.ui.res.stringResource(R.string.text_sem_data)

@Composable
private fun timeReminderLabel(time: LocalTime?, reminder: Int?): String {
    if (time == null) return androidx.compose.ui.res.stringResource(R.string.text_nao)
    val timeText = time.format(DateTimeFormatter.ofPattern("HH:mm"))
    val reminderText = when (reminder) {
        null -> androidx.compose.ui.res.stringResource(R.string.text_sem_lembrete)
        0 -> androidx.compose.ui.res.stringResource(R.string.text_na_hora_70)
        10 -> androidx.compose.ui.res.stringResource(R.string.text_10_min_antes)
        30 -> androidx.compose.ui.res.stringResource(R.string.text_30_min_antes)
        60 -> androidx.compose.ui.res.stringResource(R.string.text_1_h_antes)
        1440 -> androidx.compose.ui.res.stringResource(R.string.text_1_dia_antes)
        else -> androidx.compose.ui.res.stringResource(R.string.text_min_antes , reminder)
    }
    return "$timeText · $reminderText"
}

@Composable
private fun recurrenceLabel(type: RecurrenceType, days: Set<Int>): String = when (type) {
    RecurrenceType.NONE -> androidx.compose.ui.res.stringResource(R.string.text_nao)
    RecurrenceType.DAILY -> androidx.compose.ui.res.stringResource(R.string.text_todo_dia)
    RecurrenceType.MONTHLY -> androidx.compose.ui.res.stringResource(R.string.text_todo_mes)
    RecurrenceType.WEEKLY -> {
        val names = mapOf(1 to androidx.compose.ui.res.stringResource(R.string.text_seg), 2 to androidx.compose.ui.res.stringResource(R.string.text_ter), 3 to androidx.compose.ui.res.stringResource(R.string.text_qua), 4 to androidx.compose.ui.res.stringResource(R.string.text_qui), 5 to androidx.compose.ui.res.stringResource(R.string.text_sex), 6 to androidx.compose.ui.res.stringResource(R.string.text_sab), 7 to androidx.compose.ui.res.stringResource(R.string.text_dom))
        if (days.isEmpty()) androidx.compose.ui.res.stringResource(R.string.text_toda_semana) else days.sorted().mapNotNull(names::get).joinToString(", ")
    }
}

@Composable
private fun priorityLabel(priority: ActionPriority): String = when (priority) {
    ActionPriority.LOW -> androidx.compose.ui.res.stringResource(R.string.text_baixa)
    ActionPriority.NORMAL -> androidx.compose.ui.res.stringResource(R.string.text_normal)
    ActionPriority.HIGH -> androidx.compose.ui.res.stringResource(R.string.text_alta)
}
