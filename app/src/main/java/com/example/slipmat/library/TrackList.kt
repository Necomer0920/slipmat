package com.example.slipmat.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.slipmat.core.data.db.TrackEntity
import com.example.slipmat.core.media.QueueItem
import java.util.Locale

/** The one track list. Every screen that shows tracks uses this, so rows stay identical. */
@Composable
fun TrackList(
    tracks: List<TrackEntity>,
    onTrackClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        itemsIndexed(tracks, onTrackClick)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.itemsIndexed(
    tracks: List<TrackEntity>,
    onTrackClick: (Int) -> Unit,
) {
    items(items = tracks, key = { it.id }) { track ->
        val index = tracks.indexOf(track)
        TrackRow(track = track, onClick = { onTrackClick(index) })
        HorizontalDivider()
    }
}

@Composable
fun TrackRow(track: TrackEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = track.title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${track.artist} · ${formatDuration(track.durationMs)}",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** `m:ss`, widening to `h:mm:ss` only when a track actually runs past an hour. */
fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

/**
 * Carries the library's own metadata into the queue, so the notification and now-playing screen
 * show the same cover the album grid does even when the file has no embedded artwork.
 */
fun TrackEntity.toQueueItem(): QueueItem = QueueItem(
    uri = uri,
    title = title,
    artist = artist,
    artworkUri = albumArtUri,
)
