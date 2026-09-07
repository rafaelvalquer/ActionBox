from pathlib import Path
import re

root = Path('app/src/main/java/com/luminor/actionbox')
def write(p, s):
    p = Path(p); p.parent.mkdir(parents=True, exist_ok=True); p.write_text(s, encoding='utf-8')
def edit(p, f):
    p = Path(p); write(p, f(p.read_text(encoding='utf-8')))

# Preserve the versions in the working scripts, rather than the stale catalog.
app = Path('app/build.gradle.kts').read_text()
build = Path('build.gradle.kts').read_text()
versions, libraries, plugins = {}, {}, {}
for plugin, version in re.findall(r'id\("([^"]+)"\) version "([^"]+)"', build):
    alias = {'com.android.application':'android-application','org.jetbrains.kotlin.android':'kotlin-android','org.jetbrains.kotlin.plugin.compose':'kotlin-compose','com.google.devtools.ksp':'ksp'}[plugin]
    versions[alias] = version
    plugins[alias] = (plugin, alias)
def library(group, name, version=None):
    alias = (group.replace('androidx.', '').replace('com.google.', '').replace('.', '-') + '-' + name)
    if version: versions[alias] = version
    libraries[alias] = (group+':'+name, alias if version else None)
    return 'libs.'+alias.replace('-', '.')
def dep(m):
    return library(*m.group(1).split(':'))
app = re.sub(r'"([\w.-]+:[\w.-]+(?::[\w.-]+)?)"', dep, app)
for alias, (plugin, _) in plugins.items():
    app = app.replace('id("'+plugin+'")', 'alias(libs.plugins.'+alias.replace('-', '.')+')')
versions['hilt'] = '2.60.1'
plugins['hilt'] = ('com.google.dagger.hilt.android', 'hilt')
app = app.replace('plugins {', 'plugins {\n    alias(libs.plugins.hilt)')
extra = [('implementation','com.google.dagger','hilt-android','2.60.1'),('ksp','com.google.dagger','hilt-compiler','2.60.1'),('implementation','androidx.hilt','hilt-navigation-compose','1.3.0'),('implementation','androidx.hilt','hilt-work','1.3.0'),('ksp','androidx.hilt','hilt-compiler','1.3.0'),('implementation','androidx.paging','paging-runtime-ktx','3.4.2'),('implementation','androidx.paging','paging-compose','3.4.2'),('implementation','androidx.room','room-paging','2.8.4'),('testImplementation','org.jetbrains.kotlinx','kotlinx-coroutines-test','1.10.2'),('androidTestImplementation','org.jetbrains.kotlinx','kotlinx-coroutines-test','1.10.2'),('androidTestImplementation','androidx.paging','paging-testing','3.4.2')]
app += '\n' if not app.endswith('\n') else ''
idx = app.rfind('}')
app = app[:idx] + ''.join(f'    {config}({library(g,n,v)})\n' for config,g,n,v in extra) + app[idx:]
write('app/build.gradle.kts', app)
write('build.gradle.kts', 'plugins {\n'+''.join(f'    alias(libs.plugins.{a.replace("-", ".")}) apply false\n' for a in plugins)+'}\n')
write('gradle/libs.versions.toml', '[versions]\n'+''.join(f'{k} = "{v}"\n' for k,v in versions.items())+'\n[libraries]\n'+''.join(f'{k} = {{ module = "{m}"'+(f', version.ref = "{v}"' if v else '')+' }\n' for k,(m,v) in libraries.items())+'\n[plugins]\n'+''.join(f'{a} = {{ id = "{p}", version.ref = "{v}" }}\n' for a,(p,v) in plugins.items()))

write(root/'di/AppModule.kt', '''package com.luminor.actionbox.di

import android.content.Context
import com.luminor.actionbox.data.local.ActionBoxDatabase
import com.luminor.actionbox.data.preferences.SettingsRepository
import com.luminor.actionbox.data.repository.ActionRepository
import com.luminor.actionbox.domain.DetectActionUseCase
import com.luminor.actionbox.domain.action.*
import com.luminor.actionbox.domain.reminder.*
import com.luminor.actionbox.domain.routine.RoutineRuleFactory
import com.luminor.actionbox.notification.ReminderScheduler
import com.luminor.actionbox.ui.events.AppUiEventBus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton fun database(@ApplicationContext context: Context) = ActionBoxDatabase.getInstance(context)
    @Provides fun dao(database: ActionBoxDatabase) = database.actionDao()
    @Provides @Singleton fun repository(database: ActionBoxDatabase) = ActionRepository(database)
    @Provides @Singleton fun settings(@ApplicationContext context: Context) = SettingsRepository(context)
    @Provides @Singleton fun events() = AppUiEventBus()
    @Provides @Singleton fun scheduler(@ApplicationContext context: Context) = ReminderScheduler(context)
    @Provides fun reminders(scheduler: ReminderScheduler) = ScheduleReminderUseCase(ReminderPlanner(), scheduler)
    @Provides fun rules() = RoutineRuleFactory()
    @Provides fun detector() = DetectActionUseCase()
    @Provides fun createAction(repository: ActionRepository, reminders: ScheduleReminderUseCase) = CreateActionUseCase(repository, reminders)
    @Provides fun createList(repository: ActionRepository, reminders: ScheduleReminderUseCase) = CreateListUseCase(repository, reminders)
    @Provides fun createProject(repository: ActionRepository) = CreateProjectUseCase(repository)
    @Provides fun saveReply(repository: ActionRepository) = SaveReplyUseCase(repository)
}
''')
edit(root/'ActionBoxApplication.kt', lambda s: re.sub(r'    val database:.*?    private val appScope', '''    @javax.inject.Inject lateinit var repository: ActionRepository
    @javax.inject.Inject lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory
    override val workManagerConfiguration: androidx.work.Configuration
        get() = androidx.work.Configuration.Builder().setWorkerFactory(workerFactory).build()

    private val appScope''', s, flags=re.S).replace('class ActionBoxApplication : Application()', '@dagger.hilt.android.HiltAndroidApp\nclass ActionBoxApplication : Application(), androidx.work.Configuration.Provider'))
edit(root/'MainActivity.kt', lambda s: s.replace('class MainActivity', '@dagger.hilt.android.AndroidEntryPoint\nclass MainActivity'))
edit(root/'worker/TrashCleanupWorker.kt', lambda s: s.replace('class TrashCleanupWorker(', '@androidx.hilt.work.HiltWorker\nclass TrashCleanupWorker @dagger.assisted.AssistedInject constructor(').replace('    appContext: Context,','    @dagger.assisted.Assisted appContext: Context,').replace('    params: WorkerParameters','    @dagger.assisted.Assisted params: WorkerParameters,\n    private val repository: com.luminor.actionbox.data.repository.ActionRepository').replace('        val app = applicationContext as ActionBoxApplication\n        app.repository.', '        repository.'))
edit('app/src/main/AndroidManifest.xml', lambda s: s.replace('<manifest xmlns:android=', '<manifest xmlns:tools="http://schemas.android.com/tools" xmlns:android=').replace('        <activity', '''        <provider
            android:name="androidx.startup.InitializationProvider"
            android:authorities="${applicationId}.androidx-startup"
            android:exported="false"
            tools:node="merge">
            <meta-data android:name="androidx.work.WorkManagerInitializer" tools:node="remove" />
        </provider>

        <activity''', 1))
for p in (root/'notification').glob('*Receiver.kt'):
    s=p.read_text()
    s=s.replace('class '+p.stem, '@dagger.hilt.android.AndroidEntryPoint\nclass '+p.stem)
    s=s.replace(' : BroadcastReceiver() {', ''' : BroadcastReceiver() {
    @javax.inject.Inject lateinit var repository: com.luminor.actionbox.data.repository.ActionRepository
    @javax.inject.Inject lateinit var scheduler: ReminderScheduler
    @javax.inject.Inject lateinit var scheduleReminder: com.luminor.actionbox.domain.reminder.ScheduleReminderUseCase
    @javax.inject.Inject lateinit var dao: com.luminor.actionbox.data.local.ActionDao''')
    s=re.sub(r'                val repository = ActionRepository.*?\n                repository.pendingReminders', '                repository.pendingReminders', s, flags=re.S)
    s=s.replace('                val dao = ActionBoxDatabase.getInstance(context).actionDao()\n','').replace('ActionBoxDatabase.getInstance(context).actionDao().getById(id)','repository.getById(id)').replace('ReminderScheduler(context)', 'scheduler')
    write(p,s)

# Migrate existing feature ViewModels to constructor injection.
for rel in ['ui/organize/OrganizeViewModel.kt','ui/organize/notes/NotesViewModel.kt','ui/trash/TrashViewModel.kt','ui/search/GlobalSearchViewModel.kt']:
    p=root/rel; s=p.read_text()
    s=re.sub(r'class (\w+)\(application: Application\) : AndroidViewModel\(application\) \{.*?(?=\n    (?:val |fun |private val content))', r'''@dagger.hilt.android.lifecycle.HiltViewModel
class \1 @javax.inject.Inject constructor(
    private val repository: com.luminor.actionbox.data.repository.ActionRepository,
    private val uiEventBus: com.luminor.actionbox.ui.events.AppUiEventBus
) : androidx.lifecycle.ViewModel() {
''',s,flags=re.S)
    s=s.replace('app.uiEventBus','uiEventBus')
    write(p,s)
p=root/'ui/capture/CaptureViewModel.kt'
s=p.read_text(); start=s.index('class CaptureViewModel'); end=s.index('    private val _input')
s=s[:start]+'''@dagger.hilt.android.lifecycle.HiltViewModel
class CaptureViewModel @javax.inject.Inject constructor(
    private val detectAction: DetectActionUseCase,
    private val createAction: CreateActionUseCase,
    private val createList: CreateListUseCase,
    private val createProject: CreateProjectUseCase,
    private val saveReply: SaveReplyUseCase,
    private val settingsRepository: com.luminor.actionbox.data.preferences.SettingsRepository,
    private val uiEventBus: com.luminor.actionbox.ui.events.AppUiEventBus
) : androidx.lifecycle.ViewModel() {
    // Preferences are tiny and needed by share/capture callbacks even without a screen collector.
    private val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, UiSettings())

'''+s[end:]; write(p,s.replace('app.uiEventBus','uiEventBus'))
