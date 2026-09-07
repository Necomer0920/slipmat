package com.example.slipmat.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.slipmat.library.AlbumDetailScreen
import com.example.slipmat.library.AlbumListScreen
import com.example.slipmat.library.ArtistDetailScreen
import com.example.slipmat.library.ArtistListScreen
import com.example.slipmat.library.DetailArgs
import com.example.slipmat.library.FolderDetailScreen
import com.example.slipmat.library.FolderListScreen
import com.example.slipmat.core.media.QueueItem
import com.example.slipmat.library.LibraryScreen
import com.example.slipmat.nowplaying.MiniPlayer
import com.example.slipmat.nowplaying.NowPlayingScreen
import com.example.slipmat.nowplaying.QueueScreen
import com.example.slipmat.performance.PerformanceScreen
import com.example.slipmat.stub.StubScreen
import com.example.slipmat.ui.components.PillTab

/** The four browse modes. */
enum class BrowseTab(val route: String, val label: String) {
    Tracks("tracks", "Tracks"),
    Artists("artists", "Artists"),
    Albums("albums", "Albums"),
    Folders("folders", "Folders"),
}

/**
 * Arguments travel as path segments, so anything containing a `/` — every folder path, and any
 * album or artist name with a slash in it — must be [Uri.encode]d on the way in. Navigation
 * decodes on the way out, so the view models read the plain value.
 */
private object Routes {
    const val ARTIST_DETAIL = "artist/{${DetailArgs.ARTIST}}"
    const val ALBUM_DETAIL = "album/{${DetailArgs.ALBUM}}/{${DetailArgs.ALBUM_ARTIST}}"
    const val FOLDER_DETAIL = "folder/{${DetailArgs.FOLDER}}"

    fun artist(name: String) = "artist/${Uri.encode(name)}"
    fun album(album: String, artist: String) = "album/${Uri.encode(album)}/${Uri.encode(artist)}"
    fun folder(path: String) = "folder/${Uri.encode(path)}"

    const val NOW_PLAYING = "nowPlaying"
    const val QUEUE = "queue"
    const val PERFORMANCE = "performance"

    const val SEARCH = "search"
    const val SETTINGS = "settings"
}

@Composable
fun SlipmatNavHost(
    onPlay: (items: List<QueueItem>, index: Int) -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    /**
     * One `NowPlayingViewModel` for the strip, the full screen and the queue.
     *
     * A bare `hiltViewModel()` resolves against whatever owner is nearest, and these three do not
     * share one: the strip sits outside the `NavHost`, so it gets the Activity's store, while the
     * screens inside get their own `NavBackStackEntry`. That quietly produced *two* view models,
     * each running its own waveform decode on every track change — several seconds of duplicated
     * work per track, invisible because the two are never on screen at the same time.
     *
     * Resolving all three against the owner here — the Activity — also means walking back to the
     * library and returning does not re-decode the waveform that was already on screen.
     */
    val sharedOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner above SlipmatNavHost"
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val onNowPlaying = currentRoute == Routes.NOW_PLAYING ||
        currentRoute == Routes.QUEUE ||
        currentRoute == Routes.PERFORMANCE
    val onBrowseTab = BrowseTab.entries.any { it.route == currentRoute }

    Column(modifier = modifier.fillMaxSize()) {
        // Only above the four browse tabs themselves — a detail screen carries its own title and
        // back arrow (DetailScaffold), so stacking this on top of that would double up chrome.
        if (onBrowseTab) {
            Text(
                text = "Library",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LibraryTabRow(
                selected = BrowseTab.entries.first { it.route == currentRoute },
                onSelect = { tab ->
                    navController.navigate(tab.route) {
                        // Switching tabs must not stack them up: back from any tab returns to the
                        // start destination rather than replaying every tab visited.
                        popUpTo(BrowseTab.Tracks.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
        NavHost(
            navController = navController,
            startDestination = BrowseTab.Tracks.route,
            modifier = Modifier.weight(1f),
        ) {
            composable(BrowseTab.Tracks.route) {
                LibraryScreen(onPlay = onPlay)
            }
            composable(BrowseTab.Artists.route) {
                ArtistListScreen(onArtistClick = { navController.navigate(Routes.artist(it)) })
            }
            composable(BrowseTab.Albums.route) {
                AlbumListScreen(
                    onAlbumClick = { album, artist ->
                        navController.navigate(Routes.album(album, artist))
                    },
                )
            }
            composable(BrowseTab.Folders.route) {
                FolderListScreen(onFolderClick = { navController.navigate(Routes.folder(it)) })
            }

            composable(
                route = Routes.ARTIST_DETAIL,
                arguments = listOf(navArgument(DetailArgs.ARTIST) { type = NavType.StringType }),
            ) {
                ArtistDetailScreen(onBack = { navController.popBackStack() }, onPlay = onPlay)
            }
            composable(
                route = Routes.ALBUM_DETAIL,
                arguments = listOf(
                    navArgument(DetailArgs.ALBUM) { type = NavType.StringType },
                    navArgument(DetailArgs.ALBUM_ARTIST) { type = NavType.StringType },
                ),
            ) {
                AlbumDetailScreen(onBack = { navController.popBackStack() }, onPlay = onPlay)
            }
            composable(
                route = Routes.FOLDER_DETAIL,
                arguments = listOf(navArgument(DetailArgs.FOLDER) { type = NavType.StringType }),
            ) {
                FolderDetailScreen(onBack = { navController.popBackStack() }, onPlay = onPlay)
            }
            composable(Routes.NOW_PLAYING) {
                NowPlayingScreen(
                    onBack = { navController.popBackStack() },
                    onOpenQueue = { navController.navigate(Routes.QUEUE) },
                    onOpenPerformance = { navController.navigate(Routes.PERFORMANCE) },
                    viewModel = hiltViewModel(sharedOwner),
                )
            }
            composable(Routes.QUEUE) {
                QueueScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = hiltViewModel(sharedOwner),
                )
            }
            composable(Routes.PERFORMANCE) {
                PerformanceScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = hiltViewModel(sharedOwner),
                )
            }
            composable(Routes.SEARCH) {
                StubScreen(title = "Search", onBack = { navController.popBackStack() })
            }
            composable(Routes.SETTINGS) {
                StubScreen(title = "Settings", onBack = { navController.popBackStack() })
            }
        }

        // The now-playing screen already shows everything the strip does, and the tab bar is
        // meaningless there, so both step aside rather than stacking up redundant chrome.
        if (!onNowPlaying) {
            MiniPlayer(
                onClick = { navController.navigate(Routes.NOW_PLAYING) },
                viewModel = hiltViewModel(sharedOwner),
            )
            AppBottomBar(
                onLibrarySection = onBrowseTab ||
                    currentRoute in setOf(Routes.ARTIST_DETAIL, Routes.ALBUM_DETAIL, Routes.FOLDER_DETAIL),
                onSearch = currentRoute == Routes.SEARCH,
                onSettings = currentRoute == Routes.SETTINGS,
                onLibraryClick = {
                    if (currentRoute == Routes.SEARCH || currentRoute == Routes.SETTINGS) {
                        navController.popBackStack()
                    }
                },
                onSearchClick = {
                    // Switching directly from Settings must replace it, not stack on top of it -
                    // otherwise Library's single popBackStack() only peels off one stub screen.
                    navController.navigate(Routes.SEARCH) {
                        if (currentRoute == Routes.SETTINGS) {
                            popUpTo(Routes.SETTINGS) { inclusive = true }
                        }
                    }
                },
                onSettingsClick = {
                    navController.navigate(Routes.SETTINGS) {
                        if (currentRoute == Routes.SEARCH) {
                            popUpTo(Routes.SEARCH) { inclusive = true }
                        }
                    }
                },
            )
        }
    }
}

/**
 * Library / Search / Settings — down from the old four-item browse-mode bar (§5.6). Search and
 * Settings are plain pushes with default back behaviour, not tab switches: they are dead-end stub
 * screens, not persisted sibling state the way the browse tabs are.
 */
@Composable
private fun AppBottomBar(
    onLibrarySection: Boolean,
    onSearch: Boolean,
    onSettings: Boolean,
    onLibraryClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconSize = 22.dp
    NavigationBar(modifier = modifier) {
        NavigationBarItem(
            selected = onLibrarySection,
            onClick = onLibraryClick,
            icon = { Icon(Icons.Filled.LibraryMusic, contentDescription = null, modifier = Modifier.size(iconSize)) },
            label = { Text("Library", style = MaterialTheme.typography.labelMedium) },
        )
        NavigationBarItem(
            selected = onSearch,
            onClick = onSearchClick,
            icon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(iconSize)) },
            label = { Text("Search", style = MaterialTheme.typography.labelMedium) },
        )
        NavigationBarItem(
            selected = onSettings,
            onClick = onSettingsClick,
            icon = { Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(iconSize)) },
            label = { Text("Settings", style = MaterialTheme.typography.labelMedium) },
        )
    }
}

/** The four browse modes, as a pill tab row — replaces the bottom `NavigationBar` (§4.1). */
@Composable
private fun LibraryTabRow(
    selected: BrowseTab,
    onSelect: (BrowseTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BrowseTab.entries.forEach { tab ->
            PillTab(selected = tab == selected, onClick = { onSelect(tab) }, label = tab.label)
        }
    }
}
