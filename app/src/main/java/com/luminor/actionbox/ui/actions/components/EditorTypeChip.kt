package com.luminor.actionbox.ui.actions.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.ui.designsystem.ActionBoxIcons
import com.luminor.actionbox.ui.designsystem.actionTypeColor
import com.luminor.actionbox.ui.motion.pressScale

@Composable
fun EditorTypeChip(type: ActionType, locked: Boolean, onClick: () -> Unit) {
    val color = actionTypeColor(type.name)
    Surface(
        modifier = Modifier.pressScale(0.96f).then(if (locked) Modifier else Modifier.clickable(onClick = onClick)),
        shape = MaterialTheme.shapes.large,
        color = color.copy(alpha = 0.12f)
    ) {
        AnimatedContent(targetState = type, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "editor-type") { current ->
            Row(
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(ActionBoxIcons.forType(current.name), contentDescription = null, tint = color, modifier = Modifier.size(19.dp))
                Text(current.label, style = MaterialTheme.typography.labelLarge, color = color)
                if (!locked) Text("▾", color = color)
            }
        }
    }
}
