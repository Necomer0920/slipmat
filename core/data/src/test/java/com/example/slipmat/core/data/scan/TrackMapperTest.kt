package com.example.slipmat.core.data.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val BASE = "content://media/external/audio/media"

/**
 * Every case here is something MediaStore genuinely returns for a badly tagged file, and each one
 * would put visible nonsense in the browse UI if it reached the database unchanged.
 */
class TrackMapperTest {

    private fun raw(
        id: Long = 1L,
        title: String? = "Title",
        artist: String? = "Artist",
        album: String? = "Album",
        durationMs: Long = 60_000,
        displayName: String? = "file.mp3",
    ) = RawTrack(
        id = id,
        title = title,
        artist = artist,
        albumArtist = null,
        album = album,
        albumId = 7L,
        durationMs = durationMs,
        dateModified = 1_000L,
        displayName = displayName,
        data = "/storage/emulated/0/Music/file.mp3",
        relativePath = "Music/",
    )

    @Test
    fun `a well tagged row maps straight through`() {
        val entity = raw().toEntityOrNull(BASE)!!

        assertEquals("Title", entity.title)
        assertEquals("Artist", entity.artist)
        assertEquals("Album", entity.album)
        assertEquals("$BASE/1", entity.uri)
    }

    @Test
    fun `null title falls back to the filename without its extension`() {
        val entity = raw(title = null, displayName = "03 Some Song.mp3").toEntityOrNull(BASE)!!

        assertEquals("03 Some Song", entity.title)
    }

    @Test
    fun `null artist becomes Unknown artist`() {
        assertEquals(UNKNOWN_ARTIST, raw(artist = null).toEntityOrNull(BASE)!!.artist)
    }

    @Test
    fun `the literal unknown sentinel is treated as a missing tag, not as text`() {
        // The bug this guards: checking only for null puts "<unknown>" in the artist list.
        val entity = raw(artist = MEDIASTORE_UNKNOWN, album = MEDIASTORE_UNKNOWN).toEntityOrNull(BASE)!!

        assertEquals(UNKNOWN_ARTIST, entity.artist)
        assertEquals(UNKNOWN_ALBUM, entity.album)
    }

    @Test
    fun `the unknown sentinel is matched regardless of case`() {
        assertEquals(UNKNOWN_ARTIST, raw(artist = "<UNKNOWN>").toEntityOrNull(BASE)!!.artist)
    }

    @Test
    fun `whitespace-only tags count as missing`() {
        assertEquals(UNKNOWN_ARTIST, raw(artist = "   ").toEntityOrNull(BASE)!!.artist)
    }

    @Test
    fun `tags are trimmed`() {
        assertEquals("Artist", raw(artist = "  Artist  ").toEntityOrNull(BASE)!!.artist)
    }

    @Test
    fun `zero duration rows are excluded entirely`() {
        assertNull(raw(durationMs = 0).toEntityOrNull(BASE))
    }

    @Test
    fun `negative duration rows are excluded entirely`() {
        assertNull(raw(durationMs = -1).toEntityOrNull(BASE))
    }

    @Test
    fun `a row with no title and no filename still gets a title`() {
        val entity = raw(title = null, displayName = null).toEntityOrNull(BASE)!!

        assertEquals(UNKNOWN_TITLE, entity.title)
    }
}
