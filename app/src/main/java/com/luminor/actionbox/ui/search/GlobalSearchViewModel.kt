package com.luminor.actionbox.ui.search

import com.luminor.actionbox.R
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luminor.actionbox.ActionBoxApplication
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.data.local.ActionListEntity
import com.luminor.actionbox.data.local.ProjectEntity
import com.luminor.actionbox.data.local.TagEntity
import com.luminor.actionbox.data.local.TagRefEntity
import com.luminor.actionbox.domain.ActionType
import com.luminor.actionbox.domain.OrganizationOwnerType
import com.luminor.actionbox.domain.search.SearchNormalizer
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn

@OptIn(FlowPreview::class)
@dagger.hilt.android.lifecycle.HiltViewModel
class GlobalSearchViewModel @javax.inject.Inject constructor(
    private val textResources: com.luminor.actionbox.ui.events.TextResources,
    private val repository: com.luminor.actionbox.data.repository.ActionRepository,
    private val uiEventBus: com.luminor.actionbox.ui.events.AppUiEventBus
) : androidx.lifecycle.ViewModel() {

    val query = MutableStateFlow("")
    val filter = MutableStateFlow(SearchFilter.ALL)

    private val content = combine(repository.all, repository.projects, repository.lists) { actions, projects, lists ->
        SearchContent(actions, projects, lists)
    }
    private val tagData = combine(repository.tags, repository.tagRefs) { tags, refs -> tags to refs }
    private val debouncedQuery = query.debounce(250).distinctUntilChanged()

    val results = combine(debouncedQuery, filter, content, tagData) { rawQuery, selectedFilter, contentState, tagState ->
        search(
            rawQuery = rawQuery,
            filter = selectedFilter,
            content = contentState,
            tags = tagState.first,
            refs = tagState.second
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setQuery(value: String) { query.value = value }
    fun setFilter(value: SearchFilter) { filter.value = value }

    private fun search(
        rawQuery: String,
        filter: SearchFilter,
        content: SearchContent,
        tags: List<TagEntity>,
        refs: List<TagRefEntity>
    ): List<SearchResultItem> {
        val needle = SearchNormalizer.normalize(rawQuery)
        if (needle.isBlank()) return emptyList()
        val tagById = tags.associateBy { it.id }

        fun tagText(ownerType: String, ownerId: Long): String = refs
            .asSequence()
            .filter { it.ownerType == ownerType && it.ownerId == ownerId }
            .mapNotNull { tagById[it.tagId]?.name }
            .joinToString(" ")

        val results = mutableListOf<SearchResultItem>()

        content.actions.forEach { action ->
            val kind = when (action.type) {
                ActionType.NOTE.name -> SearchResultKind.NOTE
                ActionType.READ_LATER.name -> SearchResultKind.SAVED
                else -> SearchResultKind.ACTION
            }
            if (!filter.accepts(kind)) return@forEach
            val tagText = tagText(OrganizationOwnerType.ACTION, action.id)
            val score = score(
                needle = needle,
                title = action.title,
                tags = tagText,
                body = listOfNotNull(action.content, action.description, action.sourceText, action.sourceUrl).joinToString(" ")
            )
            if (score > 0) {
                results += SearchResultItem(
                    kind = kind,
                    id = action.id,
                    title = action.title.ifBlank { textResources.getString(R.string.text_sem_titulo) },
                    subtitle = subtitleFor(action, tagText),
                    score = score,
                    updatedAt = action.updatedAt ?: action.createdAt
                )
            }
        }

        if (filter.accepts(SearchResultKind.PROJECT)) {
            content.projects.forEach { project ->
                val tagText = tagText(OrganizationOwnerType.PROJECT, project.id)
                val score = score(needle, project.title, tagText, project.description)
                if (score > 0) {
                    results += SearchResultItem(
                        kind = SearchResultKind.PROJECT,
                        id = project.id,
                        title = project.title,
                        subtitle = listOf(project.description, tagText.prependTags()).filter { it.isNotBlank() }.joinToString(" · "),
                        score = score,
                        updatedAt = project.updatedAt ?: project.createdAt
                    )
                }
            }
        }

        if (filter.accepts(SearchResultKind.LIST)) {
            content.lists.forEach { list ->
                val tagText = tagText(OrganizationOwnerType.LIST, list.id)
                val score = score(needle, list.title, tagText, "")
                if (score > 0) {
                    results += SearchResultItem(
                        kind = SearchResultKind.LIST,
                        id = list.id,
                        title = list.title,
                        subtitle = tagText.prependTags(),
                        score = score,
                        updatedAt = list.updatedAt ?: list.createdAt
                    )
                }
            }
        }

        return results.sortedWith(
            compareByDescending<SearchResultItem> { it.score }
                .thenByDescending { it.updatedAt }
                .thenBy { it.title.lowercase() }
        ).take(100)
    }

    private fun score(needle: String, title: String, tags: String, body: String): Int {
        val normalizedTitle = SearchNormalizer.normalize(title)
        val normalizedTags = SearchNormalizer.normalize(tags)
        val normalizedBody = SearchNormalizer.normalize(body)
        return when {
            normalizedTitle.startsWith(needle) -> 500
            normalizedTitle.contains(needle) -> 400
            normalizedTags.split(' ').any { it.startsWith(needle) } -> 300
            normalizedTags.contains(needle) -> 280
            normalizedBody.contains(needle) -> 200
            else -> 0
        }
    }

    private fun subtitleFor(action: ActionEntity, tags: String): String {
        val base = when (action.type) {
            ActionType.NOTE.name -> action.noteCategory ?: textResources.getString(R.string.text_nota)
            ActionType.READ_LATER.name -> action.sourceUrl ?: textResources.getString(R.string.text_salvo_para_depois)
            ActionType.REMINDER.name -> textResources.getString(R.string.text_lembrete)
            ActionType.EVENT.name -> textResources.getString(R.string.text_compromisso)
            else -> action.description ?: textResources.getString(R.string.text_tarefa)
        }
        return listOf(base, tags.prependTags()).filter { it.isNotBlank() }.joinToString(" · ")
    }

    private fun String.prependTags(): String = split(' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { if (it.startsWith("#")) it else "#$it" }
}

private data class SearchContent(
    val actions: List<ActionEntity>,
    val projects: List<ProjectEntity>,
    val lists: List<ActionListEntity>
)

enum class SearchFilter(val label: Int) {
    ALL(R.string.text_tudo),
    TASKS(R.string.text_tarefas),
    PROJECTS(R.string.text_projetos),
    NOTES(R.string.text_notas),
    LISTS(R.string.text_listas),
    SAVED(R.string.text_depois);

    fun accepts(kind: SearchResultKind): Boolean = when (this) {
        ALL -> true
        TASKS -> kind == SearchResultKind.ACTION
        PROJECTS -> kind == SearchResultKind.PROJECT
        NOTES -> kind == SearchResultKind.NOTE
        LISTS -> kind == SearchResultKind.LIST
        SAVED -> kind == SearchResultKind.SAVED
    }
}

enum class SearchResultKind(val label: Int, val emoji: String) {
    ACTION(R.string.text_tarefa, "✓"),
    PROJECT(R.string.text_projeto, "📁"),
    NOTE(R.string.text_nota, "📝"),
    LIST(R.string.text_lista, "☑️"),
    SAVED(R.string.text_depois, "🔖")
}

data class SearchResultItem(
    val kind: SearchResultKind,
    val id: Long,
    val title: String,
    val subtitle: String,
    val score: Int,
    val updatedAt: Long
)
