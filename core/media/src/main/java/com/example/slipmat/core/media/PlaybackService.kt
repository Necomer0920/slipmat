package com.example.slipmat.core.media

import android.app.PendingIntent
import androidx.media3.common.MediaItem
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
        mediaSession = MediaSession.Builder(this, player)
            .apply { launchIntent()?.let(::setSessionActivity) }
            .build()

        // TEMPORARY (Phase 1 only): proves the service, session and notification work end to end
        // before there is any library to play from. Push a file with
        //   adb push <some.mp3> /sdcard/Music/test.mp3
        // Replaced in Phase 3, when the UI sets the queue through the controller.
        player.setMediaItem(MediaItem.fromUri(TEMPORARY_TEST_MEDIA))
        player.prepare()
        player.play()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    /**
     * What the media notification and lock-screen card open when tapped.
     *
     * Resolved through the package manager rather than by naming `MainActivity`: `:app` depends on
     * `:core:media`, never the reverse, so this module cannot reference the Activity class.
     * Without it the notification posts with `contentIntent=null` and tapping it does nothing.
     */
    private fun launchIntent(): PendingIntent? =
        packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

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

private const val TEMPORARY_TEST_MEDIA = "file:///sdcard/Music/test.mp3"
