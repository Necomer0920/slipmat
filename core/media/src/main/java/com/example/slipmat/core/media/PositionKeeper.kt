package com.example.slipmat.core.media

import androidx.media3.common.Player
import com.example.slipmat.core.data.db.PlaybackPositionDao
import com.example.slipmat.core.data.db.PlaybackPositionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Remembers where the user got to, so a track resumes rather than restarting.
 *
 * Saves on a timer while playing and again whenever playback stops, because the timer alone loses
 * up to [SAVE_INTERVAL_MS] and a stop is exactly when the position matters most.
 */
class PositionKeeper(
    private val player: Player,
    private val dao: PlaybackPositionDao,
    private val scope: CoroutineScope,
) : Player.Listener {

    private var ticker: Job? = null

    fun start() {
        player.addListener(this)
    }

    fun stop() {
        player.removeListener(this)
        ticker?.cancel()
        ticker = null
        save()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        if (isPlaying) startTicking() else stopTicking()
    }

    /** Save against the *outgoing* track before the player moves on and the URI changes. */
    override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
        save()
    }

    private fun startTicking() {
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                delay(SAVE_INTERVAL_MS)
                save()
            }
        }
    }

    private fun stopTicking() {
        ticker?.cancel()
        ticker = null
        save()
    }

    private fun save() {
        val uri = player.currentMediaItem?.localConfiguration?.uri?.toString() ?: return
        val position = player.currentPosition
        val duration = player.duration

        scope.launch {
            // Near the end means "finished". Storing that would make the track restart at its last
            // second next time, which is worse than starting over.
            if (duration > 0 && position > duration - FINISHED_THRESHOLD_MS) {
                dao.clear(uri)
            } else if (position > MIN_SAVE_MS) {
                dao.upsert(
                    PlaybackPositionEntity(
                        mediaUri = uri,
                        positionMs = position,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            }
        }
    }

    private companion object {
        const val SAVE_INTERVAL_MS = 5_000L

        /** Below this, resuming is more annoying than restarting. */
        const val MIN_SAVE_MS = 5_000L

        const val FINISHED_THRESHOLD_MS = 5_000L
    }
}
