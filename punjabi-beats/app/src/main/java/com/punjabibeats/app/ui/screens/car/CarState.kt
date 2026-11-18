package com.punjabibeats.app.ui.screens.car

enum class CabinProfile(
    val id: String,
    val displayName: String,
    val subLabel: String,
    val defaultSubwooferDelayMs: Float,
    val acousticBoostDb: Float,
) {
    SUV_THAR(
        id = "suv_thar",
        displayName = "SUV / Thar 4x4",
        subLabel = "High-cabin resonance • 68Hz null compensation",
        defaultSubwooferDelayMs = 1.15f,
        acousticBoostDb = 6.5f,
    ),
    SEDAN_CRUISER(
        id = "sedan_cruiser",
        displayName = "Sedan / Cruiser",
        subLabel = "Balanced cockpit image • Front driver sweet-spot",
        defaultSubwooferDelayMs = 0.85f,
        acousticBoostDb = 4.0f,
    ),
    GEDHI_HATCH(
        id = "gedhi_hatch",
        displayName = "Pind Gedhi Hatch",
        subLabel = "Hatchback boundary slam • 180° trunk phase invert",
        defaultSubwooferDelayMs = 1.45f,
        acousticBoostDb = 8.0f,
    ),
    CUSTOM_BOOMBOX(
        id = "custom_boombox",
        displayName = "Custom Boombox",
        subLabel = "Maximum SPL output • Raw dual-sub pass-through",
        defaultSubwooferDelayMs = 0.0f,
        acousticBoostDb = 10.0f,
    );

    companion object {
        fun fromId(id: String): CabinProfile = entries.firstOrNull { it.id == id } ?: SUV_THAR
    }
}

data class CarQuickPick(
    val id: String,
    val title: String,
    val subtitle: String,
    val songQuery: String,
)

data class CarDashboardState(
    val vehicleName: String = "PB 31 • THAR 4X4",
    val roadName: String = "GT ROAD • BYPASS",
    val speedKmh: Int = 64,
    val speedLimitKmh: Int = 80,
    val heading: String = "NW HEADING • SUR-TAAL HUD",
    val a2dpCodec: String = "LDAC 990k",
    val batteryPercent: Int = 94,
    val isPlaying: Boolean = true,
    val title: String = "295",
    val artist: String = "Sidhu Moose Wala • The Kidd",
    val albumArtUrl: String? = null,
    val positionMs: Long = 138000L, // 02:18
    val durationMs: Long = 270000L, // 04:30
    val isFavorite: Boolean = true,
    val activeGedhiPreset: String = "gaddi_808",
    val quickPicks: List<CarQuickPick> = listOf(
        CarQuickPick("1", "PB 31 Gedhi Mix", "Bass Heavy • 42 Songs", "PB 31 Gedhi Mix"),
        CarQuickPick("2", "Moosetape All Hits", "Tribute • Sidhu", "Moosetape Sidhu Moose Wala"),
        CarQuickPick("3", "Top 50 Punjab", "Daily Updated • Chart", "Top 50 Punjab"),
        CarQuickPick("4", "Majha Drill", "Night Shift • Hard 808s", "Majha Drill Punjabi"),
    ),
)

data class CarDspState(
    val destinationName: String = "PB-31 Custom Pioneer Double-DIN",
    val vehicleModel: String = "Mahindra Thar 4x4",
    val connectionSpec: String = "LDAC 990 kbps (24-bit/96kHz)",
    val a2dpLatencyMs: Int = 28,
    val outputLevelDb: Float = 2.5f,
    val cabinProfile: CabinProfile = CabinProfile.SUV_THAR,
    val driverDelayMs: Float = 1.15f,
    val subwooferPhaseDeg: Int = 180, // 0 or 180
    val gaddi808SlamDb: Float = 6.5f,
    val highwayNoiseLevelerEnabled: Boolean = true,
    val isAcousticSweepRunning: Boolean = false,
    val sweepProgress: Float = 1.0f,
    val isDspCalibratedActive: Boolean = true, // A/B test (false = Stock BT, true = Punjabi Beats DSP)
)
