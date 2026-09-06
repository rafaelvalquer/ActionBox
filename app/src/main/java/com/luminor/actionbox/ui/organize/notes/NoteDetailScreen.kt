package com.luminor.actionbox.ui.organize.notes

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luminor.actionbox.ActionViewModel
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.domain.OrganizationOwnerType
import com.luminor.actionbox.ui.motion.SharedKeys
import com.luminor.actionbox.ui.motion.actionSharedBounds
import com.luminor.actionbox.ui.relations.NoteLinksSection
import com.luminor.actionbox.ui.tags.TagPickerBottomSheet
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun NoteDetailScreen(viewModel: ActionViewModel, note: ActionEntity, onBack: () -> Unit) {
    val context = LocalContext.current
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val actions by viewModel.all.collectAsStateWithLifecycle()
    val links by viewModel.contentLinks.collectAsStateWithLifecycle()
    val allTags by viewModel.tags.collectAsStateWithLifecycle()
    val tagRefs by viewModel.tagRefs.collectAsStateWithLifecycle()

    var title by remember(note.id, note.updatedAt) { mutableStateOf(note.title) }
    var content by remember(note.id, note.updatedAt) { mutableStateOf(note.content) }
    var category by remember(note.id, note.updatedAt) { mutableStateOf(note.noteCategory) }
    var color by remember(note.id, note.updatedAt) { mutableStateOf(note.noteColor ?: "YELLOW") }
    var pinned by remember(note.id, note.updatedAt) { mutableStateOf(note.isPinned) }
    var menuOpen by remember { mutableStateOf(false) }
    var categoryOpen by remember { mutableStateOf(false) }
    var tagsOpen by remember { mutableStateOf(false) }
    val selectedTagIds = tagRefs
        .filter { it.ownerType == OrganizationOwnerType.ACTION && it.ownerId == note.id }
        .map { it.tagId }
        .toSet()
    val created = Instant.ofEpochMilli(note.createdAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
    val updated = note.updatedAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime() }

    fun save() {
        viewModel.updateAction(
            context,
            note,
            note.copy(
                title = title.ifBlank { "Sem título" },
                content = content,
                noteCategory = category,
                noteColor = color,
                isPinned = pinned,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = noteBackground(color)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .statusBarsPadding()
                .padding(18.dp)
                .actionSharedBounds(SharedKeys.action(note.id)),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, contentDescription = "Voltar") }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = ::save) { Text("Salvar", color = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = { pinned = !pinned }) {
                    Icon(
                        Icons.Rounded.PushPin,
                        contentDescription = "Fixar",
                        tint = if (pinned) noteAccent(color) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, contentDescription = "Opções") }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Salvar") }, onClick = { save(); menuOpen = false })
                    DropdownMenuItem(text = { Text("Compartilhar") }, onClick = {
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "$title\n\n$content")
                                },
                                "Compartilhar nota"
                            )
                        )
                        menuOpen = false
                    })
                    DropdownMenuItem(text = { Text("Arquivar") }, onClick = { viewModel.archive(note.id); menuOpen = false; onBack() })
                    DropdownMenuItem(text = { Text("Mover para lixeira") }, onClick = { viewModel.delete(note.id); menuOpen = false; onBack() })
                }
            }

            Surface(onClick = { categoryOpen = true }, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.42f)) {
                Text(category ?: "Sem categoria", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge)
            }
            DropdownMenu(expanded = categoryOpen, onDismissRequest = { categoryOpen = false }) {
                DropdownMenuItem(text = { Text("Sem categoria") }, onClick = { category = null; categoryOpen = false })
                DefaultNoteCategories.forEach { item ->
                    DropdownMenuItem(text = { Text(item) }, onClick = { category = item; categoryOpen = false })
                }
            }

            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.fillMaxWidth()
            )
            BasicTextField(
                value = content,
                onValueChange = { content = it },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.fillMaxWidth().weight(1f)
            )

            if (selectedTagIds.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    allTags.filter { it.id in selectedTagIds }.forEach { tag -> Text("#${tag.name}", color = MaterialTheme.colorScheme.primary) }
                }
            }
            TextButton(onClick = { tagsOpen = true }) { Text(if (selectedTagIds.isEmpty()) "+ Adicionar tags" else "Editar tags") }

            NoteLinksSection(
                noteId = note.id,
                projects = projects,
                actions = actions,
                links = links,
                onLink = { targetType, targetId -> viewModel.linkNote(note.id, targetType, targetId) },
                onUnlink = viewModel::unlinkContentLink
            )

            NoteColorPicker(color, onSelected = { color = it })
            Text(
                "Criada ${created.format(DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm"))}" +
                    (updated?.let { "  ·  Editada ${it.format(DateTimeFormatter.ofPattern("dd/MM · HH:mm"))}" } ?: ""),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(onClick = { save() }, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primary) {
                Text("Salvar alterações", modifier = Modifier.fillMaxWidth().padding(14.dp), color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }

    if (tagsOpen) {
        TagPickerBottomSheet(
            tags = allTags,
            selectedIds = selectedTagIds,
            onToggle = { tag ->
                viewModel.setTagsForOwner(
                    OrganizationOwnerType.ACTION,
                    note.id,
                    if (tag.id in selectedTagIds) selectedTagIds - tag.id else selectedTagIds + tag.id
                )
            },
            onCreate = { viewModel.createAndAttachTag(OrganizationOwnerType.ACTION, note.id, it) },
            onDismiss = { tagsOpen = false }
        )
    }
}
