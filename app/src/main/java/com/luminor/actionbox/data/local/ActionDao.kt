package com.luminor.actionbox.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionDao {
    @Query("SELECT * FROM actions WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ActionEntity>>

    @Query("SELECT * FROM actions WHERE deletedAt IS NULL AND status = 'PENDING' AND type IN ('TASK','REMINDER','EVENT','LIST') ORDER BY CASE WHEN scheduledAt IS NULL THEN 1 ELSE 0 END, scheduledAt ASC, createdAt DESC")
    fun observePending(): Flow<List<ActionEntity>>

    @Query("SELECT * FROM actions WHERE deletedAt IS NULL AND type = 'READ_LATER' AND status != 'ARCHIVED' ORDER BY createdAt DESC")
    fun observeSaved(): Flow<List<ActionEntity>>

    @Query("SELECT * FROM actions WHERE deletedAt IS NULL AND type = 'NOTE' AND status != 'ARCHIVED' ORDER BY isPinned DESC, COALESCE(updatedAt, createdAt) DESC")
    fun observeNotes(): Flow<List<ActionEntity>>

    @Query("SELECT * FROM actions WHERE deletedAt IS NULL AND status = 'COMPLETED' ORDER BY COALESCE(completedAt, createdAt) DESC LIMIT 100")
    fun observeHistory(): Flow<List<ActionEntity>>

    @Query("SELECT * FROM projects WHERE deletedAt IS NULL AND archived = 0 ORDER BY completedAt IS NOT NULL, sortOrder, createdAt DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM action_lists WHERE deletedAt IS NULL AND archived = 0 ORDER BY completedAt IS NOT NULL, sortOrder, createdAt DESC")
    fun observeLists(): Flow<List<ActionListEntity>>

    @Query("SELECT * FROM list_items WHERE listId IN (SELECT id FROM action_lists WHERE deletedAt IS NULL) ORDER BY listId, position, id")
    fun observeListItems(): Flow<List<ListItemEntity>>

    @Query("SELECT * FROM action_completions ORDER BY completedAt DESC")
    fun observeCompletions(): Flow<List<ActionCompletionEntity>>

    @Query("SELECT * FROM actions WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeDeletedActions(): Flow<List<ActionEntity>>

    @Query("SELECT * FROM projects WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeDeletedProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM action_lists WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeDeletedLists(): Flow<List<ActionListEntity>>

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE")
    fun observeTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tag_refs")
    fun observeTagRefs(): Flow<List<TagRefEntity>>

    @Query("SELECT * FROM content_links ORDER BY createdAt DESC")
    fun observeContentLinks(): Flow<List<ContentLinkEntity>>

    @Query("SELECT * FROM routine_rules ORDER BY effectiveFrom DESC")
    fun observeRoutineRules(): Flow<List<RoutineRuleEntity>>

    @Query("SELECT * FROM actions WHERE deletedAt IS NULL AND type IN ('REMINDER','TASK','EVENT','LIST') AND status = 'PENDING' AND scheduledAt IS NOT NULL")
    suspend fun getPendingReminders(): List<ActionEntity>

    @Query("SELECT * FROM actions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ActionEntity?

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Query("SELECT * FROM action_lists WHERE id = :id LIMIT 1")
    suspend fun getListById(id: Long): ActionListEntity?

    @Query("SELECT * FROM list_items WHERE listId = :listId ORDER BY position, id")
    suspend fun getListItems(listId: Long): List<ListItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ActionEntity): Long

    @Update
    suspend fun update(entity: ActionEntity)

    @Insert
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Insert
    suspend fun insertList(list: ActionListEntity): Long

    @Update
    suspend fun updateList(list: ActionListEntity)

    @Insert
    suspend fun insertListItem(item: ListItemEntity): Long

    @Update
    suspend fun updateListItem(item: ListItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletion(completion: ActionCompletionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Query("SELECT * FROM tags WHERE normalizedName = :normalizedName LIMIT 1")
    suspend fun findTagByNormalizedName(normalizedName: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTagRef(ref: TagRefEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertContentLink(link: ContentLinkEntity): Long

    @Insert
    suspend fun insertRoutineRule(rule: RoutineRuleEntity): Long

    @Query("UPDATE actions SET status = 'COMPLETED', completedAt = :completedAt, updatedAt = :completedAt WHERE id = :id")
    suspend fun complete(id: Long, completedAt: Long = System.currentTimeMillis())

    @Query("UPDATE actions SET status = 'PENDING', completedAt = NULL, updatedAt = :updatedAt WHERE id = :id")
    suspend fun reopen(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE actions SET status = 'ARCHIVED', updatedAt = :updatedAt WHERE id = :id")
    suspend fun archive(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE actions SET scheduledAt = :scheduledAt, status = 'PENDING', updatedAt = :updatedAt WHERE id = :id")
    suspend fun reschedule(id: Long, scheduledAt: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE list_items SET completedAt = :completedAt WHERE id = :id")
    suspend fun setListItemCompleted(id: Long, completedAt: Long?)

    @Query("UPDATE projects SET completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setProjectCompleted(id: Long, completedAt: Long?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE action_lists SET completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setListCompleted(id: Long, completedAt: Long?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE actions SET sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setActionSortOrder(id: Long, sortOrder: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE list_items SET position = :position WHERE id = :id")
    suspend fun setListItemPosition(id: Long, position: Int)

    @Query("UPDATE projects SET sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setProjectSortOrder(id: Long, sortOrder: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE action_lists SET sortOrder = :sortOrder, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setListSortOrder(id: Long, sortOrder: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE actions SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteAction(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE actions SET deletedAt = NULL, updatedAt = :updatedAt WHERE id = :id")
    suspend fun restoreAction(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE projects SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteProject(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE projects SET deletedAt = NULL, updatedAt = :updatedAt WHERE id = :id")
    suspend fun restoreProject(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE actions SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE projectId = :projectId")
    suspend fun softDeleteProjectActions(projectId: Long, deletedAt: Long)

    @Query("UPDATE actions SET deletedAt = NULL, updatedAt = :updatedAt WHERE projectId = :projectId")
    suspend fun restoreProjectActions(projectId: Long, updatedAt: Long)

    @Query("UPDATE action_lists SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteList(id: Long, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE action_lists SET deletedAt = NULL, updatedAt = :updatedAt WHERE id = :id")
    suspend fun restoreList(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE actions SET deletedAt = :deletedAt, updatedAt = :deletedAt WHERE type = 'LIST' AND metadata = :listId")
    suspend fun softDeleteListAgendaActions(listId: String, deletedAt: Long)

    @Query("UPDATE actions SET deletedAt = NULL, updatedAt = :updatedAt WHERE type = 'LIST' AND metadata = :listId")
    suspend fun restoreListAgendaActions(listId: String, updatedAt: Long)

    @Query("UPDATE routine_rules SET effectiveUntil = :effectiveUntil WHERE actionId = :actionId AND effectiveUntil IS NULL")
    suspend fun closeActiveRoutineRule(actionId: Long, effectiveUntil: Long)

    @Query("DELETE FROM action_completions WHERE actionId = :actionId AND occurrenceDate = :occurrenceDate")
    suspend fun deleteCompletion(actionId: Long, occurrenceDate: String)

    @Query("DELETE FROM list_items WHERE id = :id")
    suspend fun deleteListItem(id: Long)

    @Query("DELETE FROM tag_refs WHERE tagId = :tagId AND ownerType = :ownerType AND ownerId = :ownerId")
    suspend fun deleteTagRef(tagId: Long, ownerType: String, ownerId: Long)

    @Query("DELETE FROM tag_refs WHERE ownerType = :ownerType AND ownerId = :ownerId")
    suspend fun deleteTagRefsForOwner(ownerType: String, ownerId: Long)

    @Query("DELETE FROM tag_refs WHERE tagId = :tagId")
    suspend fun deleteTagRefsForTag(tagId: Long)

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTag(id: Long)

    @Query("DELETE FROM content_links WHERE id = :id")
    suspend fun deleteContentLink(id: Long)

    @Query("DELETE FROM content_links WHERE (sourceType = :ownerType AND sourceId = :ownerId) OR (targetType = :ownerType AND targetId = :ownerId)")
    suspend fun deleteContentLinksForOwner(ownerType: String, ownerId: Long)

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

    @Query("DELETE FROM action_completions WHERE actionId = :actionId")
    suspend fun deleteCompletionsForAction(actionId: Long)

    @Query("DELETE FROM routine_rules WHERE actionId = :actionId")
    suspend fun deleteRoutineRulesForAction(actionId: Long)

    @Query("DELETE FROM actions WHERE deletedAt IS NOT NULL AND deletedAt < :before")
    suspend fun purgeDeletedActions(before: Long)

    @Query("DELETE FROM actions WHERE projectId IN (SELECT id FROM projects WHERE deletedAt IS NOT NULL AND deletedAt < :before)")
    suspend fun purgeActionsOfDeletedProjects(before: Long)

    @Query("DELETE FROM list_items WHERE listId IN (SELECT id FROM action_lists WHERE deletedAt IS NOT NULL AND deletedAt < :before)")
    suspend fun purgeItemsOfDeletedLists(before: Long)

    @Query("DELETE FROM projects WHERE deletedAt IS NOT NULL AND deletedAt < :before")
    suspend fun purgeDeletedProjects(before: Long)

    @Query("DELETE FROM action_lists WHERE deletedAt IS NOT NULL AND deletedAt < :before")
    suspend fun purgeDeletedLists(before: Long)

    @Query("DELETE FROM tag_refs WHERE ownerType = 'ACTION' AND ownerId NOT IN (SELECT id FROM actions)")
    suspend fun purgeOrphanActionTagRefs()

    @Query("DELETE FROM tag_refs WHERE ownerType = 'PROJECT' AND ownerId NOT IN (SELECT id FROM projects)")
    suspend fun purgeOrphanProjectTagRefs()

    @Query("DELETE FROM tag_refs WHERE ownerType = 'LIST' AND ownerId NOT IN (SELECT id FROM action_lists)")
    suspend fun purgeOrphanListTagRefs()

    @Query("DELETE FROM content_links WHERE (sourceType = 'ACTION' AND sourceId NOT IN (SELECT id FROM actions)) OR (targetType = 'ACTION' AND targetId NOT IN (SELECT id FROM actions)) OR (sourceType = 'NOTE' AND sourceId NOT IN (SELECT id FROM actions)) OR (targetType = 'NOTE' AND targetId NOT IN (SELECT id FROM actions)) OR (sourceType = 'PROJECT' AND sourceId NOT IN (SELECT id FROM projects)) OR (targetType = 'PROJECT' AND targetId NOT IN (SELECT id FROM projects)) OR (sourceType = 'LIST' AND sourceId NOT IN (SELECT id FROM action_lists)) OR (targetType = 'LIST' AND targetId NOT IN (SELECT id FROM action_lists))")
    suspend fun purgeOrphanContentLinks()

    @Query("DELETE FROM routine_rules WHERE actionId NOT IN (SELECT id FROM actions)")
    suspend fun purgeOrphanRoutineRules()

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

    @Query("DELETE FROM routine_rules")
    suspend fun deleteAllRoutineRules()

    @Query("DELETE FROM content_links")
    suspend fun deleteAllContentLinks()

    @Query("DELETE FROM tag_refs")
    suspend fun deleteAllTagRefs()

    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()
}
