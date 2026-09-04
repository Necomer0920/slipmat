package com.example.slipmat.core.media

import android.app.PendingIntent
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.slipmat.core.data.db.PlaybackPositionDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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

    @Inject
    lateinit var playbackPositions: PlaybackPositionDao

    @Inject
    lateinit var sleepTimer: SleepTimer

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var positionKeeper: PositionKeeper? = null

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSession.Builder(this, player)
            .apply { launchIntent()?.let(::setSessionActivity) }
            .build()

        positionKeeper = PositionKeeper(player, playbackPositions, serviceScope).also { it.start() }
        restoreLastSession()
        observeSleepTimer()
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

    /**
     * Puts the last-played track back after a cold start, paused and at its saved position.
     *
     * Only runs when the player is empty, so it never disturbs a live queue. The queue itself is
     * not persisted — restoring the track the user was on is what "surviving process death" means
     * here, and rebuilding a 2,600-item queue on every launch would not be.
     */
    private fun restoreLastSession() {
        if (player.mediaItemCount > 0) return
        serviceScope.launch {
            val last = playbackPositions.mostRecent() ?: return@launch
            if (player.mediaItemCount > 0) return@launch
            player.setMediaItem(MediaItem.fromUri(last.mediaUri))
            player.prepare()
            player.seekTo(last.positionMs)
            // Deliberately not play(): waking to unexpected audio is worse than a tap.
        }
    }

    /**
     * Applies the sleep timer to the player.
     *
     * The fade lives here rather than in [SleepTimer] because the player belongs to this service.
     * Volume is restored on the way out, otherwise the next track after a timer would start silent.
     */
    private fun observeSleepTimer() {
        serviceScope.launch {
            sleepTimer.state.collectLatest { state ->
                when (state) {
                    is SleepTimerState.Idle -> player.volume = 1f
                    is SleepTimerState.Running -> {
                        player.volume = state.fadeFraction
                        if (state.remainingMs <= 0L) {
                            player.pause()
                            player.volume = 1f
                            sleepTimer.cancel()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        positionKeeper?.stop()
        positionKeeper = null
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
