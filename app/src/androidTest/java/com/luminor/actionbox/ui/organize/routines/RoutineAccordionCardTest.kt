package com.luminor.actionbox.ui.organize.routines

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.ui.designsystem.ActionBoxTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RoutineAccordionCardTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun expandsShowsStoredEmojiAndTogglesTheSelectedDay() {
        val today = LocalDate.now()
        val routine = ActionEntity(
            id = 12,
            type = "ROUTINE",
            title = "Academia",
            content = "",
            sourceText = "",
            status = "PENDING",
            iconEmoji = "🏋️"
        )
        var toggledDay: LocalDate? = null

        composeRule.setContent {
            var expanded by remember { mutableStateOf(false) }
            ActionBoxTheme {
                RoutineAccordionCard(
                    action = routine,
                    expanded = expanded,
                    completedDays = 0,
                    streak = 0,
                    occursOn = { true },
                    completedOn = { false },
                    onToggleExpanded = { expanded = !expanded },
                    onToggleDay = { toggledDay = it },
                    onOpenRoutine = {},
                    hapticsEnabled = false
                )
            }
        }

        composeRule.onNodeWithText("Ver rotina completa").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Expandir rotina Academia").performClick()
        composeRule.onNodeWithText("🏋️").assertExists()
        composeRule.onNodeWithText("Ver rotina completa").assertExists()
        composeRule.onNodeWithText(today.dayOfMonth.toString()).performClick()

        assertEquals(today, toggledDay)
    }
}
