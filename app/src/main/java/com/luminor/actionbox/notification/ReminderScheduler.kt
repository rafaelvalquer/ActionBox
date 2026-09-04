package com.luminor.actionbox.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(id: Long, title: String, triggerAtMillis: Long) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.toInt(),
            Intent(context, ReminderReceiver::class.java).apply {
                putExtra(ReminderReceiver.EXTRA_ID, id)
                putExtra(ReminderReceiver.EXTRA_TITLE, title)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis.coerceAtLeast(System.currentTimeMillis() + 1000),
            pendingIntent
        )
    }

    fun cancel(id: Long) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.toInt(),
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) alarmManager.cancel(pendingIntent)
    }
}
