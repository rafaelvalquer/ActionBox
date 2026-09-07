package com.luminor.actionbox.ui.actions.sheets

import com.luminor.actionbox.R
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
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources

    var showPicker by remember { mutableStateOf(false) }
    val today = LocalDate.now()
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = selected?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(textResources.getString(R.string.text_data), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 10.dp))
            if (!showPicker) {
                DateOption(textResources.getString(R.string.text_hoje)) { onSelect(today); onDismiss() }
                DateOption(textResources.getString(R.string.text_amanha)) { onSelect(today.plusDays(1)); onDismiss() }
                DateOption(textResources.getString(R.string.text_fim_de_semana)) { onSelect(today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))); onDismiss() }
                DateOption(textResources.getString(R.string.text_escolher_data)) { showPicker = true }
                DateOption(textResources.getString(R.string.text_sem_data)) { onSelect(null); onDismiss() }
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
                ) { Text(textResources.getString(R.string.text_concluir)) }
                TextButton(onClick = { showPicker = false }, modifier = Modifier.fillMaxWidth()) { Text(textResources.getString(R.string.text_voltar)) }
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
