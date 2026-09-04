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
        val action = positionSaveAction(player.currentPosition, player.duration)

        scope.launch {
            when (action) {
                PositionSaveAction.Ignore -> Unit
                PositionSaveAction.Clear -> dao.clear(uri)
                is PositionSaveAction.Save -> dao.upsert(
                    PlaybackPositionEntity(
                        mediaUri = uri,
                        positionMs = action.positionMs,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            }
        }
    }

    private companion object {
        const val SAVE_INTERVAL_MS = 5_000L
    }
}

/** What to do with a position, decided separately from the player so the rules can be tested. */
sealed interface PositionSaveAction {
    data class Save(val positionMs: Long) : PositionSaveAction
    data object Clear : PositionSaveAction
    data object Ignore : PositionSaveAction
}

/** Below this, resuming is more annoying than starting the track over. */
const val MIN_RESUME_MS = 5_000L

/** Within this of the end, a track counts as finished. */
const val FINISHED_THRESHOLD_MS = 5_000L

/**
 * Decides whether a position is worth remembering.
 *
 * Storing a position near the end would make the track resume at its final second next time, which
 * is worse than restarting it — so that case clears the stored position instead.
 */
fun positionSaveAction(positionMs: Long, durationMs: Long): PositionSaveAction = when {
    positionMs < 0L -> PositionSaveAction.Ignore
    durationMs > 0L && positionMs > durationMs - FINISHED_THRESHOLD_MS -> PositionSaveAction.Clear
    positionMs > MIN_RESUME_MS -> PositionSaveAction.Save(positionMs)
    else -> PositionSaveAction.Ignore
}
