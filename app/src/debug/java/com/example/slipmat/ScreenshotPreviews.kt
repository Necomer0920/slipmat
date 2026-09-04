package com.example.slipmat

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.slipmat.core.data.db.TrackEntity
import com.example.slipmat.core.data.library.ScanState
import com.example.slipmat.core.media.PlayerState
import com.example.slipmat.core.media.RepeatMode
import com.example.slipmat.core.media.SleepTimerState
import com.example.slipmat.library.LibraryContent
import com.example.slipmat.library.LibraryUiState
import com.example.slipmat.nowplaying.NowPlayingActions
import com.example.slipmat.nowplaying.NowPlayingContent
import com.example.slipmat.ui.theme.SlipmatTheme

/**
 * Reference screenshots for the two screens a user actually looks at.
 *
 * Fixed sample data rather than anything read from the device: a golden image has to be
 * byte-identical run to run, so it cannot depend on what happens to be in the library.
 */
private fun sampleTracks() = List(6) { index ->
    TrackEntity(
        id = index.toLong(),
        title = listOf(
            "Long Distance Love", "Burn! Masked Rider", "Shine! 8 Riders",
            "BELIEVE YOURSELF", "DEEP BREATH", "Amazon DaDaDa!!",
        )[index],
        artist = "Ichiro Mizuki",
        albumArtist = "Various Artists",
        album = "Theme Song Complete Works",
        albumId = 1L,
        durationMs = 205_000 + index * 7_000L,
        uri = "content://media/external/audio/media/$index",
        folderPath = "/storage/emulated/0/Music",
        dateModified = 0L,
        albumArtUri = null,
    )
}

@Preview(name = "Library", showBackground = true)
@Composable
fun LibraryScreenPreview() {
    SlipmatTheme {
        LibraryContent(
            state = LibraryUiState(
                tracks = sampleTracks(),
                scanState = ScanState.Complete(trackCount = 6, changeCount = 0),
            ),
            onTrackClick = {},
        )
    }
}

@Preview(name = "Library scanning", showBackground = true)
@Composable
fun LibraryScanningPreview() {
    SlipmatTheme {
        LibraryContent(
            state = LibraryUiState(
                tracks = sampleTracks(),
                scanState = ScanState.Scanning(done = 40, total = 100),
            ),
            onTrackClick = {},
        )
    }
}

@Preview(name = "Library empty", showBackground = true)
@Composable
fun LibraryEmptyPreview() {
    SlipmatTheme {
        LibraryContent(
            state = LibraryUiState(
                tracks = emptyList(),
                scanState = ScanState.Complete(trackCount = 0, changeCount = 0),
            ),
            onTrackClick = {},
        )
    }
}

@Preview(name = "Now playing", showBackground = true, heightDp = 900)
@Composable
fun NowPlayingPreview() {
    SlipmatTheme {
        NowPlayingContent(
            state = PlayerState(
                isPlaying = true,
                positionMs = 62_000,
                durationMs = 205_000,
                mediaId = "1",
                title = "\"Masked Rider (Skyrider)\" Long Distance Love",
                artist = "Ichiro Mizuki and Kōrogi '73",
                shuffleEnabled = true,
                repeatMode = RepeatMode.All,
            ),
            sleepTimer = SleepTimerState.Idle,
            actions = NowPlayingActions(),
        )
    }
}

@Preview(name = "Now playing with sleep timer", showBackground = true, heightDp = 900)
@Composable
fun NowPlayingSleepTimerPreview() {
    SlipmatTheme {
        NowPlayingContent(
            state = PlayerState(
                isPlaying = true,
                positionMs = 121_000,
                durationMs = 205_000,
                mediaId = "1",
                title = "BELIEVE YOURSELF",
                artist = "Naoto Fuuga",
            ),
            sleepTimer = SleepTimerState.Running(remainingMs = 8_000),
            actions = NowPlayingActions(),
        )
    }
}
