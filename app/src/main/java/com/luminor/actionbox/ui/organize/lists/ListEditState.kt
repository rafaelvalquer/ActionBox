package com.luminor.actionbox.ui.organize.lists

import com.luminor.actionbox.data.local.ActionListEntity
import com.luminor.actionbox.data.local.ListItemEntity

data class ListItemEditState(
    val id: Long = 0,
    val title: String,
    val completedAt: Long? = null,
    val position: Int = 0
) {
    fun toEntity(listId: Long, index: Int): ListItemEntity = ListItemEntity(
        id = id,
        listId = listId,
        title = title.trim(),
        position = index,
        completedAt = completedAt
    )
}

data class ListEditState(
    val id: Long,
    val title: String,
    val items: List<ListItemEditState>,
    val originalTitle: String,
    val originalItems: List<ListItemEditState>
) {
    val hasChanges: Boolean
        get() = title != originalTitle || items != originalItems

    companion object {
        fun from(list: ActionListEntity, items: List<ListItemEntity>): ListEditState {
            val drafts = items.sortedBy { it.position }.map {
                ListItemEditState(it.id, it.title, it.completedAt, it.position)
            }
            return ListEditState(
                id = list.id,
                title = list.title,
                items = drafts,
                originalTitle = list.title,
                originalItems = drafts
            )
        }
    }
}
