package com.luminor.actionbox.ui.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luminor.actionbox.ui.designsystem.ActionBoxIcons
import com.luminor.actionbox.ui.designsystem.components.ActionInput

@Composable
fun CaptureIdle(
    value: String,
    compact: Boolean,
    onValueChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    onPaste: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(ActionBoxIcons.Create, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Text("O que você precisa resolver?", style = MaterialTheme.typography.titleLarge)
        }
        ActionInput(
            value = value,
            onValueChange = onValueChange,
            placeholder = if (compact) "Ex.: Reunião amanhã às 14h..." else "Digite do seu jeito. Ex.: Academia segunda, quarta e sexta às 19h",
            minLines = if (compact) 2 else 4
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onPaste) {
                Icon(ActionBoxIcons.Paste, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Colar")
            }
            IconButton(onClick = onAnalyze) {
                Icon(ActionBoxIcons.Arrow, contentDescription = "Analisar", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
