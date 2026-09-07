package com.luminor.actionbox.ui.organize

import com.luminor.actionbox.R
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
import com.luminor.actionbox.data.local.ActionListEntity
import com.luminor.actionbox.data.local.ListItemEntity
import com.luminor.actionbox.ui.designsystem.components.ActionButton
import com.luminor.actionbox.ui.designsystem.components.ActionCard

@Composable
fun ListRichCard(
    list: ActionListEntity,
    items: List<ListItemEntity>,
    onToggle: (com.luminor.actionbox.data.local.ListItemEntity) -> Unit,
    onFinish: (Long) -> Unit,
    onReopen: (Long) -> Unit,
    onOpen: (() -> Unit)? = null
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources

    val done = items.count { it.completedAt != null }
    val progress = if (items.isEmpty()) 0f else done.toFloat() / items.size
    val ready = items.isNotEmpty() && done == items.size && list.completedAt == null

    ActionCard(onClick = onOpen) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(list.title, style = MaterialTheme.typography.titleLarge, textDecoration = if (list.completedAt != null) TextDecoration.LineThrough else null)
                    Text(if (list.completedAt != null) textResources.getString(R.string.text_lista_finalizada) else textResources.getString(R.string.text_de_itens , done, items.size), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("☑️", style = MaterialTheme.typography.headlineMedium)
            }
            if (items.isNotEmpty()) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            items.take(7).forEach { item ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = item.completedAt != null, onCheckedChange = { onToggle(item) })
                    Text(item.title, style = MaterialTheme.typography.bodyMedium, textDecoration = if (item.completedAt != null) TextDecoration.LineThrough else null)
                }
            }
            if (items.size > 7) Text(textResources.getString(R.string.text_itens_165 , items.size - 7), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            when {
                ready -> ActionButton(textResources.getString(R.string.text_finalizar_lista), onClick = { onFinish(list.id) })
                list.completedAt != null -> ActionButton(textResources.getString(R.string.text_reabrir_lista), onClick = { onReopen(list.id) }, primary = false)
            }
        }
    }
}
