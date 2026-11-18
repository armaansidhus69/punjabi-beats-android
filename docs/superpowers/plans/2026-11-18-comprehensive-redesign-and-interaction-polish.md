# Comprehensive Redesign & Interaction Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Standardize navigation with a fixed bottom bar and uniform screen transitions, fix the AI Studio chat dock at the bottom, embed real-time animated previews for all 22 interactive seekbars in the main player, make the queue ultra-responsive, add rich lyrics & haptics settings customization, and eliminate all dead buttons and navigation links.

**Architecture:** 
- Compose Material 3 navigation refactored to use synchronized directional/fade transitions across top-level destinations (Home, Search, Jam, AI Studio, Library) with a persistent, non-flickering bottom bar.
- AI Studio layout restructured to isolate scrollable conversation history from a sticky, keyboard-responsive (`imePadding()`) bottom chat input dock.
- `SeekbarStyle` enum and `ProgressSliderCollection` aligned to 22 verified styles (featuring Comet, Lightning, Constellation, Bubbles, RGB Glow, DNA Helix, Liquid Tube) with an embedded live preview carousel in `FullPlayerScreen` utilizing animated mini-canvases and instant animated style transitions.
- `QueueDrawer` re-architected to uncouple drag-and-drop gesture detection from tap-to-play, ensuring 100% instantaneous song selection responsiveness.
- `SettingsDataStore` expanded with DataStore preferences for lyrics alignment (Left, Center, Right), text size slider (16–32sp), line spacing (4–24dp), and touch feedback/heartbeat haptics, consumed by `LyricsScreen` and seekbars.
- Full UI audit fixing all dead buttons (e.g., Home menu button, notification bell, action chips).

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, AndroidX Navigation Compose, AndroidX DataStore Preferences, Kotlinx Coroutines & Flow, Hilt, JUnit 4.

**Spec:** User request dated 2026-11-18 for comprehensive Punjabi Beats redesign, navigation standardization, AI Studio fixed dock, 22 seekbar live player previews, responsive queue, settings customization, and dead button fixes.

---

## Global Constraints

- Android Target SDK: 34, Min SDK: 24, Compose Material 3.
- Never touch or replace legacy APK `apk/punjabi-beats-v1.0.0-legacy.apk`.
- Zero regressions across existing test suites (`.\gradlew.bat testDebugUnitTest` must pass 100%).
- All 22 seekbar styles must have working Canvas rendering, unique IDs, and live animation support.
- File references in markdown and responses must use clickable `file:///` links.

---

## Review Focus

1. **Top-Level Navigation Transition Flicker**: Rapidly clicking between bottom bar tabs (Home -> Search -> AI Studio -> Library) must not recreate viewmodels, cause jumpy frame stutters, or show black flickers.
2. **Keyboard Obscuring AI Studio Chat Input**: Opening the soft keyboard on smaller devices must smoothly slide the chat input bar above the keyboard (`imePadding()`) without hiding the send button or overlapping messages.
3. **Seekbar Live Previews Performance**: Rendering 22 miniature animated seekbars in a carousel must remain performant (60fps) by only drawing lightweight canvas previews and avoiding expensive heavy recompositions.
4. **Queue Tap vs Drag Conflict**: Tapping any song in `QueueDrawer` must instantly seek and play without being consumed or delayed by the drag-and-drop reorder gesture recognizer.
5. **Lyrics Settings Persistence & Boundary Handling**: Lyrics font size and line spacing slider values must persist across app restarts and safely clamp within legible bounds (16sp - 32sp).

---

## File Structure

### Modified Files:
- [PunjabiBeatsAppRoot.kt](file:///D:/z.ai%20app%20punjabi%20beat/punjabi-beats-all-code-v1.1/punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/PunjabiBeatsAppRoot.kt): Standardize bottom bar dock, elevation, and layout padding.
- [AppNavHost.kt](file:///D:/z.ai%20app%20punjabi%20beat/punjabi-beats-all-code-v1.1/punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/navigation/AppNavHost.kt): Implement uniform cross-fade & subtle slide transitions between top-level tabs.
- [AiStudioScreen.kt](file:///D:/z.ai%20app%20punjabi%20beat/punjabi-beats-all-code-v1.1/punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/ai/AiStudioScreen.kt): Restructure layout into fixed bottom input bar with IME padding and smooth scroll.
- [SeekbarStyle.kt](file:///D:/z.ai%20app%20punjabi%20beat/punjabi-beats-all-code-v1.1/punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/components/sliders/SeekbarStyle.kt): Align 22 styles explicitly featuring Comet, Lightning, Constellation, Bubbles, RGB Glow, DNA Helix, and Liquid Tube.
- [ProgressSliderCollection.kt](file:///D:/z.ai%20app%20punjabi%20beat/punjabi-beats-all-code-v1.1/punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/components/sliders/ProgressSliderCollection.kt): Implement updated Canvas shaders for Lightning, Constellation, Bubbles, and Liquid Tube, plus miniature live preview component `SeekbarMiniLivePreview`.
- [FullPlayerScreen.kt](file:///D:/z.ai%20app%20punjabi%20beat/punjabi-beats-all-code-v1.1/punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/player/FullPlayerScreen.kt): Embed horizontal live preview carousel directly in the player with animated style cross-fading.
- [PlayerSheets.kt](file:///D:/z.ai%20app%20punjabi%20beat/punjabi-beats-all-code-v1.1/punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/player/components/PlayerSheets.kt): Decouple drag-and-drop handles from song row clicks for instantaneous queue song selection.
- [SettingsDataStore.kt](file:///D:/z.ai%20app%20punjabi%20beat/punjabi-beats-all-code-v1.1/punjabi-beats/app/src/main/java/com/punjabibeats/app/data/local/datastore/SettingsDataStore.kt): Add keys for lyrics alignment, text size, line spacing, and touch feedback/heartbeat haptics.
- [SettingsViewModel.kt](file:///D:/z.ai%20app%20punjabi%20beat/punjabi-beats-all-code-v1.1/punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/settings/SettingsViewModel.kt): Expose StateFlows and update methods for new preferences.
- [SettingsScreen.kt](file:///D:/z.ai%20app%20punjabi%20beat/punjabi-beats-all-code-v1.1/punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/settings/SettingsScreen.kt): Add UI controls for lyrics typography (Alignment chips, text size slider, line spacing slider) and haptic feedback toggle.
- [LyricsScreen.kt](file:///D:/z.ai%20app%20punjabi%20beat/punjabi-beats-all-code-v1.1/punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/lyrics/LyricsScreen.kt): Bind text alignment, font size, and line spacing dynamically to user settings.
- [HomeScreen.kt](file:///D:/z.ai%20app%20punjabi%20beat/punjabi-beats-all-code-v1.1/punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/home/HomeScreen.kt): Wire Menu button (`Routes.SETTINGS`), Notification bell action, and verify all click targets.

### Test Files:
- [SeekbarSlidersTest.kt](file:///D:/z.ai%20app%20punjabi%20beat/punjabi-beats-all-code-v1.1/punjabi-beats/app/src/test/java/com/punjabibeats/app/SeekbarSlidersTest.kt): Verify 22 seekbar styles, unique IDs, and named styles.
- [NavigationAndQueueTest.kt](file:///D:/z.ai%20app%20punjabi%20beat/punjabi-beats-all-code-v1.1/punjabi-beats/app/src/test/java/com/punjabibeats/app/NavigationAndQueueTest.kt): Unit test for queue selection and navigation route integrity.
- [LyricsSettingsDataStoreTest.kt](file:///D:/z.ai%20app%20punjabi%20beat/punjabi-beats-all-code-v1.1/punjabi-beats/app/src/test/java/com/punjabibeats/app/LyricsSettingsDataStoreTest.kt): Unit test for lyrics customization and haptic settings persistence.

---

## Tasks

### Task 1: Standardize Navigation & Fixed Bottom Bar with Uniform Transitions

**Files:**
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/PunjabiBeatsAppRoot.kt`
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/navigation/AppNavHost.kt`
- Test: `punjabi-beats/app/src/test/java/com/punjabibeats/app/NavigationAndQueueTest.kt`

- [ ] **Step 1: Write unit test for top-level navigation routes & destinations**
  Create `NavigationAndQueueTest.kt` verifying that all top-level destinations (Home, Search, Jam, AI Studio, Library) resolve with valid unique routes, and route hierarchy matches expected navigation patterns.

- [ ] **Step 2: Run test to verify it executes**
  Run: `cd "punjabi-beats-all-code-v1.1\punjabi-beats"; .\gradlew.bat testDebugUnitTest --tests "com.punjabibeats.app.NavigationAndQueueTest"`

- [ ] **Step 3: Standardize uniform transitions in `AppNavHost.kt`**
  Update `NavHost` transitions to provide smooth, uniform transitions across top-level screens:
  - Enter: `fadeIn(animationSpec = tween(220)) + slideInHorizontally(initialOffsetX = { 40 }, animationSpec = tween(220))`
  - Exit: `fadeOut(animationSpec = tween(200))`
  - PopEnter: `fadeIn(animationSpec = tween(220)) + slideInHorizontally(initialOffsetX = { -40 }, animationSpec = tween(220))`
  - PopExit: `fadeOut(animationSpec = tween(200))`
  Ensure detail screens (Album, Artist, Player, Equalizer, Car Dashboard) have coordinated vertical/horizontal transitions.

- [ ] **Step 4: Standardize fixed bottom bar dock in `PunjabiBeatsAppRoot.kt`**
  - Ensure `PbBottomBar` is firmly anchored inside `Scaffold`'s `bottomBar` slot with zero layout shift.
  - Maintain solid dark glass surface styling (`theme.surface.copy(alpha = 0.96f)`) with clean border separation.
  - Ensure `Modifier.navigationBarsPadding()` is correctly applied so bottom navigation never overlaps Android system gesture navigation bars.

- [ ] **Step 5: Verify unit tests pass**
  Run: `cd "punjabi-beats-all-code-v1.1\punjabi-beats"; .\gradlew.bat testDebugUnitTest --tests "com.punjabibeats.app.NavigationAndQueueTest"`

- [ ] **Step 6: Commit changes**
  Run: `git add . && git commit -m "feat(nav): standardize fixed bottom bar and uniform screen transitions"`

---

### Task 2: AI Studio Fixed Bottom Chat Input & Keyboard Responsiveness

**Files:**
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/ai/AiStudioScreen.kt`
- Test: `punjabi-beats/app/src/test/java/com/punjabibeats/app/AiStudioScreenTest.kt`

- [ ] **Step 1: Write test for AI Studio state and input event flow**
  Create/update test in `AiStudioScreenTest.kt` verifying message input validation, prompt chip triggers, and send action invocations.

- [ ] **Step 2: Refactor `AiStudioScreen.kt` layout hierarchy**
  - Restructure `ChatTab`:
    ```kotlin
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Chat history bubbles
        }
        // Fixed bottom dock
        AiChatInputDock(
            input = uiState.chatInput,
            onInputChange = viewModel::onChatInputChange,
            onSend = viewModel::sendChatMessage,
            isThinking = uiState.isThinking,
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.surface)
                .border(1.dp, theme.panel2, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
    ```
  - Ensure the chat input field stays permanently visible at the bottom of the viewport, effortlessly accessible with 1-tap, and smoothly ascends above the soft keyboard without hiding behind any bottom chrome.
  - Add auto-scroll to the latest message (`listState.animateScrollToItem(messages.size - 1)`) whenever a new message arrives.

- [ ] **Step 3: Run unit tests**
  Run: `cd "punjabi-beats-all-code-v1.1\punjabi-beats"; .\gradlew.bat testDebugUnitTest`

- [ ] **Step 4: Commit changes**
  Run: `git add . && git commit -m "feat(ai): implement fixed bottom chat input dock with responsive IME handling"`

---

### Task 3: Standardize 22 Seekbar Styles (Comet, Lightning, Constellation, Bubbles, RGB Glow, DNA Helix, Liquid Tube)

**Files:**
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/components/sliders/SeekbarStyle.kt`
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/components/sliders/ProgressSliderCollection.kt`
- Test: `punjabi-beats/app/src/test/java/com/punjabibeats/app/SeekbarSlidersTest.kt`

- [ ] **Step 1: Update `SeekbarSlidersTest.kt` with exact requirement assertions**
  Update `SeekbarSlidersTest.kt` to assert that:
  - Exactly 22 styles are defined.
  - The 7 required styles exist:
    - `COMET` ("comet", "Comet Tail")
    - `LIGHTNING` ("lightning", "Lightning Bolt")
    - `CONSTELLATION` ("constellation", "Stellar Constellation")
    - `BUBBLES` ("bubbles", "Floating Bubbles")
    - `RGB_GLOW` ("rgb_glow", "RGB Chroma Glow")
    - `DNA_HELIX` ("dna_helix", "DNA Double Helix")
    - `LIQUID_TUBE` ("liquid_tube", "Liquid Neon Tube")
  - All 22 styles have unique non-empty IDs, names, icons, and descriptions.

- [ ] **Step 2: Run test to confirm updated requirements**
  Run: `cd "punjabi-beats-all-code-v1.1\punjabi-beats"; .\gradlew.bat testDebugUnitTest --tests "com.punjabibeats.app.SeekbarSlidersTest"`

- [ ] **Step 3: Align `SeekbarStyle.kt` enum**
  Update enum definitions in `SeekbarStyle.kt`:
  - `LIGHTNING("lightning", "Lightning Bolt", "⚡", "High-intensity electric lightning bolt laser arcs")`
  - `CONSTELLATION("constellation", "Stellar Constellation", "✨", "Star cluster cosmic network with connected nodes")`
  - `BUBBLES("bubbles", "Floating Bubbles", "🫧", "Effervescent floating audio bubble particles")`
  - `LIQUID_TUBE("liquid_tube", "Liquid Tube", "🧪", "Fluid visceral liquid mercury/neon plasma tube")`
  - Keep `COMET`, `RGB_GLOW`, `DNA_HELIX`, `CLASSIC_WAVEFORM`, `WAVY`, `PILL_CAPSULE`, `SEGMENTS`, `EQUALIZER`, `HEARTBEAT`, `VINYL_GROOVE`, `RETRO_TAPE`, `FIRE_EMBER`, `DOT_MATRIX`, `AUDIO_SPECTRUM`, `LASER_BEAM`, `PUNJABI_RHYTHM`, `GOLDEN_SPARKLE`, `DUAL_GRADIENT`.
  Ensure `fromId()` maps legacy IDs safely.

- [ ] **Step 4: Implement enhanced Canvas rendering in `ProgressSliderCollection.kt`**
  - **Lightning**: Draw dynamic jagged electric branching segments with intense electric cyan glow and discharge spikes.
  - **Constellation**: Draw shining stellar vertices with subtle pulsating connecting lines along the active progress path.
  - **Bubbles**: Draw buoyant circular bubbles oscillating vertically with specular highlights and fluid motion.
  - **Liquid Tube**: Draw a rounded cylindrical chamber containing animated viscous liquid meniscus with gradient wave sloshing.
  - Enhance animation loops using `rememberInfiniteTransition` for fluid 60fps movement.

- [ ] **Step 5: Run unit tests**
  Run: `cd "punjabi-beats-all-code-v1.1\punjabi-beats"; .\gradlew.bat testDebugUnitTest --tests "com.punjabibeats.app.SeekbarSlidersTest"`

- [ ] **Step 6: Commit changes**
  Run: `git add . && git commit -m "feat(seekbar): standardize 22 seekbar styles including Lightning, Constellation, Bubbles, and Liquid Tube"`

---

### Task 4: Main Player Live Seekbar Style Preview Carousel & Instant Smooth Switcher

**Files:**
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/components/sliders/ProgressSliderCollection.kt`
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/player/FullPlayerScreen.kt`
- Test: `punjabi-beats/app/src/test/java/com/punjabibeats/app/FullPlayerVisualizerTest.kt`

- [ ] **Step 1: Write test for real-time seekbar style switching**
  In `FullPlayerVisualizerTest.kt`, add assertions verifying that selecting any of the 22 seekbar styles triggers state updates in `PlayerViewModel` and persists the selection.

- [ ] **Step 2: Build `SeekbarMiniLivePreview` composable in `ProgressSliderCollection.kt`**
  Create a lightweight, high-performance miniature preview component:
  ```kotlin
  @Composable
  fun SeekbarMiniLivePreview(
      style: SeekbarStyle,
      isSelected: Boolean,
      onClick: () -> Unit,
      modifier: Modifier = Modifier
  )
  ```
  - Displays emoji + compact title.
  - Embeds an active animated mini-canvas showing the style's distinctive visual animation in real time.
  - Displays selected ring indicator with glowing border when active.

- [ ] **Step 3: Embed interactive horizontal preview carousel in `FullPlayerScreen.kt`**
  - Add `LazyRow` directly below the active seekbar or in a collapsible "Seekbar Studio" strip:
    - 22 interactive live preview cards with smooth scroll.
    - 1-tap switches the main seekbar immediately.
    - Animated transition using `AnimatedContent` or `Crossfade` with `tween(300)` when switching styles.
  - Keep the full grid bottom sheet (`SeekbarStylePickerSheet`) available as an expanded modal option.

- [ ] **Step 4: Run unit tests**
  Run: `cd "punjabi-beats-all-code-v1.1\punjabi-beats"; .\gradlew.bat testDebugUnitTest`

- [ ] **Step 5: Commit changes**
  Run: `git add . && git commit -m "feat(player): embed live preview carousel for all 22 seekbar styles in main player"`

---

### Task 5: Ultra-Responsive Queue Drawer & Song Selection Handling

**Files:**
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/player/components/PlayerSheets.kt`
- Test: `punjabi-beats/app/src/test/java/com/punjabibeats/app/NavigationAndQueueTest.kt`

- [ ] **Step 1: Write test for instant queue track selection**
  Add test asserting that clicking track at index `k` in queue dispatches `controller.seekToIndex(k)` accurately and updates playing state.

- [ ] **Step 2: Refactor `QueueDrawer` touch & gesture hierarchy**
  - In `PlayerSheets.kt` `QueueDrawer`:
    - Remove full-row drag interceptors that steal or delay tap clicks.
    - Restrict `.draggableItem` / `.dragContainer` strictly to the dedicated `DragHandle` icon on the right edge.
    - Make the entire song info area (artwork, title, artist, duration) an unhindered `Modifier.clickable(indication = ripple(), interactionSource = remember { MutableInteractionSource() }) { controller.seekToIndex(index); onDismiss() }` (or keep open with instant active song visual highlight).
    - Add active track glowing badge and equalizer wave indicator on the currently playing queue item.

- [ ] **Step 3: Run unit tests**
  Run: `cd "punjabi-beats-all-code-v1.1\punjabi-beats"; .\gradlew.bat testDebugUnitTest`

- [ ] **Step 4: Commit changes**
  Run: `git add . && git commit -m "fix(queue): decouple drag handle from row click for ultra-responsive song selection"`

---

### Task 6: Comprehensive Lyrics & Touch Customization in Settings & DataStore

**Files:**
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/data/local/datastore/SettingsDataStore.kt`
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/settings/SettingsViewModel.kt`
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/settings/SettingsScreen.kt`
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/lyrics/LyricsScreen.kt`
- Test: `punjabi-beats/app/src/test/java/com/punjabibeats/app/LyricsSettingsDataStoreTest.kt`

- [ ] **Step 1: Write unit tests for new Settings DataStore keys and viewmodel bindings**
  Create `LyricsSettingsDataStoreTest.kt` verifying:
  - Lyrics alignment persistence (`LEFT`, `CENTER`, `RIGHT`).
  - Text size slider persistence (clamped between 16sp and 32sp, default 22sp).
  - Line spacing slider persistence (clamped between 4dp and 24dp, default 10dp).
  - Touch feedback & heartbeat haptics toggle persistence.

- [ ] **Step 2: Run test to verify failure before implementation**
  Run: `cd "punjabi-beats-all-code-v1.1\punjabi-beats"; .\gradlew.bat testDebugUnitTest --tests "com.punjabibeats.app.LyricsSettingsDataStoreTest"`

- [ ] **Step 3: Update `SettingsDataStore.kt` & `SettingsViewModel.kt`**
  - Add keys:
    - `LYRICS_ALIGNMENT` (String: `"LEFT"`, `"CENTER"`, `"RIGHT"`)
    - `LYRICS_TEXT_SIZE` (Float: 16f to 32f)
    - `LYRICS_LINE_SPACING` (Float: 4f to 24f)
    - `TOUCH_FEEDBACK_ENABLED` (Boolean)
  - Expose reactive `Flow`s and suspend mutation functions.
  - Bind to `SettingsViewModel`.

- [ ] **Step 4: Add UI controls to `SettingsScreen.kt`**
  - **Lyrics & Teleprompter Typography Section**:
    - Segmented alignment selector: [Left (⇤), Center (≣), Right (⇥)].
    - Font Size Slider (16sp – 32sp) with live value label ("22 sp").
    - Line Spacing Slider (4dp – 24dp) with live value label ("10 dp").
    - Real-time lyrics teleprompter preview card displaying sample Gurmukhi/English lyrics with immediate visual feedback.
  - **Touch & Haptic Feedback Section**:
    - Haptic Heartbeat on Seek & Sliders toggle.
    - Touch Feedback on Transport Controls toggle.

- [ ] **Step 5: Bind preferences into `LyricsScreen.kt`**
  - Read `lyricsAlignment`, `lyricsTextSize`, and `lyricsLineSpacing` from `SettingsViewModel` / `PlayerViewModel`.
  - Apply `textAlign = when (alignment) { "CENTER" -> TextAlign.Center; "RIGHT" -> TextAlign.End; else -> TextAlign.Start }`.
  - Apply dynamic `fontSize = textSize.sp` and `Spacer(Modifier.height(lineSpacing.dp))`.

- [ ] **Step 6: Run unit tests**
  Run: `cd "punjabi-beats-all-code-v1.1\punjabi-beats"; .\gradlew.bat testDebugUnitTest --tests "com.punjabibeats.app.LyricsSettingsDataStoreTest"`

- [ ] **Step 7: Commit changes**
  Run: `git add . && git commit -m "feat(settings): add lyrics alignment, text size, line spacing, and touch feedback controls"`

---

### Task 7: Comprehensive Audit and Fix of All Dead Buttons & Navigation Links

**Files:**
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/home/HomeScreen.kt`
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/navigation/AppNavHost.kt`
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/library/LibraryScreen.kt`
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/player/FullPlayerScreen.kt`
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/jam/JamScreen.kt`
- Test: All unit test suites

- [ ] **Step 1: Audit and wire dead buttons on `HomeScreen.kt`**
  - Wire `HomeTopBar` menu button:
    `onMenuClick = { navController.navigate(Routes.SETTINGS) }`
  - Wire `HomeTopBar` notification bell:
    `onNotificationClick = { snackbar.showSnackbar("🔔 No new notifications — All Punjabi hits are up to date!") }`
  - Ensure all mood category pills, artist chips, and song cards have working click listeners.

- [ ] **Step 2: Audit and wire buttons in `LibraryScreen.kt` & `FullPlayerScreen.kt`**
  - Verify Podcasts icon routes to `Routes.PODCASTS`.
  - Verify Settings icon routes to `Routes.SETTINGS`.
  - Verify Hero Liked Songs & Offline Vault cards trigger playback and player navigation correctly.
  - In `FullPlayerScreen.kt`, verify all bottom action cards (Lyrics, Equalizer, Sleep Timer, Queue) open their respective sheets/screens.

- [ ] **Step 3: Run full unit test suite**
  Run: `cd "punjabi-beats-all-code-v1.1\punjabi-beats"; .\gradlew.bat testDebugUnitTest`

- [ ] **Step 4: Build complete debug APK**
  Run: `cd "punjabi-beats-all-code-v1.1\punjabi-beats"; .\gradlew.bat assembleDebug`

- [ ] **Step 5: Commit changes**
  Run: `git add . && git commit -m "fix(ui): eliminate dead buttons and wire all navigation links across Home, Library, and Player"`
