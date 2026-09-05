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

    @Query("SELECT * FROM actions WHERE status = 'PENDING' AND type IN ('TASK','REMINDER','EVENT','LIST') ORDER BY CASE WHEN scheduledAt IS NULL THEN 1 ELSE 0 END, scheduledAt ASC, createdAt DESC")
    fun observePending(): Flow<List<ActionEntity>>

    @Query("SELECT * FROM actions WHERE type = 'READ_LATER' AND status != 'ARCHIVED' ORDER BY createdAt DESC")
    fun observeSaved(): Flow<List<ActionEntity>>

    @Query("SELECT * FROM actions WHERE type = 'NOTE' AND status != 'ARCHIVED' ORDER BY createdAt DESC")
    fun observeNotes(): Flow<List<ActionEntity>>

    @Query("SELECT * FROM actions WHERE status = 'COMPLETED' ORDER BY COALESCE(completedAt, createdAt) DESC LIMIT 100")
    fun observeHistory(): Flow<List<ActionEntity>>

    @Query("SELECT * FROM projects WHERE archived = 0 ORDER BY completedAt IS NOT NULL, createdAt DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM action_lists WHERE archived = 0 ORDER BY completedAt IS NOT NULL, createdAt DESC")
    fun observeLists(): Flow<List<ActionListEntity>>

    @Query("SELECT * FROM list_items ORDER BY listId, position, id")
    fun observeListItems(): Flow<List<ListItemEntity>>

    @Query("SELECT * FROM action_completions ORDER BY completedAt DESC")
    fun observeCompletions(): Flow<List<ActionCompletionEntity>>

    @Query("SELECT * FROM actions WHERE type IN ('REMINDER','TASK','EVENT','LIST') AND status = 'PENDING' AND scheduledAt IS NOT NULL")
    suspend fun getPendingReminders(): List<ActionEntity>

    @Query("SELECT * FROM actions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ActionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ActionEntity): Long

    @Insert
    suspend fun insertProject(project: ProjectEntity): Long

    @Insert
    suspend fun insertList(list: ActionListEntity): Long

    @Insert
    suspend fun insertListItem(item: ListItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: ActionCompletionEntity): Long

    @Query("UPDATE actions SET status = 'COMPLETED', completedAt = :completedAt WHERE id = :id")
    suspend fun complete(id: Long, completedAt: Long = System.currentTimeMillis())

    @Query("UPDATE actions SET status = 'PENDING', completedAt = NULL WHERE id = :id")
    suspend fun reopen(id: Long)

    @Query("UPDATE actions SET status = 'ARCHIVED' WHERE id = :id")
    suspend fun archive(id: Long)

    @Query("UPDATE actions SET scheduledAt = :scheduledAt, status = 'PENDING' WHERE id = :id")
    suspend fun reschedule(id: Long, scheduledAt: Long)

    @Query("UPDATE list_items SET completedAt = :completedAt WHERE id = :id")
    suspend fun setListItemCompleted(id: Long, completedAt: Long?)

    @Query("UPDATE projects SET completedAt = :completedAt WHERE id = :id")
    suspend fun setProjectCompleted(id: Long, completedAt: Long?)

    @Query("UPDATE action_lists SET completedAt = :completedAt WHERE id = :id")
    suspend fun setListCompleted(id: Long, completedAt: Long?)

    @Query("DELETE FROM action_completions WHERE actionId = :actionId AND occurrenceDate = :occurrenceDate")
    suspend fun deleteCompletion(actionId: Long, occurrenceDate: String)

    @Query("DELETE FROM actions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM actions WHERE projectId = :projectId")
    suspend fun deleteProjectActions(projectId: Long)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: Long)

    @Query("DELETE FROM list_items WHERE listId = :listId")
    suspend fun deleteListItems(listId: Long)

    @Query("DELETE FROM action_lists WHERE id = :id")
    suspend fun deleteList(id: Long)

    @Query("DELETE FROM actions")
    suspend fun deleteAllActions()

    @Query("DELETE FROM projects")
    suspend fun deleteAllProjects()

    @Query("DELETE FROM action_lists")
    suspend fun deleteAllLists()

    @Query("DELETE FROM list_items")
    suspend fun deleteAllListItems()

    @Query("DELETE FROM action_completions")
    suspend fun deleteAllCompletions()
}
