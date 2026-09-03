package com.example.slipmat.core.data.library

/**
 * Where a library scan has got to.
 *
 * A first scan of a large library takes long enough to need a progress indicator, and a rescan that
 * finds nothing should be visibly distinguishable from one that never ran.
 */
sealed interface ScanState {

    data object Idle : ScanState

    /** [total] is `0` while the MediaStore query is still running and the size is unknown. */
    data class Scanning(val done: Int, val total: Int) : ScanState {
        val fraction: Float
            get() = if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f
    }

    data class Complete(val trackCount: Int, val changeCount: Int) : ScanState

    data class Failed(val reason: String) : ScanState
}
