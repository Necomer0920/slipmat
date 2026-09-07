package com.example.slipmat.performance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.slipmat.core.data.eq.EqPreset
import com.example.slipmat.core.media.dsp.DelayState
import com.example.slipmat.core.media.dsp.EqState
import com.example.slipmat.core.media.dsp.FilterMode
import com.example.slipmat.core.media.dsp.FilterState
import com.example.slipmat.nowplaying.DelayControls
import com.example.slipmat.nowplaying.EqControls
import com.example.slipmat.nowplaying.FilterControls
import com.example.slipmat.nowplaying.NowPlayingViewModel

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

/**
 * Filter/Delay/EQ, moved off Now Playing (R2.1) so effects don't compete with playback controls
 * for the same screen. Talks to [NowPlayingViewModel] directly - the same instance Now Playing and
 * the queue sheet share (R3.1's own reason for reaching this screen through a shared-state pager
 * page rather than a fresh destination: a second instance would re-decode the waveform).
 */
@Composable
fun PerformanceScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NowPlayingViewModel = hiltViewModel(),
) {
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val delay by viewModel.delay.collectAsStateWithLifecycle()
    val eq by viewModel.eq.collectAsStateWithLifecycle()
    val eqPresets by viewModel.eqPresets.collectAsStateWithLifecycle()

    PerformanceContent(
        filter = filter,
        delay = delay,
        eq = eq,
        eqPresets = eqPresets,
        actions = PerformanceActions(
            onBack = onBack,
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
        modifier = modifier,
    )
}

@Composable
internal fun PerformanceContent(
    filter: FilterState,
    delay: DelayState,
    eq: EqState,
    eqPresets: List<EqPreset>,
    actions: PerformanceActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        TextButton(onClick = actions.onBack) {
            Text("‹ Now playing")
        }
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
        EqControls(
            state = eq,
            presets = eqPresets,
            onEnabledChange = actions.onEqEnabledChange,
            onGainChange = actions.onEqGainChange,
            onSavePreset = actions.onSaveEqPreset,
            onLoadPreset = actions.onLoadEqPreset,
            onDeletePreset = actions.onDeleteEqPreset,
        )
    }
}
