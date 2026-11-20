# BitChord Section 7 UI, Canvas & Theming Architecture Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform the Punjabi Beats Android music player UI to match BitChord's Section 7 ("UI, Canvas & Theming") architecture, delivering dynamic artwork-driven palette theming, frosted glass UI components (LiquidGlass), animated motion video canvas player, spring-animated floating glass navigation dock, two-pane landscape and tablet player deck with synchronized lyrics, listening replay/wrapped analytics recap, and 2x2 & 4x2 home screen widgets.

**Architecture:**
- Real-time artwork palette extraction (`ArtworkPalette`) calculating dark background washes, elevated fills, high-contrast accents, and edge blurs with an LRU cache and smooth 260ms color transitions.
- Frosted glass pipeline (`LiquidGlass` & `OptimizedHaze`) utilizing hardware `RenderEffect` on Android 12+ (API 31+) with fallback translucent dither on older Android versions, applied to navigation dock, mini-player, and modals.
- Media3-driven looping video canvas background (`CanvasArtworkPlayer`) rendering synchronized motion video artwork seamlessly behind the Now Playing screen.
- Physics-based floating navigation bar (`FloatingGlassNavBar`) with spring-interpolated selection pills, icon scale dynamics, and specular highlight edges.
- Adaptive two-pane layout (`LandscapePlayerDeck`) for landscape and tablet screens hosting turntable controls on the left and synchronized live lyrics on the right.
- Annual & monthly listening recap (`ReplayScreen`) presenting story cards, listening analytics, and a shareable recap poster.
- Native Android `AppWidgetProvider` now-playing widgets (2x2 square and 4x2 wide) communicating with `MediaPlaybackService`.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, AndroidX Palette, AndroidX Navigation Compose, AndroidX Media3 (ExoPlayer & Session), Android RemoteViews & AppWidgetProvider, Coroutines & StateFlow, Hilt, Coil, JUnit 4.

**Spec:** `docs/superpowers/specs/2026-11-20-bitchord-section-7-ui-canvas-theming.md`

---

## Global Constraints

- Android Target SDK: 34, Min SDK: 24, Compose Material 3.
- Never overwrite, modify, or delete legacy APK `apk/punjabi-beats-v1.0.0-legacy.apk`.
- Zero regressions across existing test suites (`.\gradlew.bat testDebugUnitTest` must pass 100%).
- All new features must handle edge cases gracefully: null album art, missing video canvas URLs, portrait/landscape orientation flips, and widget updates when service is idle.
- File references in markdown and responses must use clickable `file:///` links.

---

## Review Focus

1. **Artwork Palette Contrast & Desaturation**: Grayscale or monochrome album covers must not produce arbitrary saturated hues (e.g. artificial maroon/brown); saturation floor adaptation must keep neutrals neutral while boosting chromatic covers for legible WCAG text contrast.
2. **Video Canvas Memory & Battery Leaks**: Background video player must immediately pause when the app is backgrounded, never decode audio tracks (`volume = 0f`), and cleanly release ExoPlayer instances upon composable disposal.
3. **Glass Performance on Low-End Devices**: RenderEffect blur must only activate on API 31+ without crashing or lagging on API 24-30 devices, falling back to lightweight translucent surfaces.
4. **Landscape Two-Pane Proportions**: On wide and tablet displays, lyrics must not stretch excessively wide across the screen; layout must balance a focused 45-50% left control deck and a centered 50-55% scrolling lyrics column.
5. **Widget Lifecycle & RemoteViews Update**: Play/Pause and Next clicks from the home screen widget must update state immediately without ANR or background service crashes when the app process is cold.

---

## File Structure

### New Files to Create:
- `app/src/main/java/com/punjabibeats/app/ui/theme/ArtworkPalette.kt`: Dynamic palette extraction, color derivation, and transition caching.
- `app/src/main/java/com/punjabibeats/app/ui/components/glass/LiquidGlass.kt`: Hardware-accelerated glass and frosted modifiers with API fallback and specular highlight.
- `app/src/main/java/com/punjabibeats/app/ui/components/canvas/CanvasArtwork.kt`: Video canvas model, title/artist match normalization, and source types.
- `app/src/main/java/com/punjabibeats/app/ui/components/canvas/CanvasArtworkPlayer.kt`: Media3 ExoPlayer looping video player view with alpha crossfade.
- `app/src/main/java/com/punjabibeats/app/ui/components/dock/FloatingGlassNavBar.kt`: Floating glass navigation dock with spring-animated selection pill and specular rim.
- `app/src/main/java/com/punjabibeats/app/ui/screens/player/decks/LandscapePlayerDeck.kt`: Two-pane landscape/tablet split layout with vinyl turntable and live synced lyrics.
- `app/src/main/java/com/punjabibeats/app/ui/screens/replay/ReplayModel.kt`: Analytics data models, listening stats generator, and recap metrics.
- `app/src/main/java/com/punjabibeats/app/ui/screens/replay/ReplayScreen.kt`: Story-style recap pager with animated metric cards and shareable poster sheet.
- `app/src/main/java/com/punjabibeats/app/widget/PunjabiBeatsWidgetProvider.kt`: AppWidgetProvider implementation for 2x2 and 4x2 home screen widgets.
- `app/src/main/res/layout/widget_punjabi_beats_square.xml`: 2x2 widget RemoteViews layout.
- `app/src/main/res/layout/widget_punjabi_beats_wide.xml`: 4x2 widget RemoteViews layout.
- `app/src/main/res/xml/widget_media_square_info.xml`: 2x2 widget metadata provider configuration.
- `app/src/main/res/xml/widget_media_wide_info.xml`: 4x2 widget metadata provider configuration.
- `app/src/test/java/com/punjabibeats/app/ArtworkPaletteTest.kt`: Unit tests for palette calculation and saturation adaptation.
- `app/src/test/java/com/punjabibeats/app/CanvasArtworkTest.kt`: Unit tests for canvas metadata matching and normalization.
- `app/src/test/java/com/punjabibeats/app/ReplayAnalyticsTest.kt`: Unit tests for replay recap calculations and personas.

### Existing Files to Modify:
- `app/src/main/java/com/punjabibeats/app/ui/PunjabiBeatsAppRoot.kt`: Integrate `FloatingGlassNavBar` and frosted mini-player dock.
- `app/src/main/java/com/punjabibeats/app/ui/screens/player/FullPlayerScreen.kt`: Integrate `CanvasArtworkPlayer` background and adaptive `LandscapePlayerDeck`.
- `app/src/main/java/com/punjabibeats/app/ui/navigation/Routes.kt`: Register `Routes.REPLAY`.
- `app/src/main/java/com/punjabibeats/app/ui/navigation/AppNavHost.kt`: Add route composable for `ReplayScreen`.
- `app/src/main/java/com/punjabibeats/app/ui/screens/home/HomeScreen.kt`: Add "Your 2026 Punjabi Replay" banner.
- `app/src/main/java/com/punjabibeats/app/playback/MediaPlaybackService.kt`: Broadcast widget updates on song / state changes.
- `app/src/main/AndroidManifest.xml`: Register widget providers and receivers.

---

## Tasks

### Task 1: Dynamic Artwork-Driven Theming (`ArtworkPalette`)

**Files:**
- Create: `app/src/main/java/com/punjabibeats/app/ui/theme/ArtworkPalette.kt`
- Create: `app/src/test/java/com/punjabibeats/app/ArtworkPaletteTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  data class ArtworkPalette(
      val background: Color,
      val wash: Color,
      val elevated: Color,
      val accent: Color,
      val onBackground: Color,
      val onBackgroundVariant: Color,
      val divider: Color,
  )
  fun adaptedArtworkSaturation(source: Float, minimum: Float, maximum: Float): Float
  fun relativeLuminance(argb: Int): Float
  ```

- [ ] **Step 1: Write failing unit test for `ArtworkPaletteTest`**
  Assert that `adaptedArtworkSaturation` preserves low saturation below chromatic threshold (< 0.12f) while clamping chromatic saturation between min and max; verify relative luminance calculation.
- [ ] **Step 2: Run test to verify it fails**
  Run: `cd "d:\z.ai app punjabi beat\punjabi-beats-all-code-v1.1\punjabi-beats"; .\gradlew.bat testDebugUnitTest --tests "com.punjabibeats.app.ArtworkPaletteTest"`
- [ ] **Step 3: Implement `ArtworkPalette.kt`**
  Implement `ArtworkPalette` data class, saturation adapter, luminance helpers, seed caching, and `@Composable fun rememberArtworkPalette(...)`.
- [ ] **Step 4: Run test to verify it passes**
  Run: `cd "d:\z.ai app punjabi beat\punjabi-beats-all-code-v1.1\punjabi-beats"; .\gradlew.bat testDebugUnitTest --tests "com.punjabibeats.app.ArtworkPaletteTest"`
- [ ] **Step 5: Commit changes**
  Run: `git add . && git commit -m "feat(theme): implement dynamic artwork-driven palette engine"`

---

### Task 2: Frosted Glass UI (`LiquidGlass` & `OptimizedHaze`)

**Files:**
- Create: `app/src/main/java/com/punjabibeats/app/ui/components/glass/LiquidGlass.kt`
- Modify: `app/src/main/java/com/punjabibeats/app/ui/components/MiniPlayerBar.kt`

**Interfaces:**
- Produces:
  ```kotlin
  fun Modifier.liquidGlass(
      shape: Shape = RoundedCornerShape(24.dp),
      blurRadius: Dp = 20.dp,
      tintColor: Color = Color.Black.copy(alpha = 0.55f),
      borderColor: Color = Color.White.copy(alpha = 0.20f),
      borderWidth: Dp = 0.8.dp,
  ): Modifier
  fun menuFrost(fill: Color, alpha: Float = 0.80f): Color
  ```

- [ ] **Step 1: Implement `LiquidGlass.kt`**
  Implement frosted glass modifiers with Android 12+ `RenderEffect.createBlurEffect` detection and graceful fallback for older Android versions, plus specular highlight hairline.
- [ ] **Step 2: Update `MiniPlayerBar.kt`**
  Wrap mini player container in `Modifier.liquidGlass(...)` for a frosted floating look above the navigation bar.
- [ ] **Step 3: Verify tests compile and pass**
  Run: `cd "d:\z.ai app punjabi beat\punjabi-beats-all-code-v1.1\punjabi-beats"; .\gradlew.bat testDebugUnitTest`
- [ ] **Step 4: Commit changes**
  Run: `git add . && git commit -m "feat(ui): implement frosted liquid glass modifiers and mini-player glass styling"`

---

### Task 3: Animated Motion Video Canvas Engine

**Files:**
- Create: `app/src/main/java/com/punjabibeats/app/ui/components/canvas/CanvasArtwork.kt`
- Create: `app/src/main/java/com/punjabibeats/app/ui/components/canvas/CanvasArtworkPlayer.kt`
- Create: `app/src/test/java/com/punjabibeats/app/CanvasArtworkTest.kt`
- Modify: `app/src/main/java/com/punjabibeats/app/ui/screens/player/FullPlayerScreen.kt`

**Interfaces:**
- Produces:
  ```kotlin
  enum class CanvasSource { SPOTIFY, OTHER }
  data class CanvasArtwork(
      val url: String,
      val fallbackUrl: String? = null,
      val title: String? = null,
      val artist: String? = null,
      val album: String? = null,
      val source: CanvasSource = CanvasSource.OTHER,
      val portraitUrl: String? = null,
  ) {
      fun matches(wantTitle: String, wantArtist: String, wantAlbum: String?): Boolean
  }
  @Composable fun CanvasArtworkPlayer(
      artwork: CanvasArtwork?,
      isPlaying: Boolean,
      modifier: Modifier = Modifier,
      fallbackArtworkUrl: String? = null,
  )
  ```

- [ ] **Step 1: Write failing unit test `CanvasArtworkTest`**
  Test title/artist normalization (diacritics, punctuation, feat. artist extraction) and matching logic.
- [ ] **Step 2: Run test to verify it fails**
  Run: `cd "d:\z.ai app punjabi beat\punjabi-beats-all-code-v1.1\punjabi-beats"; .\gradlew.bat testDebugUnitTest --tests "com.punjabibeats.app.CanvasArtworkTest"`
- [ ] **Step 3: Implement `CanvasArtwork.kt` & `CanvasArtworkPlayer.kt`**
  Build the canvas data model, string matching normalizer, and the looping Media3 ExoPlayer background view with silent audio and smooth crossfade.
- [ ] **Step 4: Wire canvas backdrop into `FullPlayerScreen.kt`**
  Render `CanvasArtworkPlayer` behind active Now Playing views when active canvas is present.
- [ ] **Step 5: Run tests to verify all pass**
  Run: `cd "d:\z.ai app punjabi beat\punjabi-beats-all-code-v1.1\punjabi-beats"; .\gradlew.bat testDebugUnitTest --tests "com.punjabibeats.app.CanvasArtworkTest"`
- [ ] **Step 6: Commit changes**
  Run: `git add . && git commit -m "feat(player): add animated motion video canvas engine with looping ExoPlayer"`

---

### Task 4: Floating Animated Navigation Bar

**Files:**
- Create: `app/src/main/java/com/punjabibeats/app/ui/components/dock/FloatingGlassNavBar.kt`
- Modify: `app/src/main/java/com/punjabibeats/app/ui/PunjabiBeatsAppRoot.kt`

**Interfaces:**
- Produces:
  ```kotlin
  @Composable fun FloatingGlassNavBar(
      currentRoute: String?,
      onNavigate: (String) -> Unit,
      modifier: Modifier = Modifier,
  )
  ```

- [ ] **Step 1: Implement `FloatingGlassNavBar.kt`**
  Build floating capsule pill dock with spring-animated active tab indicator (`spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)`), icon bounce (1.15x), specular light reflection rim, and tactile haptic feedback.
- [ ] **Step 2: Integrate `FloatingGlassNavBar` in `PunjabiBeatsAppRoot.kt`**
  Replace standard rectangular navigation bar with `FloatingGlassNavBar` elevated smoothly above system navigation insets.
- [ ] **Step 3: Run existing navigation tests**
  Run: `cd "d:\z.ai app punjabi beat\punjabi-beats-all-code-v1.1\punjabi-beats"; .\gradlew.bat testDebugUnitTest --tests "com.punjabibeats.app.NavigationAndQueueTest"`
- [ ] **Step 4: Commit changes**
  Run: `git add . && git commit -m "feat(dock): implement floating animated glass navigation dock"`

---

### Task 5: Two-Pane Landscape & Tablet Player Deck

**Files:**
- Create: `app/src/main/java/com/punjabibeats/app/ui/screens/player/decks/LandscapePlayerDeck.kt`
- Modify: `app/src/main/java/com/punjabibeats/app/ui/screens/player/FullPlayerScreen.kt`

**Interfaces:**
- Produces:
  ```kotlin
  @Composable fun LandscapePlayerDeck(
      controller: MusicPlayerController,
      onClose: () -> Unit,
      onOpenLyrics: () -> Unit,
      modifier: Modifier = Modifier,
  )
  ```

- [ ] **Step 1: Implement `LandscapePlayerDeck.kt`**
  Build two-column split layout:
  - Left Column (46% width): Vinyl turntable / artwork, title, artist, universal seekbar with negative remaining time, play/pause/skip buttons, and volume slider.
  - Right Column (54% width): Live synchronized lyrics deck with active line highlighting and tap-to-seek, with toggle to Up-Next queue.
- [ ] **Step 2: Connect landscape switcher in `FullPlayerScreen.kt`**
  Detect `LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE || LocalConfiguration.current.screenWidthDp >= 720` and render `LandscapePlayerDeck`.
- [ ] **Step 3: Verify tests pass**
  Run: `cd "d:\z.ai app punjabi beat\punjabi-beats-all-code-v1.1\punjabi-beats"; .\gradlew.bat testDebugUnitTest`
- [ ] **Step 4: Commit changes**
  Run: `git add . && git commit -m "feat(player): add two-pane landscape and tablet player deck with live lyrics"`

---

### Task 6: Replay / Wrapped Screen & Listening Analytics

**Files:**
- Create: `app/src/main/java/com/punjabibeats/app/ui/screens/replay/ReplayModel.kt`
- Create: `app/src/main/java/com/punjabibeats/app/ui/screens/replay/ReplayScreen.kt`
- Create: `app/src/test/java/com/punjabibeats/app/ReplayAnalyticsTest.kt`
- Modify: `app/src/main/java/com/punjabibeats/app/ui/navigation/Routes.kt`
- Modify: `app/src/main/java/com/punjabibeats/app/ui/navigation/AppNavHost.kt`
- Modify: `app/src/main/java/com/punjabibeats/app/ui/screens/home/HomeScreen.kt`

**Interfaces:**
- Produces:
  ```kotlin
  data class ReplaySummary(
      val year: Int,
      val totalMinutes: Long,
      val topArtists: List<ArtistStat>,
      val topSongs: List<SongStat>,
      val topGenres: List<GenreStat>,
      val personalityBadge: String,
      val personalityTitle: String,
      val personalityDescription: String,
  )
  fun calculateReplayStats(history: List<SongHistoryEntry>): ReplaySummary
  ```

- [ ] **Step 1: Write failing unit test `ReplayAnalyticsTest`**
  Test listening minutes summation, top artist grouping, and personality badge determination based on dominant genres.
- [ ] **Step 2: Run test to verify it fails**
  Run: `cd "d:\z.ai app punjabi beat\punjabi-beats-all-code-v1.1\punjabi-beats"; .\gradlew.bat testDebugUnitTest --tests "com.punjabibeats.app.ReplayAnalyticsTest"`
- [ ] **Step 3: Implement `ReplayModel.kt` and `ReplayScreen.kt`**
  Implement analytics calculator, multi-card story pager with progress bars, statistics cards, and shareable recap poster card.
- [ ] **Step 4: Wire `Routes.REPLAY` in `Routes.kt`, `AppNavHost.kt`, and `HomeScreen.kt`**
  Add banner card on Home screen ("Your 2026 Punjabi Replay is Live! 👑") navigating to `Routes.REPLAY`.
- [ ] **Step 5: Verify unit tests pass**
  Run: `cd "d:\z.ai app punjabi beat\punjabi-beats-all-code-v1.1\punjabi-beats"; .\gradlew.bat testDebugUnitTest --tests "com.punjabibeats.app.ReplayAnalyticsTest"`
- [ ] **Step 6: Commit changes**
  Run: `git add . && git commit -m "feat(replay): implement 2026 listening replay wrapped recap with stories and poster"`

---

### Task 7: Home Screen Widgets (2x2 and 4x2)

**Files:**
- Create: `app/src/main/java/com/punjabibeats/app/widget/PunjabiBeatsWidgetProvider.kt`
- Create: `app/src/main/res/layout/widget_punjabi_beats_square.xml`
- Create: `app/src/main/res/layout/widget_punjabi_beats_wide.xml`
- Create: `app/src/main/res/xml/widget_media_square_info.xml`
- Create: `app/src/main/res/xml/widget_media_wide_info.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/punjabibeats/app/playback/MediaPlaybackService.kt`

**Interfaces:**
- Produces:
  ```kotlin
  class PunjabiBeatsSquareWidgetProvider : AppWidgetProvider
  class PunjabiBeatsWideWidgetProvider : AppWidgetProvider
  object WidgetUpdateBroadcastReceiver {
      fun updateAllWidgets(context: Context, song: Song?, isPlaying: Boolean)
  }
  ```

- [ ] **Step 1: Create XML layouts and widget info providers**
  Create RemoteViews XML layouts:
  - 2x2 square: artwork, title, artist, circular play/pause button.
  - 4x2 wide: artwork, title, artist, previous, play/pause, next.
- [ ] **Step 2: Implement `PunjabiBeatsWidgetProvider.kt`**
  Handle `ACTION_APPWIDGET_UPDATE`, bind RemoteViews, register PendingIntents for play/pause/skip and opening `MainActivity`.
- [ ] **Step 3: Register in `AndroidManifest.xml`**
  Declare both widget receivers with intent filters for `APPWIDGET_UPDATE`.
- [ ] **Step 4: Update `MediaPlaybackService.kt`**
  Call widget update helper on player state and metadata changes.
- [ ] **Step 5: Run full test suite**
  Run: `cd "d:\z.ai app punjabi beat\punjabi-beats-all-code-v1.1\punjabi-beats"; .\gradlew.bat testDebugUnitTest`
- [ ] **Step 6: Commit changes**
  Run: `git add . && git commit -m "feat(widget): add 2x2 square and 4x2 wide home screen now-playing widgets"`
