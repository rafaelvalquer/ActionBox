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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeReminderSheet(
    initialTime: LocalTime?,
    initialReminder: Int?,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime?, Int?) -> Unit
) {
    var selectedTime by remember(initialTime) { mutableStateOf(initialTime) }
    var selectedReminder by remember(initialReminder) { mutableStateOf(initialReminder) }
    var showPicker by remember { mutableStateOf(false) }
    val pickerState = rememberTimePickerState(
        initialHour = initialTime?.hour ?: 9,
        initialMinute = initialTime?.minute ?: 0,
        is24Hour = true
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Hora", style = MaterialTheme.typography.titleLarge)
            if (!showPicker) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(9, 10, 14, 19).forEach { hour ->
                        val value = LocalTime.of(hour, 0)
                        FilterChip(
                            selected = selectedTime == value,
                            onClick = { selectedTime = value },
                            label = { Text("%02d:00".format(hour)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                TextButton(onClick = { showPicker = true }) { Text("Escolher horário") }
                TextButton(onClick = { selectedTime = null }) { Text("Sem horário") }
            } else {
                TimePicker(state = pickerState, modifier = Modifier.align(Alignment.CenterHorizontally))
                TextButton(onClick = {
                    selectedTime = LocalTime.of(pickerState.hour, pickerState.minute)
                    showPicker = false
                }, modifier = Modifier.align(Alignment.End)) { Text("Usar horário") }
            }

            Text("Lembrete", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
            listOf(
                null to "Sem lembrete",
                0 to "Na hora",
                10 to "10 min antes",
                30 to "30 min antes",
                60 to "1 hora antes",
                1440 to "1 dia antes"
            ).forEach { (minutes, label) ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { selectedReminder = minutes }.padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = selectedReminder == minutes, onClick = { selectedReminder = minutes })
                    Text(label, modifier = Modifier.padding(start = 8.dp))
                }
            }

            Button(onClick = { onConfirm(selectedTime, selectedReminder); onDismiss() }, modifier = Modifier.fillMaxWidth()) { Text("Concluir") }
            androidx.compose.foundation.layout.Spacer(Modifier.padding(bottom = 12.dp))
        }
    }
}
