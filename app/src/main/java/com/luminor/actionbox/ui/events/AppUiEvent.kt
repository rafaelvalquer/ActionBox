package com.luminor.actionbox.ui.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class UndoKind { ACTION, PROJECT, LIST }

sealed interface AppUiEvent {
    data class Message(val text: UiText) : AppUiEvent
    data class Undo(val text: UiText, val kind: UndoKind, val id: Long) : AppUiEvent
}

class AppUiEventBus {
    fun message(text: String) = message(UiText.Dynamic(text))
    fun message(@androidx.annotation.StringRes id: Int, vararg args: Any) = message(UiText.Resource(id, args.toList()))
    fun undo(@androidx.annotation.StringRes text: Int, kind: UndoKind, id: Long) = undo(UiText.Resource(text), kind, id)

    private val _events = MutableSharedFlow<AppUiEvent>(extraBufferCapacity = 12)
    val events = _events.asSharedFlow()

    fun message(text: UiText) {
        _events.tryEmit(AppUiEvent.Message(text))
    }

    fun undo(text: UiText, kind: UndoKind, id: Long) {
        _events.tryEmit(AppUiEvent.Undo(text, kind, id))
    }
}
