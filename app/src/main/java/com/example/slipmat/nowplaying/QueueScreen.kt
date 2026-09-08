package com.example.slipmat.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.slipmat.core.media.QueueItem
import com.example.slipmat.ui.theme.CornerExtraLarge

/**
 * The queue, drawn as an overlay above Now Playing rather than pushed as its own destination
 * (§4.2: "Queue and sleep timer are overlays... not separate destinations"). [onDismiss] covers the
 * close button, the scrim and back in one place - `ModalBottomSheet` routes all three through
 * `onDismissRequest` itself.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    items: List<QueueItem>,
    playingIndex: Int,
    onPlay: (index: Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = rememberModalBottomSheetState(),
        shape = RoundedCornerShape(topStart = CornerExtraLarge, topEnd = CornerExtraLarge),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Playing next",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Close")
            }
        }
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(count = items.size) { index ->
                QueueRow(item = items[index], isPlaying = index == playingIndex, onClick = { onPlay(index) })
                HorizontalDivider()
            }
        }
    }
}

private val ROW_MIN_HEIGHT = 56.dp

@Composable
private fun QueueRow(
    item: QueueItem,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ROW_MIN_HEIGHT)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.title ?: "Unknown title",
                style = MaterialTheme.typography.bodyLarge,
                // Bold and tinted, not tint-only (§5.8's rule applied to the one state this row has).
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                color = if (isPlaying) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.artist ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SixDotGlyph()
    }
}

/**
 * §5.7's settled call: a drag-handle glyph wired to nothing. Reordering is a future task, not this
 * one. Hand-drawn per §5.9 rather than adding Material Symbols for one bespoke shape the app's
 * existing icon set has no equivalent for - two columns of three dots, no `pointerInput` anywhere
 * near it, so a drag on top of it does nothing observable.
 */
private val GLYPH_SIZE = 18.dp
private val GLYPH_DOT_RADIUS = 1.5.dp

@Composable
private fun SixDotGlyph(modifier: Modifier = Modifier) {
    val dotColor = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = modifier.size(GLYPH_SIZE)) {
        drawSixDots(dotColor)
    }
}

private fun DrawScope.drawSixDots(color: Color) {
    val radiusPx = GLYPH_DOT_RADIUS.toPx()
    val colGap = size.width * 0.45f
    val rowGap = size.height * 0.4f
    val originX = (size.width - colGap) / 2f
    val originY = (size.height - rowGap * 2f) / 2f
    for (col in 0..1) {
        for (row in 0..2) {
            drawCircle(
                color = color,
                radius = radiusPx,
                center = Offset(originX + col * colGap, originY + row * rowGap),
            )
        }
    }
}
