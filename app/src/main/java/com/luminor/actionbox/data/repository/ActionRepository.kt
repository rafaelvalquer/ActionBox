package com.luminor.actionbox.data.repository

import com.luminor.actionbox.data.local.ActionCompletionEntity
import com.luminor.actionbox.data.local.ActionDao
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.data.local.ActionListEntity
import com.luminor.actionbox.data.local.ListItemEntity
import com.luminor.actionbox.data.local.ProjectEntity

class ActionRepository(private val dao: ActionDao) {
    val all = dao.observeAll()
    val pending = dao.observePending()
    val saved = dao.observeSaved()
    val notes = dao.observeNotes()
    val history = dao.observeHistory()
    val projects = dao.observeProjects()
    val lists = dao.observeLists()
    val listItems = dao.observeListItems()
    val completions = dao.observeCompletions()

    suspend fun insert(entity: ActionEntity): Long = dao.insert(entity)
    suspend fun insertProject(project: ProjectEntity): Long = dao.insertProject(project)
    suspend fun insertList(list: ActionListEntity): Long = dao.insertList(list)
    suspend fun insertListItem(item: ListItemEntity): Long = dao.insertListItem(item)
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

    suspend fun deleteProject(id: Long) {
        dao.deleteProjectActions(id)
        dao.deleteProject(id)
    }

    suspend fun deleteList(id: Long) {
        dao.deleteListItems(id)
        dao.deleteList(id)
    }

    suspend fun deleteAll() {
        dao.deleteAllCompletions()
        dao.deleteAllListItems()
        dao.deleteAllLists()
        dao.deleteAllProjects()
        dao.deleteAllActions()
    }
}
