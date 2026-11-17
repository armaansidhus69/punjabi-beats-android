# Vice City Neon Dusk UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform Punjabi Beats UI into the exact Neon Magenta / Dusk Cyberpunk Vice City Edition styling shown in the user's reference screenshots.

**Architecture:** Jetpack Compose Material 3 implementation with custom canvas drawing for neon halo glows and double glowing rings, modular composables for Home Screen header/hero/chips/moods, floating glowing mini player, floating active pill bottom dock, and full player vinyl with animated spectrum equalizer and 4 action cards.

**Tech Stack:** Kotlin 2.x, Android Jetpack Compose Material 3, Canvas Drawing & Gradients, Coil Compose, Gradle 8.x.

**Spec:** `docs/superpowers/specs/2026-11-16-vice-city-neon-ui-design.md`

## Global Constraints
- Accent color `#FF2E93` (Neon Magenta / Hot Pink)
- Background `#08060D` (Obsidian Dark)
- Surface `#130E1B` (Dark Glass)
- Golden Crown `#FFAA00` with cursive script "Punjabi Beats"
- Must pass `.\gradlew.bat testDebugUnitTest` without regressions
- Strict Constraint: Do NOT generate new APK binaries (keep existing `apk/punjabi-beats-v1.0.0-legacy.apk` untouched)

## Review Focus
1. Palette contrast: White text on Obsidian background `#08060D` must maintain crisp legibility.
2. Turntable Halo: Circular pink neon glow behind turntable platter must not clip or distort vinyl rotation.
3. Bottom Bar: Floating dock and active pill must properly accommodate system insets / navigation bars.
4. Equalizer Visualizer: Vertical dancing bars must render smoothly in sync with playback state.
5. Mood Tiles: 4 distinct glow borders (amber, cyan, purple, crimson) must scale appropriately on standard phone widths.

---

### Task 1: Theme & Palette Updates in `PBTheme.kt`

**Files:**
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/theme/PBTheme.kt`
- Test: `punjabi-beats/app/src/test/java/com/punjabibeats/app/ThemeEngineTest.kt`

**Interfaces:**
- Produces: `PBTheme.kt` with Vice City / Neon palette as the default / primary theme (`id = "classic"` / `"vice"`) featuring `#FF2E93` accent, `#08060D` background, `#130E1B` surface, `#1B1224` card, `#FFAA00` secondary, and dusk sunset hero gradient.

- [x] **Step 1: Write failing test in ThemeEngineTest for the Vice City Neon palette**
- [x] **Step 2: Run test to verify it fails**
- [x] **Step 3: Update `PBTheme.kt` with exact neon pink palette and dusk hero gradient**
- [x] **Step 4: Run test to verify it passes**
- [x] **Step 5: Commit changes**

---

### Task 2: Neon Curated Artists & Mood Cards in `CommonUi.kt`

**Files:**
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/components/CommonUi.kt`
- Test: `punjabi-beats/app/src/test/java/com/punjabibeats/app/ThemeEngineTest.kt`

**Interfaces:**
- Consumes: `LocalPBTheme.current`
- Produces: `ArtistCard` with double neon pink glowing rings (`#FF2E93`), `MoodTile` with individual neon borders (amber, cyan, purple, crimson), and category filter chips.

- [x] **Step 1: Write test verifying double-ring styling parameters**
- [x] **Step 2: Implement double neon glowing rings in `ArtistCard` and updated `MoodTile` in `CommonUi.kt`**
- [x] **Step 3: Run tests to verify they pass**
- [x] **Step 4: Commit changes**

---

### Task 3: Home Screen Layout & Top Header in `HomeScreen.kt`

**Files:**
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/home/HomeScreen.kt`

**Interfaces:**
- Consumes: `CommonUi.kt`, `PBTheme.kt`
- Produces: Top header with menu icon `☰`, centered Golden Crown `👑` with cursive "Punjabi Beats" and subtitle `"YOUR MUSIC • YOUR UNIVERSE"`, notification bell `🔔` with unread dot, glowing pink pill search bar with microphone icon, "JATTWAAD - DAILY PUNJABI HITS" hero banner with glowing play button, category chips, curated artists row, and 4-card horizontal moods row.

- [x] **Step 1: Implement custom top bar header, glowing search bar pill, category chips, hero banner, and moods row**
- [x] **Step 2: Verify `HomeScreen` compiles and runs cleanly**
- [x] **Step 3: Commit changes**

---

### Task 4: Floating Mini Player & Bottom Navigation Dock

**Files:**
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/components/MiniPlayerBar.kt`
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/PunjabiBeatsAppRoot.kt`

**Interfaces:**
- Produces: `MiniPlayerBar` with glowing `#FF2E93` border, live audio waveform equalizer at bottom, heart icon, and `PbBottomBar` with hot-pink pill highlight on active tab.

- [x] **Step 1: Implement glowing border and audio equalizer waveform in `MiniPlayerBar.kt`**
- [x] **Step 2: Implement active hot-pink pill dock styling in `PunjabiBeatsAppRoot.kt`**
- [x] **Step 3: Run unit tests to verify no regressions**
- [x] **Step 4: Commit changes**

---

### Task 5: Full Player Screen (Now Playing) in `FullPlayerScreen.kt`

**Files:**
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/player/FullPlayerScreen.kt`

**Interfaces:**
- Consumes: `TurntableDeck.kt`, `UniversalSeekbar.kt`
- Produces: Header `∨`, `// NOW PLAYING //`, `⋮`, turntable vinyl centerpiece with **vibrant circular neon pink halo glow** and dusk palm backdrop, dancing pink spectrum equalizer bars, 76dp circular double-ring glowing Play/Pause button, and 4 bottom action cards (`66 Lyrics`, `ııll Equalizer`, `⏱ Timer`, `≡🎵 Queue`).

- [x] **Step 1: Implement neon halo glow behind turntable and cover art**
- [x] **Step 2: Implement dancing pink bars spectrum equalizer above seekbar**
- [x] **Step 3: Implement 76dp double-ring glowing Play/Pause button**
- [x] **Step 4: Implement 4 bottom action cards (`66 Lyrics`, `ııll Equalizer`, `⏱ Timer`, `≡🎵 Queue`)**
- [x] **Step 5: Run tests and verify compilation**
- [x] **Step 6: Commit changes**

---

### Task 6: Verification & Test Suite

**Files:**
- Verify: Full codebase
- Constraints: Do NOT assemble new APKs or modify `apk/punjabi-beats-v1.0.0-legacy.apk`

- [x] **Step 1: Run full unit test suite `.\gradlew.bat testDebugUnitTest`**
- [x] **Step 2: Verify zero regression across all test cases**
- [x] **Step 3: Commit and push clean source changes to GitHub `main` branch**
