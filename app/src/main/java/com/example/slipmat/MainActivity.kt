package com.example.slipmat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SlipmatTheme {
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
