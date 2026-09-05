package com.example.slipmat.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.slipmat.core.data.db.ArtistSummary

@Composable
fun ArtistListScreen(
    onArtistClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val artists by viewModel.artists.collectAsStateWithLifecycle()

    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(items = artists, key = { it.artist }) { artist ->
            ArtistRow(artist = artist, onClick = { onArtistClick(artist.artist) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun ArtistRow(artist: ArtistSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = artist.artist,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${pluralise(artist.albumCount, "album")} · ${pluralise(artist.trackCount, "track")}",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

internal fun pluralise(count: Int, noun: String): String =
    if (count == 1) "1 $noun" else "$count ${noun}s"
