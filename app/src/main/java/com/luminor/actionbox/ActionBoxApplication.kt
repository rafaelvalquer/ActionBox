package com.luminor.actionbox

import android.app.Application
import com.luminor.actionbox.data.local.ActionBoxDatabase
import com.luminor.actionbox.data.preferences.SettingsRepository
import com.luminor.actionbox.data.repository.ActionRepository
import com.luminor.actionbox.notification.NotificationHelper
import com.luminor.actionbox.ui.events.AppUiEventBus
import com.luminor.actionbox.worker.TrashCleanupWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class ActionBoxApplication : Application() {
    val database: ActionBoxDatabase by lazy { ActionBoxDatabase.getInstance(this) }
    val repository: ActionRepository by lazy { ActionRepository(database) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }
    val uiEventBus: AppUiEventBus by lazy { AppUiEventBus() }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        TrashCleanupWorker.schedule(this)
        appScope.launch {
            repository.purgeTrash(System.currentTimeMillis() - TrashCleanupWorker.RETENTION_MILLIS)
        }
    }
}
