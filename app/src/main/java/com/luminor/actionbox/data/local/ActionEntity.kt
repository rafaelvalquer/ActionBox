package com.luminor.actionbox.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "actions")
data class ActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val title: String,
    val content: String,
    val sourceText: String,
    val sourceUrl: String? = null,
    val scheduledAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val status: String,
    val metadata: String? = null,
    val description: String? = null,
    val endAt: Long? = null,
    val priority: String? = null,
    val recurrenceType: String? = null,
    val recurrenceDays: String? = null,
    val reminderMinutes: Int? = null,
    val projectId: Long? = null,
    val noteCategory: String? = null,
    val noteColor: String? = null,
    val isPinned: Boolean = false,
    val updatedAt: Long? = null
)
