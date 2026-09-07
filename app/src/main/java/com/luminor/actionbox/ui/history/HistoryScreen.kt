package com.luminor.actionbox.ui.history

import com.luminor.actionbox.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey

@Composable
fun HistoryScreen(viewModel: HistoryViewModel, onBack: () -> Unit) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources

    val items = viewModel.history.collectAsLazyPagingItems()
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(textResources.getString(R.string.text_historico), style = MaterialTheme.typography.headlineLarge)
            TextButton(onClick = onBack) { Text(textResources.getString(R.string.text_voltar)) }
        }
        when {
            items.loadState.refresh is LoadState.Loading && items.itemCount == 0 -> CircularProgressIndicator()
            items.loadState.refresh is LoadState.Error -> RetryHistory { items.retry() }
            items.loadState.refresh is LoadState.NotLoading && items.itemCount == 0 -> Text(textResources.getString(R.string.text_historico_vazio))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(count = items.itemCount, key = items.itemKey { it.id }) { index ->
                items[index]?.let { action ->
                    Card {
                        Row(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(action.title, modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.delete(action.id) }) { Text(textResources.getString(R.string.text_excluir)) }
                        }
                    }
                }
            }
            if (items.loadState.append is LoadState.Loading) item { CircularProgressIndicator() }
            if (items.loadState.append is LoadState.Error) item { RetryHistory { items.retry() } }
        }
    }
}

@Composable
private fun RetryHistory(onRetry: () -> Unit) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources

    Column {
        Text(textResources.getString(R.string.text_nao_foi_possivel_carregar_o_historico))
        TextButton(onClick = onRetry) { Text(textResources.getString(R.string.text_tentar_novamente)) }
    }
}
