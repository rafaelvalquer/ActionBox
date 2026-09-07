package com.luminor.actionbox.ui.organize.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val DefaultRoutineEmojis = listOf(
    "🔁", "🏋️", "📚", "💻", "💼", "🧹", "💊", "💧",
    "🧘", "🏃", "🎸", "🍎", "😴", "💰", "❤️", "🎯"
)

fun routineEmoji(value: String?): String = value?.takeIf { it.isNotBlank() } ?: "🔁"

@Composable
fun RoutineEmojiSelector(
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Ícone", style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(DefaultRoutineEmojis, key = { it }) { emoji ->
                FilterChip(
                    selected = value == emoji,
                    onClick = { onValueChange(emoji) },
                    label = { Text(emoji) }
                )
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Emoji personalizado") },
            supportingText = { Text("Escolha um ícone acima ou digite qualquer emoji.") }
        )
    }
}
