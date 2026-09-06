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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luminor.actionbox.ActionViewModel
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.RecurrenceCalculator
import com.luminor.actionbox.domain.RecurrenceType
import com.luminor.actionbox.domain.agenda.AgendaUseCase
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

private enum class AgendaMode { DAY, WEEK, MONTH, LIST }

@Composable
fun AgendaScreen(viewModel: ActionViewModel, onActionOpen: (Long) -> Unit) {
    val all by viewModel.all.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val completions by viewModel.completions.collectAsStateWithLifecycle()
    var modeName by rememberSaveable { mutableStateOf(AgendaMode.MONTH.name) }
    val mode = runCatching { AgendaMode.valueOf(modeName) }.getOrDefault(AgendaMode.MONTH)
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selected by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val selectedDate = runCatching { LocalDate.parse(selected) }.getOrDefault(LocalDate.now())
    var dragTotal by remember { mutableFloatStateOf(0f) }

    fun setSelected(date: LocalDate) {
        selected = date.toString()
        month = YearMonth.from(date)
    }

    fun changeMonth(next: YearMonth) {
        month = next
        val day = selectedDate.dayOfMonth.coerceAtMost(next.lengthOfMonth())
        selected = next.atDay(day).toString()
    }

    fun entries(date: LocalDate): List<ActionEntity> = AgendaUseCase.entriesForDay(date, all) { action, day ->
        if (RecurrenceCalculator.recurrenceType(action) == RecurrenceType.NONE) RecurrenceCalculator.occursOn(action, day)
        else viewModel.routineOccursOn(action, day)
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = 980.dp)
                .statusBarsPadding()
                .padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Agenda", style = MaterialTheme.typography.headlineLarge)
                    Text("Tudo que tem dia, horário ou recorrência.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ActionSegmentedControl(
                        listOf("Dia", "Semana", "Mês", "Lista"),
                        AgendaMode.entries.indexOf(mode),
                        onSelected = { modeName = AgendaMode.entries[it].name }
                    )
                }
            }

            when (mode) {
                AgendaMode.DAY -> {
                    item {
                        DayNavigation(
                            date = selectedDate,
                            onPrevious = { setSelected(selectedDate.minusDays(1)) },
                            onToday = { setSelected(LocalDate.now()) },
                            onNext = { setSelected(selectedDate.plusDays(1)) }
                        )
                    }
                    item {
                        DayTimeline(
                            date = selectedDate,
                            entries = entries(selectedDate),
                            viewModel = viewModel,
                            hapticsEnabled = settings.hapticsEnabled,
                            onActionOpen = onActionOpen
                        )
                    }
                }

                AgendaMode.WEEK -> {
                    val weekStart = selectedDate.minusDays((selectedDate.dayOfWeek.value - 1).toLong())
                    item {
                        WeekNavigation(
                            weekStart = weekStart,
                            selected = selectedDate,
                            entriesFor = ::entries,
                            onSelect = ::setSelected,
                            onPrevious = { setSelected(selectedDate.minusWeeks(1)) },
                            onToday = { setSelected(LocalDate.now()) },
                            onNext = { setSelected(selectedDate.plusWeeks(1)) }
                        )
                    }
                    item {
                        DayTimeline(
                            date = selectedDate,
                            entries = entries(selectedDate),
                            viewModel = viewModel,
                            hapticsEnabled = settings.hapticsEnabled,
                            onActionOpen = onActionOpen
                        )
                    }
                }

                AgendaMode.MONTH -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                IconButton(onClick = { changeMonth(month.minusMonths(1)) }) { Icon(ActionBoxIcons.Back, contentDescription = "Mês anterior") }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "${month.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-BR")).replaceFirstChar { it.uppercase() }} ${month.year}",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                    TextButton(onClick = { setSelected(LocalDate.now()) }) { Text("Hoje") }
                                }
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
                                    selected = selectedDate,
                                    entriesFor = ::entries,
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
                                    onSelect = ::setSelected
                                )
                            }

                            DayTimeline(
                                date = selectedDate,
                                entries = entries(selectedDate),
                                viewModel = viewModel,
                                hapticsEnabled = settings.hapticsEnabled,
                                onActionOpen = onActionOpen
                            )
                        }
                    }
                }

                AgendaMode.LIST -> {
                    val today = LocalDate.now()
                    val range = AgendaUseCase.entriesForRange(today, today.plusDays(30), all) { action, day ->
                        if (RecurrenceCalculator.recurrenceType(action) == RecurrenceType.NONE) RecurrenceCalculator.occursOn(action, day)
                        else viewModel.routineOccursOn(action, day)
                    }
                    if (range.isEmpty()) item { Text("Sua agenda está livre nos próximos 30 dias.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    range.forEach { (date, dayEntries) ->
                        item(key = "timeline-$date-${completions.size}") {
                            DayTimeline(date, dayEntries, viewModel, settings.hapticsEnabled, onActionOpen)
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
            }
            item { Spacer(Modifier.height(28.dp)) }
        }
    }
}

@Composable
private fun DayNavigation(
    date: LocalDate,
    onPrevious: () -> Unit,
    onToday: () -> Unit,
    onNext: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = onPrevious) { Icon(ActionBoxIcons.Back, contentDescription = "Dia anterior") }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                date.format(DateTimeFormatter.ofPattern("EEEE", Locale.forLanguageTag("pt-BR"))).uppercase(),
                style = MaterialTheme.typography.labelLarge
            )
            Text(date.format(DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.forLanguageTag("pt-BR"))), style = MaterialTheme.typography.titleLarge)
            if (date != LocalDate.now()) TextButton(onClick = onToday) { Text("Hoje") }
        }
        IconButton(onClick = onNext) { Icon(ActionBoxIcons.Next, contentDescription = "Próximo dia") }
    }
}

@Composable
private fun WeekNavigation(
    weekStart: LocalDate,
    selected: LocalDate,
    entriesFor: (LocalDate) -> List<ActionEntity>,
    onSelect: (LocalDate) -> Unit,
    onPrevious: () -> Unit,
    onToday: () -> Unit,
    onNext: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onPrevious) { Icon(ActionBoxIcons.Back, contentDescription = "Semana anterior") }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${weekStart.format(DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("pt-BR")))} – ${weekStart.plusDays(6).format(DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("pt-BR")))}",
                    style = MaterialTheme.typography.titleMedium
                )
                TextButton(onClick = onToday) { Text("Hoje") }
            }
            IconButton(onClick = onNext) { Icon(ActionBoxIcons.Next, contentDescription = "Próxima semana") }
        }
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 1.dp) {
            Row(Modifier.fillMaxWidth().padding(8.dp)) {
                repeat(7) { offset ->
                    val date = weekStart.plusDays(offset.toLong())
                    val isSelected = date == selected
                    val hasEntries = entriesFor(date).isNotEmpty()
                    Column(
                        modifier = Modifier.weight(1f).clickable { onSelect(date) }.padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("pt-BR")).take(3).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        ) {
                            Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                                Text(date.dayOfMonth.toString(), color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        Surface(
                            modifier = Modifier.size(5.dp),
                            shape = CircleShape,
                            color = if (hasEntries) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                        ) {}
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthCalendar(
    month: YearMonth,
    selected: LocalDate,
    entriesFor: (LocalDate) -> List<ActionEntity>,
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
                        else DayCell(date, selected, entriesFor(date), viewModel, Modifier.weight(1f)) { onSelect(date) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    selected: LocalDate,
    entries: List<ActionEntity>,
    viewModel: ActionViewModel,
    modifier: Modifier,
    onClick: () -> Unit
) {
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
private fun DayTimeline(
    date: LocalDate,
    entries: List<ActionEntity>,
    viewModel: ActionViewModel,
    hapticsEnabled: Boolean,
    onActionOpen: (Long) -> Unit
) {
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
private fun TimelineAction(
    action: ActionEntity,
    date: LocalDate,
    viewModel: ActionViewModel,
    hapticsEnabled: Boolean,
    onActionOpen: (Long) -> Unit
) {
    val completed = viewModel.isCompletedOn(action, date)
    val color = if (completed) ActionBoxColors.Completed else actionTypeColor(action.type)
    val time = action.scheduledAt?.let {
        val localTime = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime()
        if (localTime.hour == 0 && localTime.minute == 0) "—" else localTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    } ?: "—"
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
