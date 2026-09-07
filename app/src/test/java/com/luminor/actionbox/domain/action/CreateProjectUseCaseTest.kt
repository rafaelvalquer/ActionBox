package com.luminor.actionbox.domain.action

import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.data.local.ProjectEntity
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.DetectedAction
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class CreateProjectUseCaseTest {
    private val zoneId = ZoneId.systemDefault()

    @Test
    fun `project creates one task per item with project id and order`() = runBlocking {
        val fixture = Fixture()

        val result = fixture.useCase(
            DetectedAction(
                type = ActionType.PROJECT,
                title = "Viagem",
                content = "Viagem",
                sourceText = "Viagem",
                items = listOf("Passagem", "Hotel", "Seguro")
            ),
            now = LocalDateTime.of(2026, 9, 6, 12, 0)
        )

        assertEquals(21L, result.projectId)
        assertEquals(3, result.taskCount)
        assertEquals(listOf("Passagem", "Hotel", "Seguro"), fixture.tasks.map { it.title })
        assertEquals(listOf(0, 1, 2), fixture.tasks.map { it.sortOrder })
        assertEquals(setOf(21L), fixture.tasks.map { it.projectId }.toSet())
        assertEquals(setOf(ActionType.TASK.name), fixture.tasks.map { it.type }.toSet())
    }

    @Test
    fun `project without items remains valid`() = runBlocking {
        val fixture = Fixture()

        val result = fixture.useCase(
            DetectedAction(ActionType.PROJECT, "Novo projeto", "Novo projeto", "Novo projeto"),
            now = LocalDateTime.of(2026, 9, 6, 12, 0)
        )

        assertEquals(21L, result.projectId)
        assertEquals(0, result.taskCount)
        assertEquals(1, fixture.projects.size)
        assertEquals(0, fixture.tasks.size)
    }

    private inner class Fixture {
        val projects = mutableListOf<ProjectEntity>()
        val tasks = mutableListOf<ActionEntity>()

        val useCase = CreateProjectUseCase(
            insertProject = { project -> projects += project.copy(id = 21); 21L },
            insertAction = { action -> tasks += action; tasks.size.toLong() },
            zoneId = zoneId
        )
    }
}
