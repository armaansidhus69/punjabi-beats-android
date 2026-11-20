package com.punjabibeats.app.ui.navigation

object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val JAM = "jam"
    const val AI = "ai"
    const val LIBRARY = "library"
    const val SETTINGS = "settings"
    const val THEMES = "themes"
    const val EQUALIZER = "equalizer"
    const val CAR_DASHBOARD = "car_dashboard"
    const val CAR_DSP = "car_dsp"
    const val PLAYER = "player"
    const val LYRICS = "lyrics"
    const val PODCASTS = "podcasts"
    const val REPLAY = "replay"
    const val PLAYLIST_DETAIL = "playlist/{playlistId}"

    const val ARG_PLAYLIST_ID = "playlistId"

    fun playlistDetail(playlistId: String): String = "playlist/$playlistId"

    const val ARTIST = "artist/{artistName}"
    const val ARG_ARTIST_NAME = "artistName"
    fun artist(name: String): String = "artist/${java.net.URLEncoder.encode(name, "UTF-8")}"

    const val ALBUM = "album/{albumName}/{artistName}"
    const val ARG_ALBUM_NAME = "albumName"
    fun album(albumName: String, artistName: String): String =
        "album/${java.net.URLEncoder.encode(albumName.ifBlank { "Album" }, "UTF-8")}/${java.net.URLEncoder.encode(artistName.ifBlank { "Unknown" }, "UTF-8")}"
}
