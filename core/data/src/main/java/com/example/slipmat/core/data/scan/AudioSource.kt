package com.example.slipmat.core.data.scan

/**
 * Where raw audio rows come from.
 *
 * Exists so the scan-and-apply path can be tested against a controlled set of rows. Depending on
 * [MediaStoreScanner] directly would make the diff-application logic — the part that can actually
 * corrupt someone's library — only testable against whatever happens to be on the device.
 */
fun interface AudioSource {
    fun queryAudio(): List<RawTrack>
}
