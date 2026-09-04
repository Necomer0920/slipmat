package com.example.slipmat.core.media

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import com.example.slipmat.core.media.dsp.AudioEffects
import com.example.slipmat.core.media.dsp.FilterMode
import com.example.slipmat.core.media.dsp.FilterState
import androidx.media3.common.Player
import androidx.media3.extractor.metadata.id3.TextInformationFrame
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
@OptIn(UnstableApi::class)
@Singleton
class MediaControllerHolder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sleepTimer: SleepTimer,
    private val effects: AudioEffects,
) : PlaybackController {

    private val _filterState = MutableStateFlow(FilterState())
    override val filterState: StateFlow<FilterState> =
        _filterState.asStateFlow()

    override fun setFilterEnabled(enabled: Boolean) {
        effects.filter.setEnabled(enabled)
        _filterState.value = _filterState.value.copy(enabled = enabled)
    }

    override fun setFilterCutoff(hz: Float) {
        effects.filter.setCutoff(hz)
        _filterState.value = _filterState.value.copy(cutoffHz = hz)
    }

    override fun setFilterMode(mode: FilterMode) {
        effects.filter.setMode(mode)
        _filterState.value = _filterState.value.copy(mode = mode)
    }

    override val sleepTimerState: StateFlow<SleepTimerState> = sleepTimer.state

    override fun startSleepTimer(durationMs: Long) = sleepTimer.start(durationMs)

    override fun cancelSleepTimer() = sleepTimer.cancel()

    override fun setSpeedPitch(speedPitch: SpeedPitch) = withController {
        it.playbackParameters = PlaybackParameters(speedPitch.speed, speedPitch.pitch)
    }

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

    override fun playQueue(items: List<QueueItem>, startIndex: Int) = withController { controller ->
        if (items.isEmpty()) return@withController
        controller.setMediaItems(
            items.map { item ->
                MediaItem.Builder()
                    .setUri(item.uri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(item.title)
                            .setArtist(item.artist)
                            .setArtworkUri(item.artworkUri?.let(Uri::parse))
                            .build(),
                    )
                    .build()
            },
            startIndex.coerceIn(items.indices),
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

    override fun skipForward() = withController {
        it.seekTo(skipSeekTarget(it.currentPosition, it.duration, SKIP_SEEK_MS))
    }

    override fun skipBack() = withController {
        it.seekTo(skipSeekTarget(it.currentPosition, it.duration, -SKIP_SEEK_MS))
    }

    override fun setShuffle(enabled: Boolean) = withController { it.shuffleModeEnabled = enabled }

    override fun setRepeatMode(mode: RepeatMode) = withController { it.repeatMode = mode.toMedia3() }

    override fun moveQueueItem(fromIndex: Int, toIndex: Int) = withController { c ->
        val count = c.mediaItemCount
        if (fromIndex !in 0 until count || toIndex !in 0 until count) return@withController
        c.moveMediaItem(fromIndex, toIndex)
    }

    override fun skipToQueueIndex(index: Int) = withController { c ->
        if (index !in 0 until c.mediaItemCount) return@withController
        c.seekTo(index, 0L)
        c.play()
    }

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
            shuffleEnabled = c.shuffleModeEnabled,
            repeatMode = c.repeatMode.toRepeatMode(),
            queue = c.readQueue(),
            queueIndex = c.currentMediaItemIndex,
            sourceBpm = c.readSourceBpm(),
            speed = c.playbackParameters.speed,
            pitch = c.playbackParameters.pitch,
        )
    }
}

/** Fast enough that a seconds readout never looks stuck, cheap enough to ignore. */
private const val POSITION_POLL_MS = 500L

/** Media3 uses integer constants; the rest of the app uses [RepeatMode]. Translate at the edge. */
private fun RepeatMode.toMedia3(): Int = when (this) {
    RepeatMode.Off -> Player.REPEAT_MODE_OFF
    RepeatMode.All -> Player.REPEAT_MODE_ALL
    RepeatMode.One -> Player.REPEAT_MODE_ONE
}

private fun Int.toRepeatMode(): RepeatMode = when (this) {
    Player.REPEAT_MODE_ALL -> RepeatMode.All
    Player.REPEAT_MODE_ONE -> RepeatMode.One
    else -> RepeatMode.Off
}

/**
 * Reads the queue back out of the controller.
 *
 * The metadata was put there by [MediaControllerHolder.playQueue], so this round-trips rather than
 * re-reading tags — which also means the queue screen shows the library's artwork, not just
 * whatever happened to be embedded in the file.
 */
private fun MediaController.readQueue(): List<QueueItem> =
    (0 until mediaItemCount).map { index ->
        val item = getMediaItemAt(index)
        QueueItem(
            uri = item.localConfiguration?.uri?.toString() ?: item.mediaId,
            title = item.mediaMetadata.title?.toString(),
            artist = item.mediaMetadata.artist?.toString(),
            artworkUri = item.mediaMetadata.artworkUri?.toString(),
        )
    }

/**
 * The track's own tempo, if the file happens to carry one.
 *
 * Neither MediaStore nor `MediaMetadataRetriever` exposes BPM — MediaStore has no such column, and
 * the retriever's key list has no equivalent — so this reads ID3's `TBPM` frame out of the decoded
 * track format, which Media3 has already parsed. Returns null for the many files that have no such
 * tag, and the readout hides rather than inventing a number.
 */
private fun MediaController.readSourceBpm(): Float? {
    for (group in currentTracks.groups) {
        for (i in 0 until group.length) {
            val metadata = group.getTrackFormat(i).metadata ?: continue
            for (e in 0 until metadata.length()) {
                val entry = metadata.get(e)
                if (entry is TextInformationFrame && entry.id.equals("TBPM", ignoreCase = true)) {
                    val bpm = entry.values.firstOrNull()?.trim()?.toFloatOrNull()
                    if (bpm != null && bpm > 0f) return bpm
                }
            }
        }
    }
    return null
}
