package com.luminor.actionbox.data.repository

import com.luminor.actionbox.data.local.ActionDao
import com.luminor.actionbox.data.local.ActionEntity

class ActionRepository(private val dao: ActionDao) {
    val all = dao.observeAll()
    val pending = dao.observePending()
    val saved = dao.observeSaved()
    val notes = dao.observeNotes()
    val history = dao.observeHistory()

    suspend fun insert(entity: ActionEntity): Long = dao.insert(entity)
    suspend fun complete(id: Long) = dao.complete(id)
    suspend fun archive(id: Long) = dao.archive(id)
    suspend fun reschedule(id: Long, scheduledAt: Long) = dao.reschedule(id, scheduledAt)
    suspend fun delete(id: Long) = dao.delete(id)
    suspend fun deleteAll() = dao.deleteAll()
    suspend fun pendingReminders(): List<ActionEntity> = dao.getPendingReminders()
    suspend fun getById(id: Long): ActionEntity? = dao.getById(id)
}
