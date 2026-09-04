package com.luminor.actionbox.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionDao {
    @Query("SELECT * FROM actions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ActionEntity>>

    @Query("SELECT * FROM actions WHERE status = 'PENDING' AND type IN ('TASK','REMINDER') ORDER BY CASE WHEN scheduledAt IS NULL THEN 1 ELSE 0 END, scheduledAt ASC, createdAt DESC")
    fun observePending(): Flow<List<ActionEntity>>

    @Query("SELECT * FROM actions WHERE type = 'READ_LATER' AND status != 'ARCHIVED' ORDER BY createdAt DESC")
    fun observeSaved(): Flow<List<ActionEntity>>

    @Query("SELECT * FROM actions WHERE type = 'NOTE' AND status != 'ARCHIVED' ORDER BY createdAt DESC")
    fun observeNotes(): Flow<List<ActionEntity>>

    @Query("SELECT * FROM actions WHERE status = 'COMPLETED' ORDER BY COALESCE(completedAt, createdAt) DESC LIMIT 100")
    fun observeHistory(): Flow<List<ActionEntity>>

    @Query("SELECT * FROM actions WHERE type = 'REMINDER' AND status = 'PENDING' AND scheduledAt IS NOT NULL")
    suspend fun getPendingReminders(): List<ActionEntity>

    @Query("SELECT * FROM actions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ActionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ActionEntity): Long

    @Query("UPDATE actions SET status = 'COMPLETED', completedAt = :completedAt WHERE id = :id")
    suspend fun complete(id: Long, completedAt: Long = System.currentTimeMillis())

    @Query("UPDATE actions SET status = 'ARCHIVED' WHERE id = :id")
    suspend fun archive(id: Long)

    @Query("UPDATE actions SET scheduledAt = :scheduledAt, status = 'PENDING' WHERE id = :id")
    suspend fun reschedule(id: Long, scheduledAt: Long)

    @Query("DELETE FROM actions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM actions")
    suspend fun deleteAll()
}
