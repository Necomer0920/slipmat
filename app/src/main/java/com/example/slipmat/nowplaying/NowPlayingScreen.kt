package com.example.slipmat.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import com.example.slipmat.core.media.PlayerState
import com.example.slipmat.core.media.PitchRange
import com.example.slipmat.core.media.dsp.DelayState
import com.example.slipmat.core.media.dsp.FilterMode
import com.example.slipmat.core.media.dsp.FilterState
import com.example.slipmat.core.media.RepeatMode
import com.example.slipmat.core.media.SleepTimerState
import com.example.slipmat.library.formatDuration

/**
 * Everything the now-playing screen can do, gathered so the body stays stateless.
 *
 * A stateless body is what makes preview screenshots possible at all — a composable that reaches
 * for a `hiltViewModel()` cannot be rendered off-device.
 */
data class NowPlayingActions(
    val onBack: () -> Unit = {},
    val onOpenQueue: () -> Unit = {},
    val onPlayPause: () -> Unit = {},
    val onNext: () -> Unit = {},
    val onPrevious: () -> Unit = {},
    val onSkipForward: () -> Unit = {},
    val onSkipBack: () -> Unit = {},
    val onSeek: (Long) -> Unit = {},
    val onSeekFraction: (Float) -> Unit = {},
    val onToggleShuffle: () -> Unit = {},
    val onCycleRepeat: () -> Unit = {},
    val onStartSleepTimer: (Int) -> Unit = {},
    val onCancelSleepTimer: () -> Unit = {},
    val onSliderChange: (Float) -> Unit = {},
    val onSliderChangeFinished: () -> Unit = {},
    val onKeyLockChange: (Boolean) -> Unit = {},
    val onRangeChange: (PitchRange) -> Unit = {},
    val onFilterEnabledChange: (Boolean) -> Unit = {},
    val onFilterCutoffChange: (Float) -> Unit = {},
    val onFilterModeChange: (FilterMode) -> Unit = {},
    val onDelayEnabledChange: (Boolean) -> Unit = {},
    val onDelayTimeChange: (Float) -> Unit = {},
    val onDelayFeedbackChange: (Float) -> Unit = {},
    val onDelayMixChange: (Float) -> Unit = {},
)

@Composable
fun NowPlayingScreen(
    onBack: () -> Unit,
    onOpenQueue: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sleepTimer by viewModel.sleepTimer.collectAsStateWithLifecycle()
    val sliderValue by viewModel.sliderValue.collectAsStateWithLifecycle()
    val waveform by viewModel.waveform.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val delay by viewModel.delay.collectAsStateWithLifecycle()
    val keyLock by viewModel.keyLock.collectAsStateWithLifecycle()
    val pitchRange by viewModel.pitchRange.collectAsStateWithLifecycle()

    NowPlayingContent(
        state = state,
        sleepTimer = sleepTimer,
        sliderValue = sliderValue,
        waveform = waveform,
        filter = filter,
        delay = delay,
        keyLock = keyLock,
        pitchRange = pitchRange,
        actions = NowPlayingActions(
            onBack = onBack,
            onOpenQueue = onOpenQueue,
            onPlayPause = viewModel::togglePlayPause,
            onNext = viewModel::next,
            onPrevious = viewModel::previous,
            onSkipForward = viewModel::skipForward,
            onSkipBack = viewModel::skipBack,
            onSeek = viewModel::seekTo,
            onSeekFraction = viewModel::seekToFraction,
            onToggleShuffle = viewModel::toggleShuffle,
            onCycleRepeat = viewModel::cycleRepeatMode,
            onStartSleepTimer = viewModel::startSleepTimer,
            onCancelSleepTimer = viewModel::cancelSleepTimer,
            onSliderChange = viewModel::onSliderChange,
            onSliderChangeFinished = viewModel::onSliderChangeFinished,
            onKeyLockChange = viewModel::onKeyLockChange,
            onRangeChange = viewModel::onRangeChange,
            onFilterEnabledChange = viewModel::onFilterEnabledChange,
            onFilterCutoffChange = viewModel::onFilterCutoffChange,
            onFilterModeChange = viewModel::onFilterModeChange,
            onDelayEnabledChange = viewModel::onDelayEnabledChange,
            onDelayTimeChange = viewModel::onDelayTimeChange,
            onDelayFeedbackChange = viewModel::onDelayFeedbackChange,
            onDelayMixChange = viewModel::onDelayMixChange,
        ),
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NowPlayingContent(
    state: PlayerState,
    sleepTimer: SleepTimerState,
    actions: NowPlayingActions,
    modifier: Modifier = Modifier,
    sliderValue: Float = 0f,
    waveform: FloatArray? = null,
    filter: FilterState = FilterState(),
    delay: DelayState = DelayState(),
    keyLock: Boolean = true,
    pitchRange: PitchRange = PitchRange.Narrow,
    sourceBpm: Float? = null,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Now playing") },
            navigationIcon = {
                IconButton(onClick = actions.onBack) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Close")
                }
            },
            actions = {
                IconButton(onClick = actions.onOpenQueue) {
                    Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Queue")
                }
            },
        )

        // Scrollable, because the content is taller than a short screen once the sleep-timer
        // presets are expanded — and on a tall one it should still sit centred.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Artwork(state, Modifier.fillMaxWidth(0.8f))

            Text(
                text = state.title ?: "Nothing playing",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = state.artist ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            WaveformSeekBar(
                peaks = waveform,
                progress = state.progress,
                durationMs = state.durationMs,
                onSeek = actions.onSeekFraction,
            )
            PitchTempoControls(
                sliderValue = sliderValue,
                keyLock = keyLock,
                range = pitchRange,
                sourceBpm = state.sourceBpm,
                onSliderChange = actions.onSliderChange,
                onSliderChangeFinished = actions.onSliderChangeFinished,
                onKeyLockChange = actions.onKeyLockChange,
                onRangeChange = actions.onRangeChange,
            )
            TransportControls(state = state, actions = actions)
            ModeControls(state = state, actions = actions)
            FilterControls(
                state = filter,
                onEnabledChange = actions.onFilterEnabledChange,
                onCutoffChange = actions.onFilterCutoffChange,
                onModeChange = actions.onFilterModeChange,
            )
            DelayControls(
                state = delay,
                onEnabledChange = actions.onDelayEnabledChange,
                onTimeChange = actions.onDelayTimeChange,
                onFeedbackChange = actions.onDelayFeedbackChange,
                onMixChange = actions.onDelayMixChange,
            )
            SleepTimerControls(
                state = sleepTimer,
                onStart = actions.onStartSleepTimer,
                onCancel = actions.onCancelSleepTimer,
            )
        }
    }
}

@Composable
private fun Artwork(state: PlayerState, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = state.artworkUri,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.aspectRatio(1f).clip(RoundedCornerShape(12.dp)),
        error = { ArtworkPlaceholder() },
        loading = { ArtworkPlaceholder() },
    )
}

@Composable
private fun ArtworkPlaceholder() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {}
}

/**
 * Dragging must not fight the player.
 *
 * While the finger is down the slider shows the dragged value and ignores incoming position
 * updates; otherwise every 500 ms tick would yank the thumb back under the user.
 */
@Composable
private fun SeekBar(state: PlayerState, onSeek: (Long) -> Unit, modifier: Modifier = Modifier) {
    var scrubPosition by remember { mutableStateOf<Float?>(null) }
    val fraction = scrubPosition ?: state.progress

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = fraction,
            onValueChange = { scrubPosition = it },
            onValueChangeFinished = {
                scrubPosition?.let { onSeek((it * state.durationMs).toLong()) }
                scrubPosition = null
            },
            enabled = state.durationMs > 0,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatDuration((fraction * state.durationMs).toLong()),
                style = MaterialTheme.typography.labelMedium,
            )
            Text(
                text = formatDuration(state.durationMs),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun TransportControls(
    state: PlayerState,
    actions: NowPlayingActions,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TransportButton(
            icon = Icons.Filled.Replay10,
            description = "Back 10 seconds",
            onClick = actions.onSkipBack,
            enabled = state.hasMedia,
        )
        TransportButton(
            icon = Icons.Filled.SkipPrevious,
            description = "Previous track",
            onClick = actions.onPrevious,
            enabled = state.hasMedia,
        )
        TransportButton(
            icon = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            description = if (state.isPlaying) "Pause" else "Play",
            onClick = actions.onPlayPause,
            enabled = state.hasMedia,
        )
        TransportButton(
            icon = Icons.Filled.SkipNext,
            description = "Next track",
            onClick = actions.onNext,
            enabled = state.hasMedia,
        )
        TransportButton(
            icon = Icons.Filled.Forward10,
            description = "Forward 10 seconds",
            onClick = actions.onSkipForward,
            enabled = state.hasMedia,
        )
    }
}

/**
 * Shuffle and repeat sit apart from the transport row: they change how the queue behaves rather
 * than moving through it, and mixing them in makes the primary controls harder to hit.
 */
@Composable
private fun ModeControls(
    state: PlayerState,
    actions: NowPlayingActions,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ModeToggle(
            active = state.shuffleEnabled,
            icon = Icons.Filled.Shuffle,
            description = if (state.shuffleEnabled) "Shuffle on" else "Shuffle off",
            onClick = actions.onToggleShuffle,
        )
        ModeToggle(
            active = state.repeatMode != RepeatMode.Off,
            // RepeatOne carries its own "1" badge, so all three states differ in shape as well
            // as in background.
            icon = if (state.repeatMode == RepeatMode.One) {
                Icons.Filled.RepeatOne
            } else {
                Icons.Filled.Repeat
            },
            description = when (state.repeatMode) {
                RepeatMode.Off -> "Repeat off"
                RepeatMode.All -> "Repeat all"
                RepeatMode.One -> "Repeat one"
            },
            onClick = actions.onCycleRepeat,
        )
    }
}

/**
 * A toggle whose on-state is a filled background, not a tint.
 *
 * Tinting alone was measured at roughly a 12/255 difference on one channel against the inactive
 * grey — invisible at a glance, and worse for anyone with reduced colour vision. The filled shape
 * reads instantly and does not depend on hue.
 */
@Composable
private fun ModeToggle(
    active: Boolean,
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    if (active) {
        FilledTonalIconButton(onClick = onClick) {
            Icon(imageVector = icon, contentDescription = description)
        }
    } else {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TransportButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(imageVector = icon, contentDescription = description)
    }
}
