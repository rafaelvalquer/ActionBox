package com.luminor.actionbox.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.luminor.actionbox.data.local.ActionBoxDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        if (id < 0) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = ActionBoxDatabase.getInstance(context).actionDao()
                when (intent.action) {
                    ACTION_COMPLETE -> {
                        dao.complete(id)
                        ReminderScheduler(context).cancel(id)
                    }
                    ACTION_SNOOZE -> {
                        val newTime = System.currentTimeMillis() + 10 * 60 * 1000
                        dao.reschedule(id, newTime)
                        val action = dao.getById(id)
                        ReminderScheduler(context).schedule(id, action?.title ?: "Lembrete", newTime)
                    }
                }
                context.getSystemService(android.app.NotificationManager::class.java).cancel(id.toInt())
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE = "com.luminor.actionbox.REMINDER_COMPLETE"
        const val ACTION_SNOOZE = "com.luminor.actionbox.REMINDER_SNOOZE"
        const val EXTRA_ID = "action_id"
    }
}
