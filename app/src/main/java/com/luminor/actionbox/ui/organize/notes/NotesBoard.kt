package com.luminor.actionbox.ui.organize.notes

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.item
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.luminor.actionbox.ActionViewModel
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.domain.ActionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private enum class NoteSort(val label: String) { RECENT("Mais recentes"), OLDEST("Mais antigas"), TITLE("Título A-Z"), CATEGORY("Categoria"), COLOR("Cor") }

@Composable
fun NotesBoard(notes: List<ActionEntity>, viewModel: ActionViewModel, onOpen: (Long) -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("Todas") }
    var sort by remember { mutableStateOf(NoteSort.RECENT) }
    var searchVisible by remember { mutableStateOf(false) }
    var menuNote by remember { mutableStateOf<ActionEntity?>(null) }
    var sortOpen by remember { mutableStateOf(false) }

    val filtered = notes.filter { note ->
        val matchesQuery = query.isBlank() || listOf(note.title, note.content, note.noteCategory.orEmpty()).any { it.contains(query, ignoreCase = true) }
        val matchesFilter = when (filter) {
            "Todas" -> true
            "Fixadas" -> note.isPinned
            else -> note.noteCategory == filter
        }
        matchesQuery && matchesFilter
    }
    val sorted = when (sort) {
        NoteSort.RECENT -> filtered.sortedByDescending { it.updatedAt ?: it.createdAt }
        NoteSort.OLDEST -> filtered.sortedBy { it.updatedAt ?: it.createdAt }
        NoteSort.TITLE -> filtered.sortedBy { it.title.lowercase() }
        NoteSort.CATEGORY -> filtered.sortedBy { it.noteCategory.orEmpty() }
        NoteSort.COLOR -> filtered.sortedBy { it.noteColor.orEmpty() }
    }
    val pinned = sorted.filter { it.isPinned }
    val others = sorted.filterNot { it.isPinned }
    val today = LocalDate.now()
    fun noteDate(note: ActionEntity) = Instant.ofEpochMilli(note.updatedAt ?: note.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
    val todayNotes = others.filter { noteDate(it) == today }
    val yesterdayNotes = others.filter { noteDate(it) == today.minusDays(1) }
    val older = others.filter { noteDate(it).isBefore(today.minusDays(1)) }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Notas", style = MaterialTheme.typography.headlineSmall)
                Text("Todas as notas · ${notes.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { searchVisible = !searchVisible }) { Icon(Icons.Rounded.Search, contentDescription = "Buscar notas") }
            Surface(onClick = {
                viewModel.processInput("Nova nota")
                viewModel.chooseType(ActionType.NOTE)
                viewModel.saveDetected(context)
            }, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text("Nova")
                }
            }
        }
        if (searchVisible) OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Buscar notas") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            NoteCategoryFilter(filter, onSelected = { filter = it })
            Text(sort.label, modifier = Modifier.padding(start = 8.dp))
            IconButton(onClick = { sortOpen = true }) { Text("⋮") }
            DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                NoteSort.entries.forEach { option -> DropdownMenuItem(text = { Text(option.label) }, onClick = { sort = option; sortOpen = false }) }
            }
        }

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalItemSpacing = 10.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            fun header(title: String, count: Int) = item(span = StaggeredGridItemSpan.FullLine) { Text("$title · $count", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)) }
            if (pinned.isNotEmpty()) {
                header("📌 FIXADAS", pinned.size)
                items(pinned, key = { "p-${it.id}" }) { note -> NoteCard(note, onClick = { onOpen(note.id) }, onMenu = { menuNote = note }) }
            }
            if (todayNotes.isNotEmpty()) {
                header("HOJE", todayNotes.size)
                items(todayNotes, key = { "t-${it.id}" }) { note -> NoteCard(note, onClick = { onOpen(note.id) }, onMenu = { menuNote = note }) }
            }
            if (yesterdayNotes.isNotEmpty()) {
                header("ONTEM", yesterdayNotes.size)
                items(yesterdayNotes, key = { "y-${it.id}" }) { note -> NoteCard(note, onClick = { onOpen(note.id) }, onMenu = { menuNote = note }) }
            }
            if (older.isNotEmpty()) {
                header("ANTERIORES", older.size)
                items(older, key = { "o-${it.id}" }) { note -> NoteCard(note, onClick = { onOpen(note.id) }, onMenu = { menuNote = note }) }
            }
            if (sorted.isEmpty()) item(span = StaggeredGridItemSpan.FullLine) { Text("Nenhuma nota encontrada.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }

    menuNote?.let { note ->
        androidx.compose.material3.ModalBottomSheet(onDismissRequest = { menuNote = null }) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                DropdownMenuItem(text = { Text(if (note.isPinned) "Desafixar" else "Fixar") }, onClick = {
                    viewModel.updateAction(context, note, note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis())); menuNote = null
                })
                DropdownMenuItem(text = { Text("Alterar cor") }, onClick = {
                    val current = NotePalette.indexOfFirst { it.key == note.noteColor }.let { if (it < 0) 0 else it }
                    val next = NotePalette[(current + 1) % NotePalette.size].key
                    viewModel.updateAction(context, note, note.copy(noteColor = next, updatedAt = System.currentTimeMillis())); menuNote = null
                })
                DropdownMenuItem(text = { Text("Alterar categoria") }, onClick = {
                    val current = DefaultNoteCategories.indexOf(note.noteCategory).let { if (it < 0) -1 else it }
                    val next = DefaultNoteCategories[(current + 1) % DefaultNoteCategories.size]
                    viewModel.updateAction(context, note, note.copy(noteCategory = next, updatedAt = System.currentTimeMillis())); menuNote = null
                })
                DropdownMenuItem(text = { Text("Compartilhar") }, onClick = {
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "${note.title}\n\n${note.content}") }, "Compartilhar nota")); menuNote = null
                })
                DropdownMenuItem(text = { Text("Arquivar") }, onClick = { viewModel.archive(note.id); menuNote = null })
                DropdownMenuItem(text = { Text("Excluir") }, onClick = { viewModel.delete(note.id); menuNote = null })
            }
        }
    }
}
