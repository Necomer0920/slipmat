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

/** Folder browsing groups on this string, so inconsistent shapes fragment the folder list. */
class FolderPathTest {

    @Test
    fun `absolute path yields its parent directory`() {
        assertEquals(
            "/storage/emulated/0/Music/Soulseek",
            folderPathOf("/storage/emulated/0/Music/Soulseek/track.flac", null),
        )
    }

    @Test
    fun `relative path is used when the absolute path is missing`() {
        assertEquals("Music/Soulseek", folderPathOf(null, "Music/Soulseek/"))
    }

    @Test
    fun `the trailing slash on a relative path is dropped`() {
        assertEquals("Music", folderPathOf(null, "Music/"))
    }

    @Test
    fun `absolute path wins when both are present`() {
        assertEquals("/storage/emulated/0/Download", folderPathOf("/storage/emulated/0/Download/a.mp3", "Download/"))
    }

    @Test
    fun `a bare filename with no directory falls through to the relative path`() {
        assertEquals("Music", folderPathOf("track.mp3", "Music/"))
    }

    @Test
    fun `no path information at all yields an empty string, never null`() {
        assertEquals("", folderPathOf(null, null))
    }
}

/**
 * Compilations are the case this exists for: twelve tracks sharing one album but each tagged with
 * a different artist. Grouping on artist turns that into twelve one-track albums.
 */
class AlbumArtistTest {

    private fun raw(artist: String?, albumArtist: String?) = RawTrack(
        id = 1L,
        title = "Title",
        artist = artist,
        albumArtist = albumArtist,
        album = "Various Hits",
        albumId = 7L,
        durationMs = 60_000,
        dateModified = 0L,
        displayName = "file.mp3",
        data = null,
        relativePath = null,
    )

    @Test
    fun `album artist is used when the file has one`() {
        val entity = raw(artist = "Guest Vocalist", albumArtist = "Various Artists")
            .toEntityOrNull(BASE)!!

        assertEquals("Various Artists", entity.albumArtist)
        assertEquals("Guest Vocalist", entity.artist)
    }

    @Test
    fun `album artist falls back to artist when absent`() {
        val entity = raw(artist = "Solo Act", albumArtist = null).toEntityOrNull(BASE)!!

        assertEquals("Solo Act", entity.albumArtist)
    }

    @Test
    fun `the unknown sentinel in album artist falls back to artist`() {
        val entity = raw(artist = "Solo Act", albumArtist = MEDIASTORE_UNKNOWN).toEntityOrNull(BASE)!!

        assertEquals("Solo Act", entity.albumArtist)
    }

    @Test
    fun `both blank yields Unknown artist in both fields`() {
        val entity = raw(artist = "  ", albumArtist = null).toEntityOrNull(BASE)!!

        assertEquals(UNKNOWN_ARTIST, entity.artist)
        assertEquals(UNKNOWN_ARTIST, entity.albumArtist)
    }
}

class AlbumArtUriTest {

    private fun raw(albumId: Long) = RawTrack(
        id = 1L, title = "T", artist = "A", albumArtist = null, album = "Al",
        albumId = albumId, durationMs = 1_000, dateModified = 0L,
        displayName = null, data = null, relativePath = null,
    )

    @Test
    fun `a real album id produces an artwork uri`() {
        assertEquals("$ALBUM_ART_URI_BASE/42", raw(42L).toEntityOrNull(BASE)!!.albumArtUri)
    }

    @Test
    fun `a missing album id yields null rather than a uri pointing at nothing`() {
        // Zero is what MediaStore reports when it has no album; a uri built from it 404s.
        assertNull(raw(0L).toEntityOrNull(BASE)!!.albumArtUri)
    }
}
