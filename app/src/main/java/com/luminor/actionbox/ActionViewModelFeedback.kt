package com.luminor.actionbox

fun ActionViewModel.showMessage(value: String) {
    getApplication<ActionBoxApplication>().uiEventBus.message(value)
}
