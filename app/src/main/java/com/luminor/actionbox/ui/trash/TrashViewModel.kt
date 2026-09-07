package com.luminor.actionbox.ui.trash

import com.luminor.actionbox.R
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luminor.actionbox.ActionBoxApplication
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@dagger.hilt.android.lifecycle.HiltViewModel
class TrashViewModel @javax.inject.Inject constructor(
    private val repository: com.luminor.actionbox.data.repository.ActionRepository,
    private val uiEventBus: com.luminor.actionbox.ui.events.AppUiEventBus
) : androidx.lifecycle.ViewModel() {

    val actions = repository.deletedActions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val projects = repository.deletedProjects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val lists = repository.deletedLists.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun restoreAction(id: Long) = viewModelScope.launch {
        repository.restoreAction(id)
        uiEventBus.message(R.string.text_item_restaurado)
    }

    fun restoreProject(id: Long) = viewModelScope.launch {
        repository.restoreProjectCascade(id)
        uiEventBus.message(R.string.text_projeto_restaurado)
    }

    fun restoreList(id: Long) = viewModelScope.launch {
        repository.restoreListCascade(id)
        uiEventBus.message(R.string.text_lista_restaurada)
    }

    fun permanentlyDeleteAction(id: Long) = viewModelScope.launch {
        repository.permanentlyDeleteAction(id)
        uiEventBus.message(R.string.text_item_excluido_definitivamente)
    }

    fun permanentlyDeleteProject(id: Long) = viewModelScope.launch {
        repository.permanentlyDeleteProject(id)
        uiEventBus.message(R.string.text_projeto_excluido_definitivamente)
    }

    fun permanentlyDeleteList(id: Long) = viewModelScope.launch {
        repository.permanentlyDeleteList(id)
        uiEventBus.message(R.string.text_lista_excluida_definitivamente)
    }
}
