package com.example.slipmat.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.slipmat.core.data.db.FolderSummary
import com.example.slipmat.ui.theme.CornerFolderTile

@Composable
fun FolderListScreen(
    onFolderClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp),
    ) {
        items(items = folders, key = { it.folderPath }) { folder ->
            FolderRow(folder = folder, onClick = { onFolderClick(folder.folderPath) })
        }
    }
}

private val TileSize = 40.dp

@Composable
private fun FolderRow(folder: FolderSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(TileSize),
            shape = RoundedCornerShape(CornerFolderTile),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(
            modifier = Modifier.padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            // The leaf name is what the user recognises; the full path disambiguates the many
            // folders that share one. Both ellipsise from the start, not the end, so a deeply
            // nested path never loses the one segment that identifies it.
            Text(
                text = folder.folderPath.substringAfterLast('/').ifEmpty { folder.folderPath },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${folder.folderPath.ellipsizeFromStart()} · " +
                    pluralise(folder.trackCount, "track"),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Compose's built-in [TextOverflow.Ellipsis] only truncates from the end, which for a folder path
 * hides the leaf directory — the one segment that actually identifies it. Collapsing to the
 * deepest few segments keeps that visible regardless of how nested the path is or how narrow the
 * screen, rather than depending on a pixel-width measurement to truncate the right character.
 */
internal fun String.ellipsizeFromStart(keepSegments: Int = 2): String {
    val segments = trim('/').split('/').filter { it.isNotEmpty() }
    return if (segments.size <= keepSegments) {
        this
    } else {
        "…/" + segments.takeLast(keepSegments).joinToString("/")
    }
}
