package com.luminor.actionbox.ui.motion

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

object MotionDuration {
    const val Instant = 80
    const val Fast = 140
    const val Standard = 240
    const val Emphasized = 360
}

object ActionBoxMotion {
    fun <T> cardMorph() = spring<T>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
    fun <T> modal() = spring<T>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
}
