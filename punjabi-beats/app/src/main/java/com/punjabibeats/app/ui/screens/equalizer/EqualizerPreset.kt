package com.punjabibeats.app.ui.screens.equalizer

enum class EqualizerPreset(
    val id: String,
    val displayName: String,
    val punjabiLabel: String,
    val iconName: String,
    val bandGainsDb: List<Float>,
    val defaultBassBoostPercent: Int = 0,
) {
    GADDI_808(
        id = "gaddi_808",
        displayName = "Gaddi 808 Bass",
        punjabiLabel = "ਗੱਡੀ 808",
        iconName = "speaker",
        bandGainsDb = listOf(8.5f, 7.0f, 4.5f, 2.0f, 0.0f, 1.5f, 3.5f, 5.0f, 6.0f, 4.0f),
        defaultBassBoostPercent = 85,
    ),
    LIVE_DHOL_PUNCH(
        id = "dhol_punch",
        displayName = "Live Dhol Punch",
        punjabiLabel = "ਢੋਲ ਪੰਚ",
        iconName = "celebration",
        bandGainsDb = listOf(5.0f, 8.0f, 6.0f, 3.0f, 0.0f, 2.0f, 4.0f, 7.0f, 5.0f, 3.0f),
        defaultBassBoostPercent = 70,
    ),
    VOCAL_TUMBI(
        id = "vocal_tumbi",
        displayName = "Vocal & Tumbi",
        punjabiLabel = "ਤੂੰਬੀ ਤੇ ਬੋਲ",
        iconName = "mic",
        bandGainsDb = listOf(0.0f, 1.0f, 2.0f, 3.0f, 4.0f, 6.0f, 7.0f, 8.0f, 7.0f, 5.0f),
        defaultBassBoostPercent = 20,
    ),
    DESI_DRILL_SKITTER(
        id = "desi_drill",
        displayName = "Desi Drill Skitter",
        punjabiLabel = "ਡ੍ਰਿਲ ਬੀਟ",
        iconName = "speed",
        bandGainsDb = listOf(7.0f, 5.0f, 2.0f, 0.0f, -1.0f, 1.0f, 3.0f, 5.0f, 7.0f, 8.0f),
        defaultBassBoostPercent = 65,
    ),
    LATE_NIGHT_SUFI(
        id = "late_night_sufi",
        displayName = "Late Night Sufi",
        punjabiLabel = "ਸੂਫ਼ੀ ਰਾਤ",
        iconName = "nightlight",
        bandGainsDb = listOf(4.0f, 3.0f, 2.0f, 3.0f, 3.0f, 3.0f, 3.0f, 2.0f, 2.0f, 1.0f),
        defaultBassBoostPercent = 40,
    ),
    STUDIO_FLAT(
        id = "studio_flat",
        displayName = "Studio Flat",
        punjabiLabel = "ਸਟੂਡੀਓ ਫਲੈਟ",
        iconName = "horizontal_rule",
        bandGainsDb = listOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f),
        defaultBassBoostPercent = 0,
    ),
    CUSTOM(
        id = "custom",
        displayName = "Custom",
        punjabiLabel = "ਆਪਣਾ ਸੁਰ",
        iconName = "tune",
        bandGainsDb = listOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f),
        defaultBassBoostPercent = 50,
    );

    companion object {
        fun fromId(id: String): EqualizerPreset = entries.firstOrNull { it.id == id } ?: GADDI_808
    }
}

data class EqualizerState(
    val isEnabled: Boolean = true,
    val preAmpDb: Float = 3.0f,
    val selectedPresetId: String = "gaddi_808",
    val bandGainsDb: List<Float> = EqualizerPreset.GADDI_808.bandGainsDb,
    val bandFrequencies: List<String> = listOf("32", "64", "125", "250", "500", "1k", "2k", "4k", "8k", "16k"),
    val bandSubLabels: List<String> = listOf("SUB", "KICK", "BASS", "BODY", "MID", "TUMBI", "VOCAL", "PLUCK", "CHANTI", "AIR"),
    val bassBoostPercent: Int = 85,
    val dholTransientPercent: Int = 70,
    val virtualizerPercent: Int = 60,
)
