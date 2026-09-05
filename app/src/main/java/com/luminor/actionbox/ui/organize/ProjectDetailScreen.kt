package com.luminor.actionbox.ui.organize

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luminor.actionbox.ActionViewModel
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.ui.designsystem.ActionBoxIcons
import com.luminor.actionbox.ui.designsystem.components.ActionButton
import com.luminor.actionbox.ui.designsystem.components.ActionCard
import com.luminor.actionbox.ui.motion.SharedKeys
import com.luminor.actionbox.ui.motion.actionSharedBounds
import java.time.LocalDate

@Composable
fun ProjectDetailScreen(viewModel: ActionViewModel, projectId: Long, onBack: () -> Unit) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val all by viewModel.all.collectAsStateWithLifecycle()
    val project = projects.firstOrNull { it.id == projectId }
    val tasks = all.filter { it.projectId == projectId }
    if (project == null) return
    val done = tasks.count { it.status == ActionStatus.COMPLETED.name }
    val progress = if (tasks.isEmpty()) 0f else done.toFloat() / tasks.size

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .widthIn(max = 760.dp)
                .statusBarsPadding()
                .padding(18.dp)
                .actionSharedBounds(SharedKeys.project(projectId)),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(ActionBoxIcons.Back, contentDescription = "Voltar") }
                Text("Projeto", style = MaterialTheme.typography.titleLarge)
            }
            Text(project.title, style = MaterialTheme.typography.headlineLarge, textDecoration = if (project.completedAt != null) TextDecoration.LineThrough else null)
            if (project.description.isNotBlank()) Text(project.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$done de ${tasks.size} concluídas · ${(progress * 100).toInt()}%", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (tasks.isNotEmpty()) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())

            ActionCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    tasks.forEach { task ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.toggleOccurrence(task, LocalDate.now()) }) {
                                Icon(if (task.status == ActionStatus.COMPLETED.name) ActionBoxIcons.Check else ActionBoxIcons.EmptyCheck, contentDescription = null)
                            }
                            Text(task.title, modifier = Modifier.weight(1f), textDecoration = if (task.status == ActionStatus.COMPLETED.name) TextDecoration.LineThrough else null)
                        }
                    }
                }
            }

            when {
                tasks.isNotEmpty() && done == tasks.size && project.completedAt == null -> ActionButton("Finalizar projeto", onClick = { viewModel.finishProject(project.id) })
                project.completedAt != null -> ActionButton("Reabrir projeto", onClick = { viewModel.reopenProject(project.id) }, primary = false)
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}
