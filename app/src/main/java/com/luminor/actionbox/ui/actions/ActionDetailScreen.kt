package com.luminor.actionbox.ui.actions

import com.luminor.actionbox.R
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
import com.luminor.actionbox.ui.actions.ActionEditorViewModel
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
import com.luminor.actionbox.ui.motion.SharedKeys
import com.luminor.actionbox.ui.motion.actionSharedBounds
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ActionDetailScreen(viewModel: ActionEditorViewModel, action: ActionEntity, onBack: () -> Unit) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val color = if (action.status == ActionStatus.COMPLETED.name) ActionBoxColors.Completed else actionTypeColor(action.type)
    val date = action.scheduledAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime() }
    val completed = viewModel.isCompletedOn(action, LocalDate.now())

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .statusBarsPadding()
                .padding(18.dp)
                .actionSharedBounds(SharedKeys.action(action.id)),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(ActionBoxIcons.Back, contentDescription = textResources.getString(R.string.text_voltar)) }
                Text(textResources.getString(R.string.text_detalhes), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
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
                    if (date != null) DetailLine(textResources.getString(R.string.text_quando), date.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM · HH:mm", Locale.forLanguageTag("pt-BR"))).replaceFirstChar { it.uppercase() })
                    if (RecurrenceCalculator.recurrenceType(action).name != "NONE") DetailLine(textResources.getString(R.string.text_repeticao), recurrenceLabel(action))
                    action.reminderMinutes?.let { DetailLine(textResources.getString(R.string.text_aviso), if (it == 0) textResources.getString(R.string.text_na_hora) else textResources.getString(R.string.text_min_antes , it)) }
                    action.priority?.let { DetailLine(textResources.getString(R.string.text_prioridade), it.lowercase().replaceFirstChar { ch -> ch.uppercase() }) }
                    if (action.content.isNotBlank() && action.content != action.title) DetailLine(textResources.getString(R.string.text_conteudo), action.content)
                }
            }

            ActionButton(
                text = if (completed) textResources.getString(R.string.text_marcar_como_pendente) else textResources.getString(R.string.text_concluir_acao),
                onClick = {
                    if (settings.hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.toggleOccurrence(action, LocalDate.now())
                }
            )

            if (action.type == ActionType.EVENT.name) {
                ActionButton(textResources.getString(R.string.text_adicionar_ao_calendario_do_celular), onClick = { viewModel.addToSystemCalendar(context, action) }, primary = false)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.material3.OutlinedButton(onClick = { viewModel.archive(action.id); onBack() }, modifier = Modifier.weight(1f)) { Text(textResources.getString(R.string.text_arquivar)) }
                androidx.compose.material3.OutlinedButton(onClick = { viewModel.delete(action.id); onBack() }, modifier = Modifier.weight(1f)) { Text(textResources.getString(R.string.text_excluir)) }
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

@Composable
private fun actionTypeLabel(type: String) = when (type) {
    ActionType.REMINDER.name -> androidx.compose.ui.res.stringResource(R.string.text_lembrete)
    ActionType.EVENT.name -> androidx.compose.ui.res.stringResource(R.string.text_compromisso)
    ActionType.NOTE.name -> androidx.compose.ui.res.stringResource(R.string.text_nota)
    ActionType.LIST.name -> androidx.compose.ui.res.stringResource(R.string.text_lista)
    ActionType.PROJECT.name -> androidx.compose.ui.res.stringResource(R.string.text_projeto)
    ActionType.READ_LATER.name -> androidx.compose.ui.res.stringResource(R.string.text_depois)
    else -> androidx.compose.ui.res.stringResource(R.string.text_tarefa)
}

@Composable
private fun recurrenceLabel(action: ActionEntity): String = when (RecurrenceCalculator.recurrenceType(action).name) {
    "DAILY" -> androidx.compose.ui.res.stringResource(R.string.text_todo_dia)
    "WEEKLY" -> androidx.compose.ui.res.stringResource(R.string.text_semanal)
    "MONTHLY" -> androidx.compose.ui.res.stringResource(R.string.text_mensal)
    else -> androidx.compose.ui.res.stringResource(R.string.text_nao_repetir)
}
