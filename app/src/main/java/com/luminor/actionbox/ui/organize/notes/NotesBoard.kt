package com.luminor.actionbox.ui.organize.notes

import com.luminor.actionbox.R
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import com.luminor.actionbox.data.local.ActionEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private enum class NoteSort(val label: Int) { RECENT(R.string.text_mais_recentes), OLDEST(R.string.text_mais_antigas), TITLE(R.string.text_titulo_a_z), CATEGORY(R.string.text_categoria), COLOR(R.string.text_cor) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesBoard(notes: List<ActionEntity>, onUpdate: (android.content.Context, ActionEntity, ActionEntity) -> Unit, onArchive: (Long) -> Unit, onDelete: (Long) -> Unit, onCreate: () -> Unit, onOpen: (Long) -> Unit, initialScrollIndex: Int = 0, initialScrollOffset: Int = 0, onScrollChanged: (Int, Int) -> Unit = { _, _ -> }) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources

    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("Todas") }
    var sort by remember { mutableStateOf(NoteSort.RECENT) }
    var searchVisible by remember { mutableStateOf(false) }
    var menuNote by remember { mutableStateOf<ActionEntity?>(null) }
    var sortOpen by remember { mutableStateOf(false) }
    val gridState = rememberLazyStaggeredGridState(initialFirstVisibleItemIndex = initialScrollIndex, initialFirstVisibleItemScrollOffset = initialScrollOffset)

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.firstVisibleItemIndex to gridState.firstVisibleItemScrollOffset }
            .collect { (index, offset) -> onScrollChanged(index, offset) }
    }

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
                Text(textResources.getString(R.string.text_notas), style = MaterialTheme.typography.headlineSmall)
                Text(textResources.getString(R.string.text_todas_as_notas , notes.size), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { searchVisible = !searchVisible }) { Icon(Icons.Rounded.Search, contentDescription = textResources.getString(R.string.text_buscar_notas)) }
            Surface(onClick = {
                onCreate()
            }, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text(textResources.getString(R.string.text_nova))
                }
            }
        }
        if (searchVisible) OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text(textResources.getString(R.string.text_buscar_notas)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f)) { NoteCategoryFilter(filter, onSelected = { filter = it }) }
            IconButton(onClick = { sortOpen = true }) { Text("⋮") }
            DropdownMenu(expanded = sortOpen, onDismissRequest = { sortOpen = false }) {
                NoteSort.entries.forEach { option -> DropdownMenuItem(text = { Text(textResources.getString(option.label)) }, onClick = { sort = option; sortOpen = false }) }
            }
        }

        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalItemSpacing = 10.dp,
            modifier = Modifier.fillMaxSize(),
            state = gridState
        ) {
            if (pinned.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) { Text(textResources.getString(R.string.text_fixadas , pinned.size), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)) }
                items(pinned, key = { textResources.getString(R.string.text_p , it.id) }) { note -> NoteCard(note, onClick = { onOpen(note.id) }, onMenu = { menuNote = note }) }
            }
            if (todayNotes.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) { Text(textResources.getString(R.string.text_hoje_297 , todayNotes.size), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)) }
                items(todayNotes, key = { textResources.getString(R.string.text_t , it.id) }) { note -> NoteCard(note, onClick = { onOpen(note.id) }, onMenu = { menuNote = note }) }
            }
            if (yesterdayNotes.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) { Text(textResources.getString(R.string.text_ontem , yesterdayNotes.size), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)) }
                items(yesterdayNotes, key = { textResources.getString(R.string.text_y , it.id) }) { note -> NoteCard(note, onClick = { onOpen(note.id) }, onMenu = { menuNote = note }) }
            }
            if (older.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) { Text(textResources.getString(R.string.text_anteriores , older.size), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)) }
                items(older, key = { textResources.getString(R.string.text_o , it.id) }) { note -> NoteCard(note, onClick = { onOpen(note.id) }, onMenu = { menuNote = note }) }
            }
            if (sorted.isEmpty()) item(span = StaggeredGridItemSpan.FullLine) { Text(textResources.getString(R.string.text_nenhuma_nota_encontrada), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }

    menuNote?.let { note ->
        androidx.compose.material3.ModalBottomSheet(onDismissRequest = { menuNote = null }) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                DropdownMenuItem(text = { Text(if (note.isPinned) textResources.getString(R.string.text_desafixar) else textResources.getString(R.string.text_fixar)) }, onClick = {
                    onUpdate(context, note, note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis())); menuNote = null
                })
                DropdownMenuItem(text = { Text(textResources.getString(R.string.text_alterar_cor)) }, onClick = {
                    val current = NotePalette.indexOfFirst { it.key == note.noteColor }.let { if (it < 0) 0 else it }
                    val next = NotePalette[(current + 1) % NotePalette.size].key
                    onUpdate(context, note, note.copy(noteColor = next, updatedAt = System.currentTimeMillis())); menuNote = null
                })
                DropdownMenuItem(text = { Text(textResources.getString(R.string.text_alterar_categoria)) }, onClick = {
                    val current = DefaultNoteCategories.indexOf(note.noteCategory)
                    val next = DefaultNoteCategories[(current + 1).coerceAtLeast(0) % DefaultNoteCategories.size]
                    onUpdate(context, note, note.copy(noteCategory = next, updatedAt = System.currentTimeMillis())); menuNote = null
                })
                DropdownMenuItem(text = { Text(textResources.getString(R.string.text_compartilhar)) }, onClick = {
                    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, textResources.getString(R.string.text_n_n , note.title, note.content)) }, textResources.getString(R.string.text_compartilhar_nota))); menuNote = null
                })
                DropdownMenuItem(text = { Text(textResources.getString(R.string.text_arquivar)) }, onClick = { onArchive(note.id); menuNote = null })
                DropdownMenuItem(text = { Text(textResources.getString(R.string.text_excluir)) }, onClick = { onDelete(note.id); menuNote = null })
            }
        }
    }
}
