from pathlib import Path
import re,json,html
exec(Path('refactor_strings.py').read_text(encoding='utf-8').split('# Resolve presentation-only')[0])
resources=json.loads(Path('refactor_resource_map.json').read_text(encoding='utf-8'))
def edit(p,f):
    p=Path(p);write(p,f(p.read_text(encoding='utf-8')))

# Commands must survive removal of their originating navigation destination.
write(root/'ui/events/CommandRunner.kt','''package com.luminor.actionbox.ui.events

import com.luminor.actionbox.R
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommandRunner @Inject constructor(private val events: AppUiEventBus) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    fun execute(block: suspend () -> Unit) = scope.launch {
        try { block() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            android.util.Log.e("ActionBox", "Command failed", error)
            events.message(R.string.text_nao_foi_possivel_concluir_tente_novamente)
        }
    }
}
''')
write(root/'ui/events/EventViewModel.kt','''package com.luminor.actionbox.ui.events

import androidx.lifecycle.ViewModel

abstract class EventViewModel(protected val uiEventBus: AppUiEventBus, private val commandRunner: CommandRunner) : ViewModel() {
    protected fun execute(block: suspend () -> Unit) = commandRunner.execute(block)
    fun showMessage(@androidx.annotation.StringRes id: Int, vararg args: Any) = uiEventBus.message(id, *args)
    fun showMessage(value: String) = uiEventBus.message(value)
}
''')
for p in (root/'ui').rglob('*ViewModel.kt'):
    s=p.read_text(encoding='utf-8')
    if ': EventViewModel(uiEventBus)' in s or ': com.luminor.actionbox.ui.events.EventViewModel(uiEventBus)' in s:
        s=s.replace('constructor(', 'constructor(\n    commandRunner: com.luminor.actionbox.ui.events.CommandRunner,',1).replace('EventViewModel(uiEventBus)', 'EventViewModel(uiEventBus, commandRunner)')
        write(p,s)

edit(root/'ui/search/GlobalSearchScreen.kt',lambda s:s.replace('kind.label.uppercase()', 'textResources.getString(kind.label).uppercase()').replace('textResources.getString(R.string.text_header , kind.name)', '"header-${kind.name}"'))

# Remove Android Context from business command signatures and call sites.
for p in list((root/'domain/commands').glob('*.kt'))+list((root/'ui').rglob('*.kt')):
    s=p.read_text(encoding='utf-8')
    for name in ['saveRoutineEdits','setRoutinePaused','updateAction','duplicateAction']:
        s=re.sub(r'(fun '+name+r'\(\s*)context: (?:android.content.)?Context,\s*',r'\1',s)
        s=s.replace(name+'(context, ',name+'(')
    if p.name in ['RoutineCommands.kt','RoutineViewModel.kt','RoutineDetailScreen.kt']:
        s=s.replace('            context = context,\n','').replace('                                    context = context,\n','')
    if p.name=='NoteDetailScreen.kt':s=s.replace('            context,\n','')
    # Retain Context only on the NotesBoard callback boundary until all call sites are updated.
    if p.name=='NotesBoard.kt':
        s=s.replace('onUpdate: (android.content.Context, ActionEntity, ActionEntity)', 'onUpdate: (ActionEntity, ActionEntity)').replace('onUpdate(context, ', 'onUpdate(')
    if p.name=='OrganizeScreen.kt':s=s.replace('onUpdate = { context, original, updated ->', 'onUpdate = { original, updated ->')
    if p.name=='OrganizeViewModel.kt':s=s.replace('fun updateAction(context: android.content.Context, ', 'fun updateAction(')
    write(p,s)

# Do not open Room transactions around DataStore writes.
edit(root/'domain/commands/SettingsCommands.kt',lambda s:s.replace('= repository.transaction {\n        settingsRepository.', '{\n        settingsRepository.'))

# Restore notification scheduling along with persisted data, including Undo.
p=root/'domain/commands/TrashCommands.kt';s=p.read_text(encoding='utf-8')
s=s.replace('constructor(\n','constructor(\n    private val scheduleReminder: ScheduleReminderUseCase,\n',1)
s=s.replace('UndoKind.ACTION -> repository.restoreAction(event.id)','UndoKind.ACTION -> restoreAction(event.id)').replace('UndoKind.PROJECT -> repository.restoreProjectCascade(event.id)','UndoKind.PROJECT -> restoreProject(event.id)').replace('UndoKind.LIST -> repository.restoreListCascade(event.id)','UndoKind.LIST -> restoreList(event.id)')
s=s.replace('        repository.restoreAction(id)','        repository.restoreAction(id)\n        repository.getById(id)?.let(scheduleReminder::scheduleFuture)').replace('        repository.restoreProjectCascade(id)','        repository.restoreProjectCascade(id)\n        repository.projectActions(id).forEach(scheduleReminder::scheduleFuture)').replace('        repository.restoreListCascade(id)','        repository.restoreListCascade(id)\n        repository.listAgendaActions(id).forEach(scheduleReminder::scheduleFuture)')
write(p,s)
p=root/'ui/trash/TrashViewModel.kt';s=p.read_text(encoding='utf-8').replace('    private val repository:', '    private val commands: com.luminor.actionbox.domain.commands.TrashCommands,\n    private val repository:')
for a,b in [('restoreAction','restoreAction'),('restoreProjectCascade','restoreProject'),('restoreListCascade','restoreList'),('permanentlyDeleteAction','permanentlyDeleteAction'),('permanentlyDeleteProject','permanentlyDeleteProject'),('permanentlyDeleteList','permanentlyDeleteList')]:s=s.replace('repository.'+a+'(id)','commands.'+b+'(id)')
write(p,s)

# Observe completion/rule state explicitly: delegated state never read by Compose
# would otherwise leave completion indicators stale.
for rel in ['ui/agenda/AgendaScreen.kt','ui/actions/ActionEditorScreen.kt','ui/actions/ActionDetailScreen.kt','ui/organize/routines/RoutineDetailScreen.kt']:
    p=root/rel;s=p.read_text(encoding='utf-8')
    if 'val completions by viewModel.completions' in s:s=s.replace('val completions by viewModel.completions.collectAsStateWithLifecycle()', 'val completions = viewModel.completions.collectAsStateWithLifecycle().value')
    else:s=s.replace('    val settings by', '    val completions = viewModel.completions.collectAsStateWithLifecycle().value\n    val settings by',1)
    if rel.startswith('ui/actions/'):
        s=s.replace('viewModel.isCompletedOn(action, LocalDate.now())','com.luminor.actionbox.domain.routine.RoutineEvaluation.isCompletedOn(action, LocalDate.now(), completions)')
    if rel.endswith('RoutineDetailScreen.kt'):
        s=s.replace('viewModel.routineOccursOn(action, it)','com.luminor.actionbox.domain.routine.RoutineEvaluation.routineOccursOn(action, it, rules)').replace('viewModel.isCompletedOn(action, it)','com.luminor.actionbox.domain.routine.RoutineEvaluation.isCompletedOn(action, it, completions)')
    write(p,s)

# Resource IDs for ActionType labels; stored enum names remain unchanged.
p=root/'domain/Models.kt';s=p.read_text(encoding='utf-8').replace('val label: String, val emoji:', 'val label: Int, val emoji:').replace('val confidenceLabel: String','val confidenceLabel: Int')
s=re.sub(r'\b(TASK|REMINDER|EVENT|NOTE|LIST|PROJECT|READ_LATER|ADDRESS|CONTACT|REPLY)\("([^"]+)"',lambda m:m[1]+'('+resource(m[2]),s)
for v in ['Alta','Média','Baixa']:s=s.replace('-> "'+v+'"','-> '+resource(v))
s=s.replace('\n\n','\n\nimport com.luminor.actionbox.R\n',1);write(p,s)
for p in (root/'ui').rglob('*.kt'):
    s=p.read_text(encoding='utf-8')
    if p.name in ['Common.kt','ActionTypeSheet.kt','CaptureEditor.kt','CaptureResult.kt','EditorTypeChip.kt']:
        s=re.sub(r'(?<!\w)(action\.type\.label|type\.label|current\.label|action\.confidenceLabel)(?!\w)',r'androidx.compose.ui.res.stringResource(\1)',s)
    write(p,s)

# Remaining presentation strings skipped by the conservative first pass.
for rel in ['ui/DetailState.kt','ui/home/HomeScreen.kt','ui/trash/TrashScreen.kt','ui/organize/notes/NoteDetailScreen.kt','ui/organize/routines/RoutineDetailScreen.kt','ui/capture/CaptureEditor.kt','ui/tags/TagPickerBottomSheet.kt']:
    p=root/rel;s=p.read_text(encoding='utf-8');changes=[]
    for a,b,v in strings(s):
        if v in ['Voltar','Este item não está mais disponível.','Não foi possível carregar este item.','Tentar novamente','Informe um horário válido no formato HH:mm','Todas'] or v.startswith(('Hoje · ${','Excluído em ${','Criada ${',' · Editada ${')):
            resolver='textResources.getString' if 'val textResources =' in s else 'androidx.compose.ui.res.stringResource'
            changes.append((a,b,call(v,resolver)))
    for a,b,v in reversed(changes):s=s[:a]+v+s[b:]
    if changes and 'import com.luminor.actionbox.R' not in s:s=s.replace('\n\n','\n\nimport com.luminor.actionbox.R\n',1)
    write(p,s)

# Display a localized label for persisted note categories without rewriting keys.
mapping={v:resource(v) for v in ['Todas','Fixadas','Trabalho','Pessoal','Ideias','Compras','Viagem']}
write(root/'ui/organize/notes/NoteCategoryLabels.kt','''package com.luminor.actionbox.ui.organize.notes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.luminor.actionbox.R

@Composable
fun noteCategoryLabel(key: String): String = when (key) {
'''+''.join(f'    "{k}" -> stringResource({v})\n' for k,v in mapping.items())+'    else -> key\n}\n')
edit(root/'ui/organize/notes/NoteCategoryFilter.kt',lambda s:s.replace('Text(category)', 'Text(noteCategoryLabel(category))'))

# Use plurals for counts rather than constructing suffixes in Kotlin.
plurals={
'completed_actions':('concluída','concluídas'),
'created_list_items':('Lista criada com %1$d item','Lista criada com %1$d itens'),
'created_project_tasks':('Projeto criado com %1$d tarefa','Projeto criado com %1$d tarefas'),
'routine_days':('%1$d dia este mês','%1$d dias este mês')}
edit(root/'ui/home/HomeScreen.kt',lambda s:s.replace('textResources.getString(R.string.text_pendentes_concluida_sem_data , pending, completed, if (completed == 1) "" else "s", undated)', 'textResources.getString('+resource('%1$s pendentes · %2$s %3$s · %4$s sem data')+', pending, completed, textResources.getQuantityString(R.plurals.completed_actions, completed), undated)'))
edit(root/'ui/organize/HabitCard.kt',lambda s:s.replace('textResources.getString(R.string.text_dias_este_mes , completed)', 'textResources.getQuantityString(R.plurals.routine_days, completed, completed)'))

# Unify resource messages emitted from UI callbacks, avoiding early localization.
for p in (root/'ui').rglob('*.kt'):
    s=p.read_text(encoding='utf-8')
    s=re.sub(r'viewModel.showMessage\(textResources.getString\((R.string.\w+)\)\)',r'viewModel.showMessage(\1)',s)
    write(p,s)

# Remove unused imports left by extraction, without changing executable code.
for p in list((root/'domain/commands').glob('*.kt'))+list((root/'ui').rglob('*ViewModel.kt')):
    s=p.read_text(encoding='utf-8'); body=re.sub(r'^import .*\n','',s,flags=re.M)
    lines=[]
    for line in s.splitlines():
        if line.startswith('import ') and not line.endswith('.*'):
            name=line.split('.')[-1]
            if not re.search(r'\b'+re.escape(name)+r'\b',body):continue
        lines.append(line)
    write(p,'\n'.join(lines)+'\n')

entries=[]
for text,name in resources.items():
    escaped=html.escape(text,quote=False).replace("'","\\'").replace('"','\\"')
    entries.append(f'    <string name="{name}">{escaped}</string>')
for name,(one,other) in plurals.items():entries.append(f'    <plurals name="{name}">\n        <item quantity="one">{one}</item>\n        <item quantity="other">{other}</item>\n    </plurals>')
write('app/src/main/res/values/strings.xml','<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'+'\n'.join(entries)+'\n</resources>\n')
write('refactor_resource_map.json',json.dumps(resources,ensure_ascii=False,indent=2))
