package com.punjabibeats.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Design tokens matching the 7 modern aesthetic styles from user mockups:
 * 1. Punjabi Royale (midnight plum, aurora violet, and dhol gold)
 * 2. AI Mood Studio (Screenshot 2)
 * 3. Aquamarine Frosted Glass (Screenshot 3)
 * 4. Cloud Blue Neumorphic (Screenshot 4)
 * 5. Urban Lime Minimal (Screenshot 5)
 * 6. MoodBeat Dark Turntable (Screenshot 6)
 * 7. Electric Aurora & Waves (Screenshot 7)
 */
object DesignTokens {
    // Punjabi Royale: midnight plum, aurora violet, and a restrained dhol-gold accent.
    val SunsetBg = Color(0xFF090811)
    val SunsetSurface = Color(0xFF13111D)
    val SunsetCard = Color(0xFF1D1928)
    val SunsetAccent = Color(0xFFB77AFF)
    val SunsetSecondary = Color(0xFFE8BF72)
    val SunsetText = Color(0xFFF8F5FC)
    val SunsetMuted = Color(0xFFA9A3B5)

    // 2. AI Mood Studio
    val MoodBg = Color(0xFF0D0B12)
    val MoodSurface = Color(0xFF171421)
    val MoodCard = Color(0xFF241F33)
    val MoodAccent = Color(0xFFFF4081)         // Neon Coral Pink
    val MoodSecondary = Color(0xFF7C4DFF)      // Electric Purple
    val MoodText = Color(0xFFFFFFFF)
    val MoodMuted = Color(0xFFA69EB8)

    // 3. Aquamarine Frosted Glass
    val AquaBg = Color(0xFF08181A)
    val AquaSurface = Color(0xFF0E2529)
    val AquaCard = Color(0xFF15373C)
    val AquaAccent = Color(0xFF00E5C9)          // Turquoise Neon
    val AquaSecondary = Color(0xFF80DEEA)       // Soft Aqua Frost
    val AquaText = Color(0xFFF0FDFD)
    val AquaMuted = Color(0xFF80CBC4)

    // 4. Cloud Blue Neumorphic
    val CloudBg = Color(0xFF0A131F)
    val CloudSurface = Color(0xFF132238)
    val CloudCard = Color(0xFF1D324F)
    val CloudAccent = Color(0xFF29B6F6)         // Sky Blue
    val CloudSecondary = Color(0xFFE1F5FE)      // Cloud White
    val CloudText = Color(0xFFFFFFFF)
    val CloudMuted = Color(0xFF90CAF9)

    // 5. Urban Lime & Yellow Minimal
    val UrbanBg = Color(0xFF0A0A0A)
    val UrbanSurface = Color(0xFF161616)
    val UrbanCard = Color(0xFF242424)
    val UrbanAccent = Color(0xFFD4E157)         // Electric Lime
    val UrbanSecondary = Color(0xFFFFEE58)      // Sunny Lemon
    val UrbanText = Color(0xFFFFFFFF)
    val UrbanMuted = Color(0xFFAAAAAA)

    // 6. MoodBeat Dark Turntable
    val TurntableBg = Color(0xFF0A0710)
    val TurntableSurface = Color(0xFF140F1F)
    val TurntableCard = Color(0xFF201830)
    val TurntableAccent = Color(0xFFB388FF)      // Radiant Orchid
    val TurntableSecondary = Color(0xFFEA80FC)   // Neon Pink/Purple
    val TurntableText = Color(0xFFFFFFFF)
    val TurntableMuted = Color(0xFFA69AB8)

    // 7. Electric Aurora & Waves
    val AuroraBg = Color(0xFF070514)
    val AuroraSurface = Color(0xFF110C24)
    val AuroraCard = Color(0xFF1B1438)
    val AuroraAccent = Color(0xFF9D4EDD)         // Luminous Electric Violet
    val AuroraSecondary = Color(0xFF536DFE)      // Electric Indigo
    val AuroraText = Color(0xFFFFFFFF)
    val AuroraMuted = Color(0xFFAFA7CE)

    // Backward compatibility aliases
    val SaffronGold = SunsetSecondary
    val DeepIndigo = SunsetSurface
    val RoyalTeal = AquaAccent
    val BrightPink = MoodAccent
    val GoldenCrown = SunsetSecondary
    val PunjabBackground = SunsetBg
    val PunjabSurface = SunsetSurface
    val PunjabCard = SunsetCard
    val PunjabAccent = SunsetAccent
    val PunjabSecondary = SunsetSecondary
    val PunjabText = SunsetText
    val PunjabMuted = SunsetMuted
}
