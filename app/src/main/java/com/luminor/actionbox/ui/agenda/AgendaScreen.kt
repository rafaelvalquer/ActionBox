package com.luminor.actionbox.ui.agenda

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luminor.actionbox.ActionViewModel
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.RecurrenceCalculator
import com.luminor.actionbox.ui.designsystem.ActionBoxColors
import com.luminor.actionbox.ui.designsystem.ActionBoxIcons
import com.luminor.actionbox.ui.designsystem.actionTypeColor
import com.luminor.actionbox.ui.designsystem.components.ActionSegmentedControl
import com.luminor.actionbox.ui.motion.AnimatedCheck
import com.luminor.actionbox.ui.motion.MotionDuration
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun AgendaScreen(viewModel: ActionViewModel, onActionOpen: (Long) -> Unit) {
    val all by viewModel.all.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val completions by viewModel.completions.collectAsStateWithLifecycle()
    var mode by rememberSaveable { mutableStateOf(0) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selected by remember { mutableStateOf(LocalDate.now()) }
    var dragTotal by remember { mutableFloatStateOf(0f) }

    fun changeMonth(next: YearMonth) {
        month = next
        selected = next.atDay(selected.dayOfMonth.coerceAtMost(next.lengthOfMonth()))
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().widthIn(max = 980.dp).statusBarsPadding().padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Agenda", style = MaterialTheme.typography.headlineLarge)
                    Text("Tudo que tem dia, horário ou recorrência.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ActionSegmentedControl(listOf("Mês", "Lista"), mode, onSelected = { mode = it })
                }
            }

            if (mode == 0) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            IconButton(onClick = { changeMonth(month.minusMonths(1)) }) { Icon(ActionBoxIcons.Back, contentDescription = "Mês anterior") }
                            Text(
                                "${month.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-BR")).replaceFirstChar { it.uppercase() }} ${month.year}",
                                style = MaterialTheme.typography.titleLarge
                            )
                            IconButton(onClick = { changeMonth(month.plusMonths(1)) }) { Icon(ActionBoxIcons.Next, contentDescription = "Próximo mês") }
                        }

                        AnimatedContent(
                            targetState = month,
                            transitionSpec = {
                                val forward = targetState > initialState
                                (slideInHorizontally(tween(MotionDuration.Standard)) { if (forward) it else -it } + fadeIn()) togetherWith
                                    (slideOutHorizontally(tween(MotionDuration.Standard)) { if (forward) -it else it } + fadeOut())
                            },
                            label = "month-transition"
                        ) { targetMonth ->
                            MonthCalendar(
                                month = targetMonth,
                                selected = selected,
                                all = all,
                                viewModel = viewModel,
                                modifier = Modifier.pointerInput(targetMonth) {
                                    detectHorizontalDragGestures(
                                        onDragStart = { dragTotal = 0f },
                                        onHorizontalDrag = { _, amount -> dragTotal += amount },
                                        onDragEnd = {
                                            when {
                                                dragTotal < -80f -> changeMonth(targetMonth.plusMonths(1))
                                                dragTotal > 80f -> changeMonth(targetMonth.minusMonths(1))
                                            }
                                            dragTotal = 0f
                                        }
                                    )
                                },
                                onSelect = { selected = it }
                            )
                        }

                        DayTimeline(
                            date = selected,
                            entries = entriesFor(selected, all),
                            viewModel = viewModel,
                            hapticsEnabled = settings.hapticsEnabled,
                            onActionOpen = onActionOpen
                        )
                    }
                }
            } else {
                val today = LocalDate.now()
                val days = (0..30).map { today.plusDays(it.toLong()) }.filter { entriesFor(it, all).isNotEmpty() }
                if (days.isEmpty()) item { Text("Sua agenda está livre nos próximos 30 dias.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                days.forEach { date ->
                    item(key = "timeline-$date-${completions.size}") {
                        DayTimeline(date, entriesFor(date, all), viewModel, settings.hapticsEnabled, onActionOpen)
                    }
                }
                val undated = all.filter { it.type == ActionType.TASK.name && it.scheduledAt == null && it.status == ActionStatus.PENDING.name }
                if (undated.isNotEmpty()) {
                    item { Text("Sem data", style = MaterialTheme.typography.titleLarge) }
                    undated.forEach { action ->
                        item(key = "undated-${action.id}") { TimelineAction(action, today, viewModel, settings.hapticsEnabled, onActionOpen) }
                    }
                }
            }
            item { Spacer(Modifier.height(28.dp)) }
        }
    }
}

@Composable
private fun MonthCalendar(
    month: YearMonth,
    selected: LocalDate,
    all: List<ActionEntity>,
    viewModel: ActionViewModel,
    modifier: Modifier = Modifier,
    onSelect: (LocalDate) -> Unit
) {
    Surface(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth()) {
                listOf("S", "T", "Q", "Q", "S", "S", "D").forEach { day ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(day, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            val first = month.atDay(1)
            val cells = MutableList<LocalDate?>(first.dayOfWeek.value - 1) { null }
            repeat(month.lengthOfMonth()) { cells += month.atDay(it + 1) }
            while (cells.size % 7 != 0) cells += null
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        if (date == null) Spacer(Modifier.weight(1f).aspectRatio(1f))
                        else DayCell(date, selected, entriesFor(date, all), viewModel, Modifier.weight(1f)) { onSelect(date) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, selected: LocalDate, entries: List<ActionEntity>, viewModel: ActionViewModel, modifier: Modifier, onClick: () -> Unit) {
    val isToday = date == LocalDate.now()
    val done = entries.isNotEmpty() && entries.all { viewModel.isCompletedOn(it, date) }
    Column(
        modifier = modifier.aspectRatio(1f).clickable(onClick = onClick).padding(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = when {
                date == selected -> MaterialTheme.colorScheme.primary
                isToday -> MaterialTheme.colorScheme.primaryContainer
                else -> androidx.compose.ui.graphics.Color.Transparent
            }
        ) {
            Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                Text(
                    date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (date == selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
        if (entries.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                if (done) {
                    Box(Modifier.size(5.dp).background(ActionBoxColors.Completed, CircleShape))
                } else {
                    entries.take(3).forEach { entry -> Box(Modifier.size(4.dp).background(actionTypeColor(entry.type), CircleShape)) }
                }
            }
        }
    }
}

@Composable
private fun DayTimeline(date: LocalDate, entries: List<ActionEntity>, viewModel: ActionViewModel, hapticsEnabled: Boolean, onActionOpen: (Long) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            date.format(DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("pt-BR"))).replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleLarge
        )
        if (entries.isEmpty()) Text("Nada planejado para este dia.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        entries.forEach { TimelineAction(it, date, viewModel, hapticsEnabled, onActionOpen) }
    }
}

@Composable
private fun TimelineAction(action: ActionEntity, date: LocalDate, viewModel: ActionViewModel, hapticsEnabled: Boolean, onActionOpen: (Long) -> Unit) {
    val completed = viewModel.isCompletedOn(action, date)
    val color = if (completed) ActionBoxColors.Completed else actionTypeColor(action.type)
    val time = action.scheduledAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) } ?: "—"
    val haptic = LocalHapticFeedback.current

    Row(Modifier.fillMaxWidth().clickable { onActionOpen(action.id) }.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(time, modifier = Modifier.width(56.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = CircleShape, color = color.copy(alpha = 0.13f)) {
                Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                    Icon(ActionBoxIcons.forType(action.type), contentDescription = null, tint = color, modifier = Modifier.size(19.dp))
                }
            }
            Box(Modifier.width(2.dp).height(18.dp).background(MaterialTheme.colorScheme.surfaceVariant))
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(action.title, style = MaterialTheme.typography.bodyLarge, textDecoration = if (completed) TextDecoration.LineThrough else null)
            if (RecurrenceCalculator.recurrenceType(action).name != "NONE") Text("Recorrente", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = {
            if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.toggleOccurrence(action, date)
        }) { AnimatedCheck(completed) }
    }
}

private fun entriesFor(date: LocalDate, all: List<ActionEntity>): List<ActionEntity> = all
    .filter { it.type in setOf(ActionType.TASK.name, ActionType.REMINDER.name, ActionType.EVENT.name, ActionType.LIST.name) }
    .filter { RecurrenceCalculator.occursOn(it, date) }
    .sortedBy { it.scheduledAt ?: Long.MAX_VALUE }
