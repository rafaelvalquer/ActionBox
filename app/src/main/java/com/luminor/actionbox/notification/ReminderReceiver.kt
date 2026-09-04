package com.luminor.actionbox.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Você tem um lembrete" }
        if (id >= 0) NotificationHelper.showReminder(context, id, title)
    }

    companion object {
        const val EXTRA_ID = "action_id"
        const val EXTRA_TITLE = "action_title"
    }
}
