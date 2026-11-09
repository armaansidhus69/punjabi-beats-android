#!/usr/bin/env python3
"""Integrates the zenmusic engine into Punjabi Beats (no backend API)."""
import pathlib, re, shutil

P = pathlib.Path(__file__).resolve().parent.parent / "punjabi-beats"
if not P.exists():
    P = pathlib.Path("/home/z/my-project/download/punjabi-beats")
Z = P.parent / "zenmusic"
if not Z.exists():
    Z = pathlib.Path("/home/z/my-project/zenmusic")

def sub(path, old, new, label):
    p = P / path if not str(path).startswith("/") else pathlib.Path(path)
    s = p.read_text(encoding="utf-8", errors="ignore")
    if old in s:
        if new in s:
            print(f"skip  {label} (already applied)")
            return
        p.write_text(s.replace(old, new, 1), encoding="utf-8")
        print(f"OK    {label}")
    else:
        raise SystemExit(f"ANCHOR MISSING: {label}")

# ------------------------------------------------ 1. vendor modules
MODULES = ["zen-core", "zen-cipher", "zen-potoken", "zen-extractor",
           "zen-resolver", "zen-datasource", "zenmusic"]
for m in MODULES:
    dst = P / m
    if dst.exists():
        shutil.rmtree(dst)
    shutil.copytree(Z / m, dst, ignore=shutil.ignore_patterns("build", ".gradle"))
print("OK    vendored 7 zen modules")

for m in ["zen-cipher", "zen-datasource", "zenmusic"]:
    b = P / m / "build.gradle.kts"
    s = b.read_text().replace("compileSdk = 35", "compileSdk = 34")
    s = re.sub(r"minSdk = 26", "minSdk = 24", s)
    b.write_text(s)
for m in ["zen-core", "zen-potoken", "zen-extractor", "zen-resolver"]:
    b = P / m / "build.gradle.kts"
    b.write_text(b.read_text().replace('kotlin("jvm") version "2.0.21"', 'kotlin("jvm")'))
print("OK    module build files adapted (compileSdk 34, minSdk 24)")

# keep media3 on the app's 1.4.1 — 1.5.1 would force compileSdk 35
for m in ["zen-datasource"]:
    b = P / m / "build.gradle.kts"
    b.write_text(b.read_text()
                 .replace("media3-datasource:1.5.1", "media3-datasource:1.4.1")
                 .replace("media3-common:1.5.1", "media3-common:1.4.1"))
print("OK    zen-datasource media3 pinned to 1.4.1")

# ------------------------------------------------ 2. settings includes
sub("settings.gradle.kts", 'include(":app")',
    'include(":app")\n'
    'include(":zen-core")\ninclude(":zen-cipher")\ninclude(":zen-potoken")\n'
    'include(":zen-extractor")\ninclude(":zen-resolver")\ninclude(":zen-datasource")\n'
    'include(":zenmusic")',
    "settings includes zen modules")

# ------------------------------------------------ 3. app deps
sub("app/build.gradle.kts", "dependencies {",
    "dependencies {\n    // Zen engine — search + stream resolution + channel transport, no backend API\n"
    "    implementation(project(\":zenmusic\"))",
    "app depends on :zenmusic")

# ------------------------------------------------ 4. engine init in Application
sub("app/src/main/java/com/punjabibeats/app/PunjabiBeatsApp.kt",
    '''    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }''',
    '''    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        // Zen engine: warms the whole playback chain in the background
        // (session visitor, PO-token WebView, cipher) — search and playback
        // then run entirely on-device with no backend API.
        runCatching { com.zenmusic.ZenMusic.initialize(applicationContext) }
    }''',
    "Application init of Zen engine")

# ------------------------------------------------ 5. ZenMusicResolver
resolver = P / "app/src/main/java/com/punjabibeats/app/data/resolver/ZenMusicResolver.kt"
if not resolver.exists():
    resolver.write_text('''package com.punjabibeats.app.data.resolver

import com.punjabibeats.app.core.util.runSuspendCatching
import com.punjabibeats.app.domain.model.MusicSource
import com.punjabibeats.app.domain.model.Song
import com.zenmusic.ZenMusic
import com.zenmusic.core.uri.ZenUri
import com.zenmusic.datasource.ZenMediaItems
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plays through the bundled Zen engine — no backend API. Resolves a Song to
 * an opaque `zen://…#zh=<handle>` stream url served by the engine's channel
 * transport (see [com.punjabibeats.app.playback.PbDataSourceFactory], which
 * routes zen:// uris into it).
 *
 * Songs from YouTube already carry a videoId; every other source gets a
 * best-effort engine search by "title artist". Null when the engine cannot
 * help — the StreamResolver chain then moves to the next resolver.
 */
@Singleton
class ZenMusicResolver @Inject constructor() : TrackResolver {

    override suspend fun resolve(song: Song): Song? = runSuspendCatching {
        val zen = ZenMusic.get() ?: return@runSuspendCatching null
        val streamUri = song.videoId?.takeIf { it.isNotBlank() }
            ?.let { ZenUri.mint(it) }
            ?: zen.search.songs("${song.title} ${song.artist}".trim())
                .firstOrNull()?.uri
            ?: return@runSuspendCatching null
        val stream = zen.resolve(streamUri)
        song.copy(
            streamUrl = ZenMediaItems.withHandle(stream),
            source = MusicSource.YOUTUBE,
            videoId = stream.uri.removePrefix("zen://").substringBefore('.'),
        )
    }.getOrNull()
}
''')
    print("OK    ZenMusicResolver added")

# ------------------------------------------------ 6. chain insertion
sub("app/src/main/java/com/punjabibeats/app/data/resolver/StreamResolver.kt",
    '''class StreamResolver @Inject constructor(
    private val jioSaavnResolver: JioSaavnResolver,''',
    '''class StreamResolver @Inject constructor(
    private val zenMusicResolver: ZenMusicResolver,
    private val jioSaavnResolver: JioSaavnResolver,''',
    "StreamResolver injects ZenMusicResolver")

sub("app/src/main/java/com/punjabibeats/app/data/resolver/StreamResolver.kt",
    '''        return listOf(
            first,
            jioSaavnResolver,''',
    '''        return listOf(
            first,
            zenMusicResolver,
            jioSaavnResolver,''',
    "chain: zen right after source-first resolver")

# ------------------------------------------------ 7. DataSource routing
sub("app/src/main/java/com/punjabibeats/app/playback/PbDataSourceFactory.kt",
    '''    private var httpDelegate: DataSource? = null
    private var encryptedDelegate: EncryptedFileDataSource? = null''',
    '''    private var httpDelegate: DataSource? = null
    private var encryptedDelegate: EncryptedFileDataSource? = null
    private var zenDelegate: DataSource? = null''',
    "RoutingDataSource zen delegate field")

sub("app/src/main/java/com/punjabibeats/app/playback/PbDataSourceFactory.kt",
    '''    override fun open(dataSpec: DataSpec): Long = if (dataSpec.uri.scheme.equals("pbenc", ignoreCase = true)) {
        val source = EncryptedFileDataSource(context)
        encryptedDelegate = source
        source.open(dataSpec)
    } else {
        httpDelegate = upstream
        upstream.open(dataSpec)
    }''',
    '''    override fun open(dataSpec: DataSpec): Long = when {
        dataSpec.uri.scheme.equals("pbenc", ignoreCase = true) -> {
            val source = EncryptedFileDataSource(context)
            encryptedDelegate = source
            source.open(dataSpec)
        }
        // Zen engine stream/artwork uris are served by the engine's channel
        // transport — the real media location never leaves the engine.
        dataSpec.uri.scheme.equals("zen", ignoreCase = true) -> {
            val engine = com.zenmusic.ZenMusic.get()
                ?: throw java.io.IOException("zen engine not initialised")
            val source = engine.dataSourceFactory.createDataSource()
            zenDelegate = source
            source.open(dataSpec)
        }
        else -> {
            httpDelegate = upstream
            upstream.open(dataSpec)
        }
    }''',
    "RoutingDataSource routes zen:// into the engine")

sub("app/src/main/java/com/punjabibeats/app/playback/PbDataSourceFactory.kt",
    '''    override fun read(target: ByteArray, offset: Int, length: Int): Int {
        encryptedDelegate?.let { return it.read(target, offset, length) }
        httpDelegate?.let { return it.read(target, offset, length) }''',
    '''    override fun read(target: ByteArray, offset: Int, length: Int): Int {
        encryptedDelegate?.let { return it.read(target, offset, length) }
        zenDelegate?.let { return it.read(target, offset, length) }
        httpDelegate?.let { return it.read(target, offset, length) }''',
    "RoutingDataSource read routes zen delegate")

sub("app/src/main/java/com/punjabibeats/app/playback/PbDataSourceFactory.kt",
    '''    override fun getUri(): android.net.Uri? =
        encryptedDelegate?.getUri() ?: httpDelegate?.getUri()

    override fun close() {
        runCatching { encryptedDelegate?.close() }
        runCatching { httpDelegate?.close() }
        encryptedDelegate = null
        httpDelegate = null
    }''',
    '''    override fun getUri(): android.net.Uri? =
        encryptedDelegate?.getUri() ?: zenDelegate?.getUri() ?: httpDelegate?.getUri()

    override fun close() {
        runCatching { encryptedDelegate?.close() }
        runCatching { zenDelegate?.close() }
        runCatching { httpDelegate?.close() }
        encryptedDelegate = null
        zenDelegate = null
        httpDelegate = null
    }''',
    "RoutingDataSource close/getUri include zen delegate")

# ------------------------------------------------ 8. zen-backed search
sub("app/src/main/java/com/punjabibeats/app/data/repository/SongRepositoryImpl.kt",
    '''    private suspend fun searchYouTube(query: String): List<Song> {
        val direct = runSuspendCatching {''',
    '''    private suspend fun searchYouTube(query: String): List<Song> {
        // Zen engine first: on-device YouTube Music search, no backend API.
        val zen = runSuspendCatching {
            com.zenmusic.ZenMusic.get()?.search?.songs(query)?.take(25)?.map { track ->
                val videoId = track.uri.removePrefix("zen://").substringBefore('.')
                Song(
                    id = "yt_$videoId",
                    title = track.title,
                    artist = track.artists.firstOrNull() ?: "Unknown Artist",
                    durationMs = track.durationMs,
                    source = MusicSource.YOUTUBE,
                    videoId = videoId,
                )
            }.orEmpty()
        }.getOrDefault(emptyList())
        if (zen.isNotEmpty()) return zen

        val direct = runSuspendCatching {''',
    "searchYouTube: zen engine first, InnerTube/Piped fallback")

print("ZENMUSIC INTEGRATION DONE")
