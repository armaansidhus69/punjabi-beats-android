# Punjabi Beats 🎵

A production-grade, native **Android music streaming & group-listening app** built with **100% Kotlin + Jetpack Compose** — Material 3, strict dark theme, Media3 audio engine, Jam Rooms with millisecond clock sync, AI playlist generation, synced Gurmukhi lyrics with **word-by-word karaoke**, encrypted offline downloads, podcast support, a **direct YouTube Music (InnerTube) source with WebView cipher deciphering**, and **Metrolist-style audio customization** (equalizer, bass boost, surround, crossfade, tempo/pitch, skip silence).

```
minSdk 24 (Android 7.0)   targetSdk 34 (Android 14)   Kotlin 2.0.21   AGP 8.5.2   v1.1.0
```

---

## 1. Getting started

1. Open the project in **Android Studio Koala (2024.1)+**, or build from a terminal with **JDK 17**:
   `./gradlew :app:assembleDebug` (Windows: `gradlew.bat :app:assembleDebug`).
2. The Gradle wrapper is committed (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`,
   `gradle-wrapper.properties` → Gradle 8.9); the first run downloads Gradle and the Android Gradle
   Plugin 8.5.2 automatically.
3. Run on a device/emulator with API 24+.

No API keys are required — all endpoints are public or belong to the project's own backend.

## 2. Architecture

Clean Architecture + MVVM + unidirectional (MVI-style) `UiState` flows:

```
app/src/main/java/com/punjabibeats/app/
├── core/            # PbJson, lenient JSON helpers, UiState, time utils, Gurmukhi transliterator, NetworkMonitor
├── core/di/         # Hilt modules (AppModule, NetworkModule, RepositoryModule)
├── domain/
│   ├── model/       # Song, HomeFeed, JamRoomSnapshot, SyncedLyrics, TimedWord, Podcast… (pure Kotlin)
│   └── repository/  # Repository interfaces
├── data/
│   ├── remote/      # Retrofit APIs: backend, JioSaavn (saavn.dev), Audius, iTunes, Piped (YT bridge), LRCLIB
│   ├── local/       # Room DB (7 entities / 5 DAOs) + DataStore settings
│   ├── parser/      # LRC (+ enhanced-LRC word tags), SRT/WebVTT, RSS (XmlPullParser — zero extra deps)
│   ├── cipher/      # zemer-cipher pattern: YtCipherManager, CipherDeobfuscator (WebView), PlayerConfigStore, StreamValidator, PoTokenProvider
│   ├── resolver/    # StreamResolver fallback chain
│   ├── repository/  # Repository implementations
│   └── security/    # AndroidKeyStore AES-256/GCM crypto + EncryptedFileDataSource
├── innertube/       # Metrolist-style YouTube Music module: InnerTubeClient (search/player), InnerTubeResolver
├── playback/        # MediaPlaybackService (Media3), PlayerController (PlayerConnection pattern), DataSources, mapper
│   └── audio/       # AudioEffectsManager (EQ/bass/virtualizer), CrossfadeAudioProcessor
├── jam/             # JamClockSync (NTP-style), WebSocket + REST-polling transports, JamEngine
└── ui/              # theme, components (GlassCard, Waveform, Vinyl, Reactions, DragDrop), screens, navigation
```

**Key flows**

- **Playback resolution** — when a song is queued, `MediaPlaybackService.onAddMediaItems` resolves each item:
  `offline download (pbenc://) → known streamUrl → JioSaavn 320kbps → /api/yt-stream → InnerTube+decipher → Audius → iTunes preview`.
  If **everything** fails, a 1-second silence asset keeps queue indices stable and the UI shows a snackbar — playback **never crashes**, it auto-skips.
- **Direct YouTube Music source (zemer-cipher pattern)** — `innertube/InnerTubeClient` calls
  `music.youtube.com/youtubei/v1/player|search` with the WEB_REMIX descriptor from the self-healing cipher config
  (`data/cipher/PlayerConfigStore`: asset seed → remote refresh with regex-locked validation → file cache).
  `YtCipherManager` deciphers `signatureCipher` by executing the live player JS in a headless WebView (with a pure-Kotlin
  fast path for the classic reverse/splice/swap op sequence), applies the n-parameter transform, provisions an operator-supplied
  PoToken when available, and validates candidates with two byte-range probes (HTTP 206 + the 1-MiB free-window test) before
  ExoPlayer sees the URL. Every stage degrades to null and the chain simply falls through.
- **Audio customization (Metrolist pattern)** — the renderers factory plugs a `CrossfadeAudioProcessor` into the audio sink
  (frame-accurate fade-out/in between tracks); `AudioEffectsManager` binds the platform 5-band Equalizer, BassBoost and
  Virtualizer to the player's audio session; skip-silence, tempo (0.5–2×) and pitch (0.5–2×) follow the persisted settings —
  all configurable from the Settings screen. Every effect call is guarded so device-level failures never kill playback.
- **Jam Rooms** — the host broadcasts full `HostState` snapshots every 4 s and on every control change. Guests compute
  `expected = (serverNow − anchorServerMs) + anchorPositionMs` using the `/api/jam-clock` offset and **re-seek when |drift| > 400 ms**.
  Queue additions are dispatched **fair-turn** (round-robin per member). Presence prunes after 35 s; if the host disappears, the
  eldest member (lowest userId) is promoted. Reactions (🔥 💃 🎧 …) float over the room.
- **Offline mode** — downloads stream through `AES/GCM` (AndroidKeyStore key, file layout `[1B nonceLen][nonce][ciphertext]`)
  into app-private storage and play back through the `pbenc://` DataSource.
- **Queue persistence** — queue + index + repeat/shuffle/speed persist to DataStore (debounced) and are restored on the next launch.

## 3. Backend contract (the parts that are deliberately tolerant)

The app talks to `https://punjabi-beats-api.vercel.app` (all fields parsed leniently, multiple alias keys accepted):

| Endpoint | Used for | Assumption |
|---|---|---|
| `GET /api/health` | connection check | — |
| `GET /api/yt-stream?id=` | InnerTube audio resolver | response contains `url` / `audioUrl` / `formats[].url` |
| `POST /api/ai-playlist` | Gemini playlist generator | body `{prompt, type, language, count}`; response `{title, description, songs[]}` |
| `POST /api/ai-assistant` | conversational assistant | body `{messages[], prompt, language}`; response `{reply}` |
| `GET /api/jam-clock` | NTP-style clock sync | response contains `serverTimeMs` (or `time`/`now`/`clock`/`timestamp`) |
| `POST/GET /api/publish` | Jam Room sync fallback | POST a `JamMessage` JSON; GET returns `{messages: [<JamMessage JSON>…]}` |
| `POST /api/share` | share card generator | body `{type, code, link}`; response `{url}` (falls back to the deep link) |

Jam realtime tries the **WebSocket** (`wss://…/api/jam-ws?code=`, configurable via `BuildConfig.JAM_WS_URL`)
and transparently falls back to **REST polling** on `/api/publish`. Both use the same message protocol
(`JamMessages.kt`: `join`, `state`, `reaction`, `queue_add`, `queue_remove`, `leave`, `ping`).

Third-party catalogue sources (public, unofficial):
`saavn.dev` (JioSaavn 320kbps + search), direct `music.youtube.com/youtubei` (WEB_REMIX, search + player),
`api.audius.co` (host discovery, rotating nodes),
`itunes.apple.com` (previews), Piped instances (YouTube Music search bridge fallback, rotating hosts), `lrclib.net` (synced lyrics).

The cipher configuration additionally expects an operator-maintained `GET /api/player-configs` returning the same JSON shape as
`app/src/main/assets/player_configs.json` (players with `playerId` / `signatureTimestamp` / `playerJsUrl` / n-transform URL classes,
optional `poToken`). Until that endpoint exists, the bundled asset and runtime extraction from the live player JS are used.

## 4. Feature map

| Spec section | Where |
|---|---|
| Media3 foreground service, focus/duck, noisy-pause, lockscreen | `playback/MediaPlaybackService.kt`, `PlayerControllerImpl` |
| Root scaffold + mini player → full player | `ui/PunjabiBeatsAppRoot.kt`, `components/MiniPlayerBar.kt` |
| Home (hero pager, curated artists, moods, quick picks) | `screens/home/` |
| Debounced search + recent chips + filter tabs (All/Songs/Artists/Albums/Podcasts) | `screens/search/` |
| Full player: Palette gradient, vinyl rotation, waveform seekbar, sleep timer, drag-to-reorder queue | `screens/player/` |
| Synced LRC lyrics, Gurmukhi ⇄ Roman toggle, share-card export | `screens/lyrics/`, `core/util/Text.kt` |
| Word-by-word karaoke (enhanced-LRC `<mm:ss.xx>` tags, Better-Lyrics pattern) | `data/parser/LrcParser.kt`, `screens/lyrics/LyricsScreen.kt` |
| InnerTube YouTube Music source + cipher deciphering + 206 validation | `innertube/`, `data/cipher/` |
| Audio effects: EQ/bass/surround, crossfade, tempo/pitch, skip silence (Settings) | `playback/audio/`, `screens/settings/`, `SettingsDataStore` |
| Jam Rooms: code/QR join, 400 ms drift correction, fair queue, reactions, presence | `jam/`, `screens/jam/` |
| AI Studio: `/api/ai-playlist` + `/api/ai-assistant` chat | `screens/ai/` |
| Library: optimistic likes, playlists, encrypted downloads | `screens/library/`, `data/security/` |
| Podcasts: RSS parser, 0.5×–2× speed, SRT/WebVTT transcript sheet | `screens/podcast/`, `data/parser/` |

## 5. Reference projects (patterns, not copies)

v1.1.0 integrates architecture patterns from two GPL-3.0 projects, re-implemented from scratch (no source code copied):

| Project | What was adopted | Where |
|---|---|---|
| [Metrolist](https://github.com/MetrolistGroup/Metrolist) | dedicated `innertube` YouTube module; `PlayerConnection`-style MediaController wrapper; audio customization (EQ, bass, virtualizer, crossfade, tempo/pitch, skip silence); synced-lyrics focus | `innertube/`, `playback/PlayerControllerImpl.kt`, `playback/audio/`, `screens/settings/` |
| [zemer-cipher](https://github.com/ZemerTeam/zemer-cipher) | WebView signature/n-param deciphering driven by live player JS; remote self-healing `player_configs` with regex-locked validation; PoToken slot; HTTP-206 + 1-MiB window stream validation | `data/cipher/`, `app/src/main/assets/player_configs.json` |

## 6. Notes & honest limitations

- **Boot persistence**: Android does not allow an app to start audio playback after a reboot. What IS implemented:
  the queue/session is persisted and restored automatically on the next app launch (and the service keeps playing
  across app minimize / task removal).
- **No seed podcast feeds** are hard-coded — the module parses any RSS URL you add, so nothing unverifiable ships in the binary.
- Lyrics transliteration covers Gurmukhi → Roman. If a track's LRC is already romanized, the toggle is a no-op for it.
- **Cipher specifics are not hard-coded**: the bundled config contains only client descriptors; player hashes, signature
  timestamps and decipher logic are extracted from the live player script at runtime and healed via `/api/player-configs`.
  Without PoToken provisioning some web-client URLs may only serve the 1-MiB preview window — `StreamValidator` detects this
  and the chain falls through to the next source.
- The n-transform and BotGuard run only when WebView can execute the extracted challenge; on failure the URL is used
  un-transformed (throttled) or skipped — never a crash.
- Unit/UI tests: the `junit`/`espresso` wiring is present; test source sets contain unit tests for the time-format,
  LRC/enhanced-LRC and subtitle parsers.
