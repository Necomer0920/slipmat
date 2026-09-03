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
    RequestNotificationPermission()
}

/**
 * Asks for the notification permission once, on first composition.
 *
 * The result is deliberately ignored: the media notification is a convenience, not a requirement,
 * and playback must work whether or not the user grants it.
 */
@Composable
private fun RequestNotificationPermission() {
    val context = LocalContext.current
    val permission = MediaPermissions.notification

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not, playback is unaffected */ }

    LaunchedEffect(permission) {
        if (permission == null) return@LaunchedEffect
        val alreadyGranted = ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
        if (!alreadyGranted) launcher.launch(permission)
    }
}
