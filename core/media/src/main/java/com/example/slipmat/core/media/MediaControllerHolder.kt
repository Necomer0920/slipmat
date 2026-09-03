package com.example.slipmat.core.media

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single bridge between the UI and the playback service.
 *
 * Connects a [MediaController] to [PlaybackService] and republishes its state as a plain
 * [PlayerState] flow. The controller itself never leaves this class — see [PlaybackController]
 * for why that matters.
 *
 * All controller access happens on the main thread, which Media3 requires.
 */
@Singleton
class MediaControllerHolder @Inject constructor(
    @ApplicationContext private val context: Context,
) : PlaybackController {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(PlayerState.EMPTY)
    override val state: StateFlow<PlayerState> = _state.asStateFlow()

    private var controller: MediaController? = null

    /** Drives position updates while playing; see [syncTicker]. */
    private var ticker: Job? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish()
    }

    override fun connect() {
        if (controller != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                controller = future.get().also { it.addListener(listener) }
                publish()
            },
            MoreExecutors.directExecutor(),
        )
    }

    override fun release() {
        ticker?.cancel()
        ticker = null
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        _state.value = PlayerState.EMPTY
    }

    override fun playQueue(uris: List<String>, startIndex: Int) = withController { controller ->
        if (uris.isEmpty()) return@withController
        controller.setMediaItems(
            uris.map(MediaItem::fromUri),
            startIndex.coerceIn(uris.indices),
            0L,
        )
        controller.prepare()
        controller.play()
    }

    override fun play() = withController { it.play() }

    override fun pause() = withController { it.pause() }

    override fun next() = withController { it.seekToNextMediaItem() }

    override fun previous() = withController { it.seekToPreviousMediaItem() }

    override fun seekTo(positionMs: Long) = withController { it.seekTo(positionMs) }

    private fun withController(block: (MediaController) -> Unit) {
        scope.launch { controller?.let(block) }
    }

    /**
     * Keeps [PlayerState.positionMs] moving while a track plays.
     *
     * [Player.Listener.onEvents] fires on discrete changes — play, pause, item transition — and
     * never as position advances, so a listener alone leaves the displayed time frozen until
     * something else happens. Polling runs only while playing, and stops the moment it does not.
     */
    private fun syncTicker(isPlaying: Boolean) {
        if (!isPlaying) {
            ticker?.cancel()
            ticker = null
            return
        }
        if (ticker?.isActive == true) return
        ticker = scope.launch {
            while (isActive) {
                delay(POSITION_POLL_MS)
                val c = controller ?: break
                _state.value = _state.value.copy(
                    positionMs = c.currentPosition.coerceAtLeast(0L),
                )
            }
        }
    }

    private fun publish() {
        val c = controller ?: return
        syncTicker(c.isPlaying)
        val metadata: MediaMetadata = c.mediaMetadata
        _state.value = playerStateOf(
            isPlaying = c.isPlaying,
            rawPositionMs = c.currentPosition,
            rawDurationMs = c.duration,
            mediaId = c.currentMediaItem?.mediaId,
            title = metadata.title?.toString(),
            artist = metadata.artist?.toString(),
            artworkUri = metadata.artworkUri?.toString(),
        )
    }
}

/** Fast enough that a seconds readout never looks stuck, cheap enough to ignore. */
private const val POSITION_POLL_MS = 500L
