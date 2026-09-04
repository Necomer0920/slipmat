package com.example.slipmat.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.slipmat.core.media.QueueItem
import kotlin.math.roundToInt

/** Row height, used to convert a drag distance into a number of positions moved. */
private val ROW_HEIGHT = 64.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Queue") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Close")
                }
            },
        )
        DraggableQueue(
            items = state.queue,
            playingIndex = state.queueIndex,
            onMove = viewModel::moveQueueItem,
            onPlay = viewModel::skipToQueueIndex,
        )
    }
}

/**
 * Long-press the handle to pick a row up, drag to reorder.
 *
 * The move reaches the player only when the finger lifts. Reordering on every frame would re-issue
 * `moveMediaItem` dozens of times per drag, and each call shifts the very queue the drag distance
 * is measured against.
 */
@Composable
private fun DraggableQueue(
    items: List<QueueItem>,
    playingIndex: Int,
    onMove: (from: Int, to: Int) -> Unit,
    onPlay: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowHeightPx = with(LocalDensity.current) { ROW_HEIGHT.toPx() }

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(count = items.size) { index ->
            val isDragging = draggingIndex == index
            QueueRow(
                item = items[index],
                isPlaying = index == playingIndex,
                isDragging = isDragging,
                offsetY = if (isDragging) dragOffset.roundToInt() else 0,
                onClick = { onPlay(index) },
                dragModifier = Modifier.pointerInput(index, items.size) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            draggingIndex = index
                            dragOffset = 0f
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            dragOffset += amount.y
                        },
                        onDragEnd = {
                            val from = draggingIndex
                            if (from != null) {
                                val moved = (dragOffset / rowHeightPx).roundToInt()
                                val to = (from + moved).coerceIn(items.indices)
                                if (to != from) onMove(from, to)
                            }
                            draggingIndex = null
                            dragOffset = 0f
                        },
                        onDragCancel = {
                            draggingIndex = null
                            dragOffset = 0f
                        },
                    )
                },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun QueueRow(
    item: QueueItem,
    isPlaying: Boolean,
    isDragging: Boolean,
    offsetY: Int,
    onClick: () -> Unit,
    dragModifier: Modifier,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // A lifted row must paint above its neighbours, or it slides underneath them.
            .zIndex(if (isDragging) 1f else 0f)
            .offset { IntOffset(0, offsetY) }
            .background(
                if (isDragging) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.title ?: "Unknown title",
                style = MaterialTheme.typography.bodyLarge,
                // The playing row is bold rather than coloured, so it stays legible for anyone
                // who cannot rely on hue.
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
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
        // Only the handle starts a drag, so tapping anywhere else still plays that row.
        Icon(
            imageVector = Icons.Filled.DragHandle,
            contentDescription = "Reorder",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = dragModifier,
        )
    }
}
