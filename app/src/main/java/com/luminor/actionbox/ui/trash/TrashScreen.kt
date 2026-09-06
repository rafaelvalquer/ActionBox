package com.luminor.actionbox.ui.trash

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.ui.designsystem.ActionBoxIcons
import com.luminor.actionbox.ui.designsystem.components.ActionCard
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class TrashKind { ACTION, PROJECT, LIST }
private data class PendingPermanentDelete(val kind: TrashKind, val id: Long, val title: String)

@Composable
fun TrashScreen(viewModel: TrashViewModel, onBack: () -> Unit) {
    val deletedActions by viewModel.actions.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val lists by viewModel.lists.collectAsStateWithLifecycle()
    var permanentDelete by remember { mutableStateOf<PendingPermanentDelete?>(null) }

    val actions = deletedActions.filter { it.projectId == null && it.type != ActionType.LIST.name }
    val empty = projects.isEmpty() && lists.isEmpty() && actions.isEmpty()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 820.dp)
            .statusBarsPadding()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(ActionBoxIcons.Back, contentDescription = "Voltar") }
                Column(Modifier.weight(1f)) {
                    Text("Lixeira", style = MaterialTheme.typography.headlineMedium)
                    Text("Itens são apagados definitivamente após 30 dias.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (empty) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 44.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🗑️", style = MaterialTheme.typography.headlineLarge)
                    Text("Lixeira vazia", style = MaterialTheme.typography.titleLarge)
                    Text("Itens excluídos aparecerão aqui durante 30 dias.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (projects.isNotEmpty()) {
            item { SectionTitle("PROJETOS") }
            items(projects, key = { "project-${it.id}" }) { project ->
                TrashCard(
                    emoji = "📁",
                    title = project.title,
                    deletedAt = project.deletedAt,
                    onRestore = { viewModel.restoreProject(project.id) },
                    onDelete = { permanentDelete = PendingPermanentDelete(TrashKind.PROJECT, project.id, project.title) }
                )
            }
        }

        if (lists.isNotEmpty()) {
            item { SectionTitle("LISTAS") }
            items(lists, key = { "list-${it.id}" }) { list ->
                TrashCard(
                    emoji = "☑️",
                    title = list.title,
                    deletedAt = list.deletedAt,
                    onRestore = { viewModel.restoreList(list.id) },
                    onDelete = { permanentDelete = PendingPermanentDelete(TrashKind.LIST, list.id, list.title) }
                )
            }
        }

        if (actions.isNotEmpty()) {
            item { SectionTitle("ITENS") }
            items(actions, key = { "action-${it.id}" }) { action ->
                TrashCard(
                    emoji = actionEmoji(action),
                    title = action.title,
                    deletedAt = action.deletedAt,
                    onRestore = { viewModel.restoreAction(action.id) },
                    onDelete = { permanentDelete = PendingPermanentDelete(TrashKind.ACTION, action.id, action.title) }
                )
            }
        }

        item { Spacer(Modifier.height(30.dp)) }
    }

    permanentDelete?.let { pending ->
        AlertDialog(
            onDismissRequest = { permanentDelete = null },
            title = { Text("Excluir definitivamente?") },
            text = { Text("“${pending.title}” não poderá ser restaurado depois desta ação.") },
            dismissButton = { TextButton(onClick = { permanentDelete = null }) { Text("Cancelar") } },
            confirmButton = {
                TextButton(onClick = {
                    when (pending.kind) {
                        TrashKind.ACTION -> viewModel.permanentlyDeleteAction(pending.id)
                        TrashKind.PROJECT -> viewModel.permanentlyDeleteProject(pending.id)
                        TrashKind.LIST -> viewModel.permanentlyDeleteList(pending.id)
                    }
                    permanentDelete = null
                }) { Text("Excluir definitivamente", color = MaterialTheme.colorScheme.error) }
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun TrashCard(
    emoji: String,
    title: String,
    deletedAt: Long?,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    ActionCard {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(end = 10.dp))
                Column(Modifier.weight(1f)) {
                    Text(title.ifBlank { "Sem título" }, style = MaterialTheme.typography.titleMedium)
                    Text(deletedLabel(deletedAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onRestore) { Text("Restaurar") }
                TextButton(onClick = onDelete) { Text("Excluir", color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

private fun actionEmoji(action: ActionEntity): String = when (action.type) {
    ActionType.NOTE.name -> "📝"
    ActionType.READ_LATER.name -> "🔖"
    ActionType.REMINDER.name -> "⏰"
    ActionType.EVENT.name -> "📅"
    else -> "✓"
}

private fun deletedLabel(value: Long?): String {
    if (value == null) return "Excluído"
    val local = Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).toLocalDateTime()
    return "Excluído em ${local.format(DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm"))}"
}
