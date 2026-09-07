package com.luminor.actionbox.ui.events

import com.luminor.actionbox.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommandRunner @Inject constructor(private val events: AppUiEventBus) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun execute(block: suspend () -> Unit) = scope.launch {
        try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            android.util.Log.e("ActionBox", "Command failed", error)
            events.message(R.string.text_nao_foi_possivel_concluir_tente_novamente)
        }
    }
}
