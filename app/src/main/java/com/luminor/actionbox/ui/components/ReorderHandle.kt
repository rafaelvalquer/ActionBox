package com.luminor.actionbox.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@Composable
fun ReorderHandle(
    index: Int,
    itemCount: Int,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    threshold: Dp = 44.dp
) {
    val thresholdPx = with(LocalDensity.current) { threshold.toPx() }
    var accumulated by remember(index) { mutableFloatStateOf(0f) }

    Icon(
        imageVector = Icons.Rounded.DragHandle,
        contentDescription = "Arrastar para reordenar",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .size(32.dp)
            .pointerInput(index, itemCount) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { accumulated = 0f },
                    onDragCancel = { accumulated = 0f },
                    onDragEnd = { accumulated = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulated += dragAmount.y
                        if (abs(accumulated) >= thresholdPx) {
                            val destination = if (accumulated > 0) index + 1 else index - 1
                            if (destination in 0 until itemCount) onMove(index, destination)
                            accumulated = 0f
                        }
                    }
                )
            }
    )
}

fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices || from == to) return this
    return toMutableList().also { list ->
        val item = list.removeAt(from)
        list.add(to, item)
    }
}
