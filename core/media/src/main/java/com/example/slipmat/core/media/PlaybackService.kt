package com.example.slipmat.core.media

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Owns playback for the whole app.
 *
 * The player lives here, in the service process, rather than in any state holder — so it survives
 * activity recreation, configuration change, and process death. Nothing in `:app` may hold a
 * player reference; the UI drives playback through a controller and observes state as a Flow.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession
}
