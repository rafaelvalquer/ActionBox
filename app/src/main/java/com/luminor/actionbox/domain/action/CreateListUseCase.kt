package com.luminor.actionbox.domain.action

import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.data.local.ActionListEntity
import com.luminor.actionbox.data.local.ListItemEntity
import com.luminor.actionbox.data.repository.ActionRepository
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.DetectedAction
import com.luminor.actionbox.domain.RecurrenceType
import com.luminor.actionbox.domain.reminder.ScheduleReminderUseCase
import java.time.LocalDateTime
import java.time.ZoneId

data class CreatedList(
    val listId: Long,
    val actionId: Long?,
    val itemCount: Int
)

class CreateListUseCase(
    private val insertList: suspend (ActionListEntity) -> Long,
    private val insertListItem: suspend (ListItemEntity) -> Long,
    private val insertAction: suspend (ActionEntity) -> Long,
    private val scheduleReminder: (ActionEntity) -> Unit,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    constructor(
        repository: ActionRepository,
        scheduleReminderUseCase: ScheduleReminderUseCase,
        zoneId: ZoneId = ZoneId.systemDefault()
    ) : this(
        insertList = repository::insertList,
        insertListItem = repository::insertListItem,
        insertAction = repository::insert,
        scheduleReminder = scheduleReminderUseCase::invoke,
        zoneId = zoneId
    )

    suspend operator fun invoke(
        action: DetectedAction,
        now: LocalDateTime = LocalDateTime.now()
    ): CreatedList {
        require(action.type == ActionType.LIST) { "CreateListUseCase exige uma ação LIST" }

        val nowMillis = now.atZone(zoneId).toInstant().toEpochMilli()
        val listId = insertList(
            ActionListEntity(
                title = action.title.ifBlank { "Nova lista" },
                createdAt = nowMillis
            )
        )
        action.items.forEachIndexed { index, item ->
            insertListItem(
                ListItemEntity(
                    listId = listId,
                    title = item,
                    position = index
                )
            )
        }

        val actionId = if (action.scheduledAt != null || action.recurrenceType != RecurrenceType.NONE) {
            val entity = ActionEntity(
                type = ActionType.LIST.name,
                title = action.title,
                content = action.content,
                sourceText = action.sourceText,
                sourceUrl = action.sourceUrl,
                scheduledAt = action.scheduledAt?.atZone(zoneId)?.toInstant()?.toEpochMilli(),
                createdAt = nowMillis,
                status = ActionStatus.PENDING.name,
                metadata = listId.toString(),
                description = action.description.ifBlank { null },
                priority = action.priority.name,
                recurrenceType = action.recurrenceType.name,
                recurrenceDays = action.recurrenceDays.sorted().joinToString(",").ifBlank { null },
                reminderMinutes = action.reminderMinutes
            )
            val storedId = insertAction(entity)
            scheduleReminder(entity.copy(id = storedId))
            storedId
        } else {
            null
        }

        return CreatedList(
            listId = listId,
            actionId = actionId,
            itemCount = action.items.size
        )
    }
}
