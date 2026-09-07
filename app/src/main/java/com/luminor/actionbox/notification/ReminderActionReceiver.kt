package com.luminor.actionbox.notification

import com.luminor.actionbox.R
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.luminor.actionbox.data.local.ActionBoxDatabase
import com.luminor.actionbox.data.local.ActionCompletionEntity
import com.luminor.actionbox.domain.RecurrenceCalculator
import com.luminor.actionbox.domain.RecurrenceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

@dagger.hilt.android.AndroidEntryPoint
class ReminderActionReceiver : BroadcastReceiver() {
    @javax.inject.Inject lateinit var repository: com.luminor.actionbox.data.repository.ActionRepository
    @javax.inject.Inject lateinit var scheduler: ReminderScheduler
    @javax.inject.Inject lateinit var scheduleReminder: com.luminor.actionbox.domain.reminder.ScheduleReminderUseCase
    @javax.inject.Inject lateinit var dao: com.luminor.actionbox.data.local.ActionDao
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        if (id < 0) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val action = dao.getById(id)
                when (intent.action) {
                    ACTION_COMPLETE -> {
                        if (action != null && RecurrenceCalculator.recurrenceType(action) != RecurrenceType.NONE) {
                            dao.insertCompletion(ActionCompletionEntity(actionId = id, occurrenceDate = LocalDate.now().toString()))
                        } else {
                            dao.complete(id)
                            scheduler.cancel(id)
                        }
                    }
                    ACTION_SNOOZE -> {
                        val newTime = System.currentTimeMillis() + 10 * 60 * 1000
                        if (action == null || RecurrenceCalculator.recurrenceType(action) == RecurrenceType.NONE) dao.reschedule(id, newTime)
                        scheduler.schedule(id, action?.title ?: context.getString(R.string.text_lembrete), newTime)
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
