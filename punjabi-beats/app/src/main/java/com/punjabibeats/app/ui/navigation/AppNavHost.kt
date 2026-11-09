package com.punjabibeats.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.punjabibeats.app.playback.MusicPlayerController
import com.punjabibeats.app.ui.screens.ai.AiStudioScreen
import com.punjabibeats.app.ui.screens.home.HomeScreen
import com.punjabibeats.app.ui.screens.jam.JamScreen
import com.punjabibeats.app.ui.screens.library.LibraryScreen
import com.punjabibeats.app.ui.screens.library.PlaylistDetailScreen
import com.punjabibeats.app.ui.screens.lyrics.LyricsScreen
import com.punjabibeats.app.ui.screens.player.FullPlayerScreen
import com.punjabibeats.app.ui.screens.podcast.PodcastScreen
import com.punjabibeats.app.ui.screens.search.SearchScreen
import com.punjabibeats.app.ui.screens.settings.SettingsScreen

/** Screens receive PaddingValues(0.dp): the root overlay clears space itself. */
private val NoPadding = PaddingValues(0.dp)

@Composable
fun AppNavHost(navController: NavHostController, controller: MusicPlayerController) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        enterTransition = { fadeIn(tween(220)) },
        exitTransition = { fadeOut(tween(220)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(220)) },
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                controller = controller,
                onOpenPlayer = { navController.navigate(Routes.PLAYER) },
                bottomPadding = NoPadding,
            )
        }
        composable(Routes.SEARCH) {
            SearchScreen(
                controller = controller,
                onOpenPlayer = { navController.navigate(Routes.PLAYER) },
                bottomPadding = NoPadding,
            )
        }
        composable(Routes.JAM) {
            JamScreen(
                onOpenPlayer = { navController.navigate(Routes.PLAYER) },
                bottomPadding = NoPadding,
            )
        }
        composable(Routes.AI) {
            AiStudioScreen(
                controller = controller,
                onOpenPlayer = { navController.navigate(Routes.PLAYER) },
                bottomPadding = NoPadding,
            )
        }
        composable(Routes.LIBRARY) {
            LibraryScreen(
                controller = controller,
                onOpenPlayer = { navController.navigate(Routes.PLAYER) },
                onOpenPodcasts = { navController.navigate(Routes.PODCASTS) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenPlaylist = { playlistId ->
                    navController.navigate(Routes.playlistDetail(playlistId))
                },
                bottomPadding = NoPadding,
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.PLAYLIST_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_PLAYLIST_ID) { type = NavType.StringType }),
        ) { entry ->
            PlaylistDetailScreen(
                playlistId = entry.arguments?.getString(Routes.ARG_PLAYLIST_ID).orEmpty(),
                controller = controller,
                onOpenPlayer = { navController.navigate(Routes.PLAYER) },
                onBack = { navController.popBackStack() },
                bottomPadding = NoPadding,
            )
        }
        composable(Routes.PODCASTS) {
            PodcastScreen(
                onBack = { navController.popBackStack() },
                bottomPadding = NoPadding,
            )
        }
        composable(
            route = Routes.PLAYER,
            enterTransition = { slideInVertically(tween(320)) { it } + fadeIn(tween(320)) },
            exitTransition = { fadeOut(tween(200)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = { slideOutVertically(tween(280)) { it } + fadeOut(tween(280)) },
        ) {
            FullPlayerScreen(
                onBack = { navController.popBackStack() },
                onOpenLyrics = { navController.navigate(Routes.LYRICS) },
            )
        }
        composable(
            route = Routes.LYRICS,
            enterTransition = { slideInVertically(tween(280)) { it } + fadeIn(tween(280)) },
            popExitTransition = { slideOutVertically(tween(260)) { it } + fadeOut(tween(260)) },
        ) {
            LyricsScreen(onBack = { navController.popBackStack() })
        }
    }
}
