package com.luminor.actionbox.ui.home

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luminor.actionbox.ActionViewModel
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.RecurrenceCalculator
import com.luminor.actionbox.ui.capture.CaptureFlow
import com.luminor.actionbox.ui.designsystem.ActionBoxIcons
import com.luminor.actionbox.ui.designsystem.components.ActionEmptyState
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(viewModel: ActionViewModel, onSettings: () -> Unit, onActionOpen: (Long) -> Unit) {
    val all by viewModel.all.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val todayActions = all
        .filter { it.type in setOf(ActionType.TASK.name, ActionType.REMINDER.name, ActionType.EVENT.name, ActionType.LIST.name) }
        .filter { RecurrenceCalculator.occursOn(it, today) }
        .sortedBy { it.scheduledAt ?: Long.MAX_VALUE }
    val completed = todayActions.count { viewModel.isCompletedOn(it, today) }
    val pending = todayActions.size - completed
    val undated = all.count { it.type == ActionType.TASK.name && it.scheduledAt == null && it.status == ActionStatus.PENDING.name }
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Bom dia"
        in 12..17 -> "Boa tarde"
        else -> "Boa noite"
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = 920.dp)
                .statusBarsPadding()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ActionBox", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = onSettings) {
                        Icon(ActionBoxIcons.Settings, contentDescription = "Ajustes")
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("$greeting 👋", style = MaterialTheme.typography.headlineLarge)
                    Text("O que precisa sair da sua cabeça?", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item { CaptureFlow(viewModel = viewModel, compact = true) }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        "Hoje · ${today.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("pt-BR"))).replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "$pending pendentes · $completed concluída${if (completed == 1) "" else "s"} · $undated sem data",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (todayActions.isEmpty()) {
                item { ActionEmptyState("✨", "Dia livre", "Quando algo tiver data ou recorrência, aparecerá aqui.") }
            } else {
                items(todayActions, key = { it.id }) { action ->
                    TodayActionRow(
                        action = action,
                        date = today,
                        completed = viewModel.isCompletedOn(action, today),
                        hapticsEnabled = settings.hapticsEnabled,
                        onToggle = { viewModel.toggleOccurrence(action, today) },
                        onOpen = { onActionOpen(action.id) }
                    )
                }
            }

            item { Spacer(Modifier.height(28.dp)) }
        }
    }
}
