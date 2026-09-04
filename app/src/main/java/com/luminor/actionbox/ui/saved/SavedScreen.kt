package com.luminor.actionbox.ui.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luminor.actionbox.ActionViewModel
import com.luminor.actionbox.ui.components.EmptyState

@Composable
fun SavedScreen(viewModel: ActionViewModel) {
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text("Depois", style = MaterialTheme.typography.headlineMedium)
            Text("Links e conteúdos que você guardou para voltar depois.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (saved.isEmpty()) {
            EmptyState("🔖", "Nada salvo", "Compartilhe um link com o ActionBox ou cole um link na tela inicial.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(saved, key = { it.id }) { item ->
                    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(categoryEmoji(item.metadata))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.title, fontWeight = FontWeight.Medium)
                                Text(item.sourceUrl ?: item.content, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            TextButton(onClick = { viewModel.openSaved(context, item.sourceUrl) }) { Text("Abrir") }
                            TextButton(onClick = { viewModel.archive(item.id) }) { Text("Arquivar") }
                        }
                    }
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
