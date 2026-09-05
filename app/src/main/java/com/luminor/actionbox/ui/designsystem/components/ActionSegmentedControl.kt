package com.luminor.actionbox.ui.designsystem.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun ActionSegmentedControl(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val itemWidthPx = if (labels.isNotEmpty()) size.width / labels.size else 0
    val density = androidx.compose.ui.platform.LocalDensity.current
    val offset by animateDpAsState(
        targetValue = with(density) { (itemWidthPx * selectedIndex).toDp() },
        label = "segment-offset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .padding(4.dp)
            .onSizeChanged { size = it },
        contentAlignment = Alignment.CenterStart
    ) {
        if (labels.isNotEmpty() && itemWidthPx > 0) {
            Surface(
                modifier = Modifier
                    .offset(x = offset)
                    .width(with(density) { itemWidthPx.toDp() } - 8.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) { Box(Modifier.padding(vertical = 16.dp)) }
        }
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { index, label ->
                Box(
                    modifier = Modifier.weight(1f).clickable { onSelected(index) }.padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (index == selectedIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
