package com.example.slipmat.library

import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.slipmat.core.data.scan.MEDIASTORE_UNKNOWN
import com.example.slipmat.core.data.scan.MediaStoreScanner
import com.example.slipmat.core.data.scan.UNKNOWN_ALBUM
import com.example.slipmat.core.data.scan.UNKNOWN_ARTIST
import com.example.slipmat.core.data.scan.UNKNOWN_TITLE
import com.example.slipmat.core.data.scan.toEntityOrNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

private const val TAG = "RealLibraryScan"

/**
 * Runs the scanner against whatever is actually on this device.
 *
 * Lives in `:app` rather than `:core:data` deliberately: it needs `READ_MEDIA_AUDIO`, and this
 * device refuses every programmatic grant — `pm grant`, `appops set`, and even
 * `UiAutomation.grantRuntimePermission` via `GrantPermissionRule`. Running under the app's own
 * package is the only way to inherit a permission the user granted by hand.
 *
 * Counts are logged rather than asserted, since the library changes. The assertions are invariants
 * that must hold for any library, however badly tagged.
 */
@RunWith(AndroidJUnit4::class)
class RealLibraryScanTest {

    @Test
    fun scanTheRealLibraryAndReportWhatItFound() {
        val scanner = MediaStoreScanner(ApplicationProvider.getApplicationContext())

        val raw = scanner.queryAudio()
        val mapped = raw.mapNotNull { it.toEntityOrNull("content://media/external/audio/media") }

        Log.i(TAG, "RAW_ROWS=${raw.size}")
        Log.i(TAG, "MAPPED=${mapped.size}")
        Log.i(TAG, "DROPPED=${raw.size - mapped.size}")
        Log.i(TAG, "UNKNOWN_TITLE=${mapped.count { it.title == UNKNOWN_TITLE }}")
        Log.i(TAG, "UNKNOWN_ARTIST=${mapped.count { it.artist == UNKNOWN_ARTIST }}")
        Log.i(TAG, "UNKNOWN_ALBUM=${mapped.count { it.album == UNKNOWN_ALBUM }}")
        Log.i(TAG, "ALBUM_ARTIST_DIFFERS=${mapped.count { it.albumArtist != it.artist }}")
        Log.i(TAG, "NO_ARTWORK=${mapped.count { it.albumArtUri == null }}")
        Log.i(TAG, "DISTINCT_ALBUMS=${mapped.map { it.album to it.albumArtist }.distinct().size}")
        Log.i(TAG, "DISTINCT_FOLDERS=${mapped.map { it.folderPath }.distinct().size}")

        mapped.take(6).forEach {
            Log.i(TAG, "SAMPLE | ${it.title} | artist=${it.artist} | albumArtist=${it.albumArtist} | album=${it.album}")
        }
        mapped.filter { it.albumArtist != it.artist }.take(4).forEach {
            Log.i(TAG, "COMPILATION | ${it.title} | artist=${it.artist} | albumArtist=${it.albumArtist} | album=${it.album}")
        }
        mapped.map { it.folderPath }.distinct().take(8).forEach { Log.i(TAG, "FOLDER | $it") }

        assertTrue("scanner found nothing at all", mapped.isNotEmpty())
        assertTrue("a title was left blank", mapped.none { it.title.isBlank() })
        assertTrue("an artist was left blank", mapped.none { it.artist.isBlank() })
        assertTrue("an album was left blank", mapped.none { it.album.isBlank() })
        assertTrue("the <unknown> sentinel leaked through", mapped.none { it.artist == MEDIASTORE_UNKNOWN })
        assertTrue("a non-positive duration was kept", mapped.all { it.durationMs > 0 })
        assertTrue("album artist was left empty", mapped.none { it.albumArtist.isBlank() })
    }
}
