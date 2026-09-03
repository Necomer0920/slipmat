package com.example.slipmat.library

import com.example.slipmat.core.data.db.TrackEntity
import com.example.slipmat.core.data.library.ScanState

/**
 * Everything the library screen renders, in one immutable snapshot.
 *
 * Tracks come straight from the repository as [TrackEntity]. That is a deliberate simplification:
 * §4 calls the domain layer optional, and the entity is a plain data class whose fields carry no
 * Room types, so nothing framework-shaped reaches Compose. Contrast the player, where a domain
 * type is mandatory because the alternative drags Media3 onto `:app`'s classpath.
 */
data class LibraryUiState(
    val tracks: List<TrackEntity> = emptyList(),
    val scanState: ScanState = ScanState.Idle,
) {
    val isScanning: Boolean get() = scanState is ScanState.Scanning

    val scanProgress: Float get() = (scanState as? ScanState.Scanning)?.fraction ?: 0f

    /** True only once a scan has finished and genuinely found nothing — not before one runs. */
    val isEmptyAfterScan: Boolean
        get() = tracks.isEmpty() && scanState is ScanState.Complete
}
