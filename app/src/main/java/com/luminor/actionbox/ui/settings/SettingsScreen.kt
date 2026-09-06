package com.luminor.actionbox.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luminor.actionbox.ActionViewModel
import com.luminor.actionbox.BuildConfig
import com.luminor.actionbox.ui.components.SectionTitle

@Composable
fun SettingsScreen(
    viewModel: ActionViewModel,
    onBack: () -> Unit,
    onTrash: () -> Unit = {}
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← Voltar") }
                Text("Configurações", style = MaterialTheme.typography.titleLarge)
            }
        }

        item { SectionTitle("Aparência") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("SYSTEM" to "Sistema", "LIGHT" to "Claro", "DARK" to "Escuro").forEach { (value, label) ->
                    FilterChip(selected = settings.themeMode == value, onClick = { viewModel.setTheme(value) }, label = { Text(label) })
                }
            }
        }

        item { SectionTitle("Respostas") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tom padrão", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("PROFESSIONAL" to "Profissional", "FRIENDLY" to "Amigável", "SHORT" to "Curto").forEach { (value, label) ->
                        FilterChip(selected = settings.replyTone == value, onClick = { viewModel.setReplyTone(value) }, label = { Text(label) })
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Feedback tátil", fontWeight = FontWeight.Medium)
                    Text("Microinterações e confirmações de toque", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = settings.hapticsEnabled, onCheckedChange = viewModel::setHaptics)
            }
        }

        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))) {
                Column(Modifier.padding(16.dp)) {
                    Text("🔒 Privacidade local", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text("Esta versão não envia tarefas, notas, links ou lembretes para servidores. Os dados ficam no banco local do aparelho.")
                }
            }
        }

        item { SectionTitle("Dados") }
        item {
            Column {
                TextButton(onClick = onTrash, modifier = Modifier.fillMaxWidth()) { Text("🗑️ Abrir lixeira") }
                Text("Itens excluídos ficam disponíveis para restauração por 30 dias.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) { Text("Apagar todos os dados locais") } }
        item { Text("ActionBox ${BuildConfig.VERSION_NAME} · Local-first", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Apagar dados?") },
            text = { Text("Tarefas, lembretes, notas, histórico, projetos, listas, tags e itens salvos serão removidos deste aparelho.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAllData(); confirmDelete = false }) { Text("Apagar") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } }
        )
    }
}
