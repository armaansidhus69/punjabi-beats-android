package com.punjabibeats.app.ui.components.dynamicisland

enum class DynamicIslandMode {
    COMPACT_PILL,
    EXPANDED_CARD,
    DISMISSED,
}

data class DynamicIslandState(
    val mode: DynamicIslandMode = DynamicIslandMode.COMPACT_PILL,
    val title: String = "295",
    val artist: String = "Sidhu Moose Wala • The Kidd",
    val albumArtUrl: String? = null,
    val isPlaying: Boolean = true,
    val positionMs: Long = 138000L,
    val durationMs: Long = 270000L,
    val isFavorite: Boolean = true,
    val activeOutputDevice: String = "PB 31 Thar 4x4",
    val codecBadge: String = "LDAC 990k",
    val isEqualizerAnimating: Boolean = true,
) {
    val isVisible: Boolean
        get() = mode != DynamicIslandMode.DISMISSED
}
