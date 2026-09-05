package com.luminor.actionbox.ui.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.luminor.actionbox.ActionViewModel
import com.luminor.actionbox.ui.components.SmartCapture

@Composable
fun CaptureScreen(viewModel: ActionViewModel) {
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().widthIn(max = 860.dp).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Criar", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Transforme uma frase em tarefa, lembrete, compromisso, lista ou projeto.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            SmartCapture(viewModel = viewModel, expanded = true)
        }
    }
}
