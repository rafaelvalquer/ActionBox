package com.luminor.actionbox.ui.capture

import android.content.ClipboardManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luminor.actionbox.ui.motion.MotionDuration

private enum class CaptureVisualState { IDLE, RESULT }

@Composable
fun CaptureFlow(viewModel: CaptureViewModel, compact: Boolean) {
    val input by viewModel.input.collectAsStateWithLifecycle()
    val detected by viewModel.detected.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val editorVisible = remember { mutableStateOf(false) }

    fun analyze(text: String? = null) {
        if (text != null) viewModel.setInput(text)
        val candidate = text ?: input
        if (candidate.isBlank()) {
            viewModel.showMessage("Digite ou cole algo primeiro.")
            return
        }

        editorVisible.value = false
        viewModel.analyze()
    }

    val state = if (detected != null) CaptureVisualState.RESULT else CaptureVisualState.IDLE

    Surface(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = if (compact) 3.dp else 5.dp
    ) {
        Column(Modifier.padding(if (compact) 18.dp else 22.dp)) {
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    (fadeIn(tween(MotionDuration.Standard)) + scaleIn(tween(MotionDuration.Standard), initialScale = 0.985f)) togetherWith
                        (fadeOut(tween(MotionDuration.Fast)) + scaleOut(tween(MotionDuration.Fast), targetScale = 0.985f))
                },
                label = "capture-morph"
            ) { visualState ->
                when (visualState) {
                    CaptureVisualState.IDLE -> CaptureIdle(
                        value = input,
                        compact = compact,
                        onValueChange = viewModel::setInput,
                        onAnalyze = { analyze() },
                        onPaste = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString().orEmpty()
                            if (text.isNotBlank()) analyze(text)
                        }
                    )
                    CaptureVisualState.RESULT -> detected?.let { action ->
                        CaptureResult(
                            viewModel = viewModel,
                            action = action,
                            editorVisible = editorVisible.value,
                            onToggleEditor = { editorVisible.value = !editorVisible.value },
                            onReset = {
                                editorVisible.value = false
                                viewModel.clearInput()
                            }
                        )
                    }
                }
            }
        }
    }
}
