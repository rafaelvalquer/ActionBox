package com.luminor.actionbox.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luminor.actionbox.ui.designsystem.ActionBoxIcons
import com.luminor.actionbox.ui.designsystem.components.ActionCard

@Composable
fun GlobalSearchScreen(
    viewModel: GlobalSearchViewModel,
    onBack: () -> Unit,
    onOpenAction: (Long) -> Unit,
    onOpenProject: (Long) -> Unit,
    onOpenNote: (Long) -> Unit,
    onOpenList: (Long) -> Unit,
    onOpenSaved: (Long) -> Unit
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 860.dp)
            .statusBarsPadding()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(ActionBoxIcons.Back, contentDescription = "Voltar") }
                Text("Buscar", style = MaterialTheme.typography.headlineMedium)
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Buscar no ActionBox") },
                placeholder = { Text("Tarefa, projeto, nota, lista ou link") },
                leadingIcon = { Text("🔍") }
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SearchFilter.entries, key = { it.name }) { item ->
                    FilterChip(
                        selected = item == filter,
                        onClick = { viewModel.setFilter(item) },
                        label = { Text(item.label) }
                    )
                }
            }
        }

        when {
            query.isBlank() -> item {
                SearchEmpty(
                    title = "Busque qualquer coisa",
                    description = "A pesquisa encontra tarefas, projetos, notas, listas, itens salvos e também suas tags."
                )
            }
            results.isEmpty() -> item {
                SearchEmpty(
                    title = "Nada encontrado",
                    description = "Tente outro termo ou selecione Tudo para ampliar a busca."
                )
            }
            else -> {
                val grouped = results.groupBy { it.kind }
                SearchResultKind.entries.forEach { kind ->
                    val kindResults = grouped[kind].orEmpty()
                    if (kindResults.isNotEmpty()) {
                        item(key = "header-${kind.name}") {
                            Text("${kind.emoji} ${kind.label.uppercase()}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        items(kindResults, key = { "${it.kind.name}-${it.id}" }) { result ->
                            ActionCard(
                                onClick = {
                                    when (result.kind) {
                                        SearchResultKind.ACTION -> onOpenAction(result.id)
                                        SearchResultKind.PROJECT -> onOpenProject(result.id)
                                        SearchResultKind.NOTE -> onOpenNote(result.id)
                                        SearchResultKind.LIST -> onOpenList(result.id)
                                        SearchResultKind.SAVED -> onOpenSaved(result.id)
                                    }
                                }
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(result.kind.emoji, style = MaterialTheme.typography.titleLarge)
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(result.title, style = MaterialTheme.typography.titleMedium)
                                        if (result.subtitle.isNotBlank()) {
                                            Text(
                                                result.subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2
                                            )
                                        }
                                    }
                                    Icon(ActionBoxIcons.Arrow, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
}

@Composable
private fun SearchEmpty(title: String, description: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("🔎", style = MaterialTheme.typography.headlineLarge)
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
