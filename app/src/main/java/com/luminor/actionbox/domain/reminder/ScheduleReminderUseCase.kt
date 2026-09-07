package com.luminor.actionbox.domain.reminder

import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.notification.ReminderScheduler

class ScheduleReminderUseCase(
    private val planner: ReminderPlanner,
    private val scheduleAlarm: (Long, String, Long) -> Unit,
    private val cancelAlarm: (Long) -> Unit
) {
    constructor(
        planner: ReminderPlanner,
        scheduler: ReminderScheduler
    ) : this(
        planner = planner,
        scheduleAlarm = scheduler::schedule,
        cancelAlarm = scheduler::cancel
    )

    operator fun invoke(action: ActionEntity) {
        val plan = planner.plan(action) ?: return
        scheduleAlarm(plan.actionId, plan.title, plan.triggerAtMillis)
    }

    fun scheduleFuture(action: ActionEntity, nowMillis: Long = System.currentTimeMillis()) {
        val plan = planner.plan(action) ?: return
        if (plan.triggerAtMillis > nowMillis) {
            scheduleAlarm(plan.actionId, plan.title, plan.triggerAtMillis)
        }
    }

    fun cancel(actionId: Long) {
        cancelAlarm(actionId)
    }

    fun reschedule(action: ActionEntity) {
        cancel(action.id)
        invoke(action)
    }
}
