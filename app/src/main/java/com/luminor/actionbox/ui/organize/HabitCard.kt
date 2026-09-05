package com.luminor.actionbox.ui.organize

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luminor.actionbox.ActionViewModel
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.domain.RecurrenceCalculator
import com.luminor.actionbox.ui.designsystem.ActionBoxColors
import com.luminor.actionbox.ui.designsystem.ActionBoxIcons
import com.luminor.actionbox.ui.designsystem.components.ActionCard
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HabitRichCard(action: ActionEntity, viewModel: ActionViewModel) {
    val today = LocalDate.now()
    val month = YearMonth.from(today)
    val occurrences = (1..month.lengthOfMonth()).map { month.atDay(it) }.filter { RecurrenceCalculator.occursOn(action, it) && !it.isAfter(today) }
    val completed = occurrences.count { viewModel.isCompletedOn(action, it) }
    val progress = if (occurrences.isEmpty()) 0f else completed.toFloat() / occurrences.size
    val streak = currentStreak(action, viewModel, today)

    ActionCard {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("🏋️ ${action.title}", style = MaterialTheme.typography.titleLarge)
                    Text("$completed dias este mês", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            if (occurrences.isNotEmpty()) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            Text(month.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-BR")).replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelLarge)
            HabitMonthGrid(action, month, today, viewModel)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(ActionBoxIcons.Fire, contentDescription = null, tint = ActionBoxColors.Reminder)
                Text("Sequência: $streak", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun HabitMonthGrid(action: ActionEntity, month: YearMonth, today: LocalDate, viewModel: ActionViewModel) {
    Row(Modifier.fillMaxWidth()) {
        listOf("S", "T", "Q", "Q", "S", "S", "D").forEach { label ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
    val cells = MutableList<LocalDate?>(month.atDay(1).dayOfWeek.value - 1) { null }
    repeat(month.lengthOfMonth()) { cells += month.atDay(it + 1) }
    while (cells.size % 7 != 0) cells += null
    cells.chunked(7).forEach { week ->
        Row(Modifier.fillMaxWidth()) {
            week.forEach { date ->
                if (date == null) Spacer(Modifier.weight(1f).height(38.dp))
                else {
                    val occurs = RecurrenceCalculator.occursOn(action, date)
                    val done = occurs && viewModel.isCompletedOn(action, date)
                    val future = date.isAfter(today)
                    val clickable = occurs && !future
                    Column(
                        modifier = Modifier.weight(1f).height(38.dp).then(if (clickable) Modifier.clickable { viewModel.toggleOccurrence(action, date) } else Modifier),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box(
                            Modifier.height(8.dp).fillMaxWidth(0.35f).background(
                                when {
                                    done -> ActionBoxColors.Completed
                                    occurs && !future -> MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }, CircleShape
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun currentStreak(action: ActionEntity, viewModel: ActionViewModel, today: LocalDate): Int {
    var date = today
    var streak = 0
    var checked = 0
    while (checked < 365) {
        if (RecurrenceCalculator.occursOn(action, date)) {
            if (viewModel.isCompletedOn(action, date)) streak++
            else if (date != today) break
        }
        date = date.minusDays(1)
        checked++
    }
    return streak
}
