package com.example.slipmat.core.data.scan

import com.example.slipmat.core.data.db.TrackEntity
import com.example.slipmat.core.data.db.TrackStamp

/**
 * What a rescan needs to change, and nothing more.
 *
 * Rewriting every row on every scan works but churns the database and invalidates every Flow
 * watching it, so a library of several thousand tracks visibly stutters. This narrows a rescan to
 * the files that actually moved.
 */
data class ScanDiff(
    val inserted: List<TrackEntity>,
    val updated: List<TrackEntity>,
    val deletedIds: List<Long>,
) {
    val isEmpty: Boolean
        get() = inserted.isEmpty() && updated.isEmpty() && deletedIds.isEmpty()

    val changeCount: Int
        get() = inserted.size + updated.size + deletedIds.size
}

/**
 * Compares a fresh scan against what is already stored.
 *
 * Identity is MediaStore's id; freshness is `dateModified`. A file whose tags were edited keeps its
 * id but gets a new timestamp, which is what separates "update" from "insert".
 *
 * Pure, so the rules are testable without a database.
 */
fun computeScanDiff(scanned: List<TrackEntity>, stored: List<TrackStamp>): ScanDiff {
    val storedDates: Map<Long, Long> = stored.associate { it.id to it.dateModified }
    val scannedIds = HashSet<Long>(scanned.size)

    val inserted = ArrayList<TrackEntity>()
    val updated = ArrayList<TrackEntity>()

    for (track in scanned) {
        scannedIds.add(track.id)
        val previous = storedDates[track.id]
        when {
            previous == null -> inserted.add(track)
            previous != track.dateModified -> updated.add(track)
            else -> Unit // unchanged; leave it alone
        }
    }

    val deletedIds = stored.map { it.id }.filterNot { it in scannedIds }

    return ScanDiff(inserted = inserted, updated = updated, deletedIds = deletedIds)
}
