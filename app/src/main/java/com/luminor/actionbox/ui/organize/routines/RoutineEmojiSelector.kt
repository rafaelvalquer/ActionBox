package com.luminor.actionbox.ui.organize.routines

import com.luminor.actionbox.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val routineEmojis = listOf("🔁", "🏋️", "📚", "💻", "💼", "🧹", "💊", "💧", "🧘", "🏃", "🎸", "🍎", "😴", "💰", "❤️", "🎯")

@Composable
fun RoutineEmojiSelector(value: String, onValueChange: (String) -> Unit) {
    val resources = androidx.compose.ui.platform.LocalContext.current.resources
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(resources.getString(R.string.text_icone_da_rotina))
        routineEmojis.chunked(8).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { emoji ->
                    FilterChip(
                        selected = value == emoji,
                        onClick = { onValueChange(emoji) },
                        label = { Text(emoji) },
                        modifier = Modifier.size(44.dp)
                    )
                }
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.take(8)) },
            singleLine = true,
            label = { Text(resources.getString(R.string.text_emoji_personalizado)) }
        )
    }
}
