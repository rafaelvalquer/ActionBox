package com.luminor.actionbox

import android.app.Application
import com.luminor.actionbox.data.local.ActionBoxDatabase
import com.luminor.actionbox.data.preferences.SettingsRepository
import com.luminor.actionbox.data.repository.ActionRepository
import com.luminor.actionbox.notification.NotificationHelper

class ActionBoxApplication : Application() {
    val database: ActionBoxDatabase by lazy { ActionBoxDatabase.getInstance(this) }
    val repository: ActionRepository by lazy { ActionRepository(database.actionDao()) }
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
    }
}
