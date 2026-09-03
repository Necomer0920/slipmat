package com.example.slipmat.core.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs against real SQLite, because that is the only thing that proves the generated queries and
 * the schema agree. A JVM fake would pass regardless.
 */
@RunWith(AndroidJUnit4::class)
class TrackDaoTest {

    private lateinit var database: SlipmatDatabase
    private lateinit var dao: TrackDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            SlipmatDatabase::class.java,
        ).build()
        dao = database.trackDao()
    }

    @After
    fun tearDown() = database.close()

    private fun track(id: Long, title: String = "Track $id", dateModified: Long = 100L) =
        TrackEntity(
            id = id,
            title = title,
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
    fun insertedTracksAreReadBack() = runTest {
        dao.upsertAll(listOf(track(1), track(2), track(3)))

        assertEquals(3, dao.count())
        assertEquals(listOf(1L, 2L, 3L), dao.observeAllTracks().first().map { it.id })
    }

    @Test
    fun upsertReplacesRatherThanDuplicating() = runTest {
        dao.upsertAll(listOf(track(1, title = "Before")))
        dao.upsertAll(listOf(track(1, title = "After")))

        assertEquals(1, dao.count())
        assertEquals("After", dao.observeAllTracks().first().single().title)
    }

    @Test
    fun deleteByIdsRemovesOnlyThoseRows() = runTest {
        dao.upsertAll(listOf(track(1), track(2), track(3)))

        dao.deleteByIds(listOf(1L, 3L))

        assertEquals(listOf(2L), dao.observeAllTracks().first().map { it.id })
    }

    @Test
    fun stampsCarryTheTimestampTheDiffCompares() = runTest {
        dao.upsertAll(listOf(track(1, dateModified = 111L), track(2, dateModified = 222L)))

        val stamps = dao.getAllIdsWithDateModified().associate { it.id to it.dateModified }

        assertEquals(mapOf(1L to 111L, 2L to 222L), stamps)
    }
}
