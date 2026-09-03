package com.example.slipmat.core.data.library

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.slipmat.core.data.db.SlipmatDatabase
import com.example.slipmat.core.data.scan.AudioSource
import com.example.slipmat.core.data.scan.RawTrack
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers the scan-and-apply path against real SQLite: the code that can actually damage someone's
 * library by deleting rows it should have kept.
 */
@RunWith(AndroidJUnit4::class)
class LibraryScanTest {

    private lateinit var database: SlipmatDatabase
    private var rows: List<RawTrack> = emptyList()
    private lateinit var repository: LibraryRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SlipmatDatabase::class.java,
        ).build()
        repository = LibraryRepository(
            scanner = AudioSource { rows },
            trackDao = database.trackDao(),
            database = database,
        )
    }

    @After
    fun tearDown() = database.close()

    private fun row(id: Long, dateModified: Long = 100L, title: String = "Track $id") = RawTrack(
        id = id,
        title = title,
        artist = "Artist",
        albumArtist = null,
        album = "Album",
        albumId = 1L,
        durationMs = 60_000,
        dateModified = dateModified,
        displayName = "$id.mp3",
        data = "/storage/emulated/0/Music/$id.mp3",
        relativePath = "Music/",
    )

    @Test
    fun firstScanInsertsEverything() = runTest {
        rows = listOf(row(1), row(2), row(3))

        val diff = repository.scanIncremental()

        assertEquals(3, diff.inserted.size)
        assertEquals(3, database.trackDao().count())
    }

    @Test
    fun rescanWithNothingChangedWritesNothing() = runTest {
        rows = listOf(row(1), row(2))
        repository.scanIncremental()

        val second = repository.scanIncremental()

        assertEquals(0, second.changeCount)
        assertEquals(2, database.trackDao().count())
    }

    @Test
    fun aRetaggedFileIsUpdatedInPlaceRatherThanDuplicated() = runTest {
        rows = listOf(row(1, dateModified = 100L, title = "Before"))
        repository.scanIncremental()

        rows = listOf(row(1, dateModified = 200L, title = "After"))
        val diff = repository.scanIncremental()

        assertEquals(1, diff.updated.size)
        assertEquals(1, database.trackDao().count())
        assertEquals("After", database.trackDao().observeAllTracks().first().single().title)
    }

    @Test
    fun aFileRemovedFromTheDeviceIsDeleted() = runTest {
        rows = listOf(row(1), row(2), row(3))
        repository.scanIncremental()

        rows = listOf(row(1), row(3))
        val diff = repository.scanIncremental()

        assertEquals(listOf(2L), diff.deletedIds)
        assertEquals(2, database.trackDao().count())
    }

    @Test
    fun zeroDurationRowsNeverReachTheDatabase() = runTest {
        rows = listOf(row(1), row(2).copy(durationMs = 0))

        repository.scanIncremental()

        assertEquals(1, database.trackDao().count())
    }

    @Test
    fun aLibraryLargerThanTheSqliteParameterLimitIsWrittenAndDeletedInFull() = runTest {
        // 2000 rows exceeds SQLite's 999-host-parameter cap; without chunking this throws.
        rows = (1L..2000L).map { row(it) }
        repository.scanIncremental()
        assertEquals(2000, database.trackDao().count())

        rows = emptyList()
        repository.scanIncremental()
        assertEquals(0, database.trackDao().count())
    }
}
