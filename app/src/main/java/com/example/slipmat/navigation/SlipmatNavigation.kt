package com.example.slipmat.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.example.slipmat.library.LibraryScreen
import com.example.slipmat.nowplaying.MiniPlayer
import com.example.slipmat.nowplaying.NowPlayingScreen

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
}

@Composable
fun SlipmatNavHost(
    onPlay: (uris: List<String>, index: Int) -> Unit,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val onNowPlaying = currentRoute == Routes.NOW_PLAYING

    Column(modifier = modifier.fillMaxSize()) {
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
                NowPlayingScreen(onBack = { navController.popBackStack() })
            }
        }

        // The now-playing screen already shows everything the strip does, and the tab bar is
        // meaningless there, so both step aside rather than stacking up redundant chrome.
        if (!onNowPlaying) {
            MiniPlayer(onClick = { navController.navigate(Routes.NOW_PLAYING) })
            BrowseBottomBar(navController, currentRoute)
        }
    }
}

@Composable
private fun BrowseBottomBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar {
        BrowseTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = {
                    navController.navigate(tab.route) {
                        // Switching tabs must not stack them up: back from any tab returns to the
                        // start destination rather than replaying every tab visited.
                        popUpTo(BrowseTab.Tracks.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {},
                label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
            )
        }
    }
}
