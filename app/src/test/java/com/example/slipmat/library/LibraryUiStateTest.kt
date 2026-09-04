package com.example.slipmat.library

import com.example.slipmat.core.data.db.TrackEntity
import com.example.slipmat.core.data.library.ScanState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryUiStateTest {

    private fun track(id: Long) = TrackEntity(
        id = id, title = "T$id", artist = "A", albumArtist = "A", album = "Al", albumId = 1,
        durationMs = 1000, uri = "u$id", folderPath = "/M", dateModified = 0, albumArtUri = null,
    )

    @Test
    fun `an empty library before any scan is not reported as empty`() {
        // Reporting "no music found" before looking would be a lie, and it is what the user sees
        // for the whole of a first scan.
        val state = LibraryUiState(tracks = emptyList(), scanState = ScanState.Idle)

        assertFalse(state.isEmptyAfterScan)
    }

    @Test
    fun `an empty library mid-scan is still not reported as empty`() {
        val state = LibraryUiState(tracks = emptyList(), scanState = ScanState.Scanning(10, 100))

        assertFalse(state.isEmptyAfterScan)
    }

    @Test
    fun `an empty library after a completed scan is genuinely empty`() {
        val state = LibraryUiState(tracks = emptyList(), scanState = ScanState.Complete(0, 0))

        assertTrue(state.isEmptyAfterScan)
    }

    @Test
    fun `a library with tracks is never reported as empty`() {
        val state = LibraryUiState(tracks = listOf(track(1)), scanState = ScanState.Complete(1, 1))

        assertFalse(state.isEmptyAfterScan)
    }

    @Test
    fun `progress reflects how far the scan has got`() {
        val state = LibraryUiState(scanState = ScanState.Scanning(done = 25, total = 100))

        assertTrue(state.isScanning)
        assertEquals(0.25f, state.scanProgress, 0.001f)
    }

    @Test
    fun `progress is zero while the total is still unknown`() {
        // The scanner reports total 0 until the MediaStore query returns.
        val state = LibraryUiState(scanState = ScanState.Scanning(done = 0, total = 0))

        assertEquals(0f, state.scanProgress, 0f)
    }

    @Test
    fun `progress is zero when no scan is running`() {
        assertEquals(0f, LibraryUiState(scanState = ScanState.Idle).scanProgress, 0f)
        assertFalse(LibraryUiState(scanState = ScanState.Idle).isScanning)
    }
}
