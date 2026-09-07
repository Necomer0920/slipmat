package com.example.slipmat.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class BlueprintGridTest {

    @Test
    fun `the first line falls at the -1dp offset`() {
        assertEquals(-1f, gridLinePosition(0), 0.001f)
    }

    @Test
    fun `the nth line falls at 22n minus 1`() {
        for (n in 1..10) {
            assertEquals((22 * n - 1).toFloat(), gridLinePosition(n), 0.001f)
        }
    }
}
