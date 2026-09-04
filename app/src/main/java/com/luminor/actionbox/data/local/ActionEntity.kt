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
    val metadata: String? = null
)
