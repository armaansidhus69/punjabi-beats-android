# Design Specification: BitChord Section 7 UI, Canvas & Theming Architecture

- **Date**: 2026-11-20
- **Author**: Antigravity Assistant & User
- **Status**: Approved / In-Progress
- **Target File**: `docs/superpowers/specs/2026-11-20-bitchord-section-7-ui-canvas-theming.md`
- **Reference**: BitChord Section 7 Architecture (`D:\new download\BitChord-main\BitChord-main`)
- **Target App**: Punjabi Beats Android Music Player (`punjabi-beats-all-code-v1.1\punjabi-beats`)

---

## 1. Intent & The Why

Punjabi Beats is a premier dark-mode music streaming application tailored for Punjabi and Desi music enthusiasts. While the app already features custom seekbars and basic theming, the user has requested a comprehensive overhaul to adopt **BitChord's Section 7 ("UI, Canvas & Theming") architecture**.

This design transforms the app's visual identity from a standard Compose UI into a liquid, tactile, artwork-responsive music experience with:
1. **Dynamic Artwork-Driven Theming**: Real-time palette extraction deriving background washes, elevated glass fills, accents, and contrast-adapted text colors from currently playing album art.
2. **Frosted Glass UI (LiquidGlass & OptimizedHaze)**: GPU-accelerated blur (`RenderEffect` on Android 12+ API 31, translucent fallbacks on API 24-30), liquid refraction, and specular highlight edges across navigation bars, mini-players, header controls, and bottom sheets.
3. **Animated Motion Video Canvas**: Ambient looping canvas video playback seamlessly integrated behind the Now Playing screen, with automatic fallback to high-resolution album artwork and seamless loop recycling.
4. **Floating Animated Navigation Bar**: A spring-animated floating pill dock featuring physical spring travel, squashing/stretching physics, specular light highlights, and icon bounce.
5. **Two-Pane Landscape & Tablet Player Deck**: Adaptive split-screen deck on landscape and tablet form-factors (left pane: vinyl turntable / album scrubber / player transport; right pane: live synchronized auto-scrolling lyrics).
6. **Replay / Wrapped Screen**: An annual & monthly listening recap with story-style paging, animated stats cards (listening minutes, top artists, top tracks, listening personality badge), and exportable shareable posters.
7. **Home Screen Widgets**: Native Android 2x2 square and 4x2 wide now-playing widgets with live metadata, playback controls (Play/Pause, Next, Previous), and sleek frosted glass aesthetic.

---

## 2. Architecture & Subsystems

### 2.1 Dynamic Artwork-Driven Theming (`ArtworkPalette`)
- **Palette Extraction Engine**:
  Extract dominant, vibrant, and edge colors using `androidx.palette` and custom luminance/saturation adapters.
  - `background`: Dark tone (luminance ~ 0.13f in dark mode) matching the record's primary hue with adapted saturation floor to prevent neutral greys from turning artificially red.
  - `wash`: Average bottom-edge color (sampled from lower 18% of artwork) providing a seamless transition between artwork and background.
  - `elevated`: Surface variant for glass chips, pills, and card containers.
  - `accent`: Vibrantly weighted color (saturation * sqrt(population)) adapted for WCAG 4.5:1 text and control contrast.
  - `onBackground` / `onBackgroundVariant`: Pure white/black and tinted secondary metadata text.
- **Caching & Transition Spec**:
  - LRU seed cache of 128 artwork URLs.
  - Cached art settles instantly (`snap()`); newly loaded art crossfades in 260ms (`tween(260)`).

### 2.2 Frosted Glass UI (`LiquidGlass` & `OptimizedHaze`)
- **Glass Pipeline**:
  - On API 31+ (Android 12+), leverage hardware RenderEffect blur combined with a specular highlight line (`GLASS_EDGE_WIDTH = 0.5.dp`) and high-translucency tint (`alpha = 0.70f - 0.85f`).
  - On API 24-30, graceful degradation to multi-stop linear gradient dither and translucent scrims.
- **Components Covered**:
  - `FloatingGlassNavBar`: Floating pill dock at the bottom of the screen.
  - `MiniPlayerBar`: Floating frosted card floating above the navigation bar.
  - `PlayerSheets` & `menuFrost`: Modals, queue drawers, lyrics sheets, and audio output dialogs.

### 2.3 Animated Motion Video Canvas (`CanvasArtworkPlayer`)
- **Canvas Model (`CanvasArtwork`)**:
  - Holds video URL, fallback URL, title, artist, source (`CanvasSource.SPOTIFY`, `CanvasSource.OTHER`), and portrait URL.
  - Title/artist matching normalization to prevent mismatched clips.
- **Video Playback Engine**:
  - Lightweight Media3 ExoPlayer instance configured for silent (`volume = 0f`), infinite looping (`REPEAT_MODE_ALL`), and surface texture rendering.
  - Auto-pauses when the app leaves the foreground or player is minimized.
  - Smooth 300ms alpha crossfade from static artwork to video once the first frame renders.

### 2.4 Floating Animated Navigation Bar (`FloatingGlassNavBar`)
- **Physics-Driven Motion**:
  - Damped spring animations for active tab pill travel (`Spring.DampingRatioMediumBouncy`, `Spring.StiffnessLow`).
  - Active icon bounce and scaling (`1.15x`).
  - Subtle specular highlight border along top edge (`Highlight` effect) giving the illusion of physical curved glass refraction.

### 2.5 Two-Pane Landscape & Tablet Player Deck (`LandscapePlayerDeck`)
- **Adaptive Breakpoint Detection**:
  - Activated when `orientation == ORIENTATION_LANDSCAPE` or `screenWidthDp >= 720.dp`.
- **Layout Distribution**:
  - Left Column (46% width): Vinyl record / Squircle artwork with rotation, playback seekbar with elapsed and negative remaining time, play/pause/skip buttons, and volume bar.
  - Right Column (54% width): Synchronized active lyrics stream with tap-to-seek, smooth auto-centering on current timestamp, and quick toggle to queue.

### 2.6 Replay / Wrapped Screen (`ReplayScreen`)
- **Story-Driven Recap Flow**:
  - Step 1: Total Listening Time (minutes & hours, with percentile).
  - Step 2: Top Punjabi Artists (Sidhu Moose Wala, Diljit Dosanjh, Karan Aujla, etc.) with play count and hours.
  - Step 3: Top 5 Tracks with album art and play metrics.
  - Step 4: Top Genres & Moods (Bhangra, Desi Hip-Hop, Sufi, Romantic).
  - Step 5: Persona Badge ("Royal Dhol Champion", "Midnight Basshead").
  - Step 6: Shareable Recap Card (Generates clean Bitmap via Canvas for Instagram Stories / WhatsApp).

### 2.7 Home Screen Widgets (`PunjabiBeatsWidgetProvider`)
- **Widget Variants**:
  1. **2x2 Square Widget**: Album art, title, artist, and circular play/pause button.
  2. **4x2 Wide Widget**: Large album art, title, artist, previous, play/pause, next, and favorite star toggle.
- **Broadcast & RemoteViews**:
  - Standard Android `AppWidgetProvider` with `RemoteViews`.
  - Intent actions for Play/Pause, Next, Previous routed through `MediaPlaybackService`.
  - Broadcast receiver updating all active widgets when track, art, or play-state changes.

---

## 3. Constraints & Verification Requirements
- Target SDK 34, Min SDK 24.
- `apk/punjabi-beats-v1.0.0-legacy.apk` MUST NOT be modified or deleted.
- All unit tests must pass: `.\gradlew.bat testDebugUnitTest`.
