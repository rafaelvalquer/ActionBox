package com.luminor.actionbox.ui.organize.routines

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
import com.luminor.actionbox.ActionViewModel
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
    viewModel: ActionViewModel,
    actionId: Long,
    onBack: () -> Unit
) {
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
                IconButton(onClick = ::requestBack) { Icon(ActionBoxIcons.Back, contentDescription = "Voltar") }
                Text("Rotina", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (editing) {
                    TextButton(onClick = { if (dirty) discardDialog = true else editing = false }) { Text("Cancelar") }
                } else {
                    TextButton(onClick = ::beginEditing) {
                        Icon(Icons.Rounded.Edit, contentDescription = null)
                        Text("Editar")
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
                    label = { Text("Nome da rotina") }
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Repetição", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            RecurrenceType.DAILY to "Diária",
                            RecurrenceType.WEEKLY to "Semanal",
                            RecurrenceType.MONTHLY to "Mensal"
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
                        Text("Dias da semana", style = MaterialTheme.typography.titleMedium)
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
                    label = { Text("Horário") },
                    placeholder = { Text("19:00") }
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Lembrete", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf(null to "Não", 0 to "Na hora", 10 to "10 min", 30 to "30 min", 60 to "1 h").forEach { (minutes, label) ->
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
                    Text("Prioridade", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(ActionPriority.LOW to "Baixa", ActionPriority.NORMAL to "Normal", ActionPriority.HIGH to "Alta").forEach { (priority, label) ->
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
                        Text("Pausar rotina", style = MaterialTheme.typography.titleMedium)
                        Text("Uma rotina pausada mantém o histórico, mas deixa de aparecer na agenda futura.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = edit.paused, onCheckedChange = { edit = edit.copy(paused = it) })
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { if (dirty) discardDialog = true else editing = false }, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                    Button(
                        onClick = {
                            val parsed = runCatching { LocalTime.parse(timeText, DateTimeFormatter.ofPattern("HH:mm")) }.getOrNull()
                            if (parsed == null) {
                                viewModel.showMessage("Informe um horário válido no formato HH:mm")
                            } else if (edit.recurrenceType == RecurrenceType.WEEKLY && edit.recurrenceDays.isEmpty()) {
                                viewModel.showMessage("Escolha pelo menos um dia da semana")
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
                    ) { Text("Salvar") }
                }
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🏋️ ${action.title}", style = MaterialTheme.typography.headlineLarge)
                    if (action.status == "CANCELLED") Text("Pausada", color = MaterialTheme.colorScheme.error)
                    Text("🔥 Sequência atual: $streak", style = MaterialTheme.typography.titleMedium)
                    Text("$completed de ${occurrences.size} ocorrências este mês", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    RecurrenceType.DAILY -> "Todos os dias"
                    RecurrenceType.MONTHLY -> "Todo mês"
                    RecurrenceType.WEEKLY -> {
                        val names = mapOf(1 to "Seg", 2 to "Ter", 3 to "Qua", 4 to "Qui", 5 to "Sex", 6 to "Sáb", 7 to "Dom")
                        edit.recurrenceDays.sorted().mapNotNull(names::get).joinToString(" · ").ifBlank { "Semanal" }
                    }
                    RecurrenceType.NONE -> "Sem repetição"
                }
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Frequência", style = MaterialTheme.typography.labelLarge)
                    Text(recurrenceText)
                    Text("Horário", style = MaterialTheme.typography.labelLarge)
                    Text(edit.time.format(DateTimeFormatter.ofPattern("HH:mm")))
                    Text("Lembrete", style = MaterialTheme.typography.labelLarge)
                    Text(reminderLabel(edit.reminderMinutes))
                    Text("Histórico de configuração", style = MaterialTheme.typography.labelLarge)
                    Text("${rules.count { it.actionId == actionId }} regra(s) registrada(s)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (selectedTagIds.isNotEmpty()) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        allTags.filter { it.id in selectedTagIds }.forEach { tag -> Text("#${tag.name}", color = MaterialTheme.colorScheme.primary) }
                    }
                }
            }
            item { TextButton(onClick = { tagsOpen = true }) { Text(if (selectedTagIds.isEmpty()) "+ Adicionar tags" else "Editar tags") } }
            item {
                TextButton(onClick = { deleteDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Mover rotina para a lixeira", color = MaterialTheme.colorScheme.error)
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
            title = { Text("Descartar alterações?") },
            text = { Text("As alterações da rotina não serão salvas.") },
            dismissButton = { TextButton(onClick = { discardDialog = false }) { Text("Continuar editando") } },
            confirmButton = { TextButton(onClick = { discardDialog = false; editing = false }) { Text("Descartar") } }
        )
    }

    if (deleteDialog) {
        AlertDialog(
            onDismissRequest = { deleteDialog = false },
            title = { Text("Mover rotina para a lixeira?") },
            text = { Text("O histórico será preservado enquanto a rotina estiver na lixeira.") },
            dismissButton = { TextButton(onClick = { deleteDialog = false }) { Text("Cancelar") } },
            confirmButton = {
                TextButton(onClick = { deleteDialog = false; viewModel.delete(actionId); onBack() }) {
                    Text("Mover", color = MaterialTheme.colorScheme.error)
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

private fun reminderLabel(minutes: Int?): String = when (minutes) {
    null -> "Sem lembrete"
    0 -> "Na hora"
    10 -> "10 min antes"
    30 -> "30 min antes"
    60 -> "1 h antes"
    1440 -> "1 dia antes"
    else -> "$minutes min antes"
}
