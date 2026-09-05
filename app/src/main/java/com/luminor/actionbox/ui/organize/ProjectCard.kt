package com.luminor.actionbox.ui.organize

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.data.local.ProjectEntity
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.ui.designsystem.ActionBoxColors
import com.luminor.actionbox.ui.designsystem.ActionBoxIcons
import com.luminor.actionbox.ui.designsystem.components.ActionCard
import com.luminor.actionbox.ui.motion.SharedKeys
import com.luminor.actionbox.ui.motion.actionSharedBounds

@Composable
fun ProjectRichCard(project: ProjectEntity, tasks: List<ActionEntity>, onOpen: () -> Unit) {
    val done = tasks.count { it.status == ActionStatus.COMPLETED.name }
    val progress = if (tasks.isEmpty()) 0f else done.toFloat() / tasks.size
    val next = tasks.firstOrNull { it.status != ActionStatus.COMPLETED.name }

    ActionCard(modifier = Modifier.actionSharedBounds(SharedKeys.project(project.id)), onClick = onOpen) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = MaterialTheme.shapes.medium, color = ActionBoxColors.Project.copy(alpha = 0.12f)) {
                    Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                        Icon(ActionBoxIcons.forType("PROJECT"), contentDescription = null, tint = ActionBoxColors.Project)
                    }
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(project.title, style = MaterialTheme.typography.titleLarge, textDecoration = if (project.completedAt != null) TextDecoration.LineThrough else null)
                    Text(if (project.completedAt != null) "Projeto finalizado" else "$done de ${tasks.size} concluídas", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = ActionBoxColors.Project)
            }
            if (tasks.isNotEmpty()) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            if (next != null) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Próxima", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("○  ${next.title}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                Text("Abrir", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Icon(ActionBoxIcons.Arrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}
