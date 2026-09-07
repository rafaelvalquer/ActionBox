from pathlib import Path
import re, unicodedata, html, json
root=Path('app/src/main/java/com/luminor/actionbox')
def write(p,s):
    p=Path(p);p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf-8')
resources={'ActionBox':'app_name'}
def resource(value):
    if value not in resources:
        slug=unicodedata.normalize('NFKD',re.sub(r'%\d+\$s','',value)).encode('ascii','ignore').decode().lower()
        slug=re.sub('[^a-z0-9]+','_',slug).strip('_')[:65] or 'text'
        name='text_'+slug
        if name in resources.values(): name+='_'+str(len(resources))
        resources[value]=name
    return 'R.string.'+resources[value]

# Kotlin string scanner: preserve nested expressions and quotes in string templates.
def string_end(s,i):
    j=i+1
    while j<len(s):
        if s[j]=='\\':j+=2;continue
        if s[j]=='"':return j+1
        if s[j:j+2]=='${':
            j=expr_end(s,j+2);continue
        j+=1
    return len(s)
def expr_end(s,j):
    depth=1
    while j<len(s):
        if s[j]=='"':j=string_end(s,j);continue
        if s[j]=='{':depth+=1
        if s[j]=='}':
            depth-=1
            if depth==0:return j+1
        j+=1
    return j
def strings(s):
    i=0
    while i<len(s):
        if s[i:i+2]=='//':
            end=s.find('\n',i);i=len(s) if end<0 else end;continue
        if s[i:i+2]=='/*':
            end=s.find('*/',i+2);i=len(s) if end<0 else end+2;continue
        if s[i]=='"':
            end=string_end(s,i);yield i,end,s[i+1:end-1];i=end
        elif s[i]=="'":
            i+=1
            while i<len(s) and s[i]!="'":i+=2 if s[i]=='\\' else 1
            i+=1
        else:i+=1
def template(v):
    out='';args=[];i=0
    while i<len(v):
        if v[i:i+2]=='${':
            end=expr_end(v,i+2);args.append(v[i+2:end-1]);out+=f'%{len(args)}$s';i=end
        elif v[i]=='$' and re.match(r'\w',v[i+1:i+2]):
            m=re.match(r'\w+',v[i+1:]);args.append(m[0]);out+=f'%{len(args)}$s';i+=len(m[0])+1
        else:out+=v[i];i+=1
    return out,args
technical={'SYSTEM','LIGHT','DARK','FORMAL','CASUAL','FRIENDLY','NEUTRAL','NONE','DAILY','WEEKLY','MONTHLY','TASK','NOTE','PROJECT','LIST','ACTION','READ_LATER','EVENT','REMINDER','CONTACT','ADDRESS','REPLY','PENDING','COMPLETED','ARCHIVED','CANCELLED','NORMAL','HIGH','LOW','HH:mm','dd/MM/yyyy','dd/MM/yyyy · HH:mm','yyyy-MM-dd','pt-BR','text/plain','www.','ACTION_SEND','S','T','Q','D','SEG','TER','QUA','QUI','SEX','SAB','DOM'}
# These values are stored in the database; display labels are mapped separately.
persisted={'Trabalho','Pessoal','Ideias','Compras','Viagem','Artigo','Vídeo','Video','Produto','Todas','Fixadas'}
def human(v):
    if v in technical or v in persisted or not v.strip():return False
    if re.match(r'^[a-z0-9_./:#-]+$',v):return False
    if v.startswith(('http','com.','android.')) or 'DateTime' in v:return False
    text,args=template(v)
    stripped=re.sub(r'%\d+\$s','',text)
    if not re.search('[A-Za-zÀ-ÿ]',stripped):return False
    if re.fullmatch(r'[A-Z_]+',v) and len(v)>1:return False
    if re.search(r"(?:EEEE|MMMM|yyyy|dd/MM|HH:mm|MMM|d 'de')",stripped):return False
    return True
def call(v,resolver='textResources.getString'):
    text,args=template(v)
    return resolver+'('+resource(text)+(' , '+', '.join(args) if args else '')+')'

# Resolve presentation-only enum/palette labels at the UI boundary.
for rel in ['ui/search/GlobalSearchViewModel.kt','ui/organize/notes/NotesBoard.kt','ui/organize/notes/NoteColors.kt','navigation/ActionBoxRoot.kt']:
    p=root/rel;s=p.read_text(encoding='utf-8')
    s=s.replace('val label: String','val label: Int')
    end=s.find('@Composable') if rel!='ui/search/GlobalSearchViewModel.kt' else s.find('enum class SearchFilter')
    # Only constructor arguments for label-bearing enums/data objects.
    patterns={'ui/search/GlobalSearchViewModel.kt':r'\b(?:ALL|TASKS|PROJECTS|NOTES|LISTS|SAVED|ACTION|PROJECT|NOTE|LIST)\("([^"]+)"', 'ui/organize/notes/NotesBoard.kt':r'\b(?:RECENT|OLDEST|TITLE|CATEGORY|COLOR)\("([^"]+)"','ui/organize/notes/NoteColors.kt':r'NotePaletteEntry\("[^"]+", "([^"]+)"','navigation/ActionBoxRoot.kt':r'BottomDestination\("[^"]+", "([^"]+)"'}
    s=re.sub(patterns[rel],lambda m:m[0][:m.start(1)-m.start()-1]+resource(m[1])+m[0][m.end(1)-m.start()+1:],s)
    s=s.replace('Text(item.label','Text(textResources.getString(item.label)').replace('Text(sort.label','Text(textResources.getString(sort.label)').replace('contentDescription = item.label','contentDescription = textResources.getString(item.label)')
    if 'R.string' in s:s=s.replace('\n\n','\n\nimport com.luminor.actionbox.R\n',1)
    write(p,s)

# Extract strings in UI function bodies. Capture Resources once per composable so
# callbacks can use it without making composable calls from event handlers.
skip={'ActionEditState.kt','ListEditState.kt','RoutineEditState.kt','DetailState.kt'}
pending=[]
for p in list((root/'ui').rglob('*.kt'))+[root/'navigation/ActionBoxRoot.kt']:
    if p.name.endswith('ViewModel.kt') or p.name in skip or '/events/' in p.as_posix():continue
    s=p.read_text(encoding='utf-8'); changes=[]
    functions=list(re.finditer(r'^(?:(?:private|internal) )?fun (?:<[^\n]+?> )?([\w.]+)\(',s,re.M))
    for index,m in enumerate(functions):
        stop=functions[index+1].start() if index+1<len(functions) else len(s)
        segment=s[m.start():stop]
        # Find the end of the parameter list, accounting for lambda types.
        a=s.index('(',m.start());depth=1;j=a+1
        while depth and j<len(s):
            if s[j]=='(':depth+=1
            elif s[j]==')':depth-=1
            j+=1
        body_start=j
        vals=[(a+m.start(),b+m.start(),v) for a,b,v in strings(segment) if a+m.start()>=body_start and human(v)]
        # Existing label getString uses require a captured Resources too.
        needs=bool(vals) or 'textResources.' in segment
        if not needs:continue
        before=s[max(0,m.start()-140):m.start()]
        composable=bool(re.search(r'@Composable\s*$',before))
        if not composable:
            # Text helper functions called by composables. Context-based helpers
            # (share/copy) already have a non-composable Android boundary.
            if 'context: Context' in s[m.start():j]:
                resolver='context.getString'
            else:
                changes.append((m.start(),m.start(),'@Composable\n'))
                composable=True
                resolver='androidx.compose.ui.res.stringResource'
        else:resolver='textResources.getString'
        if resolver=='textResources.getString':
            k=s.find('{',j,stop)
            if k<0:continue
            changes.append((k+1,k+1,'\n    val textResources = androidx.compose.ui.platform.LocalContext.current.resources\n'))
        for a,b,v in vals:
            # Do not translate logging labels or serialized route/key strings.
            prefix=s[max(body_start,a-55):a]
            if re.search(r'(?:label\s*=|ofPattern\(|forLanguageTag\(|navigate\(|putExtra\(|Log\.[a-z]\()\s*$',prefix):continue
            changes.append((a,b,call(v,resolver)))
    if changes:
        for a,b,v in sorted(changes,reverse=True):s=s[:a]+v+s[b:]
        if 'import com.luminor.actionbox.R' not in s:s=s.replace('\n\n','\n\nimport com.luminor.actionbox.R\n',1)
        if '@Composable' in s and 'import androidx.compose.runtime.Composable' not in s:s=s.replace('\n\n','\n\nimport androidx.compose.runtime.Composable\n',1)
        write(p,s)

write(root/'ui/events/UiText.kt','''package com.luminor.actionbox.ui.events

import android.content.Context
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

sealed interface UiText {
    data class Resource(@param:StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Dynamic(val value: String) : UiText
    fun resolve(context: Context): String = when (this) {
        is Resource -> context.getString(id, *args.toTypedArray())
        is Dynamic -> value
    }
}
class TextResources @Inject constructor(@ApplicationContext private val context: Context) {
    fun getString(@StringRes id: Int, vararg args: Any): String = context.getString(id, *args)
}
''')
p=root/'ui/events/AppUiEvent.kt';s=p.read_text(encoding='utf-8').replace('val text: String','val text: UiText').replace('fun message(text: String)', 'fun message(text: UiText)').replace('fun undo(text: String,','fun undo(text: UiText,')
s=s.replace('class AppUiEventBus {','''class AppUiEventBus {
    fun message(text: String) = message(UiText.Dynamic(text))
    fun message(@androidx.annotation.StringRes id: Int, vararg args: Any) = message(UiText.Resource(id, args.toList()))
    fun undo(@androidx.annotation.StringRes text: Int, kind: UndoKind, id: Long) = undo(UiText.Resource(text), kind, id)
''');write(p,s)

# Commands and ViewModels emit resource references; only generated entity titles
# and external Android APIs need the injected resolver.
for p in list((root/'domain/commands').glob('*.kt'))+list((root/'ui').rglob('*ViewModel.kt')):
    s=p.read_text(encoding='utf-8');changes=[];inject=False
    for a,b,v in strings(s):
        if not human(v):continue
        if s.rfind('enum class ',0,a)>s.rfind('\n}',0,a):continue
        prefix=s[max(0,a-80):a]
        if 'android.util.Log' in prefix:continue
        text,args=template(v)
        if re.search(r'(?:uiEventBus\.message|uiEventBus\.undo|showMessage)\(\s*$',prefix):
            value=resource(text)+(', '+', '.join(args) if args else '')
        else:
            value=call(v);inject=True
        changes.append((a,b,value))
    if changes:
        for a,b,v in reversed(changes):s=s[:a]+v+s[b:]
        if 'import com.luminor.actionbox.R' not in s:s=s.replace('\n\n','\n\nimport com.luminor.actionbox.R\n',1)
    if inject:
        if 'constructor(' in s:s=s.replace('constructor(', 'constructor(\n    private val textResources: com.luminor.actionbox.ui.events.TextResources,',1)
    # Public event functions support resource references from screen callbacks.
    if p.name in ['EventViewModel.kt','CaptureViewModel.kt']:
        s=s.replace('    fun showMessage(value: String)', '    fun showMessage(@androidx.annotation.StringRes id: Int, vararg args: Any) = uiEventBus.message(id, *args)\n    fun showMessage(value: String)')
    write(p,s)

# Notification/platform boundaries already have a Context.
for p in (root/'notification').glob('*.kt'):
    s=p.read_text(encoding='utf-8');changes=[]
    for a,b,v in strings(s):
        if human(v):changes.append((a,b,call(v,'context.getString')))
    if changes:
        for a,b,v in reversed(changes):s=s[:a]+v+s[b:]
        s=s.replace('\n\n','\n\nimport com.luminor.actionbox.R\n',1);write(p,s)

# The root resolves feedback in the current configuration at display time.
p=root/'navigation/ActionBoxRoot.kt';s=p.read_text(encoding='utf-8').replace('    val navController =','    val context = androidx.compose.ui.platform.LocalContext.current\n    val navController =').replace('snackbar.showSnackbar(event.text)','snackbar.showSnackbar(event.text.resolve(context))').replace('message = event.text,','message = event.text.resolve(context),');write(p,s)

def save_resources():
    entries=[]
    for text,name in resources.items():
        # Android string escaping is independent of XML escaping.
        text=html.escape(text,quote=False).replace("'", "\\'").replace('"','\\"')
        entries.append(f'    <string name="{name}">{text}</string>')
    write('app/src/main/res/values/strings.xml','<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'+'\n'.join(entries)+'\n</resources>\n')
save_resources()
write('refactor_resource_map.json',json.dumps(resources,ensure_ascii=False,indent=2))
