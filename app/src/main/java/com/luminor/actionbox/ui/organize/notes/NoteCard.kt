package com.luminor.actionbox.ui.organize.notes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.ui.motion.SharedKeys
import com.luminor.actionbox.ui.motion.actionSharedBounds
import com.luminor.actionbox.ui.motion.pressScale
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(note: ActionEntity, onClick: () -> Unit, onMenu: () -> Unit) {
    val background = noteBackground(note.noteColor)
    val accent = noteAccent(note.noteColor)
    val date = Instant.ofEpochMilli(note.updatedAt ?: note.createdAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .actionSharedBounds(SharedKeys.action(note.id))
            .pressScale()
            .combinedClickable(onClick = onClick, onLongClick = onMenu),
        shape = MaterialTheme.shapes.large,
        color = background
    ) {
        Row {
            Box(Modifier.width(4.dp).fillMaxHeight().background(accent))
            Column(Modifier.weight(1f).padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        (note.noteCategory ?: "Sem categoria").uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (note.isPinned) Icon(Icons.Rounded.PushPin, contentDescription = "Fixada", tint = accent)
                    IconButton(onClick = onMenu) { Icon(Icons.Rounded.MoreVert, contentDescription = "Opções") }
                }
                Text(note.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (note.content.isNotBlank() && note.content != note.title) {
                    Text(
                        note.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Text(
                    date.format(DateTimeFormatter.ofPattern("dd/MM · HH:mm")),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}
