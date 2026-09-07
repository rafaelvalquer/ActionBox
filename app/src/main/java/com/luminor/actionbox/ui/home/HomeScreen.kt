package com.luminor.actionbox.ui.home

import com.luminor.actionbox.R
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
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
import com.luminor.actionbox.ui.home.HomeViewModel
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.RecurrenceCalculator
import com.luminor.actionbox.ui.capture.CaptureFlow
import com.luminor.actionbox.ui.capture.CaptureViewModel
import com.luminor.actionbox.ui.designsystem.ActionBoxIcons
import com.luminor.actionbox.ui.designsystem.components.ActionEmptyState
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    captureViewModel: CaptureViewModel,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onActionOpen: (Long) -> Unit,
    onSearch: () -> Unit = {}
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources

    val all by viewModel.all.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val completions by viewModel.completions.collectAsStateWithLifecycle()
    val rules by viewModel.routineRules.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val todayActions = all
        .filter { it.type in setOf(ActionType.TASK.name, ActionType.REMINDER.name, ActionType.EVENT.name, ActionType.LIST.name) }
        .filter { com.luminor.actionbox.domain.routine.RoutineEvaluation.routineOccursOn(it, today, rules) }
        .sortedBy { it.scheduledAt ?: Long.MAX_VALUE }
    val completed = todayActions.count { com.luminor.actionbox.domain.routine.RoutineEvaluation.isCompletedOn(it, today, completions) }
    val pending = todayActions.size - completed
    val undated = all.count { it.type == ActionType.TASK.name && it.scheduledAt == null && it.status == ActionStatus.PENDING.name }
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> textResources.getString(R.string.text_bom_dia)
        in 12..17 -> textResources.getString(R.string.text_boa_tarde)
        else -> textResources.getString(R.string.text_boa_noite)
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
                    Text(textResources.getString(R.string.app_name), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    androidx.compose.material3.TextButton(onClick = onHistory) { Text(textResources.getString(R.string.text_historico)) }
                    IconButton(onClick = onSearch) { Icon(Icons.Rounded.Search, contentDescription = textResources.getString(R.string.text_buscar)) }
                    IconButton(onClick = onSettings) { Icon(ActionBoxIcons.Settings, contentDescription = textResources.getString(R.string.text_ajustes)) }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("$greeting 👋", style = MaterialTheme.typography.headlineLarge)
                    Text(textResources.getString(R.string.text_o_que_precisa_sair_da_sua_cabeca), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            item { CaptureFlow(viewModel = captureViewModel, compact = true) }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        "Hoje · ${today.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("pt-BR"))).replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        textResources.getString(R.string.text_pendentes_concluida_sem_data , pending, completed, if (completed == 1) "" else "s", undated),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (todayActions.isEmpty()) {
                item { ActionEmptyState("✨", textResources.getString(R.string.text_dia_livre), textResources.getString(R.string.text_quando_algo_tiver_data_ou_recorrencia_aparecera_aqui)) }
            } else {
                items(todayActions, key = { it.id }) { action ->
                    TodayActionRow(
                        action = action,
                        date = today,
                        completed = com.luminor.actionbox.domain.routine.RoutineEvaluation.isCompletedOn(action, today, completions),
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
