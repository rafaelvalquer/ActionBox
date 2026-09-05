package com.luminor.actionbox.ui.actions.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.luminor.actionbox.domain.ActionPriority
import com.luminor.actionbox.ui.designsystem.ActionBoxColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrioritySheet(current: ActionPriority, onDismiss: () -> Unit, onSelect: (ActionPriority) -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text("Prioridade", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 10.dp))
            PriorityOption("Baixa", ActionPriority.LOW, ActionBoxColors.Completed, current, onSelect, onDismiss)
            PriorityOption("Normal", ActionPriority.NORMAL, MaterialTheme.colorScheme.primary, current, onSelect, onDismiss)
            PriorityOption("Alta", ActionPriority.HIGH, ActionBoxColors.Danger, current, onSelect, onDismiss)
            androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 12.dp))
        }
    }
}

@Composable
private fun PriorityOption(
    label: String,
    value: ActionPriority,
    color: Color,
    current: ActionPriority,
    onSelect: (ActionPriority) -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onSelect(value); onDismiss() }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = current == value, onClick = { onSelect(value); onDismiss() })
        Text(label, modifier = Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.bodyLarge)
        Text("●", color = color)
    }
}
