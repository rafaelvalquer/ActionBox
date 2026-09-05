package com.luminor.actionbox.ui.actions.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luminor.actionbox.domain.RecurrenceType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrenceSheet(
    initialType: RecurrenceType,
    initialDays: Set<Int>,
    defaultDay: Int,
    onDismiss: () -> Unit,
    onConfirm: (RecurrenceType, Set<Int>) -> Unit
) {
    var type by remember(initialType) { mutableStateOf(initialType) }
    var days by remember(initialDays) { mutableStateOf(initialDays) }

    fun chooseWeekly(custom: Boolean) {
        type = RecurrenceType.WEEKLY
        if (days.isEmpty()) days = setOf(defaultDay)
        if (!custom) days = setOf(defaultDay)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Repetir", style = MaterialTheme.typography.titleLarge)
            RecurrenceOption("Não repetir", type == RecurrenceType.NONE) { type = RecurrenceType.NONE; days = emptySet() }
            RecurrenceOption("Todo dia", type == RecurrenceType.DAILY) { type = RecurrenceType.DAILY; days = emptySet() }
            RecurrenceOption("Toda semana", type == RecurrenceType.WEEKLY && days.size <= 1) { chooseWeekly(false) }
            RecurrenceOption("Todo mês", type == RecurrenceType.MONTHLY) { type = RecurrenceType.MONTHLY; days = emptySet() }
            RecurrenceOption("Personalizado", type == RecurrenceType.WEEKLY && days.size > 1) { chooseWeekly(true) }

            if (type == RecurrenceType.WEEKLY) {
                Text("Dias da semana", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("S", "T", "Q", "Q", "S", "S", "D").forEachIndexed { index, label ->
                        val day = index + 1
                        FilterChip(
                            selected = day in days,
                            onClick = {
                                val next = days.toMutableSet()
                                if (!next.add(day)) next.remove(day)
                                days = if (next.isEmpty()) setOf(day) else next
                            },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Button(
                onClick = { onConfirm(type, if (type == RecurrenceType.WEEKLY) days else emptySet()); onDismiss() },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) { Text("Concluir") }
            androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 12.dp))
        }
    }
}

@Composable
private fun RecurrenceOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyLarge)
    }
}
