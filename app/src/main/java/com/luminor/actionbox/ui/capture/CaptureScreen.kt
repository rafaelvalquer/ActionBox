package com.luminor.actionbox.ui.capture

import com.luminor.actionbox.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CaptureScreen(viewModel: CaptureViewModel) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth().widthIn(max = 860.dp).statusBarsPadding().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(textResources.getString(R.string.text_criar), style = MaterialTheme.typography.headlineLarge)
            Text(
                textResources.getString(R.string.text_transforme_uma_frase_em_uma_acao_sem_preencher_um_formulario_enor),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            CaptureFlow(viewModel = viewModel, compact = false)
        }
    }
}
