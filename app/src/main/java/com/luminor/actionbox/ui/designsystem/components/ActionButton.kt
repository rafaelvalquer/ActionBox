package com.luminor.actionbox.ui.designsystem.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.luminor.actionbox.ui.motion.pressScale

@Composable
fun ActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, primary: Boolean = true) {
    if (primary) {
        Button(
            onClick = onClick,
            modifier = modifier.fillMaxWidth().pressScale(),
            shape = MaterialTheme.shapes.medium,
            contentPadding = ButtonDefaults.ContentPadding
        ) { Text(text) }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.fillMaxWidth().pressScale(),
            shape = MaterialTheme.shapes.medium
        ) { Text(text) }
    }
}
