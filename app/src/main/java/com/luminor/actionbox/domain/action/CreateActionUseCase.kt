package com.luminor.actionbox.domain.action

import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.data.local.RoutineRuleEntity
import com.luminor.actionbox.data.repository.ActionRepository
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.DetectedAction
import com.luminor.actionbox.domain.RecurrenceType
import com.luminor.actionbox.domain.reminder.ScheduleReminderUseCase
import com.luminor.actionbox.domain.routine.RoutineRuleFactory
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class CreateActionUseCase(
    private val insertAction: suspend (ActionEntity) -> Long,
    private val replaceRoutineRule: suspend (RoutineRuleEntity, Long) -> Unit,
    private val scheduleReminder: (ActionEntity) -> Unit,
    private val routineRuleFactory: RoutineRuleFactory = RoutineRuleFactory(),
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    constructor(
        repository: ActionRepository,
        scheduleReminderUseCase: ScheduleReminderUseCase,
        routineRuleFactory: RoutineRuleFactory = RoutineRuleFactory(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ) : this(
        insertAction = repository::insert,
        replaceRoutineRule = repository::replaceRoutineRule,
        scheduleReminder = scheduleReminderUseCase::invoke,
        routineRuleFactory = routineRuleFactory,
        zoneId = zoneId
    )

    suspend operator fun invoke(
        action: DetectedAction,
        now: LocalDateTime = LocalDateTime.now()
    ): ActionEntity {
        require(action.type !in setOf(ActionType.LIST, ActionType.PROJECT, ActionType.REPLY)) {
            "CreateActionUseCase não cria LIST, PROJECT ou REPLY"
        }

        val normalized = if (action.type == ActionType.REMINDER && action.scheduledAt == null) {
            action.copy(scheduledAt = now.plusHours(1))
        } else {
            action
        }
        val status = when (normalized.type) {
            ActionType.NOTE, ActionType.ADDRESS, ActionType.CONTACT -> ActionStatus.COMPLETED
            else -> ActionStatus.PENDING
        }
        val nowMillis = now.atZone(zoneId).toInstant().toEpochMilli()
        val entity = normalized.toEntity(status, nowMillis)
        val id = insertAction(entity)
        val stored = entity.copy(id = id)

        if (normalized.recurrenceType != RecurrenceType.NONE) {
            val createdDate = Instant.ofEpochMilli(stored.createdAt).atZone(zoneId).toLocalDate()
            val effectiveFrom = routineRuleFactory.startOfDayMillis(createdDate, zoneId)
            replaceRoutineRule(
                routineRuleFactory.create(stored, effectiveFrom, zoneId),
                effectiveFrom - 1
            )
        }

        scheduleReminder(stored)
        return stored
    }

    private fun DetectedAction.toEntity(status: ActionStatus, nowMillis: Long): ActionEntity = ActionEntity(
        type = type.name,
        title = title,
        content = content,
        sourceText = sourceText,
        sourceUrl = sourceUrl,
        scheduledAt = scheduledAt?.atZone(zoneId)?.toInstant()?.toEpochMilli(),
        createdAt = nowMillis,
        completedAt = if (status == ActionStatus.COMPLETED) nowMillis else null,
        status = status.name,
        metadata = metadata,
        description = description.ifBlank { null },
        priority = priority.name,
        recurrenceType = recurrenceType.name,
        recurrenceDays = recurrenceDays.sorted().joinToString(",").ifBlank { null },
        reminderMinutes = reminderMinutes
    )
}
