package com.example.slipmat.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.example.slipmat.core.data.db.TrackEntity
import com.example.slipmat.core.media.QueueItem
import com.example.slipmat.ui.theme.CornerExtraSmall
import com.example.slipmat.ui.theme.CornerSmall
import java.util.Locale

/** The one track list. Every screen that shows tracks uses this, so rows stay identical. */
@Composable
fun TrackList(
    tracks: List<TrackEntity>,
    onTrackClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
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
    }
}

private val RowMinHeight = 56.dp
private val ArtworkSize = 40.dp

@Composable
fun TrackRow(track: TrackEntity, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = RowMinHeight)
            .clip(RoundedCornerShape(CornerSmall))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SubcomposeAsyncImage(
            model = track.albumArtUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(ArtworkSize)
                .clip(RoundedCornerShape(CornerExtraSmall)),
            error = { TrackArtworkPlaceholder() },
            loading = { TrackArtworkPlaceholder() },
        )
        Column(
            modifier = Modifier.padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${track.artist} · ${formatDuration(track.durationMs)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TrackArtworkPlaceholder() {
    Surface(
        modifier = Modifier.aspectRatio(1f),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {}
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
