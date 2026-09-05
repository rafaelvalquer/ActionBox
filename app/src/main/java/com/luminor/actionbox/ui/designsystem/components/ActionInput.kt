package com.luminor.actionbox.ui.designsystem.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ActionInput(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, minLines: Int = 2) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        minLines = minLines,
        maxLines = 7,
        shape = MaterialTheme.shapes.medium,
        placeholder = { Text(placeholder) }
    )
}
