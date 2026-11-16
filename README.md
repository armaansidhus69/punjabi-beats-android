# Punjabi Beats — Complete Code (v1.1.0, zen-integrated)

[![Download APK](https://img.shields.io/badge/Download-APK%20(25%20MB)-brightgreen?style=for-the-badge&logo=android)](https://github.com/armaansidhus69/punjabi-beats-android/raw/main/apk/punjabi-beats-zen-v1.1-debug.apk)

Direct APK Download Link: [**Download Punjabi Beats APK (v1.1)**](https://github.com/armaansidhus69/punjabi-beats-android/raw/main/apk/punjabi-beats-zen-v1.1-debug.apk)

One archive with everything: the full Android project source, the build/integration
scripts, and the compiled debug APK.

## Contents

| Path | What it is |
|---|---|
| `punjabi-beats/` | Complete Android project source, ready to build |
| `apk/punjabi-beats-zen-v1.1-debug.apk` | Compiled debug APK — install directly on a device |
| `scripts/` | Everything used to integrate, fix and build (reproducible pipeline) |

Inside `punjabi-beats/`:
- `app/` — the Punjabi Beats app (Jetpack Compose UI, Media3 playback, jam mode,
  lyrics, podcasts, AI studio, offline downloads)
- `zen-core/`, `zen-cipher/`, `zen-potoken/`, `zen-extractor/`, `zen-resolver/`,
  `zen-datasource/`, `zenmusic/` — the vendored zen engine: on-device YouTube Music
  search + stream resolution + channel transport. **No backend API involved.**

## How playback works (no API)

1. `PunjabiBeatsApp.onCreate()` calls `ZenMusic.initialize(context)` — the engine
   warms its session, PO-token WebView and cipher in the background.
2. `ZenMusicResolver` maps a song to a `zen://<videoId>#zh=<handle>` stream URI
   (songs without a video id get an engine search by "title artist").
3. `StreamResolver` tries the zen resolver right after the source-first resolver;
   JioSaavn / Audius resolvers remain as fallbacks.
4. `PbDataSourceFactory.RoutingDataSource` routes `zen://` URIs into the engine's
   channel transport `DataSource`; stale handles re-resolve transparently (12 s bound).

## Build from scratch

### Windows (PowerShell)
```powershell
cd punjabi-beats
.\gradlew.bat :app:assembleDebug
```
Or run the automated pipeline:
```powershell
.\scripts\build_punjabi.ps1
```

### Linux / macOS
```bash
cd punjabi-beats
./gradlew :app:assembleDebug        # needs JDK 17+ + Android SDK (platform 34)
```

Or use the packaged pipeline, which installs JDK 17 + SDK 34 into the workspace
and builds with retries through network hiccups:

```bash
bash scripts/install_toolchain.sh   # writes punjabi-beats/local.properties
bash scripts/build_punjabi.sh       # -> punjabi-beats/app/build/outputs/apk/debug/
```

## Notes

- `compileSdk 34`, `targetSdk 34`, `minSdk 24`, Kotlin 2.0, media3 1.4.1.
- KSP is resolved by artifact coordinates via `settings.gradle.kts` (mirrors first).
- ~66 verified patches are already applied: zen integration (7 modules vendored,
  resolver chain, DataSource routing, zen-backed search), source bug fixes
  (missing imports, suspend misuse, null-leaks, Jam/InnerTube fixes) and
  media3 1.4.1 API alignment (AudioProcessor signatures, PlaybackParameters,
  skipSilence, timeline access).
- `local.properties` is machine-specific and intentionally not in this archive —
  the toolchain script generates it, or add your own `sdk.dir=...`.
