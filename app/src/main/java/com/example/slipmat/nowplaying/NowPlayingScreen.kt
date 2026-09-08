package com.example.slipmat.nowplaying

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import com.example.slipmat.ui.components.Chip
import com.example.slipmat.ui.theme.CornerAlbumCell
import com.example.slipmat.ui.theme.CornerLarge
import com.example.slipmat.ui.theme.CornerPerformanceDoor
import com.example.slipmat.ui.theme.accentShadow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.SubcomposeAsyncImage
import com.example.slipmat.core.media.PlayerState
import com.example.slipmat.core.media.PitchRange
import com.example.slipmat.core.media.RepeatMode
import com.example.slipmat.core.media.SleepTimerState
import com.example.slipmat.performance.PerformanceActions
import com.example.slipmat.performance.PerformanceContent
import kotlinx.coroutines.launch

/**
 * Everything the now-playing screen can do, gathered so the body stays stateless.
 *
 * A stateless body is what makes preview screenshots possible at all — a composable that reaches
 * for a `hiltViewModel()` cannot be rendered off-device.
 */
data class NowPlayingActions(
    val onBack: () -> Unit = {},
    val onOpenQueue: () -> Unit = {},
    val onOpenPerformance: () -> Unit = {},
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
)

/**
 * Now Playing and Performance as two pages of one pager (§5.3), reached by swiping left or via the
 * Performance door - not two separate nav destinations, so both keep sharing this one
 * [NowPlayingViewModel] instance without a second waveform decode ever starting.
 */
@Composable
fun NowPlayingScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sleepTimer by viewModel.sleepTimer.collectAsStateWithLifecycle()
    val sliderValue by viewModel.sliderValue.collectAsStateWithLifecycle()
    val waveform by viewModel.waveform.collectAsStateWithLifecycle()
    val keyLock by viewModel.keyLock.collectAsStateWithLifecycle()
    val pitchRange by viewModel.pitchRange.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val delay by viewModel.delay.collectAsStateWithLifecycle()
    val eq by viewModel.eq.collectAsStateWithLifecycle()
    val eqPresets by viewModel.eqPresets.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    fun openPerformance() {
        scope.launch { pagerState.animateScrollToPage(1) }
    }
    fun closePerformance() {
        scope.launch { pagerState.animateScrollToPage(0) }
    }

    // An overlay above this screen, not a nav destination (§4.2) - local UI state rather than
    // anything the view model tracks.
    var showQueue by rememberSaveable { mutableStateOf(false) }

    HorizontalPager(state = pagerState, modifier = modifier.fillMaxSize()) { page ->
        when (page) {
            0 -> NowPlayingContent(
                state = state,
                sleepTimer = sleepTimer,
                sliderValue = sliderValue,
                waveform = waveform,
                keyLock = keyLock,
                pitchRange = pitchRange,
                actions = NowPlayingActions(
                    onBack = onBack,
                    onOpenQueue = { showQueue = true },
                    onOpenPerformance = ::openPerformance,
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
                ),
            )

            else -> PerformanceContent(
                trackTitle = state.title,
                artworkUri = state.artworkUri,
                filter = filter,
                delay = delay,
                eq = eq,
                eqPresets = eqPresets,
                actions = PerformanceActions(
                    onBack = ::closePerformance,
                    onFilterEnabledChange = viewModel::onFilterEnabledChange,
                    onFilterCutoffChange = viewModel::onFilterCutoffChange,
                    onFilterModeChange = viewModel::onFilterModeChange,
                    onDelayEnabledChange = viewModel::onDelayEnabledChange,
                    onDelayTimeChange = viewModel::onDelayTimeChange,
                    onDelayFeedbackChange = viewModel::onDelayFeedbackChange,
                    onDelayMixChange = viewModel::onDelayMixChange,
                    onEqEnabledChange = viewModel::onEqEnabledChange,
                    onEqGainChange = viewModel::onEqGainChange,
                    onSaveEqPreset = viewModel::onSaveEqPreset,
                    onLoadEqPreset = viewModel::onLoadEqPreset,
                    onDeleteEqPreset = viewModel::onDeleteEqPreset,
                ),
            )
        }
    }

    if (showQueue) {
        QueueSheet(
            items = state.queue,
            playingIndex = state.queueIndex,
            onPlay = viewModel::skipToQueueIndex,
            onDismiss = { showQueue = false },
        )
    }
}

@Composable
internal fun NowPlayingContent(
    state: PlayerState,
    sleepTimer: SleepTimerState,
    actions: NowPlayingActions,
    modifier: Modifier = Modifier,
    sliderValue: Float = 0f,
    waveform: FloatArray? = null,
    keyLock: Boolean = true,
    pitchRange: PitchRange = PitchRange.Standard,
    sourceBpm: Float? = null,
) {
    Column(modifier = modifier.fillMaxSize()) {
        NowPlayingHeader(sleepTimer = sleepTimer, actions = actions)

        // Scrollable, because the content is taller than a short screen on top of a long title —
        // and on a tall one it should still sit centred. The door sits outside this weighted,
        // scrolling region rather than as its last item, so it stays bottom-anchored (§4.2)
        // regardless of how much the content above it scrolls.
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Artwork(state)

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
            )
            TransportRow(state = state, actions = actions)
            SleepTimerControls(state = sleepTimer, onCancel = actions.onCancelSleepTimer)
        }

        PerformanceDoor(
            onClick = actions.onOpenPerformance,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        )
    }
}

/**
 * §4.2's header: leading and trailing zones both a fixed 84dp width (the README's own number) so
 * the centred eyebrow is optically centred regardless of how many icons sit in either zone.
 */
private val HeaderZoneWidth = 84.dp
private val HeaderHeight = 48.dp

@Composable
private fun NowPlayingHeader(
    sleepTimer: SleepTimerState,
    actions: NowPlayingActions,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(HeaderHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(HeaderZoneWidth), contentAlignment = Alignment.CenterStart) {
            IconButton(onClick = actions.onBack) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Close")
            }
        }
        Text(
            text = "PLAYING FROM LIBRARY",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Row(
            modifier = Modifier.width(HeaderZoneWidth),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(onClick = actions.onOpenQueue) {
                Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Queue")
            }
            SleepTimerButton(
                sleepTimer = sleepTimer,
                onStart = actions.onStartSleepTimer,
                onCancel = actions.onCancelSleepTimer,
            )
        }
    }
}

/**
 * §4.3/R4.3: the sleep timer as an anchored popover under this button, replacing the inline
 * preset row that used to sit in the scrolling content. [SleepTimerControls] still renders a
 * compact countdown further down while a timer is running, so cancelling doesn't require
 * reopening this popover - the choice made here just picks the duration.
 */
private val SLEEP_MENU_WIDTH = 168.dp
private val SLEEP_ICON_SIZE = 19.dp
private val SLEEP_MENU_PRESET_MINUTES = listOf(15, 30, 45, 60)

@Composable
private fun SleepTimerButton(
    sleepTimer: SleepTimerState,
    onStart: (minutes: Int) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    // Which preset is marked as active (README/mock §5.9's popover) - the engine only tracks a
    // remaining duration, not which preset produced it, so this is purely a UI memory of the tap
    // that started it, cleared the moment the timer goes back to idle by any path (cancel or
    // natural completion) so a stale chip never reads as selected for a timer that isn't running.
    var selectedMinutes by rememberSaveable { mutableStateOf<Int?>(null) }
    LaunchedEffect(sleepTimer) {
        if (sleepTimer is SleepTimerState.Idle) selectedMinutes = null
    }

    Box(modifier = modifier) {
        val description = if (sleepTimer is SleepTimerState.Running) {
            "Sleep timer, sleeping in ${formatCountdown(sleepTimer.remainingMs)}"
        } else {
            "Sleep timer"
        }
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.semantics { contentDescription = description },
        ) {
            SleepTimerIcon()
        }
        if (expanded) {
            val gapPx = with(LocalDensity.current) { 4.dp.roundToPx() }
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, gapPx),
                onDismissRequest = { expanded = false },
            ) {
                Surface(
                    modifier = Modifier.width(SLEEP_MENU_WIDTH),
                    shape = RoundedCornerShape(CornerAlbumCell),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 12.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "SLEEP TIMER",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                        when (sleepTimer) {
                            is SleepTimerState.Idle -> SLEEP_MENU_PRESET_MINUTES.forEach { minutes ->
                                Chip(
                                    selected = selectedMinutes == minutes,
                                    onClick = {
                                        selectedMinutes = minutes
                                        onStart(minutes)
                                        expanded = false
                                    },
                                    label = "$minutes min",
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }

                            is SleepTimerState.Running -> Chip(
                                selected = true,
                                onClick = {
                                    onCancel()
                                    expanded = false
                                },
                                label = if (sleepTimer.isFading) {
                                    "Fading out · ${formatCountdown(sleepTimer.remainingMs)}"
                                } else {
                                    "Sleeping in ${formatCountdown(sleepTimer.remainingMs)}"
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The README's "3-dot sleep-timer icon" (§5.9) - a bespoke glyph the app's icon set has no stock
 * equivalent for, hand-drawn the way the filter/EQ curves already are rather than pulling in a
 * second icon library for one shape. Three horizontal dots, not Material's vertical `MoreVert`
 * kebab - the mock's own glyph, at the same fractional positions (`Slipmat.dc.html` line 158).
 */
@Composable
private fun SleepTimerIcon(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurface
    Canvas(modifier = modifier.size(SLEEP_ICON_SIZE)) {
        val radius = size.minDimension * (1.8f / 24f)
        val y = size.height / 2f
        listOf(5f, 12f, 19f).forEach { fx ->
            drawCircle(color = color, radius = radius, center = Offset(fx / 24f * size.width, y))
        }
    }
}

/**
 * The pill that opens Performance (§4.2/§4.3) - bottom-anchored below the scrolling content rather
 * than as its last item, so it stays put and visible on a short screen instead of scrolling away
 * or landing wherever the content above it happens to end.
 */
@Composable
private fun PerformanceDoor(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(CornerPerformanceDoor),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = "Filter · Delay · EQ", style = MaterialTheme.typography.labelLarge)
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private val ARTWORK_SIZE = 168.dp

@Composable
private fun Artwork(state: PlayerState, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(CornerLarge)
    SubcomposeAsyncImage(
        model = state.artworkUri,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(ARTWORK_SIZE)
            .shadow(elevation = 16.dp, shape = shape)
            .clip(shape),
        error = { ArtworkPlaceholder() },
        loading = { ArtworkPlaceholder() },
    )
}

@Composable
private fun ArtworkPlaceholder() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {}
}

private val PLAY_BUTTON_SIZE = 66.dp
private val TRANSPORT_SIDE_PADDING = 22.dp

/**
 * All seven controls in one row per §4.2 - shuffle and repeat included, not set apart. They used
 * to sit in their own row below; the redesign spreads all seven across one 22dp-padded row instead.
 */
@Composable
private fun TransportRow(
    state: PlayerState,
    actions: NowPlayingActions,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = TRANSPORT_SIDE_PADDING),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ModeToggle(
            active = state.shuffleEnabled,
            icon = Icons.Filled.Shuffle,
            description = if (state.shuffleEnabled) "Shuffle on" else "Shuffle off",
            onClick = actions.onToggleShuffle,
        )
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
        IconButton(
            onClick = actions.onPlayPause,
            enabled = state.hasMedia,
            // 66dp, accent-filled, coloured shadow (§3.4/R0.7) - the one control the eye lands on
            // first.
            modifier = Modifier
                .size(PLAY_BUTTON_SIZE)
                .accentShadow()
                .background(MaterialTheme.colorScheme.primary, CircleShape),
        ) {
            Icon(
                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (state.isPlaying) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
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
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        Icon(imageVector = icon, contentDescription = description)
    }
}
