package com.example.slipmat.nowplaying

import androidx.lifecycle.ViewModel
import com.example.slipmat.core.media.PlaybackController
import com.example.slipmat.core.media.PlayerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * Mirrors playback state for the UI and forwards commands to the service.
 *
 * Holds no state of its own: [state] is the controller's flow, unchanged. Anything cached here
 * would be a second source of truth that a process death could silently desynchronise.
 */
@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val playback: PlaybackController,
) : ViewModel() {

    val state: StateFlow<PlayerState> = playback.state

    fun togglePlayPause() {
        if (state.value.isPlaying) playback.pause() else playback.play()
    }

    fun next() = playback.next()

    fun previous() = playback.previous()

    fun seekTo(positionMs: Long) = playback.seekTo(positionMs)
}
