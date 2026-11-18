# Apple Music UI Archetype & Dynamic Theme Player Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform the Punjabi Beats player and UI to dynamically adopt the authentic Apple Music glass layout when the Apple Music theme is selected, while maintaining dedicated archetype layouts for Neon Dusk, Cyberpunk, Anime Beats, and Punjab Legend.

**Architecture:** Add `apple` as the 5th flagship theme in `PBThemes` with `PlayerStyle.APPLE`. Build an isolated, high-performance `AppleMusicPlayerContent` component that renders an ambient dynamic mesh backdrop, pause/play squircle scaling physics (1.0x to 0.85x), negative remaining time scrubber, iOS 3-button controls, inline volume slider, and Cupertino bottom action dock. Wire into `FullPlayerScreen.kt` via theme archetype dispatching.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, Coil, Palette API, ExoPlayer Media3.

**Spec:** `docs/superpowers/specs/2026-11-18-apple-music-ui-archetype-design.md`

## Global Constraints
- `compileSdk = 34`, `minSdk = 24`, Kotlin 2.0+
- Strictly retain `punjabi-beats-v1.0.0-legacy.apk` untouched and intact.
- Keep DataStore wrapped in `safeData` (`IOException` catch) to guarantee zero startup/theme crashes.
- Maintain `LocalPBTheme` as `compositionLocalOf` for 60fps in-place color rendering without recreation.

## Review Focus
1. `pbThemeById("apple")`: must resolve cleanly with `accent = Color(0xFFFA2D48)` and `style = PlayerStyle.APPLE`.
2. Artwork pause scaling: when paused, artwork must smoothly animate to 0.85x scale; when playing, smoothly return to 1.0x scale.
3. Negative remaining time calculation: `"-${formatDuration(maxOf(0L, durationMs - positionMs))}"` must never produce negative duration overflows or NaN.
4. Volume slider: must smoothly control Android audio stream volume without freezing the UI thread.
5. All 5 themes selectable in Theme Studio and Settings Quick Switcher without crashing or resetting.

---

### Task 1: Theme Definition & Unit Tests

**Files:**
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/theme/PBTheme.kt`
- Modify: `punjabi-beats/app/src/test/java/com/punjabibeats/app/ThemeEngineTest.kt`

- [ ] Add `apple` theme to `PBThemes` in `PBTheme.kt`:
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
- [ ] Add `"apple"` case in `heroGradient` in `PBTheme.kt`.
- [ ] Write unit tests in `ThemeEngineTest.kt`:
  - `appleThemeResolvableById()`
  - `appleThemeHasApplePlayerStyle()`
- [ ] Run `.\gradlew.bat testDebugUnitTest` to verify test passes.
- [ ] Commit: `feat(theme): add Apple Music 5th flagship theme and unit tests`.

---

### Task 2: Dedicated Apple Music Player Component (`AppleMusicPlayerContent.kt`)

**Files:**
- Create: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/player/archetypes/AppleMusicPlayerContent.kt`

- [ ] Create `AppleMusicPlayerContent` composable:
  - Top header: Dismiss downward chevron `Icons.Rounded.KeyboardArrowDown`, center subtle title `Now Playing`, more options `Icons.Rounded.MoreVert`.
  - Artwork Box: Size 280.dp squircle with `RoundedCornerShape(22.dp)`.
  - Pause/Play scale animation: `val artScale by animateFloatAsState(targetValue = if (isPlaying) 1.0f else 0.85f, animationSpec = tween(350, easing = FastOutSlowInEasing))`.
  - Song Title (23.sp, Bold, Color.White, single line with Marquee/Ellipsis) & Artist Name (16.sp, Color.White.copy(alpha = 0.65f)).
  - Favorite Star/Heart toggle button.
  - Apple Scrub bar:
    - Custom slim track slider.
    - Position elapsed on left (`formatDuration(positionMs)`).
    - Remaining time on right (`"-${formatDuration(maxOf(0L, durationMs - positionMs))}"`).
  - Apple 3-Button Controls:
    - Previous track (`Icons.Rounded.SkipPrevious`, 36.dp).
    - Play / Pause (filled white button or bold glyph, 64.dp).
    - Next track (`Icons.Rounded.SkipNext`, 36.dp).
  - Apple Volume Slider:
    - Low speaker icon (`Icons.Rounded.VolumeDown`, tint = muted).
    - Slider track (`height = 6.dp`).
    - High speaker icon (`Icons.Rounded.VolumeUp`, tint = muted).
  - Apple Bottom Action Dock:
    - Lyrics bubble button (`Icons.Rounded.Lyrics`).
    - Audio route / AirPlay button (`Icons.Rounded.Airplay` / Cast).
    - Queue playlist button (`Icons.Rounded.QueueMusic`).
- [ ] Commit: `feat(player): implement AppleMusicPlayerContent archetype with iOS squircle scaling and volume slider`.

---

### Task 3: Integrate Dynamic Archetypes into `FullPlayerScreen.kt`

**Files:**
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/player/FullPlayerScreen.kt`

- [ ] In `FullPlayerScreen.kt`, check `theme.style`:
  ```kotlin
  when (theme.style) {
      PlayerStyle.APPLE -> {
          AppleMusicPlayerContent(
              song = song,
              isPlaying = isPlaying,
              positionMs = positionMs,
              durationMs = durationMs,
              isLiked = isLiked,
              onSeek = { player.seekTo(it) },
              onTogglePlay = { player.togglePlayPause() },
              onSkipNext = { player.seekToNext() },
              onSkipPrevious = { player.seekToPrevious() },
              onToggleFavorite = { viewModel.toggleFavorite() },
              onOpenLyrics = onOpenLyrics,
              onOpenQueue = { showQueue = true },
              onOpenDna = { showDnaSheet = true },
              onBack = onBack,
          )
      }
      else -> {
          // Classic Neon Dusk / Cyber / Anime / Punjab layout with turntable & waveform
          ...
      }
  }
  ```
- [ ] Ensure background gradient mesh for Apple Music uses the vibrant/dominant colors extracted via Palette API.
- [ ] Commit: `feat(player): wire PlayerStyle.APPLE archetype into FullPlayerScreen`.

---

### Task 4: Theme Studio & Settings Quick Switcher Update

**Files:**
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/settings/SettingsScreen.kt`
- Modify: `punjabi-beats/app/src/main/java/com/punjabibeats/app/ui/screens/theme/ThemeStudioScreen.kt`

- [ ] In `ThemeStudioScreen.kt`:
  - Ensure the 5-theme grid/list smoothly renders all 5 entries (Neon Dusk, Apple Music, Anime Beats, Cyberpunk, Punjab Legend).
- [ ] In `SettingsScreen.kt`:
  - Update Quick Theme Switcher Pills row to handle 5 items with `weight(1f)`.
- [ ] Commit: `feat(settings): support 5 core themes across Theme Studio and Settings Quick Switcher`.

---

### Task 5: Unit Tests, Assemble APK v1.1.3, and Publish GitHub Release

**Files:**
- Modify: `punjabi-beats/app/build.gradle.kts` (bump `versionCode = 5`, `versionName = "1.1.3"`)

- [ ] Run `.\gradlew.bat testDebugUnitTest` to verify all unit tests pass (100% success).
- [ ] Run `.\gradlew.bat assembleDebug` to compile fresh APK.
- [ ] Copy `app-debug.apk` to `apk/punjabi-beats-v1.1.3-latest.apk`, `apk/punjabi-beats-v1.1.3-apple-music.apk`, and `apk/PunjabiBeats-v1.1.3-debug.apk`.
- [ ] Verify `apk/punjabi-beats-v1.0.0-legacy.apk` is untouched.
- [ ] Commit and push to `origin/main`.
- [ ] Publish GitHub Release `v1.1.3` as **Latest** using `gh release create`.
