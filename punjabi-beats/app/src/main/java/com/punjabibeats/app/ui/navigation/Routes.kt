package com.punjabibeats.app.ui.navigation

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val JAM = "jam"
    const val AI = "ai"
    const val LIBRARY = "library"
    const val SETTINGS = "settings"
    const val PLAYER = "player"
    const val LYRICS = "lyrics"
    const val PODCASTS = "podcasts"
    const val PLAYLIST_DETAIL = "playlist/{playlistId}"

    const val ARG_PLAYLIST_ID = "playlistId"

    fun playlistDetail(playlistId: String): String = "playlist/$playlistId"
}
