package com.example.slipmat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.slipmat.ui.theme.ArtworkPalette
import com.example.slipmat.core.media.PlaybackController
import com.example.slipmat.navigation.SlipmatNavHost
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

    @Inject
    lateinit var palette: ArtworkPalette

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by playback.state.collectAsStateWithLifecycle()
            val artworkUri = state.queue.getOrNull(state.queueIndex)?.artworkUri

            // Kept here rather than inside the theme so the decode is not re-run by every
            // recomposition the theme takes part in, which is all of them.
            var artworkSeed by remember { mutableStateOf<Color?>(null) }
            LaunchedEffect(artworkUri) {
                artworkSeed = palette.seedFor(artworkUri)
            }

            SlipmatTheme(artworkSeed = artworkSeed) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AudioPermissionGate(modifier = Modifier.padding(innerPadding)) {
                        SlipmatNavHost(
                            onPlay = playback::playQueue,
                            modifier = Modifier.fillMaxSize(),
                        )
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
