package com.luminor.actionbox.ui.motion

import com.luminor.actionbox.R
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.luminor.actionbox.ui.designsystem.ActionBoxIcons

@Composable
fun AnimatedCheck(
    checked: Boolean,
    modifier: Modifier = Modifier,
    checkedTint: Color? = null,
    uncheckedTint: Color? = null
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources

    val doneTint = checkedTint ?: MaterialTheme.colorScheme.primary
    val pendingTint = uncheckedTint ?: MaterialTheme.colorScheme.onSurfaceVariant
    AnimatedContent(
        targetState = checked,
        transitionSpec = { scaleIn() togetherWith scaleOut() },
        label = "animated-check",
        modifier = modifier
    ) { done ->
        Icon(
            imageVector = if (done) ActionBoxIcons.Check else ActionBoxIcons.EmptyCheck,
            contentDescription = if (done) textResources.getString(R.string.text_concluido) else textResources.getString(R.string.text_pendente),
            tint = if (done) doneTint else pendingTint
        )
    }
}
