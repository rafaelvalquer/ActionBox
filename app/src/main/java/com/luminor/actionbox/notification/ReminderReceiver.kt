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
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Você tem um lembrete" }
        if (id < 0) return
        NotificationHelper.showReminder(context, id, title)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val action = ActionBoxDatabase.getInstance(context).actionDao().getById(id) ?: return@launch
                if (RecurrenceCalculator.recurrenceType(action) != RecurrenceType.NONE) {
                    val next = RecurrenceCalculator.nextOccurrence(action, LocalDateTime.now().plusMinutes(1)) ?: return@launch
                    val trigger = next.minusMinutes((action.reminderMinutes ?: 0).toLong())
                    ReminderScheduler(context).schedule(id, action.title, trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_ID = "action_id"
        const val EXTRA_TITLE = "action_title"
    }
}
