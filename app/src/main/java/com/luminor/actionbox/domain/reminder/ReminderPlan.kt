package com.luminor.actionbox.domain.reminder

data class ReminderPlan(
    val actionId: Long,
    val title: String,
    val triggerAtMillis: Long
)
