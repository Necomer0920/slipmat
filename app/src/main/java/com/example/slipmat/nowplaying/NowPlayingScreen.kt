package com.example.slipmat.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import com.example.slipmat.core.media.PlayerState
import com.example.slipmat.library.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Now playing") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Text(text = "\u2193", style = MaterialTheme.typography.titleLarge)
                }
            },
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Artwork(state, Modifier.fillMaxWidth(0.8f))

            Text(
                text = state.title ?: "Nothing playing",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = state.artist ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            SeekBar(state = state, onSeek = viewModel::seekTo)
            TransportControls(state = state, viewModel = viewModel)
        }
    }
}

@Composable
private fun Artwork(state: PlayerState, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = state.artworkUri,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)),
        error = { ArtworkPlaceholder() },
        loading = { ArtworkPlaceholder() },
    )
}

@Composable
private fun ArtworkPlaceholder() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {}
}

/**
 * Dragging must not fight the player.
 *
 * While the finger is down the slider shows the dragged value and ignores incoming position
 * updates; otherwise every 500 ms tick would yank the thumb back under the user.
 */
@Composable
private fun SeekBar(state: PlayerState, onSeek: (Long) -> Unit, modifier: Modifier = Modifier) {
    var scrubPosition by remember { mutableStateOf<Float?>(null) }
    val fraction = scrubPosition ?: state.progress

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = fraction,
            onValueChange = { scrubPosition = it },
            onValueChangeFinished = {
                scrubPosition?.let { onSeek((it * state.durationMs).toLong()) }
                scrubPosition = null
            },
            enabled = state.durationMs > 0,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDuration((fraction * state.durationMs).toLong()),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = formatDuration(state.durationMs),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun TransportControls(
    state: PlayerState,
    viewModel: NowPlayingViewModel,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TransportButton(
            icon = Icons.Filled.SkipPrevious,
            description = "Previous track",
            onClick = viewModel::previous,
            enabled = state.hasMedia,
        )
        TransportButton(
            icon = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            description = if (state.isPlaying) "Pause" else "Play",
            onClick = viewModel::togglePlayPause,
            enabled = state.hasMedia,
        )
        TransportButton(
            icon = Icons.Filled.SkipNext,
            description = "Next track",
            onClick = viewModel::next,
            enabled = state.hasMedia,
        )
    }
}

@Composable
private fun TransportButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(imageVector = icon, contentDescription = description)
    }
}
