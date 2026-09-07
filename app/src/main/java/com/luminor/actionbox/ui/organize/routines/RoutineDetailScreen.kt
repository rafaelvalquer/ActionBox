package com.luminor.actionbox.ui.organize.routines

import com.luminor.actionbox.R
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luminor.actionbox.ui.organize.routines.RoutineViewModel
import com.luminor.actionbox.domain.ActionPriority
import com.luminor.actionbox.domain.HabitStreakCalculator
import com.luminor.actionbox.domain.OrganizationOwnerType
import com.luminor.actionbox.domain.RecurrenceType
import com.luminor.actionbox.ui.designsystem.ActionBoxColors
import com.luminor.actionbox.ui.designsystem.ActionBoxIcons
import com.luminor.actionbox.ui.motion.AnimatedCheck
import com.luminor.actionbox.ui.tags.TagPickerBottomSheet
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun RoutineDetailScreen(
    viewModel: RoutineViewModel,
    actionId: Long,
    onBack: () -> Unit
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources

    val context = LocalContext.current
    val all by viewModel.all.collectAsStateWithLifecycle()
    val completions by viewModel.completions.collectAsStateWithLifecycle()
    val rules by viewModel.routineRules.collectAsStateWithLifecycle()
    val allTags by viewModel.tags.collectAsStateWithLifecycle()
    val tagRefs by viewModel.tagRefs.collectAsStateWithLifecycle()
    val action = all.firstOrNull { it.id == actionId } ?: return
    val today = LocalDate.now()
    val month = YearMonth.from(today)
    val occurrences = (1..month.lengthOfMonth())
        .map(month::atDay)
        .filter { !it.isAfter(today) && viewModel.routineOccursOn(action, it) }
    val completed = occurrences.count { viewModel.isCompletedOn(action, it) }
    val progress = if (occurrences.isEmpty()) 0f else completed.toFloat() / occurrences.size
    val streak = HabitStreakCalculator.currentStreak(
        today = today,
        occursOn = { viewModel.routineOccursOn(action, it) },
        isCompleted = { viewModel.isCompletedOn(action, it) }
    )

    var editing by remember(actionId) { mutableStateOf(false) }
    var initialEdit by remember(actionId) { mutableStateOf(RoutineEditState.from(action)) }
    var edit by remember(actionId) { mutableStateOf(initialEdit) }
    var timeText by remember(actionId) { mutableStateOf(edit.time.format(DateTimeFormatter.ofPattern("HH:mm"))) }
    var discardDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    var tagsOpen by remember { mutableStateOf(false) }
    val dirty = edit != initialEdit || timeText != initialEdit.time.format(DateTimeFormatter.ofPattern("HH:mm"))
    val selectedTagIds = tagRefs
        .filter { it.ownerType == OrganizationOwnerType.ACTION && it.ownerId == actionId }
        .map { it.tagId }
        .toSet()

    fun beginEditing() {
        initialEdit = RoutineEditState.from(action)
        edit = initialEdit
        timeText = edit.time.format(DateTimeFormatter.ofPattern("HH:mm"))
        editing = true
    }

    fun requestBack() {
        if (editing && dirty) discardDialog = true
        else if (editing) editing = false
        else onBack()
    }

    BackHandler(enabled = editing) { requestBack() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 760.dp)
            .statusBarsPadding()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = ::requestBack) { Icon(ActionBoxIcons.Back, contentDescription = textResources.getString(R.string.text_voltar)) }
                Text(textResources.getString(R.string.text_rotina), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (editing) {
                    TextButton(onClick = { if (dirty) discardDialog = true else editing = false }) { Text(textResources.getString(R.string.text_cancelar)) }
                } else {
                    TextButton(onClick = ::beginEditing) {
                        Icon(Icons.Rounded.Edit, contentDescription = null)
                        Text(textResources.getString(R.string.text_editar))
                    }
                }
            }
        }

        if (editing) {
            item {
                OutlinedTextField(
                    value = edit.title,
                    onValueChange = { edit = edit.copy(title = it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(textResources.getString(R.string.text_nome_da_rotina)) }
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(textResources.getString(R.string.text_repeticao), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            RecurrenceType.DAILY to textResources.getString(R.string.text_diaria),
                            RecurrenceType.WEEKLY to textResources.getString(R.string.text_semanal),
                            RecurrenceType.MONTHLY to textResources.getString(R.string.text_mensal)
                        ).forEach { (type, label) ->
                            FilterChip(
                                selected = edit.recurrenceType == type,
                                onClick = { edit = edit.copy(recurrenceType = type, recurrenceDays = if (type == RecurrenceType.WEEKLY) edit.recurrenceDays else emptySet()) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
            if (edit.recurrenceType == RecurrenceType.WEEKLY) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(textResources.getString(R.string.text_dias_da_semana), style = MaterialTheme.typography.titleMedium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            listOf(1 to "S", 2 to "T", 3 to "Q", 4 to "Q", 5 to "S", 6 to "S", 7 to "D").forEach { (day, label) ->
                                FilterChip(
                                    selected = day in edit.recurrenceDays,
                                    onClick = {
                                        val days = edit.recurrenceDays.toMutableSet()
                                        if (!days.add(day)) days.remove(day)
                                        edit = edit.copy(recurrenceDays = days)
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = timeText,
                    onValueChange = { timeText = it.take(5) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(textResources.getString(R.string.text_horario)) },
                    placeholder = { Text("19:00") }
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(textResources.getString(R.string.text_lembrete), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf(null to textResources.getString(R.string.text_nao), 0 to textResources.getString(R.string.text_na_hora), 10 to textResources.getString(R.string.text_10_min), 30 to textResources.getString(R.string.text_30_min), 60 to textResources.getString(R.string.text_1_h)).forEach { (minutes, label) ->
                            FilterChip(
                                selected = edit.reminderMinutes == minutes,
                                onClick = { edit = edit.copy(reminderMinutes = minutes) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(textResources.getString(R.string.text_prioridade), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(ActionPriority.LOW to textResources.getString(R.string.text_baixa), ActionPriority.NORMAL to textResources.getString(R.string.text_normal), ActionPriority.HIGH to textResources.getString(R.string.text_alta)).forEach { (priority, label) ->
                            FilterChip(
                                selected = edit.priority == priority,
                                onClick = { edit = edit.copy(priority = priority) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(textResources.getString(R.string.text_pausar_rotina), style = MaterialTheme.typography.titleMedium)
                        Text(textResources.getString(R.string.text_uma_rotina_pausada_mantem_o_historico_mas_deixa_de_aparecer_na_ag), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = edit.paused, onCheckedChange = { edit = edit.copy(paused = it) })
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { if (dirty) discardDialog = true else editing = false }, modifier = Modifier.weight(1f)) { Text(textResources.getString(R.string.text_cancelar)) }
                    Button(
                        onClick = {
                            val parsed = runCatching { LocalTime.parse(timeText, DateTimeFormatter.ofPattern("HH:mm")) }.getOrNull()
                            if (parsed == null) {
                                viewModel.showMessage("Informe um horário válido no formato HH:mm")
                            } else if (edit.recurrenceType == RecurrenceType.WEEKLY && edit.recurrenceDays.isEmpty()) {
                                viewModel.showMessage(textResources.getString(R.string.text_escolha_pelo_menos_um_dia_da_semana))
                            } else {
                                viewModel.saveRoutineEdits(
                                    context = context,
                                    original = action,
                                    title = edit.title,
                                    recurrenceType = edit.recurrenceType,
                                    recurrenceDays = edit.recurrenceDays,
                                    time = parsed,
                                    reminderMinutes = edit.reminderMinutes,
                                    priority = edit.priority,
                                    paused = edit.paused
                                )
                                editing = false
                            }
                        },
                        enabled = edit.title.trim().isNotBlank() && dirty,
                        modifier = Modifier.weight(1f)
                    ) { Text(textResources.getString(R.string.text_salvar)) }
                }
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🏋️ ${action.title}", style = MaterialTheme.typography.headlineLarge)
                    if (action.status == "CANCELLED") Text(textResources.getString(R.string.text_pausada), color = MaterialTheme.colorScheme.error)
                    Text(textResources.getString(R.string.text_sequencia_atual , streak), style = MaterialTheme.typography.titleMedium)
                    Text(textResources.getString(R.string.text_de_ocorrencias_este_mes , completed, occurrences.size), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (occurrences.isNotEmpty()) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                }
            }
            item {
                RoutineCalendar(
                    month = month,
                    today = today,
                    actionId = actionId,
                    occursOn = { viewModel.routineOccursOn(action, it) },
                    completedOn = { viewModel.isCompletedOn(action, it) },
                    onToggle = { viewModel.toggleOccurrence(action, it) }
                )
            }
            item {
                val recurrenceText = when (val type = edit.recurrenceType) {
                    RecurrenceType.DAILY -> textResources.getString(R.string.text_todos_os_dias)
                    RecurrenceType.MONTHLY -> textResources.getString(R.string.text_todo_mes)
                    RecurrenceType.WEEKLY -> {
                        val names = mapOf(1 to textResources.getString(R.string.text_seg), 2 to textResources.getString(R.string.text_ter), 3 to textResources.getString(R.string.text_qua), 4 to textResources.getString(R.string.text_qui), 5 to textResources.getString(R.string.text_sex), 6 to textResources.getString(R.string.text_sab), 7 to textResources.getString(R.string.text_dom))
                        edit.recurrenceDays.sorted().mapNotNull(names::get).joinToString(" · ").ifBlank { textResources.getString(R.string.text_semanal) }
                    }
                    RecurrenceType.NONE -> textResources.getString(R.string.text_sem_repeticao)
                }
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(textResources.getString(R.string.text_frequencia), style = MaterialTheme.typography.labelLarge)
                    Text(recurrenceText)
                    Text(textResources.getString(R.string.text_horario), style = MaterialTheme.typography.labelLarge)
                    Text(edit.time.format(DateTimeFormatter.ofPattern("HH:mm")))
                    Text(textResources.getString(R.string.text_lembrete), style = MaterialTheme.typography.labelLarge)
                    Text(reminderLabel(edit.reminderMinutes))
                    Text(textResources.getString(R.string.text_historico_de_configuracao), style = MaterialTheme.typography.labelLarge)
                    Text(textResources.getString(R.string.text_regra_s_registrada_s , rules.count { it.actionId == actionId }), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (selectedTagIds.isNotEmpty()) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        allTags.filter { it.id in selectedTagIds }.forEach { tag -> Text("#${tag.name}", color = MaterialTheme.colorScheme.primary) }
                    }
                }
            }
            item { TextButton(onClick = { tagsOpen = true }) { Text(if (selectedTagIds.isEmpty()) textResources.getString(R.string.text_adicionar_tags) else textResources.getString(R.string.text_editar_tags)) } }
            item {
                TextButton(onClick = { deleteDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text(textResources.getString(R.string.text_mover_rotina_para_a_lixeira), color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }

    if (tagsOpen) {
        TagPickerBottomSheet(
            tags = allTags,
            selectedIds = selectedTagIds,
            onToggle = { tag ->
                viewModel.setTagsForOwner(
                    OrganizationOwnerType.ACTION,
                    actionId,
                    if (tag.id in selectedTagIds) selectedTagIds - tag.id else selectedTagIds + tag.id
                )
            },
            onCreate = { viewModel.createAndAttachTag(OrganizationOwnerType.ACTION, actionId, it) },
            onDismiss = { tagsOpen = false }
        )
    }

    if (discardDialog) {
        AlertDialog(
            onDismissRequest = { discardDialog = false },
            title = { Text(textResources.getString(R.string.text_descartar_alteracoes)) },
            text = { Text(textResources.getString(R.string.text_as_alteracoes_da_rotina_nao_serao_salvas)) },
            dismissButton = { TextButton(onClick = { discardDialog = false }) { Text(textResources.getString(R.string.text_continuar_editando)) } },
            confirmButton = { TextButton(onClick = { discardDialog = false; editing = false }) { Text(textResources.getString(R.string.text_descartar)) } }
        )
    }

    if (deleteDialog) {
        AlertDialog(
            onDismissRequest = { deleteDialog = false },
            title = { Text(textResources.getString(R.string.text_mover_rotina_para_a_lixeira_325)) },
            text = { Text(textResources.getString(R.string.text_o_historico_sera_preservado_enquanto_a_rotina_estiver_na_lixeira)) },
            dismissButton = { TextButton(onClick = { deleteDialog = false }) { Text(textResources.getString(R.string.text_cancelar)) } },
            confirmButton = {
                TextButton(onClick = { deleteDialog = false; viewModel.delete(actionId); onBack() }) {
                    Text(textResources.getString(R.string.text_mover), color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}

@Composable
private fun RoutineCalendar(
    month: YearMonth,
    today: LocalDate,
    actionId: Long,
    occursOn: (LocalDate) -> Boolean,
    completedOn: (LocalDate) -> Boolean,
    onToggle: (LocalDate) -> Unit
) {
    Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 1.dp) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                month.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-BR")).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium
            )
            Row(Modifier.fillMaxWidth()) {
                listOf("S", "T", "Q", "Q", "S", "S", "D").forEach { label ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Text(label, style = MaterialTheme.typography.labelSmall) }
                }
            }
            val cells = MutableList<LocalDate?>(month.atDay(1).dayOfWeek.value - 1) { null }
            repeat(month.lengthOfMonth()) { cells += month.atDay(it + 1) }
            while (cells.size % 7 != 0) cells += null
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        if (date == null) Spacer(Modifier.weight(1f).height(46.dp))
                        else {
                            val occurs = occursOn(date)
                            val done = occurs && completedOn(date)
                            val enabled = occurs && !date.isAfter(today)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .then(if (enabled) Modifier.clickable { onToggle(date) } else Modifier),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall)
                                when {
                                    done -> AnimatedCheck(true, modifier = Modifier.size(18.dp), checkedTint = ActionBoxColors.Completed)
                                    occurs -> AnimatedCheck(false, modifier = Modifier.size(18.dp))
                                    else -> Surface(modifier = Modifier.size(4.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun reminderLabel(minutes: Int?): String = when (minutes) {
    null -> androidx.compose.ui.res.stringResource(R.string.text_sem_lembrete_327)
    0 -> androidx.compose.ui.res.stringResource(R.string.text_na_hora)
    10 -> androidx.compose.ui.res.stringResource(R.string.text_10_min_antes)
    30 -> androidx.compose.ui.res.stringResource(R.string.text_30_min_antes)
    60 -> androidx.compose.ui.res.stringResource(R.string.text_1_h_antes)
    1440 -> androidx.compose.ui.res.stringResource(R.string.text_1_dia_antes)
    else -> androidx.compose.ui.res.stringResource(R.string.text_min_antes , minutes)
}
