package com.example.slipmat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.slipmat.core.media.PlaybackController
import com.example.slipmat.library.LibraryScreen
import com.example.slipmat.library.LibraryViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.slipmat.permission.AudioPermissionGate
import com.example.slipmat.ui.theme.SlipmatTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Injected directly rather than through a ViewModel for now: this is still Phase 1/3
     * scaffolding. Task 3.10 moves playback state behind a proper state holder.
     */
    @Inject
    lateinit var playback: PlaybackController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SlipmatTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AudioPermissionGate(modifier = Modifier.padding(innerPadding)) {
                        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                            val libraryViewModel: LibraryViewModel = hiltViewModel()
                            val library by libraryViewModel.uiState.collectAsStateWithLifecycle()

                            LibraryScreen(
                                onTrackClick = { index ->
                                    playback.playQueue(
                                        uris = library.tracks.map { it.uri },
                                        startIndex = index,
                                    )
                                },
                                viewModel = libraryViewModel,
                                modifier = Modifier.weight(1f),
                            )
                            HorizontalDivider()
                            MiniPlayer(playback)
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        playback.connect()
    }

    override fun onStop() {
        playback.release()
        super.onStop()
    }
}

/**
 * Temporary transport strip, carried over from Phase 1 so playback stays reachable while the
 * browse UI is built. Replaced by the now-playing screen in Task 3.11.
 */
@Composable
private fun MiniPlayer(playback: PlaybackController, modifier: Modifier = Modifier) {
    val state by playback.state.collectAsStateWithLifecycle()

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.title ?: "Nothing playing",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${state.positionMs / 1000}s / ${state.durationMs / 1000}s",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        TextButton(
            onClick = { if (state.isPlaying) playback.pause() else playback.play() },
            enabled = state.hasMedia,
        ) {
            Text(if (state.isPlaying) "Pause" else "Play")
        }
    }
}
