package com.luminor.actionbox.ui.tags

import com.luminor.actionbox.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.luminor.actionbox.data.local.TagEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagPickerBottomSheet(
    tags: List<TagEntity>,
    selectedIds: Set<Long>,
    onToggle: (TagEntity) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources

    var newTag by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(textResources.getString(R.string.text_adicionar_tags_260))
            if (tags.isEmpty()) {
                Text(textResources.getString(R.string.text_nenhuma_tag_criada_ainda))
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tags, key = { it.id }) { tag ->
                        FilterChip(
                            selected = tag.id in selectedIds,
                            onClick = { onToggle(tag) },
                            label = { Text("#${tag.name}") }
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = newTag,
                    onValueChange = { newTag = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(textResources.getString(R.string.text_nova_tag)) },
                    placeholder = { Text("viagem") }
                )
                TextButton(
                    onClick = {
                        val value = newTag.trim().removePrefix("#")
                        if (value.isNotBlank()) {
                            onCreate(value)
                            newTag = ""
                        }
                    },
                    enabled = newTag.trim().removePrefix("#").isNotBlank()
                ) { Text(textResources.getString(R.string.text_criar)) }
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(textResources.getString(R.string.text_concluir)) }
        }
    }
}

@Composable
fun TagFilterBar(
    tags: List<TagEntity>,
    selectedTagId: Long?,
    onSelected: (Long?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            FilterChip(
                selected = selectedTagId == null,
                onClick = { onSelected(null) },
                label = { Text("Todas") }
            )
        }
        items(tags, key = { it.id }) { tag ->
            FilterChip(
                selected = selectedTagId == tag.id,
                onClick = { onSelected(if (selectedTagId == tag.id) null else tag.id) },
                label = { Text("#${tag.name}") }
            )
        }
    }
}
