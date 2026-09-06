package com.luminor.actionbox.ui.trash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luminor.actionbox.ActionBoxApplication
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrashViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ActionBoxApplication
    private val repository = app.repository

    val actions = repository.deletedActions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val projects = repository.deletedProjects.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val lists = repository.deletedLists.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun restoreAction(id: Long) = viewModelScope.launch {
        repository.restoreAction(id)
        app.uiEventBus.message("Item restaurado")
    }

    fun restoreProject(id: Long) = viewModelScope.launch {
        repository.restoreProjectCascade(id)
        app.uiEventBus.message("Projeto restaurado")
    }

    fun restoreList(id: Long) = viewModelScope.launch {
        repository.restoreListCascade(id)
        app.uiEventBus.message("Lista restaurada")
    }

    fun permanentlyDeleteAction(id: Long) = viewModelScope.launch {
        repository.permanentlyDeleteAction(id)
        app.uiEventBus.message("Item excluído definitivamente")
    }

    fun permanentlyDeleteProject(id: Long) = viewModelScope.launch {
        repository.permanentlyDeleteProject(id)
        app.uiEventBus.message("Projeto excluído definitivamente")
    }

    fun permanentlyDeleteList(id: Long) = viewModelScope.launch {
        repository.permanentlyDeleteList(id)
        app.uiEventBus.message("Lista excluída definitivamente")
    }
}
