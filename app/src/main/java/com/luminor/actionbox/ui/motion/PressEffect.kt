package com.luminor.actionbox.ui.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

fun Modifier.pressScale(scale: Float = 0.96f): Modifier = composed {
    val pressed = remember { mutableStateOf(false) }
    val animated by animateFloatAsState(
        targetValue = if (pressed.value) scale else 1f,
        animationSpec = spring(),
        label = "press-scale"
    )
    this
        .graphicsLayer { scaleX = animated; scaleY = animated }
        .pointerInput(Unit) {
            detectTapGestures(onPress = {
                pressed.value = true
                tryAwaitRelease()
                pressed.value = false
            })
        }
}
