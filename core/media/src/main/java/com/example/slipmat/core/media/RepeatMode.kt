package com.example.slipmat.core.media

/**
 * Repeat behaviour, as the UI understands it.
 *
 * Deliberately our own enum rather than Media3's `Player.REPEAT_MODE_*` integers: putting those on
 * [PlayerState] would drag Media3 onto `:app`'s compile classpath and dissolve the module boundary
 * exactly as an exposed `MediaController` would.
 */
enum class RepeatMode {
    Off,
    All,
    One,
    ;

    /** Cycle order used by the single repeat button: off → all → one → off. */
    fun next(): RepeatMode = when (this) {
        Off -> All
        All -> One
        One -> Off
    }
}

/**
 * How far the skip-seek buttons jump.
 *
 * Ten rather than fifteen seconds so the buttons can carry Material's `Replay10` / `Forward10`
 * icons, which state the interval on their face. There is no 15-second icon, and a button
 * labelled "10" that jumps 15 is worse than either honest choice.
 */
const val SKIP_SEEK_MS = 10_000L

/**
 * Where a skip-seek should land.
 *
 * Clamps to the track rather than letting a seek run negative or past the end — Media3 tolerates
 * both, but the first rewinds to zero silently and the second ends the track, neither of which is
 * what pressing "forward 15 seconds" near the end should do.
 *
 * A non-positive [durationMs] means the duration is not known yet, in which case only the lower
 * bound can be enforced.
 */
fun skipSeekTarget(currentMs: Long, durationMs: Long, deltaMs: Long): Long {
    val target = currentMs + deltaMs
    return when {
        target < 0L -> 0L
        durationMs > 0L && target > durationMs -> durationMs
        else -> target
    }
}
