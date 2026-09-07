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

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = ActionRepository(ActionBoxDatabase.getInstance(context))
                val scheduleReminder = ScheduleReminderUseCase(
                    ReminderPlanner(),
                    ReminderScheduler(context.applicationContext)
                )
                repository.pendingReminders().forEach(scheduleReminder::scheduleFuture)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
