package com.luminor.actionbox.ui.organize

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luminor.actionbox.ActionViewModel
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.OrganizationOwnerType
import com.luminor.actionbox.ui.components.ReorderHandle
import com.luminor.actionbox.ui.components.moved
import com.luminor.actionbox.ui.designsystem.ActionBoxIcons
import com.luminor.actionbox.ui.designsystem.components.ActionButton
import com.luminor.actionbox.ui.designsystem.components.ActionCard
import com.luminor.actionbox.ui.motion.SharedKeys
import com.luminor.actionbox.ui.motion.actionSharedBounds
import com.luminor.actionbox.ui.relations.RelatedNotesSection
import com.luminor.actionbox.ui.tags.TagPickerBottomSheet
import java.time.LocalDate

private data class ProjectTaskDraft(
    val id: Long?,
    val title: String
)

@Composable
fun ProjectDetailScreen(
    viewModel: ActionViewModel,
    projectId: Long,
    onBack: () -> Unit,
    onNoteOpen: (Long) -> Unit = {}
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val all by viewModel.all.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val links by viewModel.contentLinks.collectAsStateWithLifecycle()
    val allTags by viewModel.tags.collectAsStateWithLifecycle()
    val tagRefs by viewModel.tagRefs.collectAsStateWithLifecycle()
    val project = projects.firstOrNull { it.id == projectId }
    val tasks = all.filter { it.projectId == projectId }.sortedWith(compareBy({ it.sortOrder }, { it.createdAt }))
    if (project == null) return

    val done = tasks.count { it.status == ActionStatus.COMPLETED.name }
    val progress = if (tasks.isEmpty()) 0f else done.toFloat() / tasks.size

    var editing by remember(projectId) { mutableStateOf(false) }
    var draftTitle by remember(projectId) { mutableStateOf(project.title) }
    var draftDescription by remember(projectId) { mutableStateOf(project.description) }
    var draftTasks by remember(projectId) { mutableStateOf(tasks.map { ProjectTaskDraft(it.id, it.title) }) }
    var newTaskText by remember(projectId) { mutableStateOf("") }
    var showDiscardDialog by remember(projectId) { mutableStateOf(false) }
    var showDeleteDialog by remember(projectId) { mutableStateOf(false) }
    var tagsOpen by remember(projectId) { mutableStateOf(false) }

    val selectedTagIds = tagRefs
        .filter { it.ownerType == OrganizationOwnerType.PROJECT && it.ownerId == projectId }
        .map { it.tagId }
        .toSet()
    val originalTitles = tasks.associate { it.id to it.title }
    val originalOrder = tasks.map { it.id }
    val draftExisting = draftTasks.mapNotNull { draft -> draft.id?.let { id -> id to draft.title } }.toMap()
    val draftOrder = draftTasks.mapNotNull { it.id }
    val hasChanges = editing && (
        draftTitle != project.title ||
            draftDescription != project.description ||
            draftTasks.any { it.id == null } ||
            draftExisting.keys != originalTitles.keys ||
            draftExisting.any { (id, title) -> originalTitles[id] != title } ||
            draftOrder != originalOrder
        )
    val canSave = draftTitle.trim().isNotBlank() && draftTasks.all { it.title.trim().isNotBlank() } && hasChanges

    fun beginEditing() {
        draftTitle = project.title
        draftDescription = project.description
        draftTasks = tasks.map { ProjectTaskDraft(it.id, it.title) }
        newTaskText = ""
        editing = true
    }

    fun leaveEditing() {
        editing = false
        newTaskText = ""
    }

    fun requestBack() {
        if (editing) {
            if (hasChanges) showDiscardDialog = true else leaveEditing()
        } else onBack()
    }

    BackHandler(enabled = editing) { requestBack() }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .statusBarsPadding()
                .padding(horizontal = 18.dp)
                .actionSharedBounds(SharedKeys.project(projectId)),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { requestBack() }) { Icon(ActionBoxIcons.Back, contentDescription = "Voltar") }
                    Text("Projeto", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    if (editing) {
                        TextButton(onClick = { if (hasChanges) showDiscardDialog = true else leaveEditing() }) { Text("Cancelar") }
                    } else {
                        TextButton(onClick = { beginEditing() }) {
                            Icon(Icons.Rounded.Edit, contentDescription = null)
                            Text("Editar")
                        }
                    }
                }
            }

            if (editing) {
                item {
                    OutlinedTextField(
                        value = draftTitle,
                        onValueChange = { draftTitle = it },
                        label = { Text("Título do projeto") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = draftDescription,
                        onValueChange = { draftDescription = it },
                        label = { Text("Descrição") },
                        minLines = 2,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Tarefas", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text("${draftTasks.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                itemsIndexed(
                    draftTasks,
                    key = { index, draft -> draft.id?.let { "task-$it" } ?: "new-$index-${draft.title}" }
                ) { index, draft ->
                    val originalTask = draft.id?.let { id -> tasks.firstOrNull { it.id == id } }
                    ActionCard {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            ReorderHandle(
                                index = index,
                                itemCount = draftTasks.size,
                                onMove = { from, to -> draftTasks = draftTasks.moved(from, to) }
                            )
                            Icon(
                                imageVector = if (originalTask?.status == ActionStatus.COMPLETED.name) ActionBoxIcons.Check else ActionBoxIcons.EmptyCheck,
                                contentDescription = null,
                                tint = if (originalTask?.status == ActionStatus.COMPLETED.name) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = draft.title,
                                onValueChange = { value ->
                                    draftTasks = draftTasks.toMutableList().also { list -> list[index] = draft.copy(title = value) }
                                },
                                singleLine = true,
                                modifier = Modifier.weight(1f).padding(start = 8.dp)
                            )
                            IconButton(onClick = { draftTasks = draftTasks.toMutableList().also { it.removeAt(index) } }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remover tarefa", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                item {
                    ActionCard {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newTaskText,
                                onValueChange = { newTaskText = it },
                                label = { Text("Nova tarefa") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val title = newTaskText.trim()
                                    if (title.isNotBlank()) {
                                        draftTasks = draftTasks + ProjectTaskDraft(id = null, title = title)
                                        newTaskText = ""
                                    }
                                },
                                enabled = newTaskText.isNotBlank()
                            ) { Icon(Icons.Rounded.Add, contentDescription = "Adicionar tarefa") }
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { if (hasChanges) showDiscardDialog = true else leaveEditing() },
                            modifier = Modifier.weight(1f)
                        ) { Text("Cancelar") }
                        Button(
                            onClick = {
                                val existingTitles = draftTasks.mapNotNull { draft -> draft.id?.let { id -> id to draft.title.trim() } }.toMap()
                                val deletedIds = tasks.map { it.id }.toSet() - existingTitles.keys
                                val newTitles = draftTasks.filter { it.id == null }.map { it.title.trim() }
                                viewModel.saveProjectEdits(
                                    project = project,
                                    title = draftTitle,
                                    description = draftDescription,
                                    existingTaskTitles = existingTitles,
                                    newTaskTitles = newTitles,
                                    deletedTaskIds = deletedIds,
                                    orderedTaskIds = draftTasks.mapNotNull { it.id }
                                )
                                leaveEditing()
                            },
                            enabled = canSave,
                            modifier = Modifier.weight(1f)
                        ) { Text("Salvar") }
                    }
                }
            } else {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            project.title,
                            style = MaterialTheme.typography.headlineLarge,
                            textDecoration = if (project.completedAt != null) TextDecoration.LineThrough else null
                        )
                        if (project.description.isNotBlank()) Text(project.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$done de ${tasks.size} concluídas · ${(progress * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (tasks.isNotEmpty()) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    }
                }
                if (selectedTagIds.isNotEmpty()) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            allTags.filter { it.id in selectedTagIds }.forEach { tag -> Text("#${tag.name}", color = MaterialTheme.colorScheme.primary) }
                        }
                    }
                }
                item { TextButton(onClick = { tagsOpen = true }) { Text(if (selectedTagIds.isEmpty()) "+ Adicionar tags" else "Editar tags") } }
                item { Text("Tarefas", style = MaterialTheme.typography.titleMedium) }
                if (tasks.isEmpty()) {
                    item { Text("Nenhuma tarefa ainda. Toque em Editar para adicionar.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    itemsIndexed(tasks, key = { _, task -> task.id }) { _, task ->
                        ActionCard {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.toggleOccurrence(task, LocalDate.now()) }) {
                                    Icon(
                                        if (task.status == ActionStatus.COMPLETED.name) ActionBoxIcons.Check else ActionBoxIcons.EmptyCheck,
                                        contentDescription = if (task.status == ActionStatus.COMPLETED.name) "Reabrir tarefa" else "Concluir tarefa"
                                    )
                                }
                                Text(
                                    task.title,
                                    modifier = Modifier.weight(1f),
                                    textDecoration = if (task.status == ActionStatus.COMPLETED.name) TextDecoration.LineThrough else null
                                )
                            }
                        }
                    }
                }
                item {
                    RelatedNotesSection(
                        ownerType = OrganizationOwnerType.PROJECT,
                        ownerId = projectId,
                        notes = notes,
                        links = links,
                        onLink = { viewModel.linkNote(it, OrganizationOwnerType.PROJECT, projectId) },
                        onUnlink = viewModel::unlinkContentLink,
                        onOpenNote = onNoteOpen
                    )
                }
                item {
                    when {
                        tasks.isNotEmpty() && done == tasks.size && project.completedAt == null -> ActionButton("Finalizar projeto", onClick = { viewModel.finishProject(project.id) })
                        project.completedAt != null -> ActionButton("Reabrir projeto", onClick = { viewModel.reopenProject(project.id) }, primary = false)
                    }
                }
                item {
                    HorizontalDivider()
                    TextButton(onClick = { showDeleteDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text("Mover projeto para a lixeira", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }

    if (tagsOpen) {
        TagPickerBottomSheet(
            tags = allTags,
            selectedIds = selectedTagIds,
            onToggle = { tag ->
                viewModel.setTagsForOwner(
                    OrganizationOwnerType.PROJECT,
                    projectId,
                    if (tag.id in selectedTagIds) selectedTagIds - tag.id else selectedTagIds + tag.id
                )
            },
            onCreate = { viewModel.createAndAttachTag(OrganizationOwnerType.PROJECT, projectId, it) },
            onDismiss = { tagsOpen = false }
        )
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Descartar alterações?") },
            text = { Text("As alterações feitas no título, descrição, tarefas e ordem não serão salvas.") },
            confirmButton = {
                TextButton(onClick = { showDiscardDialog = false; leaveEditing() }) { Text("Descartar") }
            },
            dismissButton = { TextButton(onClick = { showDiscardDialog = false }) { Text("Continuar editando") } }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Mover projeto para a lixeira?") },
            text = { Text("O projeto e suas tarefas poderão ser restaurados durante 30 dias.") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; viewModel.deleteProject(project.id); onBack() }) {
                    Text("Mover", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
        )
    }
}
