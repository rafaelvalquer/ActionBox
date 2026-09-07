package com.luminor.actionbox.ui.organize

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.data.local.ProjectEntity
import com.luminor.actionbox.ui.designsystem.ActionBoxTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProjectRichCardTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun expandsAndTogglesTheExactTask() {
        val task = ActionEntity(
            id = 42,
            type = "TASK",
            title = "Preparar apresentação",
            content = "",
            sourceText = "",
            status = "PENDING"
        )
        var toggled: ActionEntity? = null

        composeRule.setContent {
            var expanded by remember { mutableStateOf(false) }
            ActionBoxTheme {
                ProjectRichCard(
                    project = ProjectEntity(id = 7, title = "Trabalho"),
                    tasks = listOf(task),
                    expanded = expanded,
                    onToggleExpanded = { expanded = !expanded },
                    onToggleTask = { toggled = it },
                    onOpenProject = {}
                )
            }
        }

        composeRule.onNodeWithText(task.title).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Expandir projeto Trabalho").performClick()
        composeRule.onNodeWithText(task.title).assertExists().performClick()

        assertEquals(task, toggled)
    }
}
