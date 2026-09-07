package com.luminor.actionbox.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.luminor.actionbox.data.local.ActionBoxDatabase
import com.luminor.actionbox.data.repository.ActionRepository
import com.luminor.actionbox.domain.reminder.ReminderPlanner
import com.luminor.actionbox.domain.reminder.ScheduleReminderUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@dagger.hilt.android.AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @javax.inject.Inject lateinit var repository: com.luminor.actionbox.data.repository.ActionRepository
    @javax.inject.Inject lateinit var scheduler: ReminderScheduler
    @javax.inject.Inject lateinit var scheduleReminder: com.luminor.actionbox.domain.reminder.ScheduleReminderUseCase
    @javax.inject.Inject lateinit var dao: com.luminor.actionbox.data.local.ActionDao
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.pendingReminders().forEach(scheduleReminder::scheduleFuture)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
