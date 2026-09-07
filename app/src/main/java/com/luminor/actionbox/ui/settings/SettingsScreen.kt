package com.luminor.actionbox.ui.settings

import com.luminor.actionbox.R
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
import com.luminor.actionbox.ui.settings.SettingsViewModel
import com.luminor.actionbox.BuildConfig
import com.luminor.actionbox.ui.components.SectionTitle

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onTrash: () -> Unit = {}
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text(textResources.getString(R.string.text_voltar_237)) }
                Text(textResources.getString(R.string.text_configuracoes), style = MaterialTheme.typography.titleLarge)
            }
        }

        item { SectionTitle(textResources.getString(R.string.text_aparencia)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("SYSTEM" to textResources.getString(R.string.text_sistema), "LIGHT" to textResources.getString(R.string.text_claro), "DARK" to textResources.getString(R.string.text_escuro)).forEach { (value, label) ->
                    FilterChip(selected = settings.themeMode == value, onClick = { viewModel.setTheme(value) }, label = { Text(label) })
                }
            }
        }

        item { SectionTitle(textResources.getString(R.string.text_respostas)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(textResources.getString(R.string.text_tom_padrao), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("PROFESSIONAL" to textResources.getString(R.string.text_profissional), "FRIENDLY" to textResources.getString(R.string.text_amigavel), "SHORT" to textResources.getString(R.string.text_curto)).forEach { (value, label) ->
                        FilterChip(selected = settings.replyTone == value, onClick = { viewModel.setReplyTone(value) }, label = { Text(label) })
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(textResources.getString(R.string.text_feedback_tatil), fontWeight = FontWeight.Medium)
                    Text(textResources.getString(R.string.text_microinteracoes_e_confirmacoes_de_toque), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = settings.hapticsEnabled, onCheckedChange = viewModel::setHaptics)
            }
        }

        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))) {
                Column(Modifier.padding(16.dp)) {
                    Text(textResources.getString(R.string.text_privacidade_local), fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(textResources.getString(R.string.text_esta_versao_nao_envia_tarefas_notas_links_ou_lembretes_para_servi))
                }
            }
        }

        item { SectionTitle(textResources.getString(R.string.text_dados)) }
        item {
            Column {
                TextButton(onClick = onTrash, modifier = Modifier.fillMaxWidth()) { Text(textResources.getString(R.string.text_abrir_lixeira)) }
                Text(textResources.getString(R.string.text_itens_excluidos_ficam_disponiveis_para_restauracao_por_30_dias), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) { Text(textResources.getString(R.string.text_apagar_todos_os_dados_locais)) } }
        item { Text(textResources.getString(R.string.text_actionbox_local_first , BuildConfig.VERSION_NAME), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(textResources.getString(R.string.text_apagar_dados)) },
            text = { Text(textResources.getString(R.string.text_tarefas_lembretes_notas_historico_projetos_listas_tags_e_itens_sa)) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAllData(); confirmDelete = false }) { Text(textResources.getString(R.string.text_apagar)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(textResources.getString(R.string.text_cancelar)) } }
        )
    }
}
