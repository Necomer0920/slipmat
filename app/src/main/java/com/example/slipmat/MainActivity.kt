package com.example.slipmat

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.slipmat.core.media.PlaybackController
import com.example.slipmat.permission.MediaPermissions
import com.example.slipmat.ui.theme.SlipmatTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Injected directly rather than through a ViewModel on purpose: this screen is Phase 1
     * scaffolding to prove the service works. Real state holders arrive in Phase 3.
     */
    @Inject
    lateinit var playback: PlaybackController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SlipmatTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SlipmatApp(
                        playback = playback,
                        modifier = Modifier.padding(innerPadding),
                    )
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

@Composable
private fun SlipmatApp(playback: PlaybackController, modifier: Modifier = Modifier) {
    RequestMediaPermissions()

    val state by playback.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = state.title ?: "No track loaded")
        Text(text = if (state.isPlaying) "Playing" else "Paused")
        Text(text = "${state.positionMs / 1000}s / ${state.durationMs / 1000}s")

        Button(onClick = { if (state.isPlaying) playback.pause() else playback.play() }) {
            Text(if (state.isPlaying) "Pause" else "Play")
        }
    }
}

/**
 * Asks for audio access and, on API 33+, notifications — once, on first composition.
 *
 * The results are deliberately not gating anything yet. Audio access is genuinely required to read
 * the library and gets a proper rationale screen in Phase 3 (Task 3.2); the notification permission
 * is a convenience whose denial must never stop playback.
 */
@Composable
private fun RequestMediaPermissions() {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* handled in Phase 3; denial must not break playback */ }

    LaunchedEffect(Unit) {
        val wanted = listOfNotNull(MediaPermissions.audio, MediaPermissions.notification)
        val missing = wanted.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) launcher.launch(missing.toTypedArray())
    }
}
