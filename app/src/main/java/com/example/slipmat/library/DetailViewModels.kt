package com.example.slipmat.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.slipmat.core.data.db.TrackEntity
import com.example.slipmat.core.data.library.LibraryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val STOP_TIMEOUT_MS = 5_000L

/**
 * Route argument names. Kept here so the nav graph and the view models cannot drift apart —
 * a mismatch is a runtime crash with a message that does not name the culprit.
 */
object DetailArgs {
    const val ARTIST = "artist"
    const val ALBUM = "album"
    const val ALBUM_ARTIST = "albumArtist"
    const val FOLDER = "folderPath"
}

private fun Flow<List<TrackEntity>>.asTrackState(vm: ViewModel): StateFlow<List<TrackEntity>> =
    stateIn(vm.viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    repository: LibraryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val artist: String = checkNotNull(savedStateHandle[DetailArgs.ARTIST])
    val tracks: StateFlow<List<TrackEntity>> =
        repository.observeTracksByArtist(artist).asTrackState(this)
}

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    repository: LibraryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val album: String = checkNotNull(savedStateHandle[DetailArgs.ALBUM])
    val albumArtist: String = checkNotNull(savedStateHandle[DetailArgs.ALBUM_ARTIST])
    val tracks: StateFlow<List<TrackEntity>> =
        repository.observeTracksInAlbum(album, albumArtist).asTrackState(this)
}

@HiltViewModel
class FolderDetailViewModel @Inject constructor(
    repository: LibraryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val folderPath: String = checkNotNull(savedStateHandle[DetailArgs.FOLDER])
    val tracks: StateFlow<List<TrackEntity>> =
        repository.observeTracksInFolder(folderPath).asTrackState(this)
}
