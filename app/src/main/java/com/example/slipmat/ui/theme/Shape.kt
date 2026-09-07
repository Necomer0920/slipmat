package com.example.slipmat.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * §3.4's corner scale. Five of its ten values line up with M3's own extraSmall..extraLarge
 * ordering and are wired into [SlipmatShapes]; the rest have no M3 slot to sit in and are exposed
 * as named constants for the components that use them directly.
 */
val CornerExtraSmall: Dp = 8.dp // list art, thumbnail
val CornerSmall: Dp = 12.dp // list row
val CornerMedium: Dp = 16.dp // mini-player card
val CornerLarge: Dp = 20.dp // artwork, pill tab, pill chip
val CornerExtraLarge: Dp = 24.dp // queue sheet top corners

val CornerFolderTile: Dp = 10.dp // folder tile, filter-type strip
val CornerAlbumCell: Dp = 14.dp // album cell, sleep menu
val CornerTempoCard: Dp = 18.dp // tempo card
val CornerPerformanceDoor: Dp = 22.dp // Performance door

val FolderTileShape: Shape = RoundedCornerShape(CornerFolderTile)
val AlbumCellShape: Shape = RoundedCornerShape(CornerAlbumCell)
val TempoCardShape: Shape = RoundedCornerShape(CornerTempoCard)
val PerformanceDoorShape: Shape = RoundedCornerShape(CornerPerformanceDoor)

/** Avatars, thumbs, the play button — full round, so just Compose's own [CircleShape]. */
val FullRoundShape: Shape = CircleShape

val SlipmatShapes = Shapes(
    extraSmall = RoundedCornerShape(CornerExtraSmall),
    small = RoundedCornerShape(CornerSmall),
    medium = RoundedCornerShape(CornerMedium),
    large = RoundedCornerShape(CornerLarge),
    extraLarge = RoundedCornerShape(CornerExtraLarge),
)
