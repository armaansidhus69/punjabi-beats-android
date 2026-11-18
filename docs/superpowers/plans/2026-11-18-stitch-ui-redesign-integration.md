# Stitch UI Redesign Integration Plan - Punjabi Beats

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fully implement and integrate all 13 Stitch redesign screens into the Punjabi Beats Android Jetpack Compose app, introducing high-value features including Car Dashboard Mode, Car DSP Calibration, 10-Band Sonic Equalizer, Dynamic Island morphing overlay, and enhanced Now Playing with multi-mode visualizer & karaoke lyrics.

**Architecture:** Modular Jetpack Compose Material 3 architecture with custom Canvas graphics for DSP soundstage and equalizer curves, reactive StateFlow view models, Hilt dependency injection, ExoPlayer audio effects integration, and smooth animated morphing states for the Dynamic Island pill.

**Tech Stack:** Kotlin 2.x, Android Jetpack Compose (Material 3), Compose Animation & Canvas, AndroidX Media3 / ExoPlayer AudioEffects (`Equalizer`, `BassBoost`, `Virtualizer`), Coil Compose, Gradle 8.x.

**Spec Reference:**
- Stitch Project: `Punjabi Beats UI Redesign` (ID: `7619794539033891366`)
- Downloaded assets directory: `stitch_screens/punjabi_beats_ui_redesign/`
- Spec documentation: `stitch_screens/punjabi_beats_ui_redesign/README.md`

## Global Constraints
- Target SDK 34 / Min SDK 24.
- Must pass `.\gradlew.bat testDebugUnitTest` without regressions.
- Strict Constraint: Do NOT modify or replace `apk/punjabi-beats-v1.0.0-legacy.apk`.
- Theme & Styling: Support both the Vice City Neon palette (`#FF2E93`, `#08060D`) and the Stitch design tokens (Space Grotesk headlines, Plus Jakarta Sans body, `#a78bfa` primary accent, dark glass surfaces).
- System Insets: All screens must respect `Scaffold` `innerPadding` and system bars (Edge-to-Edge).

## Review Focus
1. **Audio Effect Lifecycle:** `Equalizer` and `BassBoost` must attach safely to active ExoPlayer `audioSessionId` without throwing `IllegalStateException` or leaking on player pause/resume.
2. **DSP 2D Soundstage Drag:** Touch gestures on the Car DSP Calibration 2D canvas must clamp coordinates smoothly within bounds without causing recomposition jank.
3. **Dynamic Island Bounds:** Floating Dynamic Island pill and expanded overlay must not obscure critical system status bar icons or crash when gesture-morphing between compact and expanded states.
4. **Equalizer Frequency Math:** Gain sliders (-12dB to +12dB) must accurately translate to milliBel (`short`) values for Android's hardware `Equalizer` API.
5. **Car Mode Large Touch Targets:** Car mode buttons must measure at least 72dp for safe in-car touch interaction.

---

### Task 1: Design Tokens, Assets & Theme Engine Alignment
**Screens:** 03 (Home), 04 (Sidhu Moose Wala 295 Artwork)

**Files:**
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/theme/DesignTokens.kt`
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/theme/PBTheme.kt`
- Create: `punjabi-beats/app/src/main/res/drawable/art_sidhu_295_fallback.png` (copy from `04_album_art_295_sidhu_moose_wala.png`)
- Test: `punjabi-beats/app/src/test/java/com/punjabibeats/app/ThemeEngineTest.kt`

**Interfaces:**
- Produces: Updated design tokens for Stitch surfaces (`#121316`, `#1f1f23`, `#292a2d`), typography styles, and high-res fallback album art resource.

- [x] **Step 1: Write unit test in `ThemeEngineTest` asserting Stitch design token presence and palette consistency**
- [x] **Step 2: Run test to verify failure**
- [x] **Step 3: Copy `04_album_art_295_sidhu_moose_wala.png` into `res/drawable/art_sidhu_295_fallback.png`**
- [x] **Step 4: Update `DesignTokens.kt` and `PBTheme.kt` with Stitch color tokens and typography definitions**
- [x] **Step 5: Run unit tests to verify they pass**
- [x] **Step 6: Commit changes**

---

### Task 2: 10-Band Sonic Equalizer Screen & Audio FX Engine
**Screen:** 12 (`12_10_band_sonic_equalizer.html` / `.png`)

**Files:**
- Create: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/equalizer/EqualizerScreen.kt`
- Create: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/equalizer/EqualizerViewModel.kt`
- Create: `punjabi-beats/app/src/main/java/com/punjabibeats/app/playback/AudioFxController.kt`
- Test: `punjabi-beats/app/src/test/java/com/punjabibeats/app/EqualizerViewModelTest.kt`

**Interfaces:**
- Produces: `EqualizerScreen` featuring:
  - 10 Frequency Band Sliders (32Hz, 64Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz) with dB indicators.
  - Frequency Curve Canvas visualizer showing real-time interpolated gain curve.
  - Punjabi Presets: `"Bhangra Dhol Punch"`, `"Deep Bass Subwoofer"`, `"Vocal Clarity"`, `"Car Acoustics"`, `"Acoustic Tumbi"`, `"Flat"`.
  - Dial sliders for: Bass Boost, 3D Surround, Dhol Punch resonance.
- Consumes: `AudioFxController` connecting to ExoPlayer audio session.

- [x] **Step 1: Write unit test `EqualizerViewModelTest` verifying preset band gains and dB-to-mB conversions**
- [x] **Step 2: Run test to verify failure**
- [x] **Step 3: Implement `AudioFxController` managing hardware `Equalizer` and `BassBoost`**
- [x] **Step 4: Implement `EqualizerViewModel` with StateFlow for band gains, active preset, and boost dials**
- [x] **Step 5: Implement `EqualizerScreen.kt` with custom spline Canvas curve and vertical band sliders**
- [x] **Step 6: Run unit tests to verify they pass**
- [x] **Step 7: Commit changes**

---

### Task 3: Car Dashboard Mode Screen
**Screen:** 06 (`06_car_dashboard_mode.html` / `.png`)

**Files:**
- Create: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/car/CarDashboardScreen.kt`
- Create: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/car/CarDashboardViewModel.kt`
- Test: `punjabi-beats/app/src/test/java/com/punjabibeats/app/CarDashboardViewModelTest.kt`

**Interfaces:**
- Produces: High-contrast, large touch target driving UI:
  - Giant Play/Pause button (min 80dp touch target).
  - Prominent Next/Previous buttons and big track title/artist display.
  - Quick driving presets row: `"Highway Bass"`, `"Late Night Sufi"`, `"Bhangra Energy"`.
  - Quick launch pill to open Car DSP Calibration screen.
  - Keep-screen-on utility flag.

- [x] **Step 1: Write unit test `CarDashboardViewModelTest` verifying driving preset switching and playback state tracking**
- [x] **Step 2: Run test to verify failure**
- [x] **Step 3: Implement `CarDashboardViewModel`**
- [x] **Step 4: Implement `CarDashboardScreen.kt` with large touch buttons and ambient contrast**
- [x] **Step 5: Run unit tests to verify they pass**
- [x] **Step 6: Commit changes**

---

### Task 4: Car DSP Calibration & 2D Soundstage Screen
**Screen:** 13 (`13_car_dsp_calibration.html` / `.png`)

**Files:**
- Create: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/car/CarDspCalibrationScreen.kt`
- Create: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/car/CarDspViewModel.kt`
- Test: `punjabi-beats/app/src/test/java/com/punjabibeats/app/CarDspViewModelTest.kt`

**Interfaces:**
- Produces: Vehicle acoustic tuning interface:
  - Interactive 2D Soundstage Canvas (Fader / Balance coordinate pad with vehicle wireframe).
  - Cabin Acoustic presets: `"Sedan"`, `"SUV"`, `"Convertible"`, `"Truck"`.
  - Channel time-alignment delay adjustments (FL, FR, RL, RR, Sub in ms).
  - Subwoofer crossover frequency selector (50Hz, 80Hz, 120Hz).

- [x] **Step 1: Write unit test `CarDspViewModelTest` verifying balance/fader calculation and cabin preset values**
- [x] **Step 2: Run test to verify failure**
- [x] **Step 3: Implement `CarDspViewModel` with StateFlow for coordinates, speaker gain offsets, and delays**
- [x] **Step 4: Implement `CarDspCalibrationScreen.kt` with interactive draggable Canvas listener**
- [x] **Step 5: Run unit tests to verify they pass**
- [x] **Step 6: Commit changes**

---

### Task 5: Dynamic Island Morphing Overlay & Lock Screen Widget
**Screens:** 02 (Morphing), 10 (Expanded Overlay), 11 (Lock Screen Widget)

**Files:**
- Create: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/components/dynamicisland/DynamicIslandPill.kt`
- Create: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/components/dynamicisland/DynamicIslandExpandedCard.kt`
- Create: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/components/dynamicisland/DynamicIslandState.kt`
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/PunjabiBeatsAppRoot.kt`
- Test: `punjabi-beats/app/src/test/java/com/punjabibeats/app/DynamicIslandStateTest.kt`

**Interfaces:**
- Produces: Dynamic Island system supporting 3 states:
  - `COMPACT`: Mini pill under camera notch with live equalizer visualizer and tiny rotating album art.
  - `EXPANDED`: Dropdown overlay card with waveform scrubber, playback controls, and audio route selector.
  - `DISMISSED`: Swipe away to collapse.

- [x] **Step 1: Write unit test `DynamicIslandStateTest` verifying state transitions and gestures**
- [x] **Step 2: Run test to verify failure**
- [x] **Step 3: Implement `DynamicIslandState.kt` with state machine and transition animators**
- [x] **Step 4: Implement `DynamicIslandPill.kt` and `DynamicIslandExpandedCard.kt`**
- [x] **Step 5: Mount Dynamic Island overlay in `PunjabiBeatsAppRoot.kt` with toggle flag**
- [x] **Step 6: Run unit tests to verify they pass**
- [x] **Step 7: Commit changes**

---

### Task 6: Now Playing & Live Lyrics Screen Polish
**Screen:** 07 (`07_now_playing_and_lyrics_295.html` / `.png`)

**Files:**
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/player/FullPlayerScreen.kt`
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/lyrics/LyricsSheet.kt`
- Test: `punjabi-beats/app/src/test/java/com/punjabibeats/app/PlayerViewModelTest.kt`

**Interfaces:**
- Produces:
  - 3-mode visualizer switch above seekbar (`"Wave"`, `"Dot"`, `"Neon Glow"`).
  - Real-time Karaoke On/Off badge toggle with animated pulse.
  - "Up Next" capsule card at bottom showing upcoming queue track.

- [x] **Step 1: Write unit test in `PlayerViewModelTest` verifying visualizer mode switching**
- [x] **Step 2: Implement multi-mode visualizer composable (Wave / Dot / Neon)**
- [x] **Step 3: Add Karaoke indicator toggle badge in lyrics view**
- [x] **Step 4: Add "Up Next" preview capsule card in `FullPlayerScreen.kt`**
- [x] **Step 5: Run unit tests to verify they pass**
- [x] **Step 6: Commit changes**

---

### Task 7: Search & Explore and Library Screens Refresh
**Screens:** 01 (Search & Explore), 08 (Library & Playlists)

**Files:**
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/search/SearchScreen.kt`
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/library/LibraryScreen.kt`
- Test: `punjabi-beats/app/src/test/java/com/punjabibeats/app/SearchViewModelTest.kt`

**Interfaces:**
- Produces:
  - Search & Explore with Category pills (All, Tracks, Artists, Playlists, Remixes), Trending tag cloud, and Curated Genre cards.
  - Library Screen with tabbed filters (Playlists, Downloads, Liked, Artists) and download storage meter.

- [x] **Step 1: Write unit test verifying category filter selection in `SearchViewModelTest`**
- [x] **Step 2: Update `SearchScreen.kt` with Stitch category filter pills and trending charts**
- [x] **Step 3: Update `LibraryScreen.kt` with tabbed header and storage indicators**
- [x] **Step 4: Run unit tests to verify they pass**
- [x] **Step 5: Commit changes**

---

### Task 8: Settings & Appearance and AI Studio Screen Refresh
**Screens:** 05 (AI Studio), 09 (Settings & Appearance)

**Files:**
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/settings/SettingsScreen.kt`
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/ai/AiStudioScreen.kt`
- Test: `punjabi-beats/app/src/test/java/com/punjabibeats/app/SettingsViewModelTest.kt`

**Interfaces:**
- Produces:
  - Settings Screen switches for: Dynamic Island enable/disable, Auto-Launch Car Mode on Bluetooth, Audio Quality selector (Lossless Hi-Res / 320kbps / Normal).
  - Navigation item in Settings to launch Equalizer and Car DSP Calibration.
  - AI Studio prompt presets row ("UK Drill", "Folk Tumbi Pop", "Old School Bhangra", "Lo-Fi Boliyan").

- [x] **Step 1: Write unit test in `SettingsViewModelTest` for new preferences**
- [x] **Step 2: Update `SettingsScreen.kt` with toggles and Equalizer / DSP navigation rows**
- [x] **Step 3: Update `AiStudioScreen.kt` with prompt preset chips**
- [x] **Step 4: Run unit tests to verify they pass**
- [x] **Step 5: Commit changes**

---

### Task 9: Navigation Wiring & Full Verification Suite
**Files:**
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/navigation/AppNavHost.kt`
- Verify: Full test suite

**Interfaces:**
- Connects new routes:
  - `Screen.Equalizer.route` -> `EqualizerScreen`
  - `Screen.CarDashboard.route` -> `CarDashboardScreen`
  - `Screen.CarDsp.route` -> `CarDspCalibrationScreen`

- [x] **Step 1: Add new navigation destinations in `AppNavHost.kt`**
- [x] **Step 2: Run full unit test suite `.\gradlew.bat testDebugUnitTest`**
- [x] **Step 3: Verify zero test regressions across all test classes**
- [x] **Step 4: Commit and push changes to git `main`**
