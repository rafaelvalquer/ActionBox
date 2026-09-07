from pathlib import Path
import re
root=Path('app/src/main/java/com/luminor/actionbox')
def write(p,s):
    p=Path(p);p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf-8')
def edit(p,f):
    p=Path(p);write(p,f(p.read_text(encoding='utf-8')))

# Use feature callbacks in reusable organization cards.
edit(root/'ui/organize/ListCard.kt',lambda s:s.replace('import com.luminor.actionbox.ActionViewModel\n','').replace('    viewModel: ActionViewModel,','    onToggle: (com.luminor.actionbox.data.local.ListItemEntity) -> Unit,\n    onFinish: (Long) -> Unit,\n    onReopen: (Long) -> Unit,').replace('viewModel.toggleListItem','onToggle').replace('viewModel.finishList','onFinish').replace('viewModel.reopenList','onReopen'))
edit(root/'ui/organize/HabitCard.kt',lambda s:s.replace('import com.luminor.actionbox.ActionViewModel\n','').replace('    viewModel: ActionViewModel,','    occursOn: (ActionEntity, LocalDate) -> Boolean,\n    completedOn: (ActionEntity, LocalDate) -> Boolean,\n    onToggle: (ActionEntity, LocalDate) -> Unit,').replace('    onOpen: (() -> Unit)? = null','    hapticsEnabled: Boolean,\n    onOpen: (() -> Unit)? = null').replace('    val settings by viewModel.settings.collectAsStateWithLifecycle()\n','').replace('viewModel.routineOccursOn','occursOn').replace('viewModel.isCompletedOn','completedOn').replace('viewModel.toggleOccurrence','onToggle').replace('                viewModel = viewModel,','                occursOn = occursOn,\n                completedOn = completedOn,\n                onToggle = onToggle,').replace('settings.hapticsEnabled','hapticsEnabled'))
edit(root/'ui/organize/notes/NotesBoard.kt',lambda s:s.replace('import com.luminor.actionbox.ActionViewModel\n','').replace('fun NotesBoard(notes: List<ActionEntity>, viewModel: ActionViewModel, onOpen: (Long) -> Unit)', 'fun NotesBoard(notes: List<ActionEntity>, onUpdate: (android.content.Context, ActionEntity, ActionEntity) -> Unit, onArchive: (Long) -> Unit, onDelete: (Long) -> Unit, onCreate: () -> Unit, onOpen: (Long) -> Unit)').replace('    val notesViewModel = composeViewModel<NotesViewModel>()\n','').replace('notesViewModel.createBlankNote()', 'onCreate()').replace('viewModel.updateAction','onUpdate').replace('viewModel.archive','onArchive').replace('viewModel.delete','onDelete'))

edit(root/'ui/organize/OrganizeViewModel.kt',lambda s:s.replace('    private val uiEventBus:', '    private val actionCommands: com.luminor.actionbox.domain.commands.ActionCommands,\n    private val listCommands: com.luminor.actionbox.domain.commands.ListCommands,\n    private val settingsRepository: com.luminor.actionbox.data.preferences.SettingsRepository,\n    uiEventBus:').replace(') : androidx.lifecycle.ViewModel()', ') : com.luminor.actionbox.ui.events.EventViewModel(uiEventBus)').replace('    val actions = repository.all.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())','''    val projectActions = repository.observeAllProjectActions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val actions = repository.observeRoutines().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val routineRules = repository.routineRules.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.luminor.actionbox.domain.UiSettings())
    fun toggleListItem(item: com.luminor.actionbox.data.local.ListItemEntity) = execute { listCommands.toggleListItem(item) }
    fun finishList(id: Long) = execute { listCommands.finishList(id) }
    fun reopenList(id: Long) = execute { listCommands.reopenList(id) }
    fun toggleOccurrence(action: com.luminor.actionbox.data.local.ActionEntity, date: java.time.LocalDate) = execute { actionCommands.toggleOccurrence(action, date) }
    fun updateAction(context: android.content.Context, original: com.luminor.actionbox.data.local.ActionEntity, updated: com.luminor.actionbox.data.local.ActionEntity) = execute { actionCommands.updateAction(context, original, updated) }
    fun archive(id: Long) = execute { actionCommands.archive(id) }
    fun delete(id: Long) = execute { actionCommands.delete(id) }
'''))
p=root/'ui/organize/OrganizeScreen.kt';s=p.read_text(encoding='utf-8')
s=s.replace('import com.luminor.actionbox.ActionViewModel\n','').replace('    actionViewModel: ActionViewModel,','    organizeViewModel: OrganizeViewModel,\n    notesViewModel: com.luminor.actionbox.ui.organize.notes.NotesViewModel,').replace('    onSearch: () -> Unit,\n    organizeViewModel: OrganizeViewModel = viewModel()', '    onSearch: () -> Unit')
start=s.index('    val all by');end=s.index('    var selectedTagId',start)
s=s[:start]+'''    var section by remember { mutableIntStateOf(0) }
    val all = when (section) {
        0 -> organizeViewModel.projectActions.collectAsStateWithLifecycle().value
        2 -> organizeViewModel.actions.collectAsStateWithLifecycle().value
        else -> emptyList()
    }
    val projects = if (section == 0) organizeViewModel.projects.collectAsStateWithLifecycle().value else emptyList()
    val lists = if (section == 1) organizeViewModel.lists.collectAsStateWithLifecycle().value else emptyList()
    val listItems = if (section == 1) organizeViewModel.listItems.collectAsStateWithLifecycle().value else emptyList()
    val notes = if (section == 3) organizeViewModel.notes.collectAsStateWithLifecycle().value else emptyList()
    val completions = if (section == 2) organizeViewModel.completions.collectAsStateWithLifecycle().value else emptyList()
    val rules = if (section == 2) organizeViewModel.routineRules.collectAsStateWithLifecycle().value else emptyList()
    val settings by organizeViewModel.settings.collectAsStateWithLifecycle()
    val tags by organizeViewModel.tags.collectAsStateWithLifecycle()
    val tagRefs by organizeViewModel.tagRefs.collectAsStateWithLifecycle()
'''+s[end:]
s=s.replace('NotesBoard(notes = visibleNotes, viewModel = actionViewModel, onOpen = onNoteOpen)','NotesBoard(notes = visibleNotes, onUpdate = { context, original, updated -> organizeViewModel.updateAction(context, original, updated) }, onArchive = { organizeViewModel.archive(it) }, onDelete = { organizeViewModel.delete(it) }, onCreate = { notesViewModel.createBlankNote() }, onOpen = onNoteOpen)')
s=s.replace('                                    viewModel = actionViewModel,','                                    onToggle = { organizeViewModel.toggleListItem(it) },\n                                    onFinish = { organizeViewModel.finishList(it) },\n                                    onReopen = { organizeViewModel.reopenList(it) },')
s=s.replace('HabitRichCard(action, actionViewModel, onOpen = { onRoutineOpen(action.id) })','''HabitRichCard(action,
                                    occursOn = { entity, date -> com.luminor.actionbox.domain.routine.RoutineEvaluation.routineOccursOn(entity, date, rules) },
                                    completedOn = { entity, date -> com.luminor.actionbox.domain.routine.RoutineEvaluation.isCompletedOn(entity, date, completions) },
                                    onToggle = { entity, date -> organizeViewModel.toggleOccurrence(entity, date) },
                                    hapticsEnabled = settings.hapticsEnabled, onOpen = { onRoutineOpen(action.id) })''')
write(p,s)

# Typed state for detail loading, missing IDs and failures, collected only by the destination.
write(root/'ui/DetailState.kt','''package com.luminor.actionbox.ui

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*

sealed interface DetailState<out T> {
    data object Loading : DetailState<Nothing>
    data object Missing : DetailState<Nothing>
    data class Loaded<T>(val value: T) : DetailState<T>
    data object Error : DetailState<Nothing>
}
fun <T> Flow<T?>.detailState(): Flow<DetailState<T>> = map<T?, DetailState<T>> {
    if (it == null) DetailState.Missing else DetailState.Loaded(it)
}.onStart { emit(DetailState.Loading) }.catch {
    if (it is CancellationException) throw it
    emit(DetailState.Error)
}

@Composable
fun <T> DetailContent(state: DetailState<T>, onBack: () -> Unit, onRetry: () -> Unit, content: @Composable (T) -> Unit) {
    when (state) {
        is DetailState.Loaded -> content(state.value)
        else -> Column(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = onBack) { Text("Voltar") }
            when (state) {
                DetailState.Loading -> CircularProgressIndicator()
                DetailState.Missing -> Text("Este item não está mais disponível.")
                DetailState.Error -> {
                    Text("Não foi possível carregar este item.")
                    TextButton(onClick = onRetry) { Text("Tentar novamente") }
                }
                else -> Unit
            }
        }
    }
}
''')
for rel,expr in [('ui/actions/ActionEditorViewModel.kt','repository.observeAction(id)'),('ui/organize/ProjectViewModel.kt','repository.observeProject(id)'),('ui/organize/lists/ListViewModel.kt','repository.observeList(id)'),('ui/organize/routines/RoutineViewModel.kt','repository.observeAction(id)'),('ui/organize/notes/NoteDetailViewModel.kt','repository.observeAction(id)'),('ui/saved/SavedDetailViewModel.kt','repository.observeAction(id)')]:
    edit(root/rel,lambda s:s.replace('import androidx.lifecycle.SavedStateHandle','import com.luminor.actionbox.ui.detailState\nimport androidx.lifecycle.SavedStateHandle').replace('    val settings =','''    private val retry = MutableStateFlow(0)
    fun retry() { retry.value++ }
    val detail = retry.flatMapLatest { '''+expr+'''.detailState() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), com.luminor.actionbox.ui.DetailState.Loading)
    val settings ='''))

p=root/'navigation/ActionBoxRoot.kt';s=p.read_text(encoding='utf-8')
s=s.replace('import com.luminor.actionbox.ActionViewModel','''import com.luminor.actionbox.ui.RootViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.luminor.actionbox.ui.DetailContent''').replace('viewModel: ActionViewModel','viewModel: RootViewModel')
s=s.replace('    LaunchedEffect(Unit) { viewModel.message.collect { snackbar.showSnackbar(it) } }\n','')
s=s.replace('                            viewModel = viewModel,\n                            captureViewModel', '                            viewModel = hiltViewModel(),\n                            captureViewModel',1)
s=s.replace('                            onSettings =', '                            onHistory = { navController.navigate("history") },\n                            onSettings =',1)
s=s.replace('AgendaScreen(viewModel,','AgendaScreen(hiltViewModel(),')
s=s.replace('                            actionViewModel = viewModel,','                            organizeViewModel = hiltViewModel(),\n                            notesViewModel = hiltViewModel(),')
s=s.replace('SavedScreen(viewModel,','SavedScreen(hiltViewModel(),').replace('                            viewModel = viewModel,','                            viewModel = hiltViewModel(),')
s=s.replace('androidx.lifecycle.viewmodel.compose.viewModel<','hiltViewModel<')
start=s.index('                composable("action/{id}")');end=s.index('\n            }\n        }\n    }\n}',start)
routes='''                composable("history") {
                    com.luminor.actionbox.ui.history.HistoryScreen(hiltViewModel(), onBack = { navController.popBackStack() })
                }
'''
for route,cls,screen,call in [
('action','ui.actions.ActionEditorViewModel','ActionEditorScreen','detailViewModel, item, onBack = { navController.popBackStack() }, onNoteOpen = { navController.navigate("note/$it") }'),
('note','ui.organize.notes.NoteDetailViewModel','NoteDetailScreen','detailViewModel, item, onBack = { navController.popBackStack() }'),
('project','ui.organize.ProjectViewModel','ProjectDetailScreen','detailViewModel, item.id, onBack = { navController.popBackStack() }, onNoteOpen = { navController.navigate("note/$it") }'),
('list','ui.organize.lists.ListViewModel','ListDetailScreen','detailViewModel, item.id, onBack = { navController.popBackStack() }'),
('routine','ui.organize.routines.RoutineViewModel','RoutineDetailScreen','detailViewModel, item.id, onBack = { navController.popBackStack() }'),
('saved','ui.saved.SavedDetailViewModel','SavedDetailScreen','detailViewModel, item.id, onBack = { navController.popBackStack() }')]:
    routes+=f'''                composable("{route}/{{id}}") {{
                    SharedDestination(sharedScope, this) {{
                        val detailViewModel = hiltViewModel<com.luminor.actionbox.{cls}>()
                        val detail by detailViewModel.detail.collectAsStateWithLifecycle()
                        DetailContent(detail, onBack = {{ navController.popBackStack() }}, onRetry = {{ detailViewModel.retry() }}) {{ item ->
                            {screen}({call})
                        }}
                    }}
                }}
'''
s=s[:start]+routes+s[end:];write(p,s)
edit(root/'MainActivity.kt',lambda s:s.replace('private val viewModel: ActionViewModel','private val viewModel: com.luminor.actionbox.ui.RootViewModel'))
edit(root/'ui/home/HomeScreen.kt',lambda s:s.replace('    onSettings: () -> Unit,','    onHistory: () -> Unit,\n    onSettings: () -> Unit,').replace('    val today = LocalDate.now()', '''    val completions by viewModel.completions.collectAsStateWithLifecycle()
    val rules by viewModel.routineRules.collectAsStateWithLifecycle()
    val today = LocalDate.now()''').replace('RecurrenceCalculator.occursOn(it, today)','com.luminor.actionbox.domain.routine.RoutineEvaluation.routineOccursOn(it, today, rules)').replace('viewModel.isCompletedOn(it, today)','com.luminor.actionbox.domain.routine.RoutineEvaluation.isCompletedOn(it, today, completions)').replace('viewModel.isCompletedOn(action, today)','com.luminor.actionbox.domain.routine.RoutineEvaluation.isCompletedOn(action, today, completions)').replace('                    IconButton(onClick = onSearch)', '                    androidx.compose.material3.TextButton(onClick = onHistory) { Text("Histórico") }\n                    IconButton(onClick = onSearch)'))
edit(root/'ui/agenda/AgendaScreen.kt',lambda s:s.replace('    val completions by viewModel.completions.collectAsStateWithLifecycle()', '    val completions by viewModel.completions.collectAsStateWithLifecycle()\n    val rules by viewModel.routineRules.collectAsStateWithLifecycle()').replace('    var dragTotal by', '''    androidx.compose.runtime.LaunchedEffect(month, selectedDate, mode) {
        // Include spillover weeks in the month grid and the upcoming list window.
        val start = minOf(month.atDay(1).minusDays(7), selectedDate.minusDays(7))
        val end = maxOf(month.atEndOfMonth().plusDays(7), selectedDate.plusDays(60))
        viewModel.setPeriod(start, end)
    }
    var dragTotal by''').replace('viewModel.routineOccursOn(action, day)','com.luminor.actionbox.domain.routine.RoutineEvaluation.routineOccursOn(action, day, rules)'))

write(root/'ui/history/HistoryScreen.kt','''package com.luminor.actionbox.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey

@Composable
fun HistoryScreen(viewModel: HistoryViewModel, onBack: () -> Unit) {
    val items = viewModel.history.collectAsLazyPagingItems()
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Histórico", style = MaterialTheme.typography.headlineLarge)
            TextButton(onClick = onBack) { Text("Voltar") }
        }
        when {
            items.loadState.refresh is LoadState.Loading && items.itemCount == 0 -> CircularProgressIndicator()
            items.loadState.refresh is LoadState.Error -> RetryHistory { items.retry() }
            items.loadState.refresh is LoadState.NotLoading && items.itemCount == 0 -> Text("Histórico vazio")
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(count = items.itemCount, key = items.itemKey { it.id }) { index ->
                items[index]?.let { action ->
                    Card {
                        Row(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(action.title, modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.delete(action.id) }) { Text("Excluir") }
                        }
                    }
                }
            }
            if (items.loadState.append is LoadState.Loading) item { CircularProgressIndicator() }
            if (items.loadState.append is LoadState.Error) item { RetryHistory { items.retry() } }
        }
    }
}

@Composable
private fun RetryHistory(onRetry: () -> Unit) {
    Column {
        Text("Não foi possível carregar o histórico.")
        TextButton(onClick = onRetry) { Text("Tentar novamente") }
    }
}
''')
edit(root/'ui/history/HistoryViewModel.kt',lambda s:s.replace('import androidx.lifecycle.SavedStateHandle','import androidx.paging.cachedIn\nimport androidx.lifecycle.SavedStateHandle').replace('.flow.let { androidx.paging.cachedIn(it, viewModelScope) }','.flow.cachedIn(viewModelScope)'))
(root/'ActionViewModel.kt').unlink()
