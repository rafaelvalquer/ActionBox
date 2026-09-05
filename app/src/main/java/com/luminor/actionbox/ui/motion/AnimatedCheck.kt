package com.luminor.actionbox.ui.motion

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.luminor.actionbox.ui.designsystem.ActionBoxIcons

@Composable
fun AnimatedCheck(checked: Boolean) {
    AnimatedContent(
        targetState = checked,
        transitionSpec = { scaleIn() togetherWith scaleOut() },
        label = "animated-check"
    ) { done ->
        Icon(
            imageVector = if (done) ActionBoxIcons.Check else ActionBoxIcons.EmptyCheck,
            contentDescription = if (done) "Concluído" else "Pendente",
            tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
