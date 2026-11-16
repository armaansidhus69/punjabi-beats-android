package com.punjabibeats.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.punjabibeats.app.ui.theme.LocalPBTheme

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

    val theme = LocalPBTheme.current

    CompositionLocalProvider(LocalPbSnackbar provides snackbarHostState) {
        Scaffold(
            containerColor = theme.background,
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
    val theme = LocalPBTheme.current
    NavigationBar(containerColor = theme.panel, tonalElevation = 0.dp) {
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
                        tint = if (selected) theme.accent else theme.muted,
                        modifier = Modifier.size(24.dp),
                    )
                },
                label = {
                    Text(
                        destination.label,
                        color = if (selected) theme.accent else theme.muted,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = theme.accent,
                    selectedTextColor = theme.accent,
                    indicatorColor = theme.accent.copy(alpha = 0.15f),
                    unselectedIconColor = theme.muted,
                    unselectedTextColor = theme.muted,
                ),
            )
        }
    }
}
