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
}
