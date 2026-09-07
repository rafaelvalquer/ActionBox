package com.luminor.actionbox.ui.organize

import com.luminor.actionbox.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.data.local.ProjectEntity
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.ui.designsystem.ActionBoxColors
import com.luminor.actionbox.ui.designsystem.ActionBoxIcons
import com.luminor.actionbox.ui.designsystem.components.ActionCard
import com.luminor.actionbox.ui.motion.AnimatedCheck
import com.luminor.actionbox.ui.motion.SharedKeys
import com.luminor.actionbox.ui.motion.actionSharedBounds

@Composable
fun ProjectRichCard(
    project: ProjectEntity,
    tasks: List<ActionEntity>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onToggleTask: (ActionEntity) -> Unit,
    onOpenProject: () -> Unit
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val done = tasks.count { it.status == ActionStatus.COMPLETED.name }
    val progress = if (tasks.isEmpty()) 0f else done.toFloat() / tasks.size
    val rotation by animateFloatAsState(if (expanded) 90f else 0f, label = "project-chevron")

    ActionCard(modifier = Modifier.actionSharedBounds(SharedKeys.project(project.id)).animateContentSize()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onToggleExpanded).semantics {
                    contentDescription = if (expanded) textResources.getString(R.string.text_recolher_projeto, project.title)
                    else textResources.getString(R.string.text_expandir_projeto, project.title)
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = MaterialTheme.shapes.medium, color = ActionBoxColors.Project.copy(alpha = 0.12f)) {
                    Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                        Icon(ActionBoxIcons.forType("PROJECT"), contentDescription = null, tint = ActionBoxColors.Project)
                    }
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(project.title, style = MaterialTheme.typography.titleLarge, textDecoration = if (project.completedAt != null) TextDecoration.LineThrough else null)
                    Text(textResources.getString(R.string.text_de_concluidas, done, tasks.size), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = ActionBoxColors.Project)
                    Icon(ActionBoxIcons.Chevron, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = rotation })
                }
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (tasks.isEmpty()) {
                        Text(textResources.getString(R.string.text_projeto_sem_tarefas), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
                    } else {
                        tasks.forEach { task -> ProjectQuickTask(task, onToggle = { onToggleTask(task) }) }
                    }
                    TextButton(onClick = onOpenProject, modifier = Modifier.align(Alignment.End)) {
                        Text(textResources.getString(R.string.text_ver_projeto_completo))
                        Icon(ActionBoxIcons.Arrow, contentDescription = null, modifier = Modifier.padding(start = 6.dp).size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProjectQuickTask(task: ActionEntity, onToggle: () -> Unit) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val completed = task.status == ActionStatus.COMPLETED.name
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).semantics {
            contentDescription = textResources.getString(if (completed) R.string.text_reabrir_tarefa_com_titulo else R.string.text_marcar_tarefa_concluida, task.title)
        }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedCheck(checked = completed, modifier = Modifier.size(20.dp))
        Text(task.title, modifier = Modifier.padding(start = 10.dp), style = MaterialTheme.typography.bodyMedium, textDecoration = if (completed) TextDecoration.LineThrough else null, color = if (completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
    }
}
