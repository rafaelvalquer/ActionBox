package com.luminor.actionbox.ui.events

import com.luminor.actionbox.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

abstract class EventViewModel(protected val uiEventBus: AppUiEventBus, private val commandRunner: CommandRunner) : ViewModel() {
    protected fun execute(block: suspend () -> Unit) = viewModelScope.launch {
        try { block() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            android.util.Log.e("ActionBox", "Command failed", error)
            uiEventBus.message(R.string.text_nao_foi_possivel_concluir_tente_novamente)
        }
    }
    fun showMessage(@androidx.annotation.StringRes id: Int, vararg args: Any) = uiEventBus.message(id, *args)
    fun showMessage(value: String) = uiEventBus.message(value)
}
