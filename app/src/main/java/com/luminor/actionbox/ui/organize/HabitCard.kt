package com.luminor.actionbox.ui.organize

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luminor.actionbox.ActionViewModel
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.domain.HabitStreakCalculator
import com.luminor.actionbox.ui.designsystem.ActionBoxColors
import com.luminor.actionbox.ui.designsystem.ActionBoxIcons
import com.luminor.actionbox.ui.designsystem.components.ActionCard
import com.luminor.actionbox.ui.motion.AnimatedCheck
import com.luminor.actionbox.ui.motion.pressScale
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HabitRichCard(
    action: ActionEntity,
    viewModel: ActionViewModel,
    onOpen: (() -> Unit)? = null
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val today = LocalDate.now()
    val month = YearMonth.from(today)
    val occurrences = (1..month.lengthOfMonth())
        .map { month.atDay(it) }
        .filter { viewModel.routineOccursOn(action, it) && !it.isAfter(today) }
    val completed = occurrences.count { viewModel.isCompletedOn(action, it) }
    val progress = if (occurrences.isEmpty()) 0f else completed.toFloat() / occurrences.size
    val streak = HabitStreakCalculator.currentStreak(
        today = today,
        occursOn = { viewModel.routineOccursOn(action, it) },
        isCompleted = { viewModel.isCompletedOn(action, it) }
    )
    val streakScale = remember { Animatable(1f) }
    var previousStreak by remember(action.id) { mutableIntStateOf(streak) }

    LaunchedEffect(streak) {
        if (streak > previousStreak) {
            streakScale.animateTo(1.12f, spring(stiffness = 700f))
            streakScale.animateTo(1f, spring(stiffness = 650f))
        }
        previousStreak = streak
    }

    ActionCard(onClick = onOpen) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("🏋️ ${action.title}", style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (action.status == "CANCELLED") "Rotina pausada" else "$completed dias este mês",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (action.status == "CANCELLED") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            if (occurrences.isNotEmpty()) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Text(
                month.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-BR")).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelLarge
            )
            HabitMonthGrid(
                action = action,
                month = month,
                today = today,
                viewModel = viewModel,
                hapticsEnabled = settings.hapticsEnabled
            )
            Row(
                modifier = Modifier.graphicsLayer { scaleX = streakScale.value; scaleY = streakScale.value },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(ActionBoxIcons.Fire, contentDescription = null, tint = ActionBoxColors.Reminder)
                Text("Sequência: $streak", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun HabitMonthGrid(
    action: ActionEntity,
    month: YearMonth,
    today: LocalDate,
    viewModel: ActionViewModel,
    hapticsEnabled: Boolean
) {
    val haptic = LocalHapticFeedback.current
    Row(Modifier.fillMaxWidth()) {
        listOf("S", "T", "Q", "Q", "S", "S", "D").forEach { label ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    val cells = MutableList<LocalDate?>(month.atDay(1).dayOfWeek.value - 1) { null }
    repeat(month.lengthOfMonth()) { cells += month.atDay(it + 1) }
    while (cells.size % 7 != 0) cells += null

    cells.chunked(7).forEach { week ->
        Row(Modifier.fillMaxWidth()) {
            week.forEach { date ->
                if (date == null) {
                    Spacer(Modifier.weight(1f).height(48.dp))
                } else {
                    val occurs = viewModel.routineOccursOn(action, date)
                    val done = occurs && viewModel.isCompletedOn(action, date)
                    val future = date.isAfter(today)
                    val enabled = occurs && !future
                    val cellModifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .then(
                            if (enabled) {
                                Modifier
                                    .pressScale(0.9f)
                                    .clickable {
                                        if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        viewModel.toggleOccurrence(action, date)
                                    }
                            } else Modifier
                        )

                    Column(
                        modifier = cellModifier,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (occurs) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        )
                        when {
                            done -> AnimatedCheck(
                                checked = true,
                                modifier = Modifier.size(19.dp),
                                checkedTint = ActionBoxColors.Completed
                            )
                            occurs && !future -> AnimatedCheck(
                                checked = false,
                                modifier = Modifier.size(19.dp),
                                uncheckedTint = MaterialTheme.colorScheme.primary
                            )
                            occurs && future -> AnimatedCheck(
                                checked = false,
                                modifier = Modifier.size(19.dp),
                                uncheckedTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            )
                            else -> Text("·", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f))
                        }
                    }
                }
            }
        }
    }
}
