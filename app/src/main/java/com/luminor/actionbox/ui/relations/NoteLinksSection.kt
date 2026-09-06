package com.luminor.actionbox.ui.relations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.data.local.ContentLinkEntity
import com.luminor.actionbox.data.local.ProjectEntity
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.OrganizationOwnerType

@Composable
fun NoteLinksSection(
    noteId: Long,
    projects: List<ProjectEntity>,
    actions: List<ActionEntity>,
    links: List<ContentLinkEntity>,
    onLink: (targetType: String, targetId: Long) -> Unit,
    onUnlink: (linkId: Long) -> Unit
) {
    var projectPicker by remember { mutableStateOf(false) }
    var taskPicker by remember { mutableStateOf(false) }
    val noteLinks = links.filter { it.sourceType == OrganizationOwnerType.NOTE && it.sourceId == noteId }
    val linkedProjectIds = noteLinks.filter { it.targetType == OrganizationOwnerType.PROJECT }.map { it.targetId }.toSet()
    val linkedActionIds = noteLinks.filter { it.targetType == OrganizationOwnerType.ACTION }.map { it.targetId }.toSet()
    val projectById = projects.associateBy { it.id }
    val actionById = actions.associateBy { it.id }
    val availableProjects = projects.filterNot { it.id in linkedProjectIds }
    val availableTasks = actions
        .filter { it.type == ActionType.TASK.name && it.id != noteId }
        .filterNot { it.id in linkedActionIds }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Vinculado a", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = { projectPicker = true }, enabled = availableProjects.isNotEmpty()) { Text("+ Projeto") }
            DropdownMenu(expanded = projectPicker, onDismissRequest = { projectPicker = false }) {
                availableProjects.take(30).forEach { project ->
                    DropdownMenuItem(
                        text = { Text(project.title) },
                        onClick = { onLink(OrganizationOwnerType.PROJECT, project.id); projectPicker = false }
                    )
                }
            }
            TextButton(onClick = { taskPicker = true }, enabled = availableTasks.isNotEmpty()) { Text("+ Tarefa") }
            DropdownMenu(expanded = taskPicker, onDismissRequest = { taskPicker = false }) {
                availableTasks.take(30).forEach { task ->
                    DropdownMenuItem(
                        text = { Text(task.title) },
                        onClick = { onLink(OrganizationOwnerType.ACTION, task.id); taskPicker = false }
                    )
                }
            }
        }

        if (noteLinks.isEmpty()) {
            Text("Nenhum projeto ou tarefa vinculado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            noteLinks.forEach { link ->
                val label = when (link.targetType) {
                    OrganizationOwnerType.PROJECT -> projectById[link.targetId]?.title?.let { "📁 $it" }
                    OrganizationOwnerType.ACTION -> actionById[link.targetId]?.title?.let { "✓ $it" }
                    else -> null
                } ?: return@forEach
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onUnlink(link.id) }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Desvincular")
                    }
                }
            }
        }
    }
}
