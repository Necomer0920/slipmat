package com.example.slipmat.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.slipmat.core.data.db.TrackEntity
import com.example.slipmat.core.media.QueueItem

/** Every detail screen is the same shape: a title, a back arrow, and a list of tracks. */
@OptIn(ExperimentalMaterial3Api::class)
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
        TopAppBar(
            title = {
                Column {
                    Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )
        TrackList(
            tracks = tracks,
            // Playing from a detail screen queues *that* list, not the whole library.
            onTrackClick = { index -> onPlay(tracks.map { it.toQueueItem() }, index) },
            modifier = Modifier.fillMaxSize(),
        )
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
