package com.luminor.actionbox.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.luminor.actionbox.data.local.ActionBoxDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = ActionBoxDatabase.getInstance(context).actionDao()
                val scheduler = ReminderScheduler(context)
                dao.getPendingReminders()
                    .filter { (it.scheduledAt ?: 0L) > System.currentTimeMillis() }
                    .forEach { scheduler.schedule(it.id, it.title, it.scheduledAt!!) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
