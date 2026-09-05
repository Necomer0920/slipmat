package com.example.slipmat.benchmark

import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cold start, measured on the real device against a non-debuggable build.
 *
 * Cold is the number that matters and the one people quote: the process does not exist, so it
 * covers Application creation, Hilt's graph, the Activity, and first frame. Warm and hot starts
 * mostly measure the framework.
 *
 * Ten iterations because startup is noisy — thermal state and whatever else the phone is doing
 * move a single run by tens of milliseconds, and a single run is how "improvements" get claimed
 * that are really just variance.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStartup() = benchmarkRule.measureRepeated(
        packageName = "com.example.slipmat",
        metrics = listOf(StartupTimingMetric()),
        iterations = 10,
        startupMode = StartupMode.COLD,
    ) {
        pressHome()
        startActivityAndWait()
    }
}
