package com.example.slipmat.core.media

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Owns playback for the whole app.
 *
 * The player lives here, in the service process, rather than in any state holder — so it survives
 * activity recreation, configuration change, and process death. Nothing in `:app` may hold a
 * player reference; the UI drives playback through a controller and observes state as a Flow.
 */
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var player: ExoPlayer

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    /**
     * Media3 1.6+ keeps the foreground service alive for ten minutes after playback stops, so
     * `player.pause()` here no longer releases it. `pauseAllPlayersAndStopSelf()` is the supported
     * way to shut down when the user swipes the task away.
     */
    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        pauseAllPlayersAndStopSelf()
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
