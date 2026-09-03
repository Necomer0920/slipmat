package com.example.slipmat.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.slipmat.core.data.db.TrackEntity
import java.util.Locale

@Composable
fun LibraryScreen(
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Rescan on entry. Incremental, so an unchanged library writes nothing.
    LaunchedEffect(Unit) { viewModel.refresh() }

    LibraryContent(state = state, modifier = modifier)
}

@Composable
private fun LibraryContent(state: LibraryUiState, modifier: Modifier = Modifier) {
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
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items = state.tracks, key = { it.id }) { track ->
                    TrackRow(track)
                    HorizontalDivider()
                }
            }
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

@Composable
private fun TrackRow(track: TrackEntity, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
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
internal fun formatDuration(durationMs: Long): String {
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
