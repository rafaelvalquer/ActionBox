package com.luminor.actionbox.ui.organize

import com.luminor.actionbox.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.OrganizationOwnerType
import com.luminor.actionbox.domain.RecurrenceCalculator
import com.luminor.actionbox.domain.RecurrenceType
import com.luminor.actionbox.domain.HabitStreakCalculator
import com.luminor.actionbox.ui.designsystem.components.ActionEmptyState
import com.luminor.actionbox.ui.designsystem.components.ActionSegmentedControl
import com.luminor.actionbox.ui.organize.notes.NotesBoard
import com.luminor.actionbox.ui.tags.TagFilterBar
import com.luminor.actionbox.ui.organize.routines.RoutineAccordionCard
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun OrganizeScreen(
    organizeViewModel: OrganizeViewModel,
    notesViewModel: com.luminor.actionbox.ui.organize.notes.NotesViewModel,
    onProjectOpen: (Long) -> Unit,
    onListOpen: (Long) -> Unit,
    onRoutineOpen: (Long) -> Unit,
    onNoteOpen: (Long) -> Unit,
    onSearch: () -> Unit
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources

    LaunchedEffect(notesViewModel) {
        notesViewModel.createdNoteIds.collect { onNoteOpen(it) }
    }

    val section by organizeViewModel.selectedSection.collectAsStateWithLifecycle()
    val all = when (section) {
        0 -> organizeViewModel.projectActions.collectAsStateWithLifecycle().value
        2 -> organizeViewModel.actions.collectAsStateWithLifecycle().value
        else -> emptyList()
    }
    val projects = if (section == 0) organizeViewModel.projects.collectAsStateWithLifecycle().value else emptyList()
    val lists = if (section == 1) organizeViewModel.lists.collectAsStateWithLifecycle().value else emptyList()
    val listItems = if (section == 1) organizeViewModel.listItems.collectAsStateWithLifecycle().value else emptyList()
    val notes = if (section == 3) organizeViewModel.notes.collectAsStateWithLifecycle().value else emptyList()
    val completions = if (section == 2) organizeViewModel.completions.collectAsStateWithLifecycle().value else emptyList()
    val rules = if (section == 2) organizeViewModel.routineRules.collectAsStateWithLifecycle().value else emptyList()
    val settings by organizeViewModel.settings.collectAsStateWithLifecycle()
    val tags by organizeViewModel.tags.collectAsStateWithLifecycle()
    val tagRefs by organizeViewModel.tagRefs.collectAsStateWithLifecycle()
    val expandedProjectIds by organizeViewModel.expandedProjectIds.collectAsStateWithLifecycle()
    val expandedRoutineIds by organizeViewModel.expandedRoutineIds.collectAsStateWithLifecycle()
    val selectedTagId by organizeViewModel.selectedTagId.collectAsStateWithLifecycle()
    val organizeSection = OrganizeSection.entries.getOrElse(section) { OrganizeSection.PROJECTS }
    val listState = rememberLazyListState()

    if (section != 3) {
        LaunchedEffect(organizeSection) {
            val (index, offset) = organizeViewModel.scrollPosition(organizeSection)
            listState.scrollToItem(index.coerceAtLeast(0), offset.coerceAtLeast(0))
        }
        LaunchedEffect(listState, organizeSection) {
            snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
                .collect { (index, offset) -> organizeViewModel.updateScroll(organizeSection, index, offset) }
        }
    }

    fun ownerHasSelectedTag(ownerType: String, ownerId: Long): Boolean =
        selectedTagId == null || tagRefs.any {
            it.tagId == selectedTagId && it.ownerType == ownerType && it.ownerId == ownerId
        }

    val visibleProjects = projects.filter { ownerHasSelectedTag(OrganizationOwnerType.PROJECT, it.id) }
    val visibleLists = lists.filter { ownerHasSelectedTag(OrganizationOwnerType.LIST, it.id) }
    val visibleRoutines = all
        .filter { RecurrenceCalculator.recurrenceType(it) != RecurrenceType.NONE && it.status != ActionStatus.ARCHIVED.name }
        .filter { ownerHasSelectedTag(OrganizationOwnerType.ACTION, it.id) }
    val activeExpandedProjectIds = expandedProjectIds.intersect(visibleProjects.map { it.id }.toSet())
    val activeExpandedRoutineIds = expandedRoutineIds.intersect(visibleRoutines.map { it.id }.toSet())
    val visibleNotes = notes.filter { ownerHasSelectedTag(OrganizationOwnerType.ACTION, it.id) }

    Column(
        modifier = Modifier.fillMaxSize().widthIn(max = 920.dp).statusBarsPadding().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(textResources.getString(R.string.text_organizar), style = MaterialTheme.typography.headlineLarge)
                    Text(textResources.getString(R.string.text_projetos_listas_rotinas_e_notas_em_um_so_lugar), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onSearch) { Text(textResources.getString(R.string.text_buscar_169)) }
            }
            ActionSegmentedControl(listOf(textResources.getString(R.string.text_projetos), textResources.getString(R.string.text_listas), textResources.getString(R.string.text_rotinas), textResources.getString(R.string.text_notas)), section, onSelected = organizeViewModel::setSelectedSection)
            if (tags.isNotEmpty()) {
                TagFilterBar(tags = tags, selectedTagId = selectedTagId, onSelected = organizeViewModel::selectTag)
            }
        }

        if (section == 3) {
            val (notesIndex, notesOffset) = organizeViewModel.scrollPosition(OrganizeSection.NOTES)
            NotesBoard(
                notes = visibleNotes,
                onUpdate = { context, original, updated -> organizeViewModel.updateAction(context, original, updated) },
                onArchive = { organizeViewModel.archive(it) },
                onDelete = { organizeViewModel.delete(it) },
                onCreate = { notesViewModel.createBlankNote() },
                onOpen = onNoteOpen,
                initialScrollIndex = notesIndex,
                initialScrollOffset = notesOffset,
                onScrollChanged = { index, offset -> organizeViewModel.updateScroll(OrganizeSection.NOTES, index, offset) }
            )
        } else {
            Box(Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    state = listState
                ) {
                    when (section) {
                        0 -> {
                            if (visibleProjects.isEmpty()) item { ActionEmptyState("📁", textResources.getString(R.string.text_nenhum_projeto), if (selectedTagId == null) textResources.getString(R.string.text_experimente_projeto_viagem_passagem_hotel_e_seguro) else textResources.getString(R.string.text_nenhum_projeto_usa_esta_tag)) }
                            items(visibleProjects, key = { it.id }) { project ->
                                ProjectRichCard(
                                    project = project,
                                    tasks = all.filter { it.projectId == project.id }.sortedWith(compareBy({ it.sortOrder }, { it.createdAt })),
                                    expanded = project.id in activeExpandedProjectIds,
                                    onToggleExpanded = { organizeViewModel.toggleProjectExpanded(project.id) },
                                    onToggleTask = organizeViewModel::toggleProjectTask,
                                    onOpenProject = { onProjectOpen(project.id) }
                                )
                            }
                        }
                        1 -> {
                            if (visibleLists.isEmpty()) item { ActionEmptyState("☑️", textResources.getString(R.string.text_nenhuma_lista), if (selectedTagId == null) textResources.getString(R.string.text_experimente_ir_ao_mercado_e_comprar_carne_pao_e_leite) else textResources.getString(R.string.text_nenhuma_lista_usa_esta_tag)) }
                            items(visibleLists, key = { it.id }) { list ->
                                ListRichCard(
                                    list = list,
                                    items = listItems.filter { it.listId == list.id },
                                    onToggle = { organizeViewModel.toggleListItem(it) },
                                    onFinish = { organizeViewModel.finishList(it) },
                                    onReopen = { organizeViewModel.reopenList(it) },
                                    onOpen = { onListOpen(list.id) }
                                )
                            }
                        }
                        2 -> {
                            if (visibleRoutines.isEmpty()) item { ActionEmptyState("🏋️", textResources.getString(R.string.text_nenhuma_rotina), if (selectedTagId == null) textResources.getString(R.string.text_crie_algo_recorrente_como_academia_segunda_quarta_e_sexta_as_19h) else textResources.getString(R.string.text_nenhuma_rotina_usa_esta_tag)) }
                            items(visibleRoutines, key = { it.id }) { action ->
                                val occursOn: (LocalDate) -> Boolean = { date -> com.luminor.actionbox.domain.routine.RoutineEvaluation.routineOccursOn(action, date, rules) }
                                val completedOn: (LocalDate) -> Boolean = { date -> com.luminor.actionbox.domain.routine.RoutineEvaluation.isCompletedOn(action, date, completions) }
                                val today = LocalDate.now()
                                val month = YearMonth.from(today)
                                val completedDays = (1..month.lengthOfMonth()).count { day ->
                                    val date = month.atDay(day)
                                    occursOn(date) && !date.isAfter(today) && completedOn(date)
                                }
                                val streak = HabitStreakCalculator.currentStreak(today, occursOn, completedOn)
                                RoutineAccordionCard(
                                    action = action,
                                    expanded = action.id in activeExpandedRoutineIds,
                                    completedDays = completedDays,
                                    streak = streak,
                                    occursOn = occursOn,
                                    completedOn = completedOn,
                                    onToggleExpanded = { organizeViewModel.toggleRoutineExpanded(action.id) },
                                    onToggleDay = { date -> organizeViewModel.toggleOccurrence(action, date) },
                                    onOpenRoutine = { onRoutineOpen(action.id) },
                                    hapticsEnabled = settings.hapticsEnabled
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(28.dp)) }
                }
            }
        }
    }
}
