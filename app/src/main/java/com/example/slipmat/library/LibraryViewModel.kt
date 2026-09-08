package com.example.slipmat.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.slipmat.core.data.db.AlbumSummary
import com.example.slipmat.core.data.db.ArtistSummary
import com.example.slipmat.core.data.db.FolderSummary
import com.example.slipmat.core.data.library.LibraryRepository
import com.example.slipmat.core.data.library.ScanState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: LibraryRepository,
) : ViewModel() {

    val uiState: StateFlow<LibraryUiState> =
        combine(repository.observeTracks(), repository.scanState) { tracks, scan ->
            LibraryUiState(tracks = tracks, scanState = scan)
        }.stateIn(
            scope = viewModelScope,
            // Keep collecting briefly across configuration changes so a rotation does not
            // re-query the whole library.
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = LibraryUiState(),
        )

    // Each list is its own flow, so opening the album tab never runs the artist query. Room
    // flows are lazy and WhileSubscribed stops them when nothing is looking.
    val artists: StateFlow<List<ArtistSummary>> = repository.observeArtists().asList()
    val albums: StateFlow<List<AlbumSummary>> = repository.observeAlbums().asList()
    val folders: StateFlow<List<FolderSummary>> = repository.observeFolders().asList()

    // Already a hot StateFlow on the repository, so exposing it here piggybacks on nothing -
    // it does not pull the track query in behind it the way combining with uiState would.
    val scanState: StateFlow<ScanState> = repository.scanState

    private fun <T> kotlinx.coroutines.flow.Flow<List<T>>.asList(): StateFlow<List<T>> = stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = emptyList(),
    )

    /** Safe to call repeatedly; an unchanged library produces no database writes. */
    fun refresh() {
        viewModelScope.launch { repository.scanIncremental() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
