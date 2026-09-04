package com.luminor.actionbox.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luminor.actionbox.ActionViewModel
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.DetectedAction
import com.luminor.actionbox.ui.components.ActionTypePill
import com.luminor.actionbox.ui.components.EmptyState
import com.luminor.actionbox.ui.components.SectionTitle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(viewModel: ActionViewModel, onSettings: () -> Unit) {
    val input by viewModel.input.collectAsStateWithLifecycle()
    val detected by viewModel.detected.collectAsStateWithLifecycle()
    val pending by viewModel.pending.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("ActionBox", style = MaterialTheme.typography.headlineMedium)
                    Text("Resolva em poucos toques ✨", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onSettings) { Text("⚙️") }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("O que você precisa resolver?", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = input,
                        onValueChange = viewModel::setInput,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        shape = RoundedCornerShape(18.dp),
                        placeholder = { Text("Digite ou cole uma mensagem, link ou informação...") }
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val pasted = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                                if (pasted.isNotBlank()) viewModel.processInput(pasted)
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("📋 Colar") }
                        Button(
                            onClick = { focus.clearFocus(); viewModel.analyze() },
                            modifier = Modifier.weight(1f)
                        ) { Text("Analisar ✨") }
                    }
                }
            }
        }

        item {
            SectionTitle("Ações rápidas")
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ActionType.entries) { type ->
                    ActionTypePill(type = type, selected = detected?.type == type) {
                        if (input.isBlank()) viewModel.setInput(defaultPrompt(type))
                        viewModel.chooseType(type)
                    }
                }
            }
        }

        item {
            AnimatedVisibility(visible = detected != null) {
                detected?.let { action ->
                    DetectionCard(
                        action = action,
                        viewModel = viewModel,
                        onExecute = {
                            if (action.type == ActionType.REMINDER && Build.VERSION.SDK_INT >= 33 &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                            ) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            viewModel.execute(context, action)
                        }
                    )
                }
            }
        }

        item {
            SectionTitle("Para resolver")
            Spacer(Modifier.height(6.dp))
            if (pending.isEmpty()) {
                EmptyState("✅", "Tudo resolvido", "Suas tarefas e lembretes pendentes aparecerão aqui.")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pending.take(4).forEach { item -> PendingMiniCard(item, onComplete = { viewModel.complete(item.id) }) }
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun DetectionCard(action: DetectedAction, viewModel: ActionViewModel, onExecute: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.09f))
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(action.type.emoji, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Encontrei ${article(action.type)} ${action.type.label.lowercase()}", fontWeight = FontWeight.SemiBold)
                    Text("Confiança ${action.confidence}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(action.title, style = MaterialTheme.typography.titleLarge)
            action.scheduledAt?.let {
                Text("📆 ${it.format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"))}")
            }
            action.sourceUrl?.let { Text(it, color = MaterialTheme.colorScheme.primary, maxLines = 1) }

            Text("Alterar ação", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(ActionType.entries) { type ->
                    ActionTypePill(type = type, selected = action.type == type) { viewModel.chooseType(type) }
                }
            }

            if (action.type == ActionType.REPLY) {
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
            } else {
                Button(onClick = onExecute, modifier = Modifier.fillMaxWidth()) {
                    Text(executeLabel(action.type))
                }
                if (action.type == ActionType.CONTACT) {
                    OutlinedButton(
                        onClick = { viewModel.insertContact(context, action.metadata ?: action.content) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("👤 Salvar contato") }
                }
            }
        }
    }
}

@Composable
private fun PendingMiniCard(item: ActionEntity, onComplete: () -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(if (item.type == "REMINDER") "⏰" else "✅")
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.Medium)
                item.scheduledAt?.let {
                    val dt = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime()
                    Text(dt.format(DateTimeFormatter.ofPattern("dd/MM · HH:mm")), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TextButton(onClick = onComplete) { Text("Concluir") }
        }
    }
}

private fun defaultPrompt(type: ActionType): String = when (type) {
    ActionType.TASK -> "Criar tarefa: "
    ActionType.REMINDER -> "Me lembra amanhã às 09h de "
    ActionType.EVENT -> "Reunião amanhã às 14h sobre "
    ActionType.NOTE -> "Nota: "
    ActionType.READ_LATER -> "https://"
    ActionType.ADDRESS -> "Avenida "
    ActionType.CONTACT -> "Contato: "
    ActionType.REPLY -> "Responder: "
}

private fun executeLabel(type: ActionType): String = when (type) {
    ActionType.TASK -> "✅ Criar tarefa"
    ActionType.REMINDER -> "⏰ Criar lembrete"
    ActionType.EVENT -> "📅 Abrir calendário"
    ActionType.NOTE -> "📝 Salvar nota"
    ActionType.READ_LATER -> "🔖 Salvar para depois"
    ActionType.ADDRESS -> "📍 Abrir endereço"
    ActionType.CONTACT -> "📞 Abrir discador"
    ActionType.REPLY -> "💬 Gerar resposta"
}

private fun article(type: ActionType) = if (type in listOf(ActionType.TASK, ActionType.NOTE, ActionType.REPLY)) "uma" else "um"
