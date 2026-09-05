package com.luminor.actionbox.ui.designsystem.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.luminor.actionbox.ui.motion.pressScale

@Composable
fun ActionCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val base = modifier.fillMaxWidth().then(if (onClick != null) Modifier.pressScale() else Modifier)
    if (onClick == null) {
        Card(
            modifier = base,
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = androidx.compose.ui.unit.Dp.Hairline),
            content = content
        )
    } else {
        Card(
            onClick = onClick,
            modifier = base,
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = androidx.compose.ui.unit.Dp.Hairline),
            content = content
        )
    }
}
