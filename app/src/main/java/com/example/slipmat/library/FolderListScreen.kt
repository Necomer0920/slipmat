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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.slipmat.core.data.db.FolderSummary

@Composable
fun FolderListScreen(
    onFolderClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val folders by viewModel.folders.collectAsStateWithLifecycle()

    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(items = folders, key = { it.folderPath }) { folder ->
            FolderRow(folder = folder, onClick = { onFolderClick(folder.folderPath) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun FolderRow(folder: FolderSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // The leaf name is what the user recognises; the full path disambiguates the many
        // folders that share one.
        Text(
            text = folder.folderPath.substringAfterLast('/').ifEmpty { folder.folderPath },
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${folder.folderPath} · ${pluralise(folder.trackCount, "track")}",
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
