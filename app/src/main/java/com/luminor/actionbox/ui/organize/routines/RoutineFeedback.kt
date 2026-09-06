package com.luminor.actionbox.ui.organize.routines

import com.luminor.actionbox.ActionBoxApplication
import com.luminor.actionbox.ActionViewModel

internal fun ActionViewModel.showMessage(value: String) {
    getApplication<ActionBoxApplication>().uiEventBus.message(value)
}
