package com.luminor.actionbox.ui.organize.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luminor.actionbox.ui.designsystem.ActionBoxIcons

@Composable
fun NoteColorPicker(selected: String?, onSelected: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        NotePalette.forEach { entry ->
            Box(
                modifier = Modifier.size(34.dp).background(noteBackground(entry.key), CircleShape).clickable { onSelected(entry.key) },
                contentAlignment = Alignment.Center
            ) {
                if (entry.key == selected) Icon(ActionBoxIcons.Check, contentDescription = null, tint = entry.accent)
            }
        }
    }
}
