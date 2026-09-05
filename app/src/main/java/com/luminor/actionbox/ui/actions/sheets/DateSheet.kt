package com.luminor.actionbox.ui.actions.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.TemporalAdjusters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSheet(selected: LocalDate?, onDismiss: () -> Unit, onSelect: (LocalDate?) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = selected?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text("Data", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 10.dp))
            if (!showPicker) {
                DateOption("Hoje") { onSelect(today); onDismiss() }
                DateOption("Amanhã") { onSelect(today.plusDays(1)); onDismiss() }
                DateOption("Fim de semana") { onSelect(today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))); onDismiss() }
                DateOption("Escolher data") { showPicker = true }
                DateOption("Sem data") { onSelect(null); onDismiss() }
            } else {
                DatePicker(state = pickerState)
                Button(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onSelect(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                        }
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Concluir") }
                TextButton(onClick = { showPicker = false }, modifier = Modifier.fillMaxWidth()) { Text("Voltar") }
            }
            androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 14.dp))
        }
    }
}

@Composable
private fun DateOption(label: String, onClick: () -> Unit) {
    Text(
        label,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 15.dp),
        style = MaterialTheme.typography.bodyLarge
    )
}
