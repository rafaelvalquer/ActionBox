package com.luminor.actionbox.domain.action

import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.data.repository.ActionRepository
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.ActionType

class SaveReplyUseCase(
    private val insertAction: suspend (ActionEntity) -> Long
) {
    constructor(repository: ActionRepository) : this(repository::insert)

    suspend operator fun invoke(text: String, source: String, nowMillis: Long = System.currentTimeMillis()): ActionEntity {
        val entity = ActionEntity(
            type = ActionType.REPLY.name,
            title = "Resposta copiada",
            content = text,
            sourceText = source,
            createdAt = nowMillis,
            status = ActionStatus.COMPLETED.name,
            completedAt = nowMillis
        )
        val id = insertAction(entity)
        return entity.copy(id = id)
    }
}
