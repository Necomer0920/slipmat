package com.example.slipmat.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.slipmat.core.media.QueueItem
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LibraryScreen(
    onPlay: (items: List<QueueItem>, index: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Rescan on entry. Incremental, so an unchanged library writes nothing.
    LaunchedEffect(Unit) { viewModel.refresh() }

    LibraryContent(
        state = state,
        // The whole library becomes the queue, starting at the tapped row.
        onTrackClick = { index -> onPlay(state.tracks.map { it.toQueueItem() }, index) },
        modifier = modifier,
    )
}

@Composable
internal fun LibraryContent(
    state: LibraryUiState,
    onTrackClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (state.isScanning) {
            // Determinate, because a first scan of a few thousand files is slow enough that a
            // spinner with no end in sight reads as a hang.
            LinearProgressIndicator(
                progress = { state.scanProgress },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        when {
            state.isEmptyAfterScan -> EmptyLibrary(Modifier.fillMaxSize())
            else -> TrackList(
                tracks = state.tracks,
                onTrackClick = onTrackClick,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun EmptyLibrary(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = "No audio files found on this device",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
