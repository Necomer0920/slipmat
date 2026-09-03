package com.example.slipmat.core.media

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = publish()
    }

    /** Idempotent: safe to call from every Activity that comes and goes. */
    fun connect() {
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

    fun release() {
        controller?.removeListener(listener)
        controller?.release()
        controller = null
        _state.value = PlayerState.EMPTY
    }

    override fun play() = withController { it.play() }

    override fun pause() = withController { it.pause() }

    override fun next() = withController { it.seekToNextMediaItem() }

    override fun previous() = withController { it.seekToPreviousMediaItem() }

    override fun seekTo(positionMs: Long) = withController { it.seekTo(positionMs) }

    private fun withController(block: (MediaController) -> Unit) {
        scope.launch { controller?.let(block) }
    }

    private fun publish() {
        val c = controller ?: return
        val metadata: MediaMetadata = c.mediaMetadata
        _state.value = playerStateOf(
            isPlaying = c.isPlaying,
            rawPositionMs = c.currentPosition,
            rawDurationMs = c.duration,
            mediaId = c.currentMediaItem?.mediaId,
            title = metadata.title?.toString(),
            artist = metadata.artist?.toString(),
        )
    }
}
