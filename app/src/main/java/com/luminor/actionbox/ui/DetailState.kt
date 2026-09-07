package com.luminor.actionbox.ui

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*

sealed interface DetailState<out T> {
    data object Loading : DetailState<Nothing>
    data object Missing : DetailState<Nothing>
    data class Loaded<T>(val value: T) : DetailState<T>
    data object Error : DetailState<Nothing>
}
fun <T> Flow<T?>.detailState(): Flow<DetailState<T>> = map<T?, DetailState<T>> {
    if (it == null) DetailState.Missing else DetailState.Loaded(it)
}.onStart { emit(DetailState.Loading) }.catch {
    if (it is CancellationException) throw it
    emit(DetailState.Error)
}

@Composable
fun <T> DetailContent(state: DetailState<T>, onBack: () -> Unit, onRetry: () -> Unit, content: @Composable (T) -> Unit) {
    when (state) {
        is DetailState.Loaded -> content(state.value)
        else -> Column(Modifier.fillMaxSize().statusBarsPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TextButton(onClick = onBack) { Text("Voltar") }
            when (state) {
                DetailState.Loading -> CircularProgressIndicator()
                DetailState.Missing -> Text("Este item não está mais disponível.")
                DetailState.Error -> {
                    Text("Não foi possível carregar este item.")
                    TextButton(onClick = onRetry) { Text("Tentar novamente") }
                }
            }
        }
    }
}
