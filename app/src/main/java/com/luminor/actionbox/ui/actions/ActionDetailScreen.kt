package com.luminor.actionbox.ui.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luminor.actionbox.ActionViewModel
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.RecurrenceCalculator
import com.luminor.actionbox.ui.designsystem.ActionBoxColors
import com.luminor.actionbox.ui.designsystem.ActionBoxIcons
import com.luminor.actionbox.ui.designsystem.actionTypeColor
import com.luminor.actionbox.ui.designsystem.components.ActionBadge
import com.luminor.actionbox.ui.designsystem.components.ActionButton
import com.luminor.actionbox.ui.designsystem.components.ActionCard
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ActionDetailScreen(viewModel: ActionViewModel, action: ActionEntity, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val color = if (action.status == ActionStatus.COMPLETED.name) ActionBoxColors.Completed else actionTypeColor(action.type)
    val date = action.scheduledAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime() }
    val completed = viewModel.isCompletedOn(action, LocalDate.now())

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().widthIn(max = 760.dp).statusBarsPadding().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(ActionBoxIcons.Back, contentDescription = "Voltar") }
                Text("Detalhes", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            }

            Surface(shape = MaterialTheme.shapes.extraLarge, color = color.copy(alpha = 0.12f)) {
                Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                    Icon(ActionBoxIcons.forType(action.type), contentDescription = null, tint = color, modifier = Modifier.size(34.dp))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionBadge(actionTypeLabel(action.type), color)
                Text(
                    action.title,
                    style = MaterialTheme.typography.headlineLarge,
                    textDecoration = if (completed) TextDecoration.LineThrough else null
                )
                if (!action.description.isNullOrBlank()) Text(action.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            ActionCard {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (date != null) DetailLine("Quando", date.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM · HH:mm", Locale.forLanguageTag("pt-BR"))).replaceFirstChar { it.uppercase() })
                    if (RecurrenceCalculator.recurrenceType(action).name != "NONE") DetailLine("Repetição", recurrenceLabel(action))
                    action.reminderMinutes?.let { DetailLine("Aviso", if (it == 0) "Na hora" else "$it min antes") }
                    action.priority?.let { DetailLine("Prioridade", it.lowercase().replaceFirstChar { ch -> ch.uppercase() }) }
                    if (action.content.isNotBlank() && action.content != action.title) DetailLine("Conteúdo", action.content)
                }
            }

            ActionButton(
                text = if (completed) "Marcar como pendente" else "Concluir ação",
                onClick = {
                    if (settings.hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.toggleOccurrence(action, LocalDate.now())
                }
            )

            if (action.type == ActionType.EVENT.name) {
                ActionButton("Adicionar ao calendário do celular", onClick = { viewModel.addToSystemCalendar(context, action) }, primary = false)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.material3.OutlinedButton(onClick = { viewModel.archive(action.id); onBack() }, modifier = Modifier.weight(1f)) { Text("Arquivar") }
                androidx.compose.material3.OutlinedButton(onClick = { viewModel.delete(action.id); onBack() }, modifier = Modifier.weight(1f)) { Text("Excluir") }
            }
            Spacer(Modifier.padding(bottom = 14.dp))
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f).padding(start = 16.dp))
    }
}

private fun actionTypeLabel(type: String) = when (type) {
    ActionType.REMINDER.name -> "Lembrete"
    ActionType.EVENT.name -> "Compromisso"
    ActionType.NOTE.name -> "Nota"
    ActionType.LIST.name -> "Lista"
    ActionType.PROJECT.name -> "Projeto"
    ActionType.READ_LATER.name -> "Depois"
    else -> "Tarefa"
}

private fun recurrenceLabel(action: ActionEntity): String = when (RecurrenceCalculator.recurrenceType(action).name) {
    "DAILY" -> "Todo dia"
    "WEEKLY" -> "Semanal"
    "MONTHLY" -> "Mensal"
    else -> "Não repetir"
}
