package com.luminor.actionbox.ui.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class UndoKind { ACTION, PROJECT, LIST }

sealed interface AppUiEvent {
    data class Message(val text: String) : AppUiEvent
    data class Undo(val text: String, val kind: UndoKind, val id: Long) : AppUiEvent
}

class AppUiEventBus {
    private val _events = MutableSharedFlow<AppUiEvent>(extraBufferCapacity = 12)
    val events = _events.asSharedFlow()

    fun message(text: String) {
        _events.tryEmit(AppUiEvent.Message(text))
    }

    fun undo(text: String, kind: UndoKind, id: Long) {
        _events.tryEmit(AppUiEvent.Undo(text, kind, id))
    }
}
