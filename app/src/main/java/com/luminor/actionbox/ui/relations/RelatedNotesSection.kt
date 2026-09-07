package com.luminor.actionbox.ui.relations

import com.luminor.actionbox.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.data.local.ContentLinkEntity
import com.luminor.actionbox.domain.OrganizationOwnerType
import com.luminor.actionbox.ui.designsystem.components.ActionCard

@Composable
fun RelatedNotesSection(
    ownerType: String,
    ownerId: Long,
    notes: List<ActionEntity>,
    links: List<ContentLinkEntity>,
    onLink: (Long) -> Unit,
    onUnlink: (Long) -> Unit,
    onOpenNote: (Long) -> Unit = {}
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources

    var pickerOpen by remember { mutableStateOf(false) }
    val ownerLinks = links.filter {
        it.sourceType == OrganizationOwnerType.NOTE &&
            it.targetType == ownerType &&
            it.targetId == ownerId
    }
    val linkedIds = ownerLinks.map { it.sourceId }.toSet()
    val linkedNotes = notes.filter { it.id in linkedIds }
    val available = notes.filterNot { it.id in linkedIds }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(textResources.getString(R.string.text_notas_relacionadas), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = { pickerOpen = true }, enabled = available.isNotEmpty()) { Text(textResources.getString(R.string.text_vincular)) }
            DropdownMenu(expanded = pickerOpen, onDismissRequest = { pickerOpen = false }) {
                if (available.isEmpty()) {
                    DropdownMenuItem(text = { Text(textResources.getString(R.string.text_nenhuma_nota_disponivel)) }, onClick = { pickerOpen = false })
                } else {
                    available.take(30).forEach { note ->
                        DropdownMenuItem(
                            text = { Text(note.title.ifBlank { textResources.getString(R.string.text_sem_titulo) }) },
                            onClick = { onLink(note.id); pickerOpen = false }
                        )
                    }
                }
            }
        }
        if (linkedNotes.isEmpty()) {
            Text(textResources.getString(R.string.text_nenhuma_nota_vinculada), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            linkedNotes.forEach { note ->
                val link = ownerLinks.firstOrNull { it.sourceId == note.id }
                ActionCard(onClick = { onOpenNote(note.id) }) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📝", modifier = Modifier.padding(end = 10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(note.title.ifBlank { textResources.getString(R.string.text_sem_titulo) }, style = MaterialTheme.typography.bodyLarge)
                            if (note.noteCategory != null) Text(note.noteCategory, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (link != null) {
                            IconButton(onClick = { onUnlink(link.id) }) {
                                Icon(Icons.Rounded.Close, contentDescription = textResources.getString(R.string.text_desvincular_nota))
                            }
                        }
                    }
                }
            }
        }
    }
}
