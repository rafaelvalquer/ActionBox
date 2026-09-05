package com.luminor.actionbox.ui.organize.notes

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val DefaultNoteCategories = listOf("Trabalho", "Pessoal", "Ideias", "Compras", "Viagem")

@Composable
fun NoteCategoryFilter(selected: String, onSelected: (String) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        (listOf("Todas", "Fixadas") + DefaultNoteCategories).forEach { category ->
            FilterChip(selected = selected == category, onClick = { onSelected(category) }, label = { Text(category) })
        }
    }
}
