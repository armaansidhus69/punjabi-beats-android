# Design Specification: Dynamic UI Archetypes & Apple Music Player Engine

- **Date**: 2026-11-18
- **Author**: Antigravity Assistant & User
- **Status**: Proposed (Awaiting Approval)
- **Target File**: `docs/superpowers/specs/2026-11-18-apple-music-ui-archetype-design.md`

---

## 1. Intent & The Why

Users want the app's visual experience to be **deeply immersive and archetype-adaptive**. Changing a theme shouldn't merely swap color hex codes—it should reshape the **Player layout, visual components, interaction physics, and ambient backdrops** to match the chosen music culture.

Selecting **Apple Music** should transform the player into the signature Cupertino music experience: dynamic blurred album mesh background, floating squircle album art that scales up on play and shrinks on pause, minimalist scrubbers with negative remaining time, iOS 3-button playback controls, an inline volume slider with speaker icons, and frosted glass sheet toggles.

---

## 2. Goals, Non-Goals & Anti-Goals

### Goals
- **Flagship Apple Music Theme**: Add `apple` as the 5th core theme in `PBThemes` with signature Apple Music Crimson Red (`#FA2D48`), iOS Dark Glass (`#0A0A0C`), and San Francisco iOS aesthetics.
- **Dedicated `PlayerStyle.APPLE` Architecture**: Build a dedicated player view in `FullPlayerScreen.kt` that triggers when `theme.style == PlayerStyle.APPLE`:
  - **Dynamic Blurred Mesh Backdrop**: Ambient palette-derived gradient mesh matching the current playing song.
  - **Animated Artwork Scaling**: 280x280dp squircle artwork with `RoundedCornerShape(22.dp)` that expands to 1.0x when playing and smoothly contracts to 0.85x on pause (exact iOS behavior).
  - **iOS Scrub Bar**: Clean 3.5dp bar with current position on left and negative remaining time (`-M:SS`) on right.
  - **Apple Music Playback Controls**: Clean iOS previous, large bold play/pause, and next icons.
  - **Interactive Volume Slider**: Horizontal volume slider flanked by low and high speaker glyphs.
  - **Bottom Action Dock**: Lyrics button, AirPlay output indicator, and Queue drawer button.
- **Dedicated Player Archetypes**: Ensure each of the 5 themes has distinct player centerpiece identity:
  - 🍎 **Apple Music**: Scaled squircle album art + dynamic mesh + iOS volume.
  - 👑 **Neon Dusk**: 12" spinning turntable with tonearm + hot-pink neon halo.
  - ⚡ **Cyberpunk**: Sci-fi angular HUD with frequency analyzer & laser seekbar.
  - 🌸 **Anime Beats**: Pulsing radial aura + bouncy cards.
  - 👑 **Punjab Legend**: Royal golden crown + Dhol folk rhythm wave.
- **Theme Studio & Settings Quick Switcher**: Add `Apple Music 🍎` to the theme list and 1-tap quick pills.
- **Zero Crashes**: Fully protected by `safeData` and `compositionLocalOf` state architecture.

### Non-Goals
- Re-architecting the entire Home / Search / Library tabs into Apple Music clones (user confirmed Option 1: focus on Player Screen, bottom navigation, cards, and theme studio).
- Connecting to Apple Music proprietary DRM or subscription APIs (streaming remains powered by our existing resilient fallback engine & local scanner).

### Anti-Goals
- Hardcoding static views that break when song artwork is missing.
- Heavy blur filters that degrade framerate on lower-end Android devices (use optimized hardware-accelerated shaders and Palette-derived gradients).

---

## 3. Approaches Considered

1. **Option 1 (Selected by User)**:
   - Add `Apple Music` as 5th theme in `PBThemes`.
   - Implement dedicated `AppleMusicPlayerContent` inside `FullPlayerScreen.kt` activated via `when (theme.style)`.
   - Retain unified bottom bar navigation with frosted Cupertino blur effect.
   - *Pros*: Best-in-class performance, clean separation of concerns, instant switching without navigation fragmentation.
2. **Option 2 (Rejected)**:
   - Complete multi-app overhaul replacing all screens with Apple Music tabs ("Listen Now", "Browse", "Radio").
   - *Cons*: Disrupts existing Punjabi Beats feature set (Jam Rooms, AI Studio, Local Scanner) and bloats scope unnecessarily.

---

## 4. Technical Architecture & Implementation Details

### 4.1 Theme Engine Update (`PBTheme.kt`)
Add Apple Music to `PBThemes`:
```kotlin
PBTheme(
    id = "apple",
    name = "Apple Music",
    emoji = "🍎",
    background = Color(0xFF0A0A0C),
    surface = Color(0xFF16161C),
    card = Color(0xFF22222A),
    accent = Color(0xFFFA2D48),
    secondary = Color(0xFF007AFF),
    text = Color.White,
    muted = Color(0xFF9898A0),
    tagline = "Cupertino Glass • Blurred mesh & iOS player controls",
    style = PlayerStyle.APPLE,
)
```

### 4.2 Apple Music Player Layout (`FullPlayerScreen.kt`)
When `theme.style == PlayerStyle.APPLE`:
1. **Dynamic Backdrop**: Vertical/Radial gradient using extracted dominant & vibrant colors with subtle dimming for text contrast.
2. **Artwork Card**:
   - `animateFloatAsState(targetValue = if (isPlaying) 1.0f else 0.85f, animationSpec = spring(stiffness = Spring.StiffnessLow))`
   - Squircle shape `RoundedCornerShape(22.dp)`, subtle border `1.dp` with `Color.White.copy(alpha = 0.15f)`.
3. **Song Info**:
   - Title: `fontSize = 24.sp`, `fontWeight = FontWeight.Bold`, `color = Color.White`.
   - Artist: `fontSize = 17.sp`, `color = Color.White.copy(alpha = 0.65f)`.
   - Actions: Favorite heart icon + More options menu.
4. **Scrub Bar**:
   - `Slider` with custom Apple-style track & thumb or `UniversalSeekbar` styled as Apple Minimalist.
   - Elapsed time: `formatDuration(positionMs)`
   - Remaining time: `"-${formatDuration(maxOf(0L, durationMs - positionMs))}"`
5. **Playback Controls**:
   - `SkipPrevious`, `PlayArrow` / `Pause` (64dp button), `SkipNext`.
6. **Volume Slider**:
   - Left: `Icons.Rounded.VolumeDown`
   - Center: `Slider` with custom iOS track thickness (8.dp)
   - Right: `Icons.Rounded.VolumeUp`
7. **Bottom Action Dock**:
   - `Icons.Rounded.Lyrics` -> opens synced karaoke sheet
   - `Icons.Rounded.Airplay` -> triggers audio output dialog / Cast
   - `Icons.Rounded.QueueMusic` -> opens Queue drawer

### 4.3 Settings & Theme Studio Integration
- Update `ThemeStudioScreen.kt`: displays 5 premium themes.
- Update `SettingsScreen.kt`: displays 5 quick theme pills (👑 Neon, 🍎 Apple, 🌸 Anime, ⚡ Cyber, 👑 Punjab).

---

## 5. Verification & Testing
- Unit tests: verify `pbThemeById("apple")` returns valid `PlayerStyle.APPLE` with correct colors.
- Build test: `.\gradlew.bat testDebugUnitTest` must pass (100% success).
- APK Assemble: `.\gradlew.bat assembleDebug` must compile clean.
- Fresh APKs generated and pushed to GitHub.
