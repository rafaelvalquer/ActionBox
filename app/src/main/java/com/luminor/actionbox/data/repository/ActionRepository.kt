package com.luminor.actionbox.data.repository

import androidx.room.withTransaction
import com.luminor.actionbox.data.local.ActionBoxDatabase
import com.luminor.actionbox.data.local.ActionCompletionEntity
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.data.local.ActionListEntity
import com.luminor.actionbox.data.local.ContentLinkEntity
import com.luminor.actionbox.data.local.ListItemEntity
import com.luminor.actionbox.data.local.ProjectEntity
import com.luminor.actionbox.data.local.RoutineRuleEntity
import com.luminor.actionbox.data.local.TagEntity
import com.luminor.actionbox.data.local.TagRefEntity

class ActionRepository(private val database: ActionBoxDatabase) {
    private val dao = database.actionDao()

    val all = dao.observeAll()
    val pending = dao.observePending()
    val saved = dao.observeSaved()
    val notes = dao.observeNotes()
    val history = dao.observeHistory()
    val projects = dao.observeProjects()
    val lists = dao.observeLists()
    val listItems = dao.observeListItems()
    val completions = dao.observeCompletions()
    val deletedActions = dao.observeDeletedActions()
    val deletedProjects = dao.observeDeletedProjects()
    val deletedLists = dao.observeDeletedLists()
    val tags = dao.observeTags()
    val tagRefs = dao.observeTagRefs()
    val contentLinks = dao.observeContentLinks()
    val routineRules = dao.observeRoutineRules()

    suspend fun insert(entity: ActionEntity): Long = dao.insert(entity)
    suspend fun update(entity: ActionEntity) = dao.update(entity)
    suspend fun insertProject(project: ProjectEntity): Long = dao.insertProject(project)
    suspend fun updateProject(project: ProjectEntity) = dao.updateProject(project)
    suspend fun insertList(list: ActionListEntity): Long = dao.insertList(list)
    suspend fun updateList(list: ActionListEntity) = dao.updateList(list)
    suspend fun insertListItem(item: ListItemEntity): Long = dao.insertListItem(item)
    suspend fun updateListItem(item: ListItemEntity) = dao.updateListItem(item)
    suspend fun deleteListItem(id: Long) = dao.deleteListItem(id)
    suspend fun insertCompletion(completion: ActionCompletionEntity): Long = dao.insertCompletion(completion)
    suspend fun complete(id: Long) = dao.complete(id)
    suspend fun reopen(id: Long) = dao.reopen(id)
    suspend fun archive(id: Long) = dao.archive(id)
    suspend fun reschedule(id: Long, scheduledAt: Long) = dao.reschedule(id, scheduledAt)
    suspend fun setListItemCompleted(id: Long, completedAt: Long?) = dao.setListItemCompleted(id, completedAt)
    suspend fun setProjectCompleted(id: Long, completedAt: Long?) = dao.setProjectCompleted(id, completedAt)
    suspend fun setListCompleted(id: Long, completedAt: Long?) = dao.setListCompleted(id, completedAt)
    suspend fun deleteCompletion(actionId: Long, occurrenceDate: String) = dao.deleteCompletion(actionId, occurrenceDate)
    suspend fun delete(id: Long) = dao.delete(id)
    suspend fun pendingReminders(): List<ActionEntity> = dao.getPendingReminders()
    suspend fun getById(id: Long): ActionEntity? = dao.getById(id)
    suspend fun getProjectById(id: Long): ProjectEntity? = dao.getProjectById(id)
    suspend fun getListById(id: Long): ActionListEntity? = dao.getListById(id)
    suspend fun getListItems(id: Long): List<ListItemEntity> = dao.getListItems(id)

    suspend fun saveListSnapshot(
        list: ActionListEntity,
        items: List<ListItemEntity>,
        deletedItemIds: Set<Long>
    ) = database.withTransaction {
        dao.updateList(list.copy(updatedAt = System.currentTimeMillis()))
        deletedItemIds.forEach { dao.deleteListItem(it) }
        items.sortedBy { it.position }.forEach { item ->
            if (item.id == 0L) dao.insertListItem(item) else dao.updateListItem(item)
        }
    }

    suspend fun saveProjectSnapshot(
        project: ProjectEntity,
        tasks: List<ActionEntity>,
        deletedTaskIds: Set<Long>
    ) = database.withTransaction {
        dao.updateProject(project.copy(updatedAt = System.currentTimeMillis()))
        deletedTaskIds.forEach { dao.delete(it) }
        tasks.sortedBy { it.sortOrder }.forEach { task ->
            if (task.id == 0L) dao.insert(task) else dao.update(task)
        }
    }

    suspend fun setActionOrder(orderedIds: List<Long>) = database.withTransaction {
        orderedIds.forEachIndexed { index, id -> dao.setActionSortOrder(id, index) }
    }

    suspend fun setListItemOrder(orderedIds: List<Long>) = database.withTransaction {
        orderedIds.forEachIndexed { index, id -> dao.setListItemPosition(id, index) }
    }

    suspend fun softDeleteAction(id: Long) = dao.softDeleteAction(id)
    suspend fun restoreAction(id: Long) = dao.restoreAction(id)
    suspend fun softDeleteProject(id: Long) = dao.softDeleteProject(id)
    suspend fun restoreProject(id: Long) = dao.restoreProject(id)
    suspend fun softDeleteList(id: Long) = dao.softDeleteList(id)
    suspend fun restoreList(id: Long) = dao.restoreList(id)

    suspend fun permanentlyDeleteAction(id: Long) = database.withTransaction {
        dao.deleteCompletionsForAction(id)
        dao.deleteRoutineRulesForAction(id)
        dao.deleteTagRefsForOwner("ACTION", id)
        dao.deleteContentLinksForOwner("ACTION", id)
        dao.delete(id)
    }

    suspend fun permanentlyDeleteProject(id: Long) = database.withTransaction {
        dao.deleteTagRefsForOwner("PROJECT", id)
        dao.deleteContentLinksForOwner("PROJECT", id)
        dao.deleteProjectActions(id)
        dao.deleteProject(id)
    }

    suspend fun permanentlyDeleteList(id: Long) = database.withTransaction {
        dao.deleteTagRefsForOwner("LIST", id)
        dao.deleteContentLinksForOwner("LIST", id)
        dao.deleteListItems(id)
        dao.deleteList(id)
    }

    suspend fun deleteProject(id: Long) = permanentlyDeleteProject(id)
    suspend fun deleteList(id: Long) = permanentlyDeleteList(id)

    suspend fun createOrGetTag(name: String, normalizedName: String, colorKey: String? = null): Long = database.withTransaction {
        val existing = dao.findTagByNormalizedName(normalizedName)
        if (existing != null) return@withTransaction existing.id
        val inserted = dao.insertTag(TagEntity(name = name.trim(), normalizedName = normalizedName, colorKey = colorKey))
        if (inserted > 0) inserted else dao.findTagByNormalizedName(normalizedName)?.id ?: 0L
    }

    suspend fun setTagsForOwner(ownerType: String, ownerId: Long, tagIds: Set<Long>) = database.withTransaction {
        dao.deleteTagRefsForOwner(ownerType, ownerId)
        tagIds.forEach { tagId -> dao.insertTagRef(TagRefEntity(tagId = tagId, ownerType = ownerType, ownerId = ownerId)) }
    }

    suspend fun addTagToOwner(ownerType: String, ownerId: Long, tagId: Long) =
        dao.insertTagRef(TagRefEntity(tagId = tagId, ownerType = ownerType, ownerId = ownerId))

    suspend fun removeTagFromOwner(ownerType: String, ownerId: Long, tagId: Long) =
        dao.deleteTagRef(tagId, ownerType, ownerId)

    suspend fun deleteTag(id: Long) = database.withTransaction {
        dao.deleteTag(id)
    }

    suspend fun addContentLink(
        sourceType: String,
        sourceId: Long,
        targetType: String,
        targetId: Long,
        relationType: String = "RELATED"
    ): Long = dao.insertContentLink(
        ContentLinkEntity(
            sourceType = sourceType,
            sourceId = sourceId,
            targetType = targetType,
            targetId = targetId,
            relationType = relationType
        )
    )

    suspend fun deleteContentLink(id: Long) = dao.deleteContentLink(id)

    suspend fun replaceRoutineRule(rule: RoutineRuleEntity, closeAt: Long) = database.withTransaction {
        dao.closeActiveRoutineRule(rule.actionId, closeAt)
        dao.insertRoutineRule(rule)
    }

    suspend fun purgeTrash(before: Long) = database.withTransaction {
        dao.purgeActionsOfDeletedProjects(before)
        dao.purgeItemsOfDeletedLists(before)
        dao.purgeDeletedActions(before)
        dao.purgeDeletedProjects(before)
        dao.purgeDeletedLists(before)
        dao.purgeOrphanActionTagRefs()
        dao.purgeOrphanProjectTagRefs()
        dao.purgeOrphanListTagRefs()
        dao.purgeOrphanContentLinks()
        dao.purgeOrphanRoutineRules()
    }

    suspend fun deleteAll() = database.withTransaction {
        dao.deleteAllRoutineRules()
        dao.deleteAllContentLinks()
        dao.deleteAllTagRefs()
        dao.deleteAllTags()
        dao.deleteAllCompletions()
        dao.deleteAllListItems()
        dao.deleteAllLists()
        dao.deleteAllProjects()
        dao.deleteAllActions()
    }
}
