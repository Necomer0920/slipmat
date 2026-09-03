package com.example.slipmat.core.data.scan

import android.content.Context
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Reads the device's audio index. Knows nothing about Room.
 *
 * Columns are read through [android.database.Cursor.getColumnIndex], which returns `-1` for a
 * column the provider does not have, rather than `getColumnIndexOrThrow`. minSdk here is 28 and
 * some columns arrived later, so a hard lookup would crash on older devices for a field we are
 * happy to treat as absent.
 */
class MediaStoreScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun queryAudio(): List<RawTrack> {
        val projection = buildList {
            add(MediaStore.Audio.Media._ID)
            add(MediaStore.Audio.Media.TITLE)
            add(MediaStore.Audio.Media.ARTIST)
            add(MediaStore.Audio.Media.ALBUM)
            add(MediaStore.Audio.Media.ALBUM_ID)
            add(MediaStore.Audio.Media.DURATION)
            add(MediaStore.Audio.Media.DATE_MODIFIED)
            add(MediaStore.Audio.Media.DISPLAY_NAME)
            @Suppress("DEPRECATION")
            add(MediaStore.Audio.Media.DATA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Audio.Media.RELATIVE_PATH)
            }
        }.toTypedArray()

        // IS_MUSIC excludes ringtones, alarms and notification sounds, which are indexed here too.
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        val cursor = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC",
        ) ?: return emptyList()

        return cursor.use { c ->
            val id = c.getColumnIndex(MediaStore.Audio.Media._ID)
            val title = c.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val artist = c.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val album = c.getColumnIndex(MediaStore.Audio.Media.ALBUM)
            val albumId = c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
            val duration = c.getColumnIndex(MediaStore.Audio.Media.DURATION)
            val dateModified = c.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
            val displayName = c.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
            @Suppress("DEPRECATION")
            val data = c.getColumnIndex(MediaStore.Audio.Media.DATA)
            val relativePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                c.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
            } else {
                -1
            }

            buildList(c.count) {
                while (c.moveToNext()) {
                    add(
                        RawTrack(
                            id = if (id >= 0) c.getLong(id) else continue,
                            title = title.takeIf { it >= 0 }?.let(c::getStringOrNull),
                            artist = artist.takeIf { it >= 0 }?.let(c::getStringOrNull),
                            albumArtist = null,
                            album = album.takeIf { it >= 0 }?.let(c::getStringOrNull),
                            albumId = if (albumId >= 0) c.getLong(albumId) else 0L,
                            durationMs = if (duration >= 0) c.getLong(duration) else 0L,
                            dateModified = if (dateModified >= 0) c.getLong(dateModified) else 0L,
                            displayName = displayName.takeIf { it >= 0 }?.let(c::getStringOrNull),
                            data = data.takeIf { it >= 0 }?.let(c::getStringOrNull),
                            relativePath = relativePath.takeIf { it >= 0 }?.let(c::getStringOrNull),
                        ),
                    )
                }
            }
        }
    }
}

private fun android.database.Cursor.getStringOrNull(index: Int): String? =
    if (isNull(index)) null else getString(index)
