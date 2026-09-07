package com.luminor.actionbox.ui.actions.sheets

import com.luminor.actionbox.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.ui.designsystem.ActionBoxIcons
import com.luminor.actionbox.ui.designsystem.actionTypeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionTypeSheet(
    current: ActionType,
    canConvert: Boolean,
    onDismiss: () -> Unit,
    onSelect: (ActionType) -> Unit
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(textResources.getString(R.string.text_tipo_da_acao), style = MaterialTheme.typography.titleLarge)
            if (!canConvert) {
                Text(textResources.getString(R.string.text_este_tipo_possui_estrutura_propria_e_nao_pode_ser_convertido_nest), color = MaterialTheme.colorScheme.onSurfaceVariant)
                TypeRow(current, selected = true, enabled = false, onClick = {})
            } else {
                listOf(ActionType.TASK, ActionType.REMINDER, ActionType.EVENT).forEach { type ->
                    TypeRow(type, selected = type == current, enabled = true) { onSelect(type); onDismiss() }
                }
            }
            androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 12.dp))
        }
    }
}

@Composable
private fun TypeRow(type: ActionType, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources

    val color = actionTypeColor(type.name)
    Row(
        modifier = Modifier.fillMaxWidth().then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(ActionBoxIcons.forType(type.name), contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Text(type.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        if (selected) Icon(ActionBoxIcons.Check, contentDescription = textResources.getString(R.string.text_selecionado), tint = MaterialTheme.colorScheme.primary)
    }
}
