package com.luminor.actionbox.domain.action

import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.data.local.ProjectEntity
import com.luminor.actionbox.data.repository.ActionRepository
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.DetectedAction
import com.luminor.actionbox.domain.RecurrenceType
import java.time.LocalDateTime
import java.time.ZoneId

data class CreatedProject(
    val projectId: Long,
    val taskCount: Int
)

class CreateProjectUseCase(
    private val insertProject: suspend (ProjectEntity) -> Long,
    private val insertAction: suspend (ActionEntity) -> Long,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    constructor(
        repository: ActionRepository,
        zoneId: ZoneId = ZoneId.systemDefault()
    ) : this(
        insertProject = repository::insertProject,
        insertAction = repository::insert,
        zoneId = zoneId
    )

    suspend operator fun invoke(
        action: DetectedAction,
        now: LocalDateTime = LocalDateTime.now()
    ): CreatedProject {
        require(action.type == ActionType.PROJECT) { "CreateProjectUseCase exige uma ação PROJECT" }

        val nowMillis = now.atZone(zoneId).toInstant().toEpochMilli()
        val projectId = insertProject(
            ProjectEntity(
                title = action.title.ifBlank { "Novo projeto" },
                description = action.description,
                createdAt = nowMillis
            )
        )

        action.items.forEachIndexed { index, item ->
            insertAction(
                ActionEntity(
                    type = ActionType.TASK.name,
                    title = item,
                    content = item,
                    sourceText = action.sourceText,
                    sourceUrl = action.sourceUrl,
                    scheduledAt = action.scheduledAt?.atZone(zoneId)?.toInstant()?.toEpochMilli(),
                    createdAt = nowMillis,
                    status = ActionStatus.PENDING.name,
                    metadata = action.metadata,
                    description = action.description.ifBlank { null },
                    priority = action.priority.name,
                    recurrenceType = RecurrenceType.NONE.name,
                    reminderMinutes = null,
                    projectId = projectId,
                    sortOrder = index
                )
            )
        }

        return CreatedProject(projectId = projectId, taskCount = action.items.size)
    }
}
