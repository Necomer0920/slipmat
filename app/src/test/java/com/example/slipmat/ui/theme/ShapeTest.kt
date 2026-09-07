package com.example.slipmat.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class ShapeTest {

    @Test
    fun `the five M3 slots carry their named corner radius`() {
        assertEquals(8.dp, CornerExtraSmall)
        assertEquals(12.dp, CornerSmall)
        assertEquals(16.dp, CornerMedium)
        assertEquals(20.dp, CornerLarge)
        assertEquals(24.dp, CornerExtraLarge)
    }

    @Test
    fun `the four values with no M3 slot carry their named corner radius`() {
        assertEquals(10.dp, CornerFolderTile)
        assertEquals(14.dp, CornerAlbumCell)
        assertEquals(18.dp, CornerTempoCard)
        assertEquals(22.dp, CornerPerformanceDoor)
    }
}
