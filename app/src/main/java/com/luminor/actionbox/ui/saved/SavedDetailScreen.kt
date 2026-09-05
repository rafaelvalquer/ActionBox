package com.luminor.actionbox.ui.saved

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luminor.actionbox.ActionViewModel
import com.luminor.actionbox.domain.ExternalActions
import com.luminor.actionbox.ui.designsystem.ActionBoxIcons
import com.luminor.actionbox.ui.designsystem.components.ActionButton
import com.luminor.actionbox.ui.designsystem.components.ActionCard

@Composable
fun SavedDetailScreen(viewModel: ActionViewModel, id: Long, onBack: () -> Unit) {
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val item = saved.firstOrNull { it.id == id } ?: return
    val context = LocalContext.current
    val link = item.sourceUrl ?: item.content

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().widthIn(max = 760.dp).statusBarsPadding().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(ActionBoxIcons.Back, contentDescription = "Voltar") }
                Text("Depois", style = MaterialTheme.typography.titleLarge)
            }
            Text(item.title, style = MaterialTheme.typography.headlineLarge)
            ActionCard {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(link, style = MaterialTheme.typography.bodyLarge)
                    if (item.content.isNotBlank() && item.content != link) Text(item.content, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            ActionButton("Abrir conteúdo", onClick = { viewModel.openSaved(context, item.sourceUrl) })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                androidx.compose.material3.OutlinedButton(onClick = { ExternalActions.copy(context, "Link", link) }, modifier = Modifier.weight(1f)) { Text("Copiar") }
                androidx.compose.material3.OutlinedButton(onClick = { shareSaved(context, link) }, modifier = Modifier.weight(1f)) { Text("Compartilhar") }
            }
            androidx.compose.material3.TextButton(onClick = { viewModel.archive(item.id); onBack() }) { Text("Arquivar") }
            androidx.compose.material3.TextButton(onClick = { viewModel.delete(item.id); onBack() }) { Text("Excluir") }
        }
    }
}

private fun shareSaved(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }
    context.startActivity(Intent.createChooser(send, "Compartilhar").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
