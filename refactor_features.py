exec(Path('refactor_setup.py').read_text(encoding='utf-8').split('# Preserve')[0]) if False else None
from pathlib import Path
import re
root=Path('app/src/main/java/com/luminor/actionbox')
def write(p,s):
    p=Path(p); p.parent.mkdir(parents=True,exist_ok=True); p.write_text(s,encoding='utf-8')
def edit(p,f):
    p=Path(p); write(p,f(p.read_text(encoding='utf-8')))
source=(root/'ActionViewModel.kt').read_text(encoding='utf-8')
imports=source[source.index('import '):source.index('class ActionViewModel')]
imports += '''import javax.inject.Inject
import com.luminor.actionbox.data.repository.ActionRepository
import com.luminor.actionbox.data.preferences.SettingsRepository
import com.luminor.actionbox.ui.events.AppUiEventBus
import kotlinx.coroutines.flow.first
'''

dao='''
    @Query("SELECT * FROM actions WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    suspend fun getActiveActions(): List<ActionEntity>
    @Query("SELECT * FROM actions WHERE deletedAt IS NULL AND projectId = :id ORDER BY sortOrder, createdAt, id")
    suspend fun getProjectActions(id: Long): List<ActionEntity>
    @Query("SELECT * FROM actions WHERE deletedAt IS NULL AND type = 'LIST' AND metadata = :id")
    suspend fun getListAgendaActions(id: String): List<ActionEntity>
    @Query("SELECT EXISTS(SELECT 1 FROM action_completions WHERE actionId = :id AND occurrenceDate = :date)")
    suspend fun isCompletedOn(id: Long, date: String): Boolean
    @Query("SELECT * FROM actions WHERE id = :id AND deletedAt IS NULL")
    fun observeAction(id: Long): Flow<ActionEntity?>
    @Query("SELECT * FROM projects WHERE id = :id AND deletedAt IS NULL")
    fun observeProject(id: Long): Flow<ProjectEntity?>
    @Query("SELECT * FROM action_lists WHERE id = :id AND deletedAt IS NULL")
    fun observeList(id: Long): Flow<ActionListEntity?>
    @Query("SELECT * FROM actions WHERE deletedAt IS NULL AND projectId = :id ORDER BY sortOrder, createdAt, id")
    fun observeProjectActions(id: Long): Flow<List<ActionEntity>>
    @Query("SELECT * FROM actions WHERE deletedAt IS NULL AND projectId IS NOT NULL")
    fun observeAllProjectActions(): Flow<List<ActionEntity>>
    @Query("SELECT * FROM list_items WHERE listId = :id ORDER BY position, id")
    fun observeItems(id: Long): Flow<List<ListItemEntity>>
    @Query("SELECT * FROM tag_refs WHERE ownerType = :type AND ownerId = :id")
    fun observeOwnerTagRefs(type: String, id: Long): Flow<List<TagRefEntity>>
    @Query("SELECT * FROM content_links WHERE (sourceType = :type AND sourceId = :id) OR (targetType = :type AND targetId = :id) ORDER BY createdAt DESC")
    fun observeOwnerLinks(type: String, id: Long): Flow<List<ContentLinkEntity>>
    @Query("SELECT * FROM action_completions WHERE actionId = :id ORDER BY completedAt DESC")
    fun observeActionCompletions(id: Long): Flow<List<ActionCompletionEntity>>
    @Query("SELECT * FROM action_completions WHERE occurrenceDate BETWEEN :start AND :end ORDER BY completedAt DESC")
    fun observePeriodCompletions(start: String, end: String): Flow<List<ActionCompletionEntity>>
    @Query("SELECT * FROM routine_rules WHERE actionId = :id ORDER BY effectiveFrom DESC")
    fun observeActionRules(id: Long): Flow<List<RoutineRuleEntity>>
    @Query("SELECT * FROM actions WHERE deletedAt IS NULL AND recurrenceType IS NOT NULL AND recurrenceType != 'NONE' AND status != 'ARCHIVED'")
    fun observeRoutines(): Flow<List<ActionEntity>>
    @Query("SELECT * FROM actions WHERE deletedAt IS NULL AND type IN ('TASK','REMINDER','EVENT','LIST') AND ((scheduledAt >= :start AND scheduledAt < :end) OR (recurrenceType IS NOT NULL AND recurrenceType != 'NONE') OR (scheduledAt IS NULL AND status = 'PENDING')) ORDER BY scheduledAt, createdAt DESC")
    fun observeAgenda(start: Long, end: Long): Flow<List<ActionEntity>>
    @Query("SELECT * FROM actions WHERE deletedAt IS NULL AND status = 'COMPLETED' ORDER BY COALESCE(completedAt, createdAt) DESC, id DESC")
    fun historyPagingSource(): androidx.paging.PagingSource<Int, ActionEntity>
'''
edit(root/'data/local/ActionDao.kt',lambda s:s.replace('interface ActionDao {','interface ActionDao {\n'+dao))
repo='''
    suspend fun <T> transaction(block: suspend () -> T): T = database.withTransaction { block() }
    suspend fun activeActions() = dao.getActiveActions()
    suspend fun projectActions(id: Long) = dao.getProjectActions(id)
    suspend fun listAgendaActions(id: Long) = dao.getListAgendaActions(id.toString())
    suspend fun isCompletedOn(id: Long, date: String) = dao.isCompletedOn(id, date)
    fun observeAction(id: Long) = dao.observeAction(id)
    fun observeProject(id: Long) = dao.observeProject(id)
    fun observeList(id: Long) = dao.observeList(id)
    fun observeProjectActions(id: Long) = dao.observeProjectActions(id)
    fun observeAllProjectActions() = dao.observeAllProjectActions()
    fun observeItems(id: Long) = dao.observeItems(id)
    fun observeOwnerTagRefs(type: String, id: Long) = dao.observeOwnerTagRefs(type, id)
    fun observeOwnerLinks(type: String, id: Long) = dao.observeOwnerLinks(type, id)
    fun observeActionCompletions(id: Long) = dao.observeActionCompletions(id)
    fun observePeriodCompletions(start: String, end: String) = dao.observePeriodCompletions(start, end)
    fun observeActionRules(id: Long) = dao.observeActionRules(id)
    fun observeRoutines() = dao.observeRoutines()
    fun observeAgenda(start: Long, end: Long) = dao.observeAgenda(start, end)
    fun historyPagingSource() = dao.historyPagingSource()
'''
edit(root/'data/repository/ActionRepository.kt',lambda s:s.replace('    private val dao',repo+'\n    private val dao'))

# Split command implementations by domain. No command reads a ViewModel's cached state.
parts=re.split(r'(?=^    (?:private )?(?:suspend )?fun )',source,flags=re.M)[1:]
funcs={re.search(r'fun (\w+)',p)[1]:p.rstrip() for p in parts}
funcs.pop('Long',None)
funcs['routineConfigurationChanged']=funcs['routineConfigurationChanged'].rstrip()
groups={
'ActionCommands':['complete','toggleOccurrence','markOccurrence','updateAction','duplicateAction','archive','delete'],
'ProjectCommands':['finishProject','reopenProject','saveProjectEdits','reorderProjectTasks','deleteProject'],
'ListCommands':['toggleListItem','finishList','reopenList','saveListEdits','setListAgendaCompleted','deleteList'],
'RoutineCommands':['saveRoutineEdits','setRoutinePaused'],
'OrganizationCommands':['setTagsForOwner','createAndAttachTag','removeTag','linkNote','unlinkContentLink'],
'TrashCommands':['undo','restoreAction','restoreProject','restoreList','permanentlyDeleteAction','permanentlyDeleteProject','permanentlyDeleteList'],
'SettingsCommands':['setTheme','setReplyTone','setHaptics','clearAllData']}
method_group={n:g for g,names in groups.items() for n in names}
shared=funcs['routineConfigurationChanged']+'\n\n    private fun Long.toLocalDate(): LocalDate =\n        Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()\n'
for group,names in groups.items():
    methods=[]
    for name in names:
        t=funcs[name]
        if 'viewModelScope.launch {' in t:
            if '= viewModelScope.launch {' in t:
                t=t.replace('= viewModelScope.launch {','= repository.transaction {')
            else:
                t=t.replace('        viewModelScope.launch {\n','',1)
                i=t.rfind('    }'); t=t[:i]+t[i+5:]
        t=t.replace('return@launch','return').replace('fun '+name+'(', 'suspend fun '+name+'(').replace('suspend suspend','suspend')
        t=t.replace('app.uiEventBus','uiEventBus').replace('_message.emit','uiEventBus.message')
        t=t.replace('all.value.filter { it.projectId == project.id }','repository.projectActions(project.id)')
        t=t.replace('all.value.filter { it.projectId == id }','repository.projectActions(id)')
        t=t.replace('all.value\n            .filter { it.type == ActionType.LIST.name && it.metadata == listId.toString() }','repository.listAgendaActions(listId)')
        t=t.replace('all.value\n                .filter { it.type == ActionType.LIST.name && it.metadata == id.toString() }','repository.listAgendaActions(id)')
        t=t.replace('all.value.forEach','repository.activeActions().forEach')
        t=t.replace('!isCompletedOn(action, date)','!repository.isCompletedOn(action.id, date.toString())')
        # Updates are based on persisted state, not a potentially stale screen entity.
        if name=='toggleOccurrence':
            t=t.replace('if (RecurrenceCalculator', 'val current = repository.getById(action.id) ?: return\n            if (RecurrenceCalculator',1).replace('recurrenceType(action)', 'recurrenceType(current)').replace('if (action.status', 'if (current.status')
        # Keep DB changes atomic. Reminder APIs are local, non-suspending side effects.
        if name in ['saveProjectEdits','saveListEdits','toggleListItem','finishList','reopenList','toggleOccurrence','saveRoutineEdits','updateAction']:
            brace=t.index('{'); t=t[:brace]+'= repository.transaction {'+t[brace+1:]
            t=t.replace('?: return','?: return@transaction').replace('                return\n','                return@transaction\n')
        methods.append(t)
    body='\n\n'.join(methods)
    extra=shared if group in ['ActionCommands','RoutineCommands'] else ''
    deps={'repository':'ActionRepository','scheduleReminder':'ScheduleReminderUseCase','routineRuleFactory':'RoutineRuleFactory','settingsRepository':'SettingsRepository','uiEventBus':'AppUiEventBus'}
    params=',\n'.join('    private val '+k+': '+v for k,v in deps.items() if k in body+extra)
    write(root/f'domain/commands/{group}.kt','package com.luminor.actionbox.domain.commands\n\n'+imports+f'\nclass {group} @Inject constructor(\n'+params+'\n) {\n'+body+'\n'+extra+'}\n')

write(root/'domain/routine/RoutineEvaluation.kt','''package com.luminor.actionbox.domain.routine
import com.luminor.actionbox.data.local.*
import com.luminor.actionbox.domain.*
import java.time.*

object RoutineEvaluation {
'''+funcs['isCompletedOn'].replace('fun isCompletedOn(action: ActionEntity, date: LocalDate)', 'fun isCompletedOn(action: ActionEntity, date: LocalDate, completions: List<ActionCompletionEntity>)').replace('completions.value','completions')+'\n'+funcs['routineOccursOn'].replace('fun routineOccursOn(action: ActionEntity, date: LocalDate)', 'fun routineOccursOn(action: ActionEntity, date: LocalDate, routineRules: List<RoutineRuleEntity>)').replace('routineRules.value','routineRules').replace('routineRuleFactory.startOfDayMillis(date)','RoutineRuleFactory().startOfDayMillis(date)')+'\n    private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()\n}\n')

write(root/'ui/events/EventViewModel.kt','''package com.luminor.actionbox.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

abstract class EventViewModel(protected val uiEventBus: AppUiEventBus) : ViewModel() {
    protected fun execute(block: suspend () -> Unit) = viewModelScope.launch {
        try { block() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            android.util.Log.e("ActionBox", "Command failed", error)
            uiEventBus.message("Não foi possível concluir. Tente novamente.")
        }
    }
    fun showMessage(value: String) = uiEventBus.message(value)
}
''')
# Capture signatures for thin, feature-specific command entry points.
def signature(n):
    t=funcs[n]; a=t.index('('); depth=1; i=a+1
    while depth:
        if t[i]=='(':depth+=1
        if t[i]==')':depth-=1
        i+=1
    return t[a+1:i-1]
def wrapper(n):
    params=signature(n)
    args=re.findall(r'(\w+)\s*:\s*',params)
    return f'    fun {n}({params}) = execute {{ {method_group[n][0].lower()+method_group[n][1:]}.{n}('+', '.join(args)+') }\n'

vm_imports=imports+'''import androidx.lifecycle.SavedStateHandle
import com.luminor.actionbox.domain.commands.*
import com.luminor.actionbox.ui.events.EventViewModel
import com.luminor.actionbox.domain.routine.RoutineEvaluation
import kotlinx.coroutines.flow.*
import dagger.hilt.android.lifecycle.HiltViewModel
'''
def vm(name,package,flows,commands,detail=None,extra='',evaluate=False):
    deps=['    private val repository: ActionRepository','    settingsRepository: SettingsRepository','    uiEventBus: AppUiEventBus']
    deps += ['    private val '+g[0].lower()+g[1:]+': '+g for g in sorted({method_group[n] for n in commands})]
    if detail:deps+=['    savedStateHandle: SavedStateHandle']
    body=''
    if detail:body+='    private val id = savedStateHandle.get<String>("id")?.toLongOrNull() ?: -1L\n'
    body+='    val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiSettings())\n'
    for k,expr in flows.items():body+=f'    val {k} = {expr}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())\n'
    if evaluate:
        body+='''    fun isCompletedOn(action: ActionEntity, date: LocalDate) = RoutineEvaluation.isCompletedOn(action, date, completions.value)
    fun routineOccursOn(action: ActionEntity, date: LocalDate) = RoutineEvaluation.routineOccursOn(action, date, routineRules.value)
'''
    body+=''.join(wrapper(n) for n in commands)
    write(root/(package.replace('.','/')+'/'+name+'.kt'),'package com.luminor.actionbox.'+package+'\n\n'+vm_imports+f'\n@HiltViewModel\nclass {name} @Inject constructor(\n'+',\n'.join(deps)+'\n) : EventViewModel(uiEventBus) {\n'+extra+body+'}\n')
tags=lambda kind:{'tags':'repository.tags','tagRefs':f'repository.observeOwnerTagRefs("{kind}", id)'}
evalflows={'completions':'repository.observeActionCompletions(id)','routineRules':'repository.observeActionRules(id)'}
org=['setTagsForOwner','createAndAttachTag','removeTag','linkNote','unlinkContentLink']
action=['complete','toggleOccurrence','updateAction','duplicateAction','archive','delete']
external='\n'.join(funcs[n] for n in ['addToSystemCalendar','openSaved','insertContact'])

vm('ActionEditorViewModel','ui.actions',{'all':'repository.observeAction(id).map { listOfNotNull(it) }','notes':'repository.notes','contentLinks':'repository.observeOwnerLinks("ACTION", id)',**tags('ACTION'),**evalflows},action+org,True,external,True)
vm('ProjectViewModel','ui.organize',{'projects':'repository.observeProject(id).map { listOfNotNull(it) }','all':'repository.observeProjectActions(id)','notes':'repository.notes','contentLinks':'repository.observeOwnerLinks("PROJECT", id)',**tags('PROJECT'),'completions':'repository.completions','routineRules':'repository.routineRules'},groups['ProjectCommands']+['toggleOccurrence']+org,True,evaluate=True)
vm('ListViewModel','ui.organize.lists',{'lists':'repository.observeList(id).map { listOfNotNull(it) }','listItems':'repository.observeItems(id)',**tags('LIST')},[n for n in groups['ListCommands'] if n!='setListAgendaCompleted']+org,True)
vm('RoutineViewModel','ui.organize.routines',{'all':'repository.observeAction(id).map { listOfNotNull(it) }',**evalflows,**tags('ACTION')},groups['RoutineCommands']+['toggleOccurrence','delete']+org,True,evaluate=True)
vm('SavedViewModel','ui.saved',{'saved':'repository.saved'},['archive','delete'],extra=external)
vm('SavedDetailViewModel','ui.saved',{'saved':'repository.observeAction(id).map { listOfNotNull(it) }'},['archive','delete'],True,external)
vm('NoteDetailViewModel','ui.organize.notes',{'notes':'repository.observeAction(id).map { listOfNotNull(it) }','projects':'repository.projects','all':'repository.all','contentLinks':'repository.observeOwnerLinks("NOTE", id)',**tags('ACTION')},['updateAction','archive','delete']+org,True)
vm('SettingsViewModel','ui.settings',{},groups['SettingsCommands'])
vm('TaskListViewModel','ui.actions',{'pending':'repository.pending'},['complete','delete'])
vm('RootViewModel','ui',{},['undo'],extra='    val uiEvents = uiEventBus.events\n')
period='''    private val period = MutableStateFlow(LocalDate.now() to LocalDate.now())
    fun setPeriod(start: LocalDate, end: LocalDate) { period.value = start to end }
    private val actionsFlow = period.flatMapLatest { (start, end) -> repository.observeAgenda(start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(), end.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()) }
    private val completionFlow = period.flatMapLatest { (start, end) -> repository.observePeriodCompletions(start.toString(), end.toString()) }
'''
for name,pkg in [('HomeViewModel','ui.home'),('AgendaViewModel','ui.agenda')]:
    vm(name,pkg,{'all':'actionsFlow','completions':'completionFlow','routineRules':'repository.routineRules'},['toggleOccurrence'],extra=period,evaluate=True)
vm('HistoryViewModel','ui.history',{},['delete'],extra='''    val history = androidx.paging.Pager(androidx.paging.PagingConfig(pageSize = 30, initialLoadSize = 60, prefetchDistance = 10, maxSize = 150, enablePlaceholders = false), pagingSourceFactory = repository::historyPagingSource).flow.let { androidx.paging.cachedIn(it, viewModelScope) }
''')

# Convert screen-level types first; reusable cards will receive callbacks next.
mapping={'ui/home/HomeScreen.kt':'ui.home.HomeViewModel','ui/agenda/AgendaScreen.kt':'ui.agenda.AgendaViewModel','ui/actions/ActionEditorScreen.kt':'ui.actions.ActionEditorViewModel','ui/actions/ActionDetailScreen.kt':'ui.actions.ActionEditorViewModel','ui/actions/ActionsScreen.kt':'ui.actions.TaskListViewModel','ui/organize/ProjectDetailScreen.kt':'ui.organize.ProjectViewModel','ui/organize/lists/ListDetailScreen.kt':'ui.organize.lists.ListViewModel','ui/organize/routines/RoutineDetailScreen.kt':'ui.organize.routines.RoutineViewModel','ui/saved/SavedScreen.kt':'ui.saved.SavedViewModel','ui/saved/SavedDetailScreen.kt':'ui.saved.SavedDetailViewModel','ui/settings/SettingsScreen.kt':'ui.settings.SettingsViewModel','ui/organize/notes/NoteDetailScreen.kt':'ui.organize.notes.NoteDetailViewModel'}
for rel,full in mapping.items():
    edit(root/rel,lambda s:s.replace('com.luminor.actionbox.ActionViewModel','com.luminor.actionbox.'+full).replace('ActionViewModel',full.split('.')[-1]))
for rel in ['ui/actions/ActionEditorFeedback.kt','ui/organize/routines/RoutineFeedback.kt']:
    (root/rel).unlink()

# Existing task list was unreachable; retain pending functionality and move history to its dedicated route.
edit(root/'ui/actions/ActionsScreen.kt',lambda s:s.replace('    val notes by viewModel.notes.collectAsStateWithLifecycle()\n','').replace('    val history by viewModel.history.collectAsStateWithLifecycle()\n','').replace('listOf("Pendentes", "Notas", "Histórico")','listOf("Pendentes")').replace('            1 -> ActionList(notes, "📝", "Nenhuma nota", "Salve uma informação como nota pela tela inicial.", viewModel)\n','').replace('            else -> ActionList(history, "✨", "Histórico vazio", "As ações concluídas aparecerão aqui.", viewModel)\n',''))
