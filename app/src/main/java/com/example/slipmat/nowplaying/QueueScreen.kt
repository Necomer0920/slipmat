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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.example.slipmat.core.media.QueueItem
import com.example.slipmat.ui.theme.CornerExtraLarge
import com.example.slipmat.ui.theme.CornerExtraSmall
import com.example.slipmat.ui.theme.CornerSmall

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
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(count = items.size) { index ->
                QueueRow(item = items[index], isPlaying = index == playingIndex, onClick = { onPlay(index) })
            }
        }
    }
}

private val ROW_MIN_HEIGHT = 56.dp
private val ROW_ARTWORK_SIZE = 40.dp

/** Same shape as [TrackRow] (§4.6 asks the detail screens for the same match) - artwork, clip,
 * padding and corner radius all pulled from that one convention rather than a second one here. */
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
            .clip(RoundedCornerShape(CornerSmall))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SubcomposeAsyncImage(
            model = item.artworkUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(ROW_ARTWORK_SIZE).clip(RoundedCornerShape(CornerExtraSmall)),
            error = { QueueArtworkPlaceholder() },
            loading = { QueueArtworkPlaceholder() },
        )
        Column(
            modifier = Modifier.weight(1f).padding(start = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.title ?: "Unknown title",
                style = MaterialTheme.typography.bodyLarge,
                // Bold only, not tinted - §5.8 rejects tint as the sole signal, and bold alone
                // already reads without depending on hue (the reasoning TrackRow's own row uses).
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
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

@Composable
private fun QueueArtworkPlaceholder() {
    Surface(
        modifier = Modifier.size(ROW_ARTWORK_SIZE),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {}
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
