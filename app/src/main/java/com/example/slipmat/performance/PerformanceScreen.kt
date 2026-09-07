package com.example.slipmat.performance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.example.slipmat.core.data.eq.EqPreset
import com.example.slipmat.core.media.dsp.DelayState
import com.example.slipmat.core.media.dsp.EqState
import com.example.slipmat.core.media.dsp.FilterMode
import com.example.slipmat.core.media.dsp.FilterState
import com.example.slipmat.nowplaying.DelayControls
import com.example.slipmat.nowplaying.EqControls
import com.example.slipmat.nowplaying.FilterControls
import com.example.slipmat.nowplaying.NowPlayingViewModel
import com.example.slipmat.ui.components.EffectDot
import com.example.slipmat.ui.components.SegmentedToggle
import com.example.slipmat.ui.theme.CornerExtraSmall
import com.example.slipmat.ui.theme.blueprintGrid

/**
 * Everything the Performance screen can do, gathered so the body stays stateless (§5.13) - the
 * same shape [com.example.slipmat.nowplaying.NowPlayingActions] follows.
 */
data class PerformanceActions(
    val onBack: () -> Unit = {},
    val onFilterEnabledChange: (Boolean) -> Unit = {},
    val onFilterCutoffChange: (Float) -> Unit = {},
    val onFilterModeChange: (FilterMode) -> Unit = {},
    val onDelayEnabledChange: (Boolean) -> Unit = {},
    val onDelayTimeChange: (Float) -> Unit = {},
    val onDelayFeedbackChange: (Float) -> Unit = {},
    val onDelayMixChange: (Float) -> Unit = {},
    val onEqEnabledChange: (Boolean) -> Unit = {},
    val onEqGainChange: (Int, Float) -> Unit = { _, _ -> },
    val onSaveEqPreset: (String) -> Unit = {},
    val onLoadEqPreset: (String) -> Unit = {},
    val onDeleteEqPreset: (String) -> Unit = {},
)

/** The three Performance tabs (§4.3) - which one is active is pure UI state, not DSP state. */
internal enum class PerformanceTab(val label: String) {
    Filter("Filter"),
    Delay("Delay"),
    Eq("EQ"),
}

/**
 * Filter/Delay/EQ, moved off Now Playing (R2.1) so effects don't compete with playback controls
 * for the same screen. [NowPlayingScreen] composes this directly as its second pager page, passing
 * the one [NowPlayingViewModel] instance it and the queue sheet already share (R3.1's own reason
 * for reaching this screen through a shared-state pager page rather than a fresh nav destination:
 * a second instance would re-decode the waveform) - there is no separate wired `PerformanceScreen`
 * to resolve its own view model, since nothing but that pager page reaches this screen.
 */
@Composable
internal fun PerformanceContent(
    trackTitle: String?,
    artworkUri: String?,
    filter: FilterState,
    delay: DelayState,
    eq: EqState,
    eqPresets: List<EqPreset>,
    actions: PerformanceActions,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(PerformanceTab.Filter) }

    Column(modifier = modifier.fillMaxSize().blueprintGrid()) {
        PerformanceHeader(trackTitle = trackTitle, artworkUri = artworkUri, onBack = actions.onBack)
        SegmentedToggle(
            options = PerformanceTab.entries,
            selected = selectedTab,
            onSelect = { selectedTab = it },
            label = PerformanceTab::label,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            trailing = { tab ->
                val enabled = when (tab) {
                    PerformanceTab.Filter -> filter.enabled
                    PerformanceTab.Delay -> delay.enabled
                    PerformanceTab.Eq -> eq.enabled
                }
                val onDisable = when (tab) {
                    PerformanceTab.Filter -> actions.onFilterEnabledChange
                    PerformanceTab.Delay -> actions.onDelayEnabledChange
                    PerformanceTab.Eq -> actions.onEqEnabledChange
                }
                EffectDot(enabled = enabled, onClick = { onDisable(false) })
            },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Exactly one panel composed at a time (README: "never all three stacked/expanded at
            // once") - the other two are not merely hidden, they do not exist in the tree.
            when (selectedTab) {
                PerformanceTab.Filter -> FilterControls(
                    state = filter,
                    onCutoffChange = actions.onFilterCutoffChange,
                    onModeChange = actions.onFilterModeChange,
                )
                PerformanceTab.Delay -> DelayControls(
                    state = delay,
                    onTimeChange = actions.onDelayTimeChange,
                    onFeedbackChange = actions.onDelayFeedbackChange,
                    onMixChange = actions.onDelayMixChange,
                )
                PerformanceTab.Eq -> EqControls(
                    state = eq,
                    presets = eqPresets,
                    onGainChange = actions.onEqGainChange,
                    onSavePreset = actions.onSaveEqPreset,
                    onLoadPreset = actions.onLoadEqPreset,
                    onDeletePreset = actions.onDeleteEqPreset,
                )
            }
        }
    }
}

private val PERFORMANCE_HEADER_HEIGHT = 48.dp
private val PERFORMANCE_ARTWORK_CHIP_SIZE = 26.dp

/**
 * §4.3's top bar: back chevron, a 26dp artwork chip, the track title, and the `PERFORMANCE`
 * eyebrow. The title carries `Modifier.weight(1f)` and the eyebrow doesn't, so a long title
 * ellipsises in its own space (R3.2) rather than pushing the eyebrow off the edge.
 */
@Composable
private fun PerformanceHeader(
    trackTitle: String?,
    artworkUri: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(PERFORMANCE_HEADER_HEIGHT).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }
        val chipShape = RoundedCornerShape(CornerExtraSmall)
        SubcomposeAsyncImage(
            model = artworkUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(PERFORMANCE_ARTWORK_CHIP_SIZE).clip(chipShape),
            error = { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerHighest) {} },
            loading = { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerHighest) {} },
        )
        Text(
            text = trackTitle ?: "Nothing playing",
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 12.dp, end = 12.dp),
        )
        Text(
            text = "PERFORMANCE",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
