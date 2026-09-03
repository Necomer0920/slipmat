package com.example.slipmat.core.data.scan

import com.example.slipmat.core.data.db.TrackEntity
import com.example.slipmat.core.data.db.TrackStamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanDiffTest {

    private fun track(id: Long, dateModified: Long) = TrackEntity(
        id = id,
        title = "Track $id",
        artist = "Artist",
        albumArtist = "Artist",
        album = "Album",
        albumId = 1L,
        durationMs = 60_000,
        uri = "content://media/external/audio/media/$id",
        folderPath = "/Music",
        dateModified = dateModified,
        albumArtUri = null,
    )

    @Test
    fun `a file not stored yet is an insert`() {
        val diff = computeScanDiff(scanned = listOf(track(1, 100)), stored = emptyList())

        assertEquals(listOf(1L), diff.inserted.map { it.id })
        assertTrue(diff.updated.isEmpty())
        assertTrue(diff.deletedIds.isEmpty())
    }

    @Test
    fun `a file whose timestamp moved is an update, not an insert`() {
        // Retagging a file keeps its MediaStore id and changes dateModified.
        val diff = computeScanDiff(
            scanned = listOf(track(1, 200)),
            stored = listOf(TrackStamp(1, 100)),
        )

        assertEquals(listOf(1L), diff.updated.map { it.id })
        assertTrue(diff.inserted.isEmpty())
    }

    @Test
    fun `an unchanged file produces no work at all`() {
        val diff = computeScanDiff(
            scanned = listOf(track(1, 100)),
            stored = listOf(TrackStamp(1, 100)),
        )

        assertTrue(diff.isEmpty)
        assertEquals(0, diff.changeCount)
    }

    @Test
    fun `a stored file that is gone from the scan is a delete`() {
        val diff = computeScanDiff(scanned = emptyList(), stored = listOf(TrackStamp(9, 100)))

        assertEquals(listOf(9L), diff.deletedIds)
    }

    @Test
    fun `all four outcomes are handled in one pass`() {
        val diff = computeScanDiff(
            scanned = listOf(track(1, 100), track(2, 250), track(4, 400)),
            stored = listOf(
                TrackStamp(1, 100),  // unchanged
                TrackStamp(2, 200),  // retagged
                TrackStamp(3, 300),  // removed from device
            ),
        )

        assertEquals(listOf(4L), diff.inserted.map { it.id })
        assertEquals(listOf(2L), diff.updated.map { it.id })
        assertEquals(listOf(3L), diff.deletedIds)
        assertEquals(3, diff.changeCount)
    }

    @Test
    fun `an empty scan against an empty database is a no-op`() {
        assertTrue(computeScanDiff(emptyList(), emptyList()).isEmpty)
    }

    @Test
    fun `a first scan of a full library is all inserts`() {
        val scanned = (1L..50L).map { track(it, it * 10) }

        val diff = computeScanDiff(scanned, stored = emptyList())

        assertEquals(50, diff.inserted.size)
        assertTrue(diff.updated.isEmpty())
        assertTrue(diff.deletedIds.isEmpty())
    }

    @Test
    fun `clearing the device deletes everything stored`() {
        val stored = (1L..50L).map { TrackStamp(it, it * 10) }

        val diff = computeScanDiff(scanned = emptyList(), stored = stored)

        assertEquals(50, diff.deletedIds.size)
    }
}
