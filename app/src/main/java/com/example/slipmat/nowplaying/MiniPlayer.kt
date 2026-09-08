package com.example.slipmat.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import com.example.slipmat.ui.theme.CornerExtraSmall
import com.example.slipmat.ui.theme.CornerMedium

private val ArtworkSize = 38.dp
private val PlayButtonSize = 34.dp

/** Always-visible transport strip; tapping it opens the full now-playing screen. */
@Composable
fun MiniPlayer(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            // Bottom padding separates the card from the bottom nav bar right below it — with
            // none, the card's edge sat flush against the nav bar's own content.
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
        shape = RoundedCornerShape(CornerMedium),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = state.hasMedia, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SubcomposeAsyncImage(
                model = state.artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(ArtworkSize)
                    .clip(RoundedCornerShape(CornerExtraSmall)),
                error = { MiniPlayerArtworkPlaceholder() },
                loading = { MiniPlayerArtworkPlaceholder() },
            )
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = state.title ?: "Nothing playing",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = state.artist ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // §5.11: visual size is the design's (34dp); touch size is at least 48dp. Putting
            // `.size(PlayButtonSize)` directly on the IconButton (as before) shrank its whole
            // touch target to 34dp, since that fixes the constraint before IconButton's own
            // default ~48dp minimum can apply - the 34dp circle now lives on an inner Box instead,
            // leaving IconButton's own bounds at its unmodified default.
            IconButton(
                onClick = viewModel::togglePlayPause,
                enabled = state.hasMedia,
            ) {
                Box(
                    modifier = Modifier
                        .size(PlayButtonSize)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerArtworkPlaceholder() {
    Surface(
        modifier = Modifier.size(ArtworkSize),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {}
}
