package com.luminor.actionbox.ui.capture

import com.luminor.actionbox.R
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
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(ActionBoxIcons.Create, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Text(textResources.getString(R.string.text_o_que_voce_precisa_resolver), style = MaterialTheme.typography.titleLarge)
        }
        ActionInput(
            value = value,
            onValueChange = onValueChange,
            placeholder = if (compact) textResources.getString(R.string.text_ex_reuniao_amanha_as_14h) else textResources.getString(R.string.text_digite_do_seu_jeito_ex_academia_segunda_quarta_e_sexta_as_19h),
            minLines = if (compact) 2 else 4
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onPaste) {
                Icon(ActionBoxIcons.Paste, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(textResources.getString(R.string.text_colar))
            }
            IconButton(onClick = onAnalyze) {
                Icon(ActionBoxIcons.Arrow, contentDescription = textResources.getString(R.string.text_analisar), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
