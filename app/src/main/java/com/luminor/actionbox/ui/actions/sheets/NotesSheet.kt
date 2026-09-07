package com.luminor.actionbox.ui.actions.sheets

import com.luminor.actionbox.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesSheet(initial: String?, onDismiss: () -> Unit, onConfirm: (String?) -> Unit) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources

    var text by remember(initial) { mutableStateOf(initial.orEmpty()) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(textResources.getString(R.string.text_notas), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 12.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                placeholder = { Text(textResources.getString(R.string.text_adicione_detalhes_contexto_ou_observacoes)) },
                minLines = 5
            )
            Button(
                onClick = { onConfirm(text.trim().ifBlank { null }); onDismiss() },
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
            ) { Text(textResources.getString(R.string.text_concluir)) }
            androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 12.dp))
        }
    }
}
