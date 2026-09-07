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

class RoutineCommands @Inject constructor(
    private val textResources: com.luminor.actionbox.ui.events.TextResources,
    private val repository: ActionRepository,
    private val scheduleReminder: ScheduleReminderUseCase,
    private val routineRuleFactory: RoutineRuleFactory,
    private val uiEventBus: AppUiEventBus
) {
    suspend fun saveRoutineEdits(
        context: Context,
        original: ActionEntity,
        title: String,
        iconEmoji: String,
        recurrenceType: RecurrenceType,
        recurrenceDays: Set<Int>,
        time: LocalTime,
        reminderMinutes: Int?,
        priority: ActionPriority,
        paused: Boolean
    ) = repository.transaction {
            val normalizedTitle = title.trim()
            if (normalizedTitle.isBlank()) {
                uiEventBus.message(R.string.text_a_rotina_precisa_de_um_nome)
                return@transaction
            }
            val baseDate = original.scheduledAt?.toLocalDate() ?: LocalDate.now()
            val scheduledAt = baseDate.atTime(time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val updated = original.copy(
                title = normalizedTitle,
                iconEmoji = iconEmoji.trim().ifBlank { "🔁" },
                content = normalizedTitle,
                scheduledAt = scheduledAt,
                recurrenceType = recurrenceType.name,
                recurrenceDays = recurrenceDays.sorted().joinToString(",").ifBlank { null },
                reminderMinutes = reminderMinutes,
                priority = priority.name,
                status = if (paused) ActionStatus.CANCELLED.name else ActionStatus.PENDING.name,
                updatedAt = System.currentTimeMillis()
            )
            scheduleReminder.cancel(original.id)
            repository.update(updated)

            if (routineConfigurationChanged(original, updated)) {
                val effectiveFrom = routineRuleFactory.startOfDayMillis(LocalDate.now())
                repository.replaceRoutineRule(
                    routineRuleFactory.create(updated, effectiveFrom),
                    effectiveFrom - 1
                )
            }
            if (!paused) scheduleReminder(updated)
            uiEventBus.message(if (paused) textResources.getString(R.string.text_rotina_pausada) else textResources.getString(R.string.text_rotina_atualizada))
        }


    suspend fun setRoutinePaused(context: Context, action: ActionEntity, paused: Boolean) {
        val recurrence = runCatching {
            RecurrenceType.valueOf(action.recurrenceType ?: RecurrenceType.WEEKLY.name)
        }.getOrDefault(RecurrenceType.WEEKLY)
        val time = action.scheduledAt?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime()
        } ?: LocalTime.of(9, 0)
        saveRoutineEdits(
            context = context,
            original = action,
            title = action.title,
            iconEmoji = action.iconEmoji ?: "🔁",
            recurrenceType = recurrence,
            recurrenceDays = RecurrenceCalculator.recurrenceDays(action),
            time = time,
            reminderMinutes = action.reminderMinutes,
            priority = runCatching {
                ActionPriority.valueOf(action.priority ?: ActionPriority.NORMAL.name)
            }.getOrDefault(ActionPriority.NORMAL),
            paused = paused
        )
    }
    private fun routineConfigurationChanged(original: ActionEntity, updated: ActionEntity): Boolean {
        val originalTime = original.scheduledAt?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime()
        }
        val updatedTime = updated.scheduledAt?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime()
        }
        return original.recurrenceType != updated.recurrenceType ||
            original.recurrenceDays != updated.recurrenceDays ||
            original.reminderMinutes != updated.reminderMinutes ||
            originalTime != updatedTime
    }

    private fun Long.toLocalDate(): LocalDate =
        Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
}
