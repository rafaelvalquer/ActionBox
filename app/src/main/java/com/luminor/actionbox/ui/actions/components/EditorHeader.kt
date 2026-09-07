package com.luminor.actionbox.ui.actions.components

import com.luminor.actionbox.R
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
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(ActionBoxIcons.Back, contentDescription = textResources.getString(R.string.text_voltar)) }
        androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
        if (dirty) {
            TextButton(onClick = onCancel) { Text(textResources.getString(R.string.text_cancelar)) }
            TextButton(onClick = onSave) { Text(textResources.getString(R.string.text_salvar)) }
        }
        IconButton(onClick = onMore) { Icon(ActionBoxIcons.More, contentDescription = textResources.getString(R.string.text_mais_opcoes)) }
    }
}
