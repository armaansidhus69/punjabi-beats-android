# Vice City Neon Dusk UI Design

**Date:** 2026-11-16  
**Status:** Approved  
**Author:** Antigravity  

## Intent & The Why
The user provided two high-fidelity reference screenshots (`media_1794846579339_49f36c57.jpg` and `media_1794846593084_2f5cd7e7.jpg`) depicting an 80s dusk neon magenta / cyberpunk Vice City edition styling for Punjabi Beats. The application must accurately mirror these visuals: glowing hot-pink outlines, cursive "Punjabi Beats" title crowned with gold, glowing double-neon artist avatars, glowing mood category tiles, floating mini player with equalizer waveform, rounded pink pill bottom navigation, and a full player turntable with vibrant circular neon pink halo glow, dancing spectrum equalizer, and a dual-ring glowing play button with 4 action cards (`66 Lyrics`, `ııll Equalizer`, `⏱ Timer`, `≡🎵 Queue`).

## Goals
1. **Color & Theme System**:
   - Background: Obsidian Black `#08060D`
   - Surface: Dark Glass `#130E1B`
   - Card / Panel: `#1B1224` with neon pink border `#FF2E93` (35%-60% alpha bloom)
   - Accent: Neon Magenta / Hot Pink `#FF2E93`
   - Secondary: Gold Crown `#FFAA00` / `#FF007F`
   - Text: Pure White `#FFFFFF`, Muted: `#A592B3`
2. **Home Screen**:
   - Header: Left menu icon `☰`, centered Golden Crown `👑` sitting directly above cursive "Punjabi Beats" and subtitle `"YOUR MUSIC • YOUR UNIVERSE"` (with `"VICE CITY EDITION"` mode), and right notification bell `🔔` with unread hot-pink dot.
   - Search bar: Pill container with hot-pink glowing border (`#FF2E93`), leading search icon, placeholder `"Search songs, artists, albums, podcasts..."`, trailing microphone icon `🎤`.
   - Hero banner: Dusk sunset palm-tree city silhouette card with `"JATTWAAD - DAILY PUNJABI HITS"`, artist name, circular hot-pink Play button `▶` with glowing drop shadow, and 4-dot pagination indicators.
   - Curated Artists row: Title `👑 Curated Artists` with `See All >` in pink, circular artist avatars with **intense glowing double neon pink rings** (`#FF2E93`) (Sidhu Moose Wala, Karan Aujla, Diljit Dosanjh, AP Dhillon).
   - Moods row: Title `🎵 Moods` with `See All >` in pink, 4 horizontal cards: `Bhangra` (amber border), `Chill` (cyan border), `Drive` (purple border), `Heartbreak` (crimson border).
   - Category chips: `All` (active pink pill), `Songs`, `Artists`, `Albums`.
3. **Mini Player & Bottom Navigation**:
   - Mini Player: Floating pill with neon pink glowing border (`#FF2E93`), album art, title, artist, live audio waveform equalizer, heart icon, pause, next.
   - Bottom Dock: Floating dark dock with active tab highlighted by a hot-pink pill (`#FF2E93`) with white icon & label.
4. **Full Player Screen**:
   - Header: Down chevron `∨`, `// NOW PLAYING //` in glowing pink text, `⋮` 3 dots menu.
   - Turntable Vinyl Centerpiece: 12" vinyl disc with glossy concentric grooves, metallic tonearm tracking the groove, center artwork label, and **vibrant circular neon pink halo glow** behind the record against dusk palm-city background.
   - Song info: Title ("Gypsy Kali"), artist in pink ("Babbu Maan"), solid pink heart icon `❤️` on right.
   - Visualizer: Vertical dancing pink bars spectrum waveform equalizer above seekbar.
   - Seekbar: Sleek neon pink line with glowing circular thumb, elapsed and duration time.
   - Controls: Shuffle, Previous, 76dp circular Play/Pause button with **double-ring glowing neon pink halo**, Next, Repeat.
   - Bottom 4 Action Cards: `66 Lyrics`, `ııll Equalizer`, `⏱ Timer`, `≡🎵 Queue`.

## Non-Goals & Anti-Goals
- Modifying audio playback engine internals (ExoPlayer / MediaSession) or network resolvers.
- Regressing existing unit tests or breaking APK build.

## Approaches Considered
- **Approach 1 (Recommended)**: Implement dedicated high-fidelity components and Vice City / Neon styling directly into `PBTheme.kt`, `HomeScreen.kt`, `CommonUi.kt`, `MiniPlayerBar.kt`, `PunjabiBeatsAppRoot.kt`, and `FullPlayerScreen.kt`.
- **Approach 2**: Generic CSS-like skinning. (Rejected: does not produce the specific custom canvas drawing required for double neon rings, halo glows, and specific layouts).
