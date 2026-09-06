package com.luminor.actionbox.ui.organize.lists

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import com.luminor.actionbox.domain.OrganizationOwnerType
import com.luminor.actionbox.ui.components.ReorderHandle
import com.luminor.actionbox.ui.components.moved
import com.luminor.actionbox.ui.designsystem.ActionBoxIcons
import com.luminor.actionbox.ui.designsystem.components.ActionButton
import com.luminor.actionbox.ui.designsystem.components.ActionCard
import com.luminor.actionbox.ui.tags.TagPickerBottomSheet

@Composable
fun ListDetailScreen(
    viewModel: ActionViewModel,
    listId: Long,
    onBack: () -> Unit
) {
    val lists by viewModel.lists.collectAsStateWithLifecycle()
    val allItems by viewModel.listItems.collectAsStateWithLifecycle()
    val allTags by viewModel.tags.collectAsStateWithLifecycle()
    val tagRefs by viewModel.tagRefs.collectAsStateWithLifecycle()
    val list = lists.firstOrNull { it.id == listId } ?: return
    val listItems = allItems.filter { it.listId == listId }.sortedBy { it.position }
    val done = listItems.count { it.completedAt != null }
    val progress = if (listItems.isEmpty()) 0f else done.toFloat() / listItems.size

    var editing by remember(listId) { mutableStateOf(false) }
    var editState by remember(listId) { mutableStateOf(ListEditState.from(list, listItems)) }
    var newItemText by remember(listId) { mutableStateOf("") }
    var discardDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    var tagsOpen by remember { mutableStateOf(false) }

    val selectedTagIds = tagRefs
        .filter { it.ownerType == OrganizationOwnerType.LIST && it.ownerId == listId }
        .map { it.tagId }
        .toSet()

    fun beginEditing() {
        editState = ListEditState.from(list, listItems)
        newItemText = ""
        editing = true
    }

    fun leaveEditing() {
        editing = false
        newItemText = ""
    }

    fun requestBack() {
        if (editing && editState.hasChanges) discardDialog = true
        else if (editing) leaveEditing()
        else onBack()
    }

    BackHandler(enabled = editing) { requestBack() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 760.dp)
            .statusBarsPadding()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = ::requestBack) { Icon(ActionBoxIcons.Back, contentDescription = "Voltar") }
                Text("Lista", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                if (editing) {
                    TextButton(onClick = { if (editState.hasChanges) discardDialog = true else leaveEditing() }) { Text("Cancelar") }
                } else {
                    TextButton(onClick = ::beginEditing) {
                        Icon(Icons.Rounded.Edit, contentDescription = null)
                        Text("Editar")
                    }
                }
            }
        }

        if (editing) {
            item {
                OutlinedTextField(
                    value = editState.title,
                    onValueChange = { editState = editState.copy(title = it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Título da lista") }
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Itens", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text("${editState.items.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(
                count = editState.items.size,
                key = { index ->
                    val item = editState.items[index]
                    if (item.id == 0L) "new-$index-${item.title}" else "item-${item.id}"
                }
            ) { index ->
                val draft = editState.items[index]
                ActionCard {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ReorderHandle(
                            index = index,
                            itemCount = editState.items.size,
                            onMove = { from, to -> editState = editState.copy(items = editState.items.moved(from, to)) }
                        )
                        Checkbox(checked = draft.completedAt != null, onCheckedChange = null)
                        OutlinedTextField(
                            value = draft.title,
                            onValueChange = { value ->
                                val changed = editState.items.toMutableList().also { it[index] = draft.copy(title = value) }
                                editState = editState.copy(items = changed)
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(onClick = {
                            editState = editState.copy(items = editState.items.toMutableList().also { it.removeAt(index) })
                        }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remover item", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            item {
                ActionCard {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newItemText,
                            onValueChange = { newItemText = it },
                            label = { Text("Novo item") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val title = newItemText.trim()
                                if (title.isNotBlank()) {
                                    editState = editState.copy(
                                        items = editState.items + ListItemEditState(title = title, position = editState.items.size)
                                    )
                                    newItemText = ""
                                }
                            },
                            enabled = newItemText.isNotBlank()
                        ) { Icon(Icons.Rounded.Add, contentDescription = "Adicionar item") }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { if (editState.hasChanges) discardDialog = true else leaveEditing() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancelar") }
                    Button(
                        onClick = {
                            val originalIds = listItems.map { it.id }.toSet()
                            val currentIds = editState.items.map { it.id }.filter { it != 0L }.toSet()
                            val deletedIds = originalIds - currentIds
                            viewModel.saveListEdits(
                                list = list,
                                title = editState.title,
                                items = editState.items.mapIndexed { index, draft -> draft.toEntity(list.id, index) },
                                deletedItemIds = deletedIds
                            )
                            leaveEditing()
                        },
                        enabled = editState.title.trim().isNotBlank() && editState.items.all { it.title.trim().isNotBlank() } && editState.hasChanges,
                        modifier = Modifier.weight(1f)
                    ) { Text("Salvar") }
                }
            }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        list.title,
                        style = MaterialTheme.typography.headlineLarge,
                        textDecoration = if (list.completedAt != null) TextDecoration.LineThrough else null
                    )
                    Text("$done de ${listItems.size} concluídos", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (listItems.isNotEmpty()) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
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
            if (listItems.isEmpty()) {
                item { Text("Nenhum item. Toque em Editar para adicionar.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                items(listItems, key = { it.id }) { listItem ->
                    ActionCard {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = listItem.completedAt != null, onCheckedChange = { viewModel.toggleListItem(listItem) })
                            Text(
                                listItem.title,
                                modifier = Modifier.weight(1f),
                                textDecoration = if (listItem.completedAt != null) TextDecoration.LineThrough else null
                            )
                        }
                    }
                }
            }
            item {
                when {
                    listItems.isNotEmpty() && done == listItems.size && list.completedAt == null -> ActionButton("Finalizar lista", onClick = { viewModel.finishList(list.id) })
                    list.completedAt != null -> ActionButton("Reabrir lista", onClick = { viewModel.reopenList(list.id) }, primary = false)
                }
            }
            item {
                HorizontalDivider()
                TextButton(onClick = { deleteDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Text("Mover lista para a lixeira", color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }

    if (tagsOpen) {
        TagPickerBottomSheet(
            tags = allTags,
            selectedIds = selectedTagIds,
            onToggle = { tag ->
                viewModel.setTagsForOwner(
                    OrganizationOwnerType.LIST,
                    listId,
                    if (tag.id in selectedTagIds) selectedTagIds - tag.id else selectedTagIds + tag.id
                )
            },
            onCreate = { viewModel.createAndAttachTag(OrganizationOwnerType.LIST, listId, it) },
            onDismiss = { tagsOpen = false }
        )
    }

    if (discardDialog) {
        AlertDialog(
            onDismissRequest = { discardDialog = false },
            title = { Text("Descartar alterações?") },
            text = { Text("As alterações da lista não serão salvas.") },
            dismissButton = { TextButton(onClick = { discardDialog = false }) { Text("Continuar editando") } },
            confirmButton = { TextButton(onClick = { discardDialog = false; leaveEditing() }) { Text("Descartar") } }
        )
    }

    if (deleteDialog) {
        AlertDialog(
            onDismissRequest = { deleteDialog = false },
            title = { Text("Mover lista para a lixeira?") },
            text = { Text("A lista poderá ser restaurada durante 30 dias.") },
            dismissButton = { TextButton(onClick = { deleteDialog = false }) { Text("Cancelar") } },
            confirmButton = {
                TextButton(onClick = { deleteDialog = false; viewModel.deleteList(list.id); onBack() }) {
                    Text("Mover", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}
