package com.luminor.actionbox.domain.commands

import com.luminor.actionbox.R
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luminor.actionbox.data.local.ActionCompletionEntity
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.data.local.ActionListEntity
import com.luminor.actionbox.data.local.ListItemEntity
import com.luminor.actionbox.data.local.ProjectEntity
import com.luminor.actionbox.data.local.TagEntity
import com.luminor.actionbox.domain.ActionPriority
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.DetectedAction
import com.luminor.actionbox.domain.ExternalActions
import com.luminor.actionbox.domain.OrganizationOwnerType
import com.luminor.actionbox.domain.RecurrenceCalculator
import com.luminor.actionbox.domain.RecurrenceType
import com.luminor.actionbox.domain.UiSettings
import com.luminor.actionbox.domain.reminder.ReminderPlanner
import com.luminor.actionbox.domain.reminder.ScheduleReminderUseCase
import com.luminor.actionbox.domain.routine.RoutineRuleFactory
import com.luminor.actionbox.domain.search.SearchNormalizer
import com.luminor.actionbox.notification.ReminderScheduler
import com.luminor.actionbox.ui.events.AppUiEvent
import com.luminor.actionbox.ui.events.UndoKind
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

import javax.inject.Inject
import com.luminor.actionbox.data.repository.ActionRepository
import com.luminor.actionbox.data.preferences.SettingsRepository
import com.luminor.actionbox.ui.events.AppUiEventBus
import kotlinx.coroutines.flow.first

class OrganizationCommands @Inject constructor(
    private val repository: ActionRepository,
    private val uiEventBus: AppUiEventBus
) {
    suspend fun setTagsForOwner(ownerType: String, ownerId: Long, tagIds: Set<Long>) {
        repository.setTagsForOwner(ownerType, ownerId, tagIds)
    }


    suspend fun createAndAttachTag(ownerType: String, ownerId: Long, name: String) {
            val normalized = SearchNormalizer.normalize(name)
            if (normalized.isBlank()) return
            val id = repository.createOrGetTag(name.trim(), normalized)
            if (id > 0) repository.addTagToOwner(ownerType, ownerId, id)
        }


    suspend fun removeTag(ownerType: String, ownerId: Long, tagId: Long) {
        repository.removeTagFromOwner(ownerType, ownerId, tagId)
    }


    suspend fun linkNote(noteId: Long, targetType: String, targetId: Long) {
            repository.addContentLink(OrganizationOwnerType.NOTE, noteId, targetType, targetId)
            uiEventBus.message(R.string.text_nota_vinculada)
        }


    suspend fun unlinkContentLink(linkId: Long) {
        repository.deleteContentLink(linkId)
    }

}
