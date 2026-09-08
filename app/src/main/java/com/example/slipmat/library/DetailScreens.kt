package com.example.slipmat.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.slipmat.core.data.db.TrackEntity
import com.example.slipmat.core.media.QueueItem

/**
 * Every detail screen is the same shape: a header, and a list of tracks. R4.6 moved the header off
 * stock `Scaffold`/`TopAppBar` - the only screen still carrying it - onto the same plain-`Column`
 * plus custom header row every other screen in the redesign uses (`PerformanceHeader`'s shape:
 * back arrow, then title/subtitle). The list itself already matched: it's [TrackList], the exact
 * composable the Tracks tab renders, so a detail list was always pixel-identical to the library
 * list it came from.
 */
@Composable
private fun DetailScaffold(
    title: String,
    subtitle: String?,
    tracks: List<TrackEntity>,
    onBack: () -> Unit,
    onPlay: (items: List<QueueItem>, index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        DetailHeader(title = title, subtitle = subtitle, onBack = onBack)
        TrackList(
            tracks = tracks,
            // Playing from a detail screen queues *that* list, not the whole library.
            onTrackClick = { index -> onPlay(tracks.map { it.toQueueItem() }, index) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun DetailHeader(
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        Column(modifier = Modifier.padding(start = 4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun ArtistDetailScreen(
    onBack: () -> Unit,
    onPlay: (List<QueueItem>, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ArtistDetailViewModel = hiltViewModel(),
) {
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    DetailScaffold(
        title = viewModel.artist,
        subtitle = pluralise(tracks.size, "track"),
        tracks = tracks,
        onBack = onBack,
        onPlay = onPlay,
        modifier = modifier,
    )
}

@Composable
fun AlbumDetailScreen(
    onBack: () -> Unit,
    onPlay: (List<QueueItem>, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlbumDetailViewModel = hiltViewModel(),
) {
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    DetailScaffold(
        title = viewModel.album,
        subtitle = "${viewModel.albumArtist} · ${pluralise(tracks.size, "track")}",
        tracks = tracks,
        onBack = onBack,
        onPlay = onPlay,
        modifier = modifier,
    )
}

@Composable
fun FolderDetailScreen(
    onBack: () -> Unit,
    onPlay: (List<QueueItem>, Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FolderDetailViewModel = hiltViewModel(),
) {
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    DetailScaffold(
        title = viewModel.folderPath.substringAfterLast('/').ifEmpty { viewModel.folderPath },
        subtitle = viewModel.folderPath,
        tracks = tracks,
        onBack = onBack,
        onPlay = onPlay,
        modifier = modifier,
    )
}
