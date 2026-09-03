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
 * The browse lists are `GROUP BY` results rather than stored rows, so the grouping rules are the
 * thing to test. The compilation case below is the one that goes wrong in most local players.
 */
@RunWith(AndroidJUnit4::class)
class BrowseQueriesTest {

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

    private fun track(
        id: Long,
        artist: String,
        albumArtist: String,
        album: String,
        folderPath: String = "/Music",
    ) = TrackEntity(
        id = id,
        title = "Track $id",
        artist = artist,
        albumArtist = albumArtist,
        album = album,
        albumId = 1L,
        durationMs = 60_000,
        uri = "content://media/external/audio/media/$id",
        folderPath = folderPath,
        dateModified = 0L,
        albumArtUri = "content://media/external/audio/albumart/1",
    )

    @Test
    fun aCompilationStaysOneAlbumDespiteDifferentPerTrackArtists() = runTest {
        dao.upsertAll(
            listOf(
                track(1, artist = "Singer A", albumArtist = "Various Artists", album = "Hits"),
                track(2, artist = "Singer B", albumArtist = "Various Artists", album = "Hits"),
                track(3, artist = "Singer C", albumArtist = "Various Artists", album = "Hits"),
            ),
        )

        val albums = dao.observeAlbums().first()

        assertEquals(1, albums.size)
        assertEquals("Hits", albums.single().album)
        assertEquals(3, albums.single().trackCount)
    }

    @Test
    fun theArtistListGroupsOnAlbumArtistToo() = runTest {
        dao.upsertAll(
            listOf(
                track(1, artist = "Singer A", albumArtist = "Various Artists", album = "Hits"),
                track(2, artist = "Singer B", albumArtist = "Various Artists", album = "Hits"),
                track(3, artist = "Solo", albumArtist = "Solo", album = "Debut"),
            ),
        )

        val artists = dao.observeArtists().first()

        assertEquals(listOf("Solo", "Various Artists"), artists.map { it.artist }.sorted())
        assertEquals(2, artists.first { it.artist == "Various Artists" }.trackCount)
    }

    @Test
    fun distinctAlbumsAreCountedPerArtist() = runTest {
        dao.upsertAll(
            listOf(
                track(1, artist = "Solo", albumArtist = "Solo", album = "First"),
                track(2, artist = "Solo", albumArtist = "Solo", album = "Second"),
                track(3, artist = "Solo", albumArtist = "Solo", album = "Second"),
            ),
        )

        assertEquals(2, dao.observeArtists().first().single().albumCount)
    }

    @Test
    fun foldersAreListedWithTheirCountsAndEmptyPathsExcluded() = runTest {
        dao.upsertAll(
            listOf(
                track(1, "A", "A", "Al", folderPath = "/Music/Rock"),
                track(2, "B", "B", "Al", folderPath = "/Music/Rock"),
                track(3, "C", "C", "Al", folderPath = "/Download"),
                track(4, "D", "D", "Al", folderPath = ""),
            ),
        )

        val folders = dao.observeFolders().first()

        assertEquals(listOf("/Download", "/Music/Rock"), folders.map { it.folderPath })
        assertEquals(2, folders.first { it.folderPath == "/Music/Rock" }.trackCount)
    }

    @Test
    fun tracksCanBeListedWithinOneAlbum() = runTest {
        dao.upsertAll(
            listOf(
                track(1, "Singer A", "Various Artists", "Hits"),
                track(2, "Singer B", "Various Artists", "Hits"),
                track(3, "Solo", "Solo", "Debut"),
            ),
        )

        val inAlbum = dao.observeTracksInAlbum("Hits", "Various Artists").first()

        assertEquals(listOf(1L, 2L), inAlbum.map { it.id })
    }
}
