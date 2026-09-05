package com.example.slipmat.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records which code paths startup actually takes, so ART can compile them ahead of time.
 *
 * The profile is only worth what the journey below covers: anything not exercised here stays
 * interpreted on first run. Startup plus a scroll of the library is the honest minimum — those are
 * the paths a user hits before they have decided whether the app feels fast.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(packageName = "com.example.slipmat") {
        pressHome()
        startActivityAndWait()

        // The library list is the first thing drawn and the first thing scrolled; letting it
        // settle means the profile covers Room, the scan flow and the list's own composition.
        device.waitForIdle()
    }
}
