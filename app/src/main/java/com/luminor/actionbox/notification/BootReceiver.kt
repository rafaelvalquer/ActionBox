package com.luminor.actionbox.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.luminor.actionbox.data.local.ActionBoxDatabase
import com.luminor.actionbox.domain.RecurrenceCalculator
import com.luminor.actionbox.domain.RecurrenceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = ActionBoxDatabase.getInstance(context).actionDao()
                val scheduler = ReminderScheduler(context)
                val now = LocalDateTime.now()
                dao.getPendingReminders().forEach { action ->
                    val shouldNotify = action.type == "REMINDER" || action.reminderMinutes != null
                    if (!shouldNotify) return@forEach
                    val base = if (RecurrenceCalculator.recurrenceType(action) == RecurrenceType.NONE) {
                        action.scheduledAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime() }
                    } else RecurrenceCalculator.nextOccurrence(action, now.minusSeconds(1))
                    val trigger = base?.minusMinutes((action.reminderMinutes ?: 0).toLong()) ?: return@forEach
                    if (trigger.isAfter(now)) scheduler.schedule(action.id, action.title, trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
