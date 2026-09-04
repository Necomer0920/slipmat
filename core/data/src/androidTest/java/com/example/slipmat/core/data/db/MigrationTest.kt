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
}
