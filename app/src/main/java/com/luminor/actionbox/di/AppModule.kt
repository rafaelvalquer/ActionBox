package com.luminor.actionbox.di

import android.content.Context
import com.luminor.actionbox.data.local.ActionBoxDatabase
import com.luminor.actionbox.data.preferences.SettingsRepository
import com.luminor.actionbox.data.repository.ActionRepository
import com.luminor.actionbox.domain.DetectActionUseCase
import com.luminor.actionbox.domain.action.*
import com.luminor.actionbox.domain.reminder.*
import com.luminor.actionbox.domain.routine.RoutineRuleFactory
import com.luminor.actionbox.notification.ReminderScheduler
import com.luminor.actionbox.ui.events.AppUiEventBus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton fun database(@ApplicationContext context: Context) = ActionBoxDatabase.getInstance(context)
    @Provides fun dao(database: ActionBoxDatabase) = database.actionDao()
    @Provides @Singleton fun repository(database: ActionBoxDatabase) = ActionRepository(database)
    @Provides @Singleton fun settings(@ApplicationContext context: Context) = SettingsRepository(context)
    @Provides @Singleton fun events() = AppUiEventBus()
    @Provides @Singleton fun scheduler(@ApplicationContext context: Context) = ReminderScheduler(context)
    @Provides fun reminders(scheduler: ReminderScheduler) = ScheduleReminderUseCase(ReminderPlanner(), scheduler)
    @Provides fun rules() = RoutineRuleFactory()
    @Provides fun detector() = DetectActionUseCase()
    @Provides fun createAction(repository: ActionRepository, reminders: ScheduleReminderUseCase) = CreateActionUseCase(repository, reminders)
    @Provides fun createList(repository: ActionRepository, reminders: ScheduleReminderUseCase) = CreateListUseCase(repository, reminders)
    @Provides fun createProject(repository: ActionRepository) = CreateProjectUseCase(repository)
    @Provides fun saveReply(repository: ActionRepository) = SaveReplyUseCase(repository)
}
