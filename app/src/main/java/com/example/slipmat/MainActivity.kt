package com.example.slipmat

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.slipmat.permission.MediaPermissions
import com.example.slipmat.ui.theme.SlipmatTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SlipmatTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SlipmatApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
private fun SlipmatApp(modifier: Modifier = Modifier) {
    RequestMediaPermissions()
}

/**
 * Asks for audio access and, on API 33+, notifications — once, on first composition.
 *
 * The results are deliberately not gating anything yet. Audio access is genuinely required to read
 * the library and will get a proper rationale screen in Phase 3 (Task 3.2); the notification
 * permission is a convenience whose denial must never stop playback.
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
