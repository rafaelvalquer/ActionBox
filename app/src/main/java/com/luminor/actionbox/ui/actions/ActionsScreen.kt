package com.luminor.actionbox.ui.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luminor.actionbox.ActionViewModel
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.ui.components.EmptyState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ActionsScreen(viewModel: ActionViewModel) {
    val pending by viewModel.pending.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Pendentes", "Notas", "Histórico")

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text("Ações", style = MaterialTheme.typography.headlineMedium)
            Text("Tudo que ainda precisa de você.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TabRow(selectedTabIndex = tab) {
            tabs.forEachIndexed { index, text ->
                Tab(selected = tab == index, onClick = { tab = index }, text = { Text(text) })
            }
        }
        when (tab) {
            0 -> ActionList(pending, "✅", "Nada pendente", "Tarefas e lembretes aparecerão aqui.", viewModel, allowComplete = true)
            1 -> ActionList(notes, "📝", "Nenhuma nota", "Salve uma informação como nota pela tela inicial.", viewModel)
            else -> ActionList(history, "✨", "Histórico vazio", "As ações concluídas aparecerão aqui.", viewModel)
        }
    }
}

@Composable
private fun ActionList(
    list: List<ActionEntity>,
    emoji: String,
    emptyTitle: String,
    emptyText: String,
    viewModel: ActionViewModel,
    allowComplete: Boolean = false
) {
    if (list.isEmpty()) {
        EmptyState(emoji, emptyTitle, emptyText)
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        items(list, key = { it.id }) { item ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(emojiFor(item.type))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, fontWeight = FontWeight.Medium)
                        item.scheduledAt?.let { millis ->
                            val dt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDateTime()
                            Text(dt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm")), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (item.type == "NOTE" && item.content != item.title) {
                            Spacer(Modifier.height(3.dp))
                            Text(item.content, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (allowComplete) TextButton(onClick = { viewModel.complete(item.id) }) { Text("Concluir") }
                    TextButton(onClick = { viewModel.delete(item.id) }) { Text("Excluir") }
                }
            }
        }
    }
}

private fun emojiFor(type: String) = when (type) {
    "TASK" -> "✅"
    "REMINDER" -> "⏰"
    "EVENT" -> "📅"
    "NOTE" -> "📝"
    "READ_LATER" -> "🔖"
    "ADDRESS" -> "📍"
    "CONTACT" -> "📞"
    "REPLY" -> "💬"
    else -> "•"
}
