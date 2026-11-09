package com.punjabibeats.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.punjabibeats.app.playback.MusicPlayerController
import com.punjabibeats.app.ui.components.LocalPbSnackbar
import com.punjabibeats.app.ui.components.MiniPlayerBar
import com.punjabibeats.app.ui.navigation.AppNavHost
import com.punjabibeats.app.ui.navigation.Routes
import com.punjabibeats.app.ui.theme.AccentGold
import com.punjabibeats.app.ui.theme.BgBase
import com.punjabibeats.app.ui.theme.SurfaceBase
import com.punjabibeats.app.ui.theme.TextSecondary

private enum class BottomDestination(val route: String, val label: String, val icon: ImageVector) {
    HOME(Routes.HOME, "Home", Icons.Rounded.Home),
    SEARCH(Routes.SEARCH, "Search", Icons.Rounded.Search),
    JAM(Routes.JAM, "Jam", Icons.Rounded.Groups),
    AI(Routes.AI, "AI Studio", Icons.Rounded.AutoAwesome),
    LIBRARY(Routes.LIBRARY, "Library", Icons.Rounded.LibraryMusic),
}

private val TOP_LEVEL_ROUTES = BottomDestination.entries.map { it.route }.toSet()

/**
 * Root shell: bottom navigation + a persistent floating mini player that
 * expands into the full player. Immersive routes (player, lyrics) hide both.
 */
@Composable
fun PunjabiBeatsAppRoot(playerController: MusicPlayerController) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showChrome = currentRoute in TOP_LEVEL_ROUTES

    CompositionLocalProvider(LocalPbSnackbar provides snackbarHostState) {
        Scaffold(
            containerColor = BgBase,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                Column {
                    if (showChrome) {
                        MiniPlayerBar(
                            controller = playerController,
                            onOpenPlayer = { navController.navigate(Routes.PLAYER) },
                        )
                    }
                    AnimatedVisibility(
                        visible = showChrome,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    ) {
                        PbBottomBar(currentRoute = currentRoute, navController = navController)
                    }
                }
            },
        ) { _ ->
            AppNavHost(navController = navController, controller = playerController)
        }
    }
}

@Composable
private fun PbBottomBar(currentRoute: String?, navController: androidx.navigation.NavHostController) {
    NavigationBar(containerColor = SurfaceBase, tonalElevation = 0.dp) {
        BottomDestination.entries.forEach { destination ->
            val selected = currentRoute == destination.route
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        destination.icon,
                        contentDescription = destination.label,
                        tint = if (selected) AccentGold else TextSecondary,
                    )
                },
                label = {
                    Text(
                        destination.label,
                        color = if (selected) AccentGold else TextSecondary,
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = AccentGold.copy(alpha = 0.15f),
                ),
            )
        }
    }
}
