package com.luminor.actionbox.ui.organize.routines

import com.luminor.actionbox.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.domain.HabitStreakCalculator
import com.luminor.actionbox.ui.designsystem.ActionBoxColors
import com.luminor.actionbox.ui.designsystem.ActionBoxIcons
import com.luminor.actionbox.ui.designsystem.components.ActionCard
import com.luminor.actionbox.ui.organize.RoutineMonthCalendar
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun RoutineAccordionCard(
    action: ActionEntity,
    expanded: Boolean,
    completedDays: Int,
    streak: Int,
    occursOn: (LocalDate) -> Boolean,
    completedOn: (LocalDate) -> Boolean,
    onToggleExpanded: () -> Unit,
    onToggleDay: (LocalDate) -> Unit,
    onOpenRoutine: () -> Unit,
    hapticsEnabled: Boolean
) {
    val resources = androidx.compose.ui.platform.LocalContext.current.resources
    val today = LocalDate.now()
    val month = YearMonth.from(today)
    val rotation by animateFloatAsState(if (expanded) 90f else 0f, label = "routine-chevron")
    val paused = action.status == "CANCELLED"
    ActionCard {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggleExpanded).semantics {
                    contentDescription = if (expanded) resources.getString(R.string.text_recolher_rotina, action.title)
                    else resources.getString(R.string.text_expandir_rotina, action.title)
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(action.routineEmoji(), style = MaterialTheme.typography.headlineSmall)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(action.title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        if (paused) resources.getString(R.string.text_rotina_pausada)
                        else resources.getString(R.string.text_dias_este_mes, completedDays),
                        color = if (paused) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(ActionBoxIcons.Fire, contentDescription = null, tint = ActionBoxColors.Reminder)
                        Text(resources.getString(R.string.text_sequencia, streak), style = MaterialTheme.typography.labelMedium)
                    }
                }
                Icon(ActionBoxIcons.Chevron, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.graphicsLayer { rotationZ = rotation })
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        month.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-BR")).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium
                    )
                    RoutineMonthCalendar(
                        month = month,
                        today = today,
                        occursOn = occursOn,
                        completedOn = completedOn,
                        onToggle = onToggleDay,
                        hapticsEnabled = hapticsEnabled
                    )
                    TextButton(onClick = onOpenRoutine, modifier = Modifier.align(Alignment.End)) {
                        Text(resources.getString(R.string.text_ver_rotina_completa))
                        Icon(ActionBoxIcons.Arrow, contentDescription = null, modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
        }
    }
}

private fun ActionEntity.routineEmoji(): String = iconEmoji?.takeIf { it.isNotBlank() } ?: "🔁"
