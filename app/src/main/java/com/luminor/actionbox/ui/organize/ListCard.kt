package com.luminor.actionbox.ui.organize

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.luminor.actionbox.ActionViewModel
import com.luminor.actionbox.data.local.ActionListEntity
import com.luminor.actionbox.data.local.ListItemEntity
import com.luminor.actionbox.ui.designsystem.components.ActionButton
import com.luminor.actionbox.ui.designsystem.components.ActionCard

@Composable
fun ListRichCard(list: ActionListEntity, items: List<ListItemEntity>, viewModel: ActionViewModel) {
    val done = items.count { it.completedAt != null }
    val progress = if (items.isEmpty()) 0f else done.toFloat() / items.size
    val ready = items.isNotEmpty() && done == items.size && list.completedAt == null

    ActionCard {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(list.title, style = MaterialTheme.typography.titleLarge, textDecoration = if (list.completedAt != null) TextDecoration.LineThrough else null)
                    Text(if (list.completedAt != null) "Lista finalizada" else "$done de ${items.size} itens", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("☑️", style = MaterialTheme.typography.headlineMedium)
            }
            if (items.isNotEmpty()) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            items.take(7).forEach { item ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = item.completedAt != null, onCheckedChange = { viewModel.toggleListItem(item) })
                    Text(item.title, style = MaterialTheme.typography.bodyMedium, textDecoration = if (item.completedAt != null) TextDecoration.LineThrough else null)
                }
            }
            when {
                ready -> ActionButton("Finalizar lista", onClick = { viewModel.finishList(list.id) })
                list.completedAt != null -> ActionButton("Reabrir lista", onClick = { viewModel.reopenList(list.id) }, primary = false)
            }
        }
    }
}
