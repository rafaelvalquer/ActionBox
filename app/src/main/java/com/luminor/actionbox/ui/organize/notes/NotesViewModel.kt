package com.luminor.actionbox.ui.organize.notes

import com.luminor.actionbox.R
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luminor.actionbox.ActionBoxApplication
import com.luminor.actionbox.data.local.ActionEntity
import com.luminor.actionbox.domain.ActionStatus
import com.luminor.actionbox.domain.ActionType
import kotlinx.coroutines.launch

@dagger.hilt.android.lifecycle.HiltViewModel
class NotesViewModel @javax.inject.Inject constructor(
    private val textResources: com.luminor.actionbox.ui.events.TextResources,
    private val repository: com.luminor.actionbox.data.repository.ActionRepository,
    private val uiEventBus: com.luminor.actionbox.ui.events.AppUiEventBus
) : androidx.lifecycle.ViewModel() {

    fun createBlankNote() {
        viewModelScope.launch {
            repository.insert(
                ActionEntity(
                    type = ActionType.NOTE.name,
                    title = textResources.getString(R.string.text_nova_nota),
                    content = textResources.getString(R.string.text_nova_nota),
                    sourceText = textResources.getString(R.string.text_nova_nota),
                    status = ActionStatus.COMPLETED.name,
                    completedAt = System.currentTimeMillis()
                )
            )
            uiEventBus.message(R.string.text_nota_salva)
        }
    }
}
