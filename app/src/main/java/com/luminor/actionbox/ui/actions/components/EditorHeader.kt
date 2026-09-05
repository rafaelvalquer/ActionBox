package com.luminor.actionbox.ui.actions.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.luminor.actionbox.ui.designsystem.ActionBoxIcons

@Composable
fun EditorHeader(
    dirty: Boolean,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onMore: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(ActionBoxIcons.Back, contentDescription = "Voltar") }
        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
        if (dirty) {
            TextButton(onClick = onCancel) { Text("Cancelar") }
            TextButton(onClick = onSave) { Text("Salvar") }
        }
        IconButton(onClick = onMore) { Icon(ActionBoxIcons.More, contentDescription = "Mais opções") }
    }
}
