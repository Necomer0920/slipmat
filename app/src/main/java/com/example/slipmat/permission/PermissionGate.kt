package com.example.slipmat.permission

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * Gates the app behind audio access, which it genuinely cannot work without.
 *
 * The awkward case is permanent denial: after two refusals Android stops showing the dialog
 * entirely, so asking again does nothing and the app looks broken. The only route left is the
 * system settings page, which is why that button exists.
 *
 * Re-checks on every resume, so returning from Settings updates without a restart.
 */
@Composable
fun AudioPermissionGate(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val permission = MediaPermissions.audio

    fun granted() = ContextCompat.checkSelfPermission(context, permission) ==
        PackageManager.PERMISSION_GRANTED

    var isGranted by remember { mutableStateOf(granted()) }
    var hasAsked by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { result ->
        isGranted = result
        hasAsked = true
    }

    // Coming back from Settings does not recompose on its own.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { isGranted = granted() }

    LaunchedEffect(Unit) {
        if (!isGranted && !hasAsked) launcher.launch(permission)
    }

    if (isGranted) {
        // The modifier carries the Scaffold's window insets. Dropping it on this branch put the
        // first list row underneath the status bar.
        Box(modifier = modifier) { content() }
        return
    }

    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Slipmat needs access to your music",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "It only reads audio files already on this device. Nothing is uploaded, and " +
                "the app never connects to the network.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )

        if (hasAsked) {
            // The dialog will not appear again; Settings is the only way back.
            Button(
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ),
                    )
                },
            ) {
                Text("Open settings")
            }
        } else {
            Button(onClick = { launcher.launch(permission) }) {
                Text("Grant access")
            }
        }
    }
}
