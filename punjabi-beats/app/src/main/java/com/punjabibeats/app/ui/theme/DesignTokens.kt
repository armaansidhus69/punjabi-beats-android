package com.punjabibeats.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Design tokens for the Punjabi Beats signature palette & design system.
 * Blends rich Punjabi cultural tones (Saffron, Royal Indigo, Teal, Hot Pink, Pure Gold)
 * with modern deep glass surfaces for high readability and contrast.
 */
object DesignTokens {
    // Core Brand Colors
    val SaffronGold = Color(0xFFFFB300)      // Royal Kesari / Saffron
    val DeepIndigo = Color(0xFF1E2640)       // Midnight Indigo base
    val RoyalTeal = Color(0xFF00B4D8)        // Vibrant Cyan / Teal
    val BrightPink = Color(0xFFFF2E93)       // Punjabi Beats Signature Pink
    val GoldenCrown = Color(0xFFFFAA00)      // Warm Gold Accent

    // Punjab Royal Theme Palette
    val PunjabBackground = Color(0xFF0F0B14) // Deep royal dark background
    val PunjabSurface = Color(0xFF1B1626)    // Elevated glass surface
    val PunjabCard = Color(0xFF261F36)       // Interactive card container
    val PunjabAccent = Color(0xFFFFB300)     // Bright Saffron Gold
    val PunjabSecondary = Color(0xFF00B4D8)  // Vibrant Teal Accent
    val PunjabText = Color(0xFFFFFDF8)       // Warm bright text
    val PunjabMuted = Color(0xFFA59EB3)      // Soft muted lavender-grey

    // Brand Gradients
    val SaffronTealGradient = Brush.linearGradient(
        listOf(SaffronGold, RoyalTeal)
    )

    val PunjabRoyalHeroGradient = Brush.verticalGradient(
        listOf(
            Color(0xFF3B1E42),
            Color(0xFF261430),
            Color(0xFF160B1C),
            PunjabBackground,
        )
    )
}
