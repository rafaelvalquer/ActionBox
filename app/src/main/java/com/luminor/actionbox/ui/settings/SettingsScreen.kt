package com.luminor.actionbox.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.luminor.actionbox.ui.components.SectionTitle

@Composable
fun SettingsScreen(viewModel: ActionViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Voltar") }
            Text("Configurações", style = MaterialTheme.typography.titleLarge)
        }

        SectionTitle("Aparência")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("SYSTEM" to "Sistema", "LIGHT" to "Claro", "DARK" to "Escuro").forEach { (value, label) ->
                FilterChip(selected = settings.themeMode == value, onClick = { viewModel.setTheme(value) }, label = { Text(label) })
            }
        }

        SectionTitle("Respostas")
        Text("Tom padrão", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("PROFESSIONAL" to "Profissional", "FRIENDLY" to "Amigável", "SHORT" to "Curto").forEach { (value, label) ->
                FilterChip(selected = settings.replyTone == value, onClick = { viewModel.setReplyTone(value) }, label = { Text(label) })
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Feedback tátil", fontWeight = FontWeight.Medium)
                Text("Preparado para microinterações futuras", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = settings.hapticsEnabled, onCheckedChange = viewModel::setHaptics)
        }

        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))) {
            Column(Modifier.padding(16.dp)) {
                Text("🔒 Privacidade local", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("Esta versão não envia tarefas, notas, links ou lembretes para servidores. Os dados ficam no banco local do aparelho.")
            }
        }

        TextButton(onClick = { confirmDelete = true }) { Text("Apagar todos os dados locais") }
        Text("ActionBox 1.0.0 · MVP local", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Apagar dados?") },
            text = { Text("Tarefas, lembretes, notas, histórico e itens salvos serão removidos deste aparelho.") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAllData(); confirmDelete = false }) { Text("Apagar") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } }
        )
    }
}
