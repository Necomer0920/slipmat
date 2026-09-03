package com.example.slipmat.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.slipmat.core.data.library.LibraryRepository
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

    /** Safe to call repeatedly; an unchanged library produces no database writes. */
    fun refresh() {
        viewModelScope.launch { repository.scanIncremental() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
