package com.example.slipmat.core.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DB = "migration-test"

/**
 * A migration that drops a column silently destroys somebody's library, and the failure only shows
 * up on a device that has the older schema. This runs the real migration against a real v1
 * database and checks the rows survive.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SlipmatDatabase::class.java,
    )

    @Test
    fun migrate1To2KeepsExistingTracks() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                """
                INSERT INTO tracks
                  (id, title, artist, albumArtist, album, albumId, durationMs, uri, folderPath, dateModified, albumArtUri)
                VALUES
                  (1, 'Kept Track', 'Artist', 'Artist', 'Album', 7, 60000, 'content://x/1', '/Music', 100, NULL)
                """.trimIndent(),
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true)

        db.query("SELECT title FROM tracks").use { cursor ->
            assertEquals(1, cursor.count)
            cursor.moveToFirst()
            assertEquals("Kept Track", cursor.getString(0))
        }
        // The new table must exist and start empty.
        db.query("SELECT COUNT(*) FROM playback_positions").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun migrate3To4AddsEqPresetsAndKeepsTheWaveformCache() {
        helper.createDatabase(TEST_DB, 3).use { db ->
            db.execSQL(
                """
                INSERT INTO tracks
                  (id, title, artist, albumArtist, album, albumId, durationMs, uri, folderPath, dateModified, albumArtUri)
                VALUES
                  (1, 'Kept Track', 'A', 'A', 'Al', 7, 60000, 'content://x/1', '/Music', 100, NULL)
                """.trimIndent(),
            )
            db.execSQL(
                "INSERT INTO playback_positions (mediaUri, positionMs, updatedAt) VALUES ('content://x/1', 4242, 1)",
            )
            db.execSQL(
                "INSERT INTO waveforms (mediaUri, peaks, generatedAt) VALUES ('content://x/1', '0.1000,0.9000', 5)",
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 4, true)

        // The waveform cache is the expensive one to lose: every track would decode again.
        db.query("SELECT peaks FROM waveforms").use { cursor ->
            cursor.moveToFirst()
            assertEquals("0.1000,0.9000", cursor.getString(0))
        }
        db.query("SELECT positionMs FROM playback_positions").use { cursor ->
            cursor.moveToFirst()
            assertEquals(4242, cursor.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM eq_presets").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
    }

    @Test
    fun migrate2To3AddsTheWaveformCacheAndKeepsPositions() {
        helper.createDatabase(TEST_DB, 2).use { db ->
            db.execSQL(
                """
                INSERT INTO tracks
                  (id, title, artist, albumArtist, album, albumId, durationMs, uri, folderPath, dateModified, albumArtUri)
                VALUES
                  (1, 'Kept Track', 'A', 'A', 'Al', 7, 60000, 'content://x/1', '/Music', 100, NULL)
                """.trimIndent(),
            )
            db.execSQL(
                "INSERT INTO playback_positions (mediaUri, positionMs, updatedAt) VALUES ('content://x/1', 4242, 1)",
            )
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true)

        // A resume position surviving a schema change is the whole point of migrating rather than
        // recreating: losing it means every track silently restarts.
        db.query("SELECT positionMs FROM playback_positions").use { cursor ->
            cursor.moveToFirst()
            assertEquals(4242, cursor.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM waveforms").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM tracks").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
    }
}
