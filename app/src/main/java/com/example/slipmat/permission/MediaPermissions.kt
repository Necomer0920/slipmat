package com.example.slipmat.permission

import android.Manifest
import android.os.Build

/**
 * The runtime permissions this app asks for, and the API levels that actually require them.
 *
 * Both are version-gated, and they are gated on *different* levels, which is why they are listed
 * separately rather than as one array.
 */
object MediaPermissions {

    /**
     * Notifications became a runtime permission in API 33. Below that the media notification is
     * granted at install time.
     *
     * Denial is not fatal — see [android.app.Notification]: playback continues, the user simply
     * loses the notification and its transport controls.
     */
    val notification: String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else {
            null
        }

    /**
     * Reading local audio. API 33 replaced the broad [Manifest.permission.READ_EXTERNAL_STORAGE]
     * with the narrow [Manifest.permission.READ_MEDIA_AUDIO]; requesting the wrong one for the
     * running API level is denied without a dialog, which looks exactly like a user refusal.
     */
    val audio: String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            @Suppress("DEPRECATION")
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
}
