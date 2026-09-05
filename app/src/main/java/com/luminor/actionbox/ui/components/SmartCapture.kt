package com.luminor.actionbox.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luminor.actionbox.ActionViewModel
import com.luminor.actionbox.domain.ActionPriority
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.RecurrenceType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val coreTypes = listOf(
    ActionType.TASK, ActionType.REMINDER, ActionType.EVENT,
    ActionType.NOTE, ActionType.LIST, ActionType.PROJECT
)

@Composable
fun SmartCapture(viewModel: ActionViewModel, expanded: Boolean = false) {
    val input by viewModel.input.collectAsStateWithLifecycle()
    val detected by viewModel.detected.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.saveDetected(context)
        else viewModel.showMessage("Permita notificações para criar ações com aviso.")
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.padding(if (expanded) 22.dp else 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("O que você precisa resolver?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "Escreva do seu jeito. O ActionBox identifica, organiza e deixa tudo editável antes de salvar.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = input,
                onValueChange = viewModel::setInput,
                modifier = Modifier.fillMaxWidth(),
                minLines = if (expanded) 4 else 2,
                maxLines = 7,
                shape = RoundedCornerShape(20.dp),
                placeholder = { Text("Ex.: Academia segunda, quarta e sexta às 19h") }
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val pasted = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                        if (pasted.isNotBlank()) viewModel.processInput(pasted)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Colar") }
                Button(onClick = viewModel::analyze, modifier = Modifier.weight(1f)) { Text("Analisar ✨") }
            }

            AnimatedVisibility(visible = detected != null) {
                detected?.let { action ->
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        HorizontalDivider()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Identificado como ${action.type.label}", fontWeight = FontWeight.SemiBold)
                                Text("Confiança ${action.confidenceLabel}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(action.type.emoji, style = MaterialTheme.typography.headlineMedium)
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(coreTypes) { type ->
                                ActionTypePill(type = type, selected = action.type == type) { viewModel.chooseType(type) }
                            }
                        }

                        OutlinedTextField(
                            value = action.title,
                            onValueChange = viewModel::updateDetectedTitle,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Título") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp)
                        )

                        if (action.type in listOf(ActionType.TASK, ActionType.REMINDER, ActionType.EVENT, ActionType.LIST)) {
                            ScheduleEditor(viewModel = viewModel, action = action)
                        }

                        if (action.type == ActionType.TASK || action.type == ActionType.PROJECT) {
                            Text("Prioridade", style = MaterialTheme.typography.labelLarge)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(ActionPriority.entries) { priority ->
                                    val label = when (priority) {
                                        ActionPriority.LOW -> "Baixa"
                                        ActionPriority.NORMAL -> "Normal"
                                        ActionPriority.HIGH -> "Alta"
                                    }
                                    FilterChip(
                                        selected = action.priority == priority,
                                        onClick = { viewModel.setPriority(priority) },
                                        label = { Text(label) }
                                    )
                                }
                            }
                        }

                        if (action.type == ActionType.LIST || action.type == ActionType.PROJECT) {
                            ItemsEditor(viewModel, action.items)
                        }

                        when (action.type) {
                            ActionType.REPLY -> {
                                viewModel.replyOptions(action.sourceText).forEach { option ->
                                    FilledTonalButton(
                                        onClick = { viewModel.copyReply(context, option.text) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(Modifier.fillMaxWidth()) {
                                            Text(option.label, fontWeight = FontWeight.SemiBold)
                                            Text(option.text, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                            else -> {
                                Button(
                                    onClick = {
                                        val requiresPermission = action.type == ActionType.REMINDER || action.reminderMinutes != null
                                        if (requiresPermission && Build.VERSION.SDK_INT >= 33 &&
                                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                        ) {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            viewModel.saveDetected(context)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        when (action.type) {
                                            ActionType.LIST -> "Criar lista"
                                            ActionType.PROJECT -> "Criar projeto"
                                            ActionType.NOTE -> "Salvar nota"
                                            ActionType.EVENT -> "Salvar compromisso"
                                            ActionType.REMINDER -> "Programar lembrete"
                                            else -> "Salvar"
                                        }
                                    )
                                }
                                if (action.type == ActionType.EVENT) {
                                    OutlinedButton(
                                        onClick = { viewModel.saveDetectedAndOpenCalendar(context) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Salvar e adicionar ao calendário do celular") }
                                }
                                if (action.type == ActionType.CONTACT) {
                                    OutlinedButton(
                                        onClick = { viewModel.insertContact(context, action.metadata ?: action.content) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Salvar nos contatos") }
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
private fun ScheduleEditor(viewModel: ActionViewModel, action: com.luminor.actionbox.domain.DetectedAction) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    var dateText by remember(action.scheduledAt?.toLocalDate()) {
        mutableStateOf(action.scheduledAt?.toLocalDate()?.format(dateFormatter).orEmpty())
    }
    var timeText by remember(action.scheduledAt?.toLocalTime()) {
        mutableStateOf(action.scheduledAt?.toLocalTime()?.format(timeFormatter).orEmpty())
    }

    Text("Quando", style = MaterialTheme.typography.labelLarge)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { FilterChip(selected = action.scheduledAt?.toLocalDate() == LocalDate.now(), onClick = { viewModel.setDetectedDate(LocalDate.now()) }, label = { Text("Hoje") }) }
        item { FilterChip(selected = action.scheduledAt?.toLocalDate() == LocalDate.now().plusDays(1), onClick = { viewModel.setDetectedDate(LocalDate.now().plusDays(1)) }, label = { Text("Amanhã") }) }
        item { FilterChip(selected = action.scheduledAt == null, onClick = { viewModel.setDetectedDate(null) }, label = { Text("Sem data") }) }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = dateText,
            onValueChange = {
                dateText = it
                if (it.length == 10) viewModel.setDetectedDateText(it)
            },
            modifier = Modifier.weight(1f),
            label = { Text("Data") },
            placeholder = { Text("dd/mm/aaaa") },
            singleLine = true
        )
        OutlinedTextField(
            value = timeText,
            onValueChange = {
                timeText = it
                if (it.length == 5) viewModel.setDetectedTimeText(it)
            },
            modifier = Modifier.weight(1f),
            label = { Text("Hora") },
            placeholder = { Text("19:00") },
            singleLine = true
        )
    }

    Text("Recorrência", style = MaterialTheme.typography.labelLarge)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val options = listOf(
            RecurrenceType.NONE to "Não repetir",
            RecurrenceType.DAILY to "Todo dia",
            RecurrenceType.WEEKLY to "Semanal",
            RecurrenceType.MONTHLY to "Mensal"
        )
        items(options) { (value, label) ->
            FilterChip(selected = action.recurrenceType == value, onClick = { viewModel.setRecurrence(value) }, label = { Text(label) })
        }
    }

    if (action.recurrenceType == RecurrenceType.WEEKLY) {
        val days = listOf(1 to "S", 2 to "T", 3 to "Q", 4 to "Q", 5 to "S", 6 to "S", 7 to "D")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(days) { (day, label) ->
                FilterChip(selected = day in action.recurrenceDays, onClick = { viewModel.toggleRecurrenceDay(day) }, label = { Text(label) })
            }
        }
    }

    Text("Aviso", style = MaterialTheme.typography.labelLarge)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val options = listOf(null to "Sem aviso", 0 to "Na hora", 10 to "10 min", 30 to "30 min", 60 to "1 hora")
        items(options) { (value, label) ->
            FilterChip(selected = action.reminderMinutes == value, onClick = { viewModel.setReminderMinutes(value) }, label = { Text(label) })
        }
    }
}

@Composable
private fun ItemsEditor(viewModel: ActionViewModel, items: List<String>) {
    var newItem by remember { mutableStateOf("") }
    Text("Itens", style = MaterialTheme.typography.labelLarge)
    items.forEachIndexed { index, item ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("☐  $item", modifier = Modifier.weight(1f))
            TextButton(onClick = { viewModel.removeDetectedItem(index) }) { Text("Remover") }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = newItem,
            onValueChange = { newItem = it },
            modifier = Modifier.weight(1f),
            label = { Text("Novo item") },
            singleLine = true
        )
        Button(onClick = { viewModel.addDetectedItem(newItem); newItem = "" }) { Text("+") }
    }
}
