package com.punjabibeats.app.ui.components.sliders

enum class SeekbarStyle(
    val id: String,
    val displayName: String,
    val iconEmoji: String,
    val description: String,
) {
    CLASSIC_WAVEFORM("classic_waveform", "Classic Waveform", "🌊", "Signature Punjabi Beats multi-harmonic waveform"),
    WAVY("wavy", "Wavy Ocean", "〰️", "Flowing sinusoidal wave with live oscillating crests"),
    RGB_GLOW("rgb_glow", "RGB Chroma Glow", "🌈", "Pulsing rainbow chroma neon glow"),
    PILL_CAPSULE("pill_capsule", "Pill Capsule", "💊", "Smooth rounded capsule slider with active time pill"),
    SEGMENTS("segments", "Segment Blocks", "🧱", "28 discrete rounded audio blocks that fill progressively"),
    EQUALIZER("equalizer", "Equalizer Bounce", "📊", "Dancing multi-bar equalizer spectrum along the seek track"),
    LIGHTNING("lightning", "Lightning Bolt", "⚡", "High-intensity electric lightning bolt laser arcs"),
    CONSTELLATION("constellation", "Stellar Constellation", "✨", "Star cluster cosmic network with connected nodes"),
    HEARTBEAT("heartbeat", "Heartbeat ECG", "💓", "Dynamic cardiac rhythm electrocardiogram pulse beat"),
    DNA_HELIX("dna_helix", "DNA Double Helix", "🧬", "Intertwined double-helix audio wave pattern"),
    COMET("comet", "Comet Tail", "☄️", "Shooting star particle stream with stardust trail"),
    VINYL_GROOVE("vinyl_groove", "Vinyl Grooves", "💽", "Concentric vinyl record groove tracks with needle guide"),
    RETRO_TAPE("retro_tape", "Retro Cassette Tape", "📼", "Magnetic cassette audio ribbon with spinning reels"),
    FIRE_EMBER("fire_ember", "Fire Ember", "🔥", "Warm golden-saffron flames with flickering sparks"),
    BUBBLES("bubbles", "Floating Bubbles", "🫧", "Effervescent floating audio bubble particles"),
    DOT_MATRIX("dot_matrix", "LED Dot Matrix", "🎛️", "Retro pro-audio hardware LED matrix indicator"),
    AUDIO_SPECTRUM("audio_spectrum", "Spectrum Analyzer", "📶", "FFT frequency analyzer bars with peak holding"),
    LASER_BEAM("laser_beam", "Laser Guide", "🔦", "Focused high-energy sci-fi laser guide beam"),
    LIQUID_TUBE("liquid_tube", "Liquid Tube", "🧪", "Fluid visceral liquid mercury/neon plasma tube"),
    PUNJABI_RHYTHM("punjabi_rhythm", "Punjabi Dhol Beat", "🥁", "Traditional Punjabi Bhangra folk rhythm waveform"),
    GOLDEN_SPARKLE("golden_sparkle", "Golden Sparkle", "✨", "Glittering golden starlight particle trail"),
    ELASTIC_STRING("elastic_string", "Elastic String", "🪕", "Physics-based vibrating elastic resonant string with harmonic tension");

    companion object {
        fun fromId(id: String): SeekbarStyle = when (id) {
            "neon_cyan" -> LIGHTNING
            "orbit" -> CONSTELLATION
            "water_ripple" -> BUBBLES
            "minimalist_hairline" -> LIQUID_TUBE
            "dual_gradient" -> ELASTIC_STRING
            "elastic" -> ELASTIC_STRING
            else -> entries.firstOrNull { it.id == id } ?: CLASSIC_WAVEFORM
        }
    }
}
