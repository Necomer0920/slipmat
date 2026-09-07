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
import com.example.slipmat.nowplaying.DelayControls
import com.example.slipmat.nowplaying.EqControls
import com.example.slipmat.nowplaying.FilterControls
import com.example.slipmat.nowplaying.NowPlayingViewModel

/**
 * Filter/Delay/EQ, moved off Now Playing (R2.1) so effects don't compete with playback controls
 * for the same screen. Bare for now — R3.1 refactors this into the stateless
 * PerformanceContent/PerformanceActions shape the rest of the app follows (§5.13); until then this
 * talks to [NowPlayingViewModel] directly, the same instance Now Playing and the queue sheet share.
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        TextButton(onClick = onBack) {
            Text("‹ Now playing")
        }
        FilterControls(
            state = filter,
            onEnabledChange = viewModel::onFilterEnabledChange,
            onCutoffChange = viewModel::onFilterCutoffChange,
            onModeChange = viewModel::onFilterModeChange,
        )
        DelayControls(
            state = delay,
            onEnabledChange = viewModel::onDelayEnabledChange,
            onTimeChange = viewModel::onDelayTimeChange,
            onFeedbackChange = viewModel::onDelayFeedbackChange,
            onMixChange = viewModel::onDelayMixChange,
        )
        EqControls(
            state = eq,
            presets = eqPresets,
            onEnabledChange = viewModel::onEqEnabledChange,
            onGainChange = viewModel::onEqGainChange,
            onSavePreset = viewModel::onSaveEqPreset,
            onLoadPreset = viewModel::onLoadEqPreset,
            onDeletePreset = viewModel::onDeleteEqPreset,
        )
    }
}
