package com.luminor.actionbox

import android.content.Context
import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.luminor.actionbox.data.local.*
import com.luminor.actionbox.data.repository.ActionRepository
import com.luminor.actionbox.domain.commands.*
import com.luminor.actionbox.domain.reminder.ReminderPlanner
import com.luminor.actionbox.domain.reminder.ScheduleReminderUseCase
import com.luminor.actionbox.ui.events.AppUiEventBus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArchitectureIntegrationTest {
    private lateinit var database: ActionBoxDatabase
    private lateinit var repository: ActionRepository
    private val cancelled = mutableListOf<Long>()
    private val events = AppUiEventBus()
    private val reminders = ScheduleReminderUseCase(ReminderPlanner(), { _, _, _ -> }, { cancelled += it })

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), ActionBoxDatabase::class.java).build()
        repository = ActionRepository(database)
    }

    @After fun tearDown() = database.close()

    @Test fun projectCommandsReadPersistedTasksWithoutCollectors() = runBlocking {
        val projectId = repository.insertProject(ProjectEntity(title = "Project"))
        val first = repository.insert(action("First").copy(projectId = projectId))
        val second = repository.insert(action("Second").copy(projectId = projectId))
        val commands = ProjectCommands(repository, reminders, events)

        commands.saveProjectEdits(repository.getProjectById(projectId)!!, "Edited", "Description", mapOf(second to "Renamed"), listOf("New"), setOf(first), listOf(second))

        assertEquals("Edited", repository.getProjectById(projectId)!!.title)
        assertNotNull(repository.getById(first)!!.deletedAt)
        assertEquals("Renamed", repository.getById(second)!!.title)
        assertEquals(2, repository.projectActions(projectId).size)
        assertTrue(first in cancelled)
        commands.deleteProject(projectId)
        assertTrue(second in cancelled)
        assertTrue(repository.projectActions(projectId).isEmpty())
        commands.reopenProject(projectId)
    }

    @Test fun listCompletionUpdatesAgendaWithoutCollectors() = runBlocking {
        val listId = repository.insertList(ActionListEntity(title = "List"))
        val agendaId = repository.insert(action("List agenda").copy(type = "LIST", metadata = listId.toString()))
        val commands = ListCommands(repository, reminders, events)

        commands.finishList(listId)
        assertEquals("COMPLETED", repository.getById(agendaId)!!.status)
        commands.reopenList(listId)
        assertEquals("PENDING", repository.getById(agendaId)!!.status)
        assertNull(repository.getListById(listId)!!.completedAt)
        commands.deleteList(listId)
        assertTrue(agendaId in cancelled)
        assertNotNull(repository.getById(agendaId)!!.deletedAt)
    }

    @Test fun ownerQueriesDoNotLeakOtherOwners() = runBlocking {
        val first = repository.insertProject(ProjectEntity(title = "First"))
        val second = repository.insertProject(ProjectEntity(title = "Second"))
        val task = repository.insert(action("Owned").copy(projectId = first))
        repository.insert(action("Other").copy(projectId = second))
        assertEquals(listOf(task), repository.observeProjectActions(first).first().map { it.id })
        assertNull(repository.observeAction(-1).first())
        repository.softDeleteAction(task)
        assertNull(repository.observeAction(task).first())
    }

    @Test fun historyPagesBeyondOneHundredWithStableTiesAndInvalidates() = runBlocking {
        val ids = (1..135).map { repository.insert(action("Completed $it").copy(status = "COMPLETED", completedAt = 1000L)) }
        repository.insert(action("Pending"))
        val source = repository.historyPagingSource()
        val loaded = mutableListOf<Long>()
        var page = source.load(PagingSource.LoadParams.Refresh(null, 60, false)) as PagingSource.LoadResult.Page
        loaded += page.data.map { it.id }
        while (page.nextKey != null) {
            page = source.load(PagingSource.LoadParams.Append(page.nextKey!!, 30, false)) as PagingSource.LoadResult.Page
            loaded += page.data.map { it.id }
        }
        assertEquals(ids.reversed(), loaded)
        assertEquals(135, loaded.distinct().size)
        repository.softDeleteAction(ids.last())
        database.invalidationTracker.refreshAsync()
        // A new source reflects both deletion and restoration, without truncation.
        val deleted = repository.historyPagingSource().load(PagingSource.LoadParams.Refresh(null, 150, false)) as PagingSource.LoadResult.Page
        assertEquals(134, deleted.data.size)
        repository.restoreAction(ids.last())
        val restored = repository.historyPagingSource().load(PagingSource.LoadParams.Refresh(null, 150, false)) as PagingSource.LoadResult.Page
        assertEquals(ids.reversed(), restored.data.map { it.id })
    }

    private fun action(title: String) = ActionEntity(type = "TASK", title = title, content = title, sourceText = title, status = "PENDING", recurrenceType = "NONE")
}
