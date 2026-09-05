package com.luminor.actionbox.ui.saved

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luminor.actionbox.ActionViewModel
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.domain.ExternalActions
import com.luminor.actionbox.ui.designsystem.ActionBoxColors
import com.luminor.actionbox.ui.designsystem.ActionBoxIcons
import com.luminor.actionbox.ui.designsystem.components.ActionCard
import com.luminor.actionbox.ui.designsystem.components.ActionEmptyState
import java.time.Duration
import java.time.Instant

@Composable
fun SavedScreen(viewModel: ActionViewModel, onOpenDetail: (Long) -> Unit) {
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.align(Alignment.TopCenter).fillMaxWidth().widthIn(max = 920.dp).statusBarsPadding()) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Depois", style = MaterialTheme.typography.headlineLarge)
                Text("Sua coleção de links e conteúdos para voltar com calma.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (saved.isEmpty()) {
                ActionEmptyState("🔖", "Nada salvo", "Compartilhe um link com o ActionBox ou cole um link na tela inicial.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(saved, key = { it.id }) { item -> SavedSwipeCard(item, viewModel, settings.hapticsEnabled) { onOpenDetail(item.id) } }
                    item { Spacer(Modifier.padding(bottom = 24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun SavedSwipeCard(item: ActionEntity, viewModel: ActionViewModel, hapticsEnabled: Boolean, onOpen: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = { value ->
        if (value == SwipeToDismissBoxValue.StartToEnd) {
            if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.archive(item.id)
            true
        } else false
    })

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Row(Modifier.fillMaxSize().padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(ActionBoxIcons.Archive, contentDescription = null, tint = ActionBoxColors.Completed)
                Text("  Arquivar", color = ActionBoxColors.Completed)
            }
        }
    ) {
        SavedCard(item, viewModel, context, onOpen)
    }
}

@Composable
private fun SavedCard(item: ActionEntity, viewModel: ActionViewModel, context: Context, onOpen: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    ActionCard(onClick = onOpen) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(categoryEmoji(item.metadata), style = MaterialTheme.typography.headlineMedium)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Text(host(item.sourceUrl), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(relativeSaved(item.createdAt), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(ActionBoxIcons.More, contentDescription = "Mais opções") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Abrir") }, onClick = { menu = false; viewModel.openSaved(context, item.sourceUrl) }, leadingIcon = { Icon(ActionBoxIcons.Open, null) })
                    DropdownMenuItem(text = { Text("Copiar link") }, onClick = { menu = false; item.sourceUrl?.let { ExternalActions.copy(context, "Link", it) } }, leadingIcon = { Icon(ActionBoxIcons.Copy, null) })
                    DropdownMenuItem(text = { Text("Compartilhar") }, onClick = { menu = false; shareText(context, item.sourceUrl ?: item.content) }, leadingIcon = { Icon(ActionBoxIcons.Share, null) })
                    DropdownMenuItem(text = { Text("Arquivar") }, onClick = { menu = false; viewModel.archive(item.id) }, leadingIcon = { Icon(ActionBoxIcons.Archive, null) })
                    DropdownMenuItem(text = { Text("Excluir") }, onClick = { menu = false; viewModel.delete(item.id) }, leadingIcon = { Icon(ActionBoxIcons.Delete, null) })
                }
            }
        }
    }
}

private fun categoryEmoji(category: String?) = when (category) {
    "WATCH" -> "🎬"
    "BUY" -> "🛒"
    "READ" -> "📖"
    else -> "🔖"
}

private fun host(url: String?): String = runCatching { Uri.parse(url.orEmpty()).host?.removePrefix("www.") }.getOrNull().orEmpty().ifBlank { "Conteúdo salvo" }

private fun relativeSaved(createdAt: Long): String {
    val days = Duration.between(Instant.ofEpochMilli(createdAt), Instant.now()).toDays()
    return when (days) {
        0L -> "Salvo hoje"
        1L -> "Salvo ontem"
        else -> "Salvo há $days dias"
    }
}

private fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
    context.startActivity(Intent.createChooser(send, "Compartilhar").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
