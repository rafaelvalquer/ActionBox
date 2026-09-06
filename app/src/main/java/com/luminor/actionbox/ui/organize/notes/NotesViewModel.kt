package com.luminor.actionbox.ui.organize.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luminor.actionbox.ActionBoxApplication
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.ActionType
import kotlinx.coroutines.launch

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ActionBoxApplication
    private val repository = app.repository

    fun createBlankNote() {
        viewModelScope.launch {
            repository.insert(
                ActionEntity(
                    type = ActionType.NOTE.name,
                    title = "Nova nota",
                    content = "Nova nota",
                    sourceText = "Nova nota",
                    status = ActionStatus.COMPLETED.name,
                    completedAt = System.currentTimeMillis()
                )
            )
            app.uiEventBus.message("Nota salva")
        }
    }
}
