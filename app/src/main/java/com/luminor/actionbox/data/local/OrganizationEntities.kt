package com.luminor.actionbox.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "projects",
    indices = [Index(value = ["deletedAt"])]
)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val archived: Boolean = false,
    val completedAt: Long? = null,
    val updatedAt: Long? = null,
    val sortOrder: Int = 0,
    val deletedAt: Long? = null
)

@Entity(
    tableName = "action_lists",
    indices = [Index(value = ["deletedAt"])]
)
data class ActionListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val archived: Boolean = false,
    val completedAt: Long? = null,
    val updatedAt: Long? = null,
    val sortOrder: Int = 0,
    val deletedAt: Long? = null
)

@Entity(tableName = "list_items")
data class ListItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val listId: Long,
    val title: String,
    val position: Int = 0,
    val completedAt: Long? = null
)

@Entity(
    tableName = "action_completions",
    indices = [Index(value = ["actionId", "occurrenceDate"], unique = true)]
)
data class ActionCompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionId: Long,
    val occurrenceDate: String,
    val completedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tags",
    indices = [Index(value = ["normalizedName"], unique = true)]
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val normalizedName: String,
    val colorKey: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tag_refs",
    primaryKeys = ["tagId", "ownerType", "ownerId"],
    indices = [Index(value = ["ownerType", "ownerId"])]
)
data class TagRefEntity(
    val tagId: Long,
    val ownerType: String,
    val ownerId: Long
)

@Entity(
    tableName = "content_links",
    indices = [
        Index(value = ["sourceType", "sourceId"]),
        Index(value = ["targetType", "targetId"]),
        Index(
            value = ["sourceType", "sourceId", "targetType", "targetId", "relationType"],
            unique = true
        )
    ]
)
data class ContentLinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceType: String,
    val sourceId: Long,
    val targetType: String,
    val targetId: Long,
    val relationType: String = "RELATED",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "routine_rules",
    indices = [Index(value = ["actionId"])]
)
data class RoutineRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionId: Long,
    val effectiveFrom: Long,
    val effectiveUntil: Long? = null,
    val recurrenceType: String,
    val recurrenceDays: String? = null,
    val scheduledTimeMinutes: Int? = null,
    val reminderMinutes: Int? = null
)
