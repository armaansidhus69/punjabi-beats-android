#!/usr/bin/env python3
"""Fixes real source bugs (missing imports, suspend misuse, missing overload),
plus the vendored engine fixes and build-infra settings."""
import pathlib

P = pathlib.Path(__file__).resolve().parent.parent / "punjabi-beats"
if not P.exists():
    P = pathlib.Path("/home/z/my-project/download/punjabi-beats")
APP = P / "app/src/main/java/com/punjabibeats/app"

def sub(path, old, new, label, must=True):
    p = P / path if not str(path).startswith("/") else pathlib.Path(path)
    s = p.read_text(encoding="utf-8", errors="ignore")
    if new in s and old not in s:
        print(f"skip  {label} (already applied)")
        return
    if s.count(old) != 1:
        msg = f"ANCHOR{'  x'+str(s.count(old)) if s.count(old)>1 else ' MISSING'}: {label}"
        if must:
            raise SystemExit(msg)
        print("warn  " + msg)
        return
    p.write_text(s.replace(old, new, 1), encoding="utf-8")
    print(f"OK    {label}")

# ---- 1. JamViewModel: SongRepository used without import (KSP NonExistentClass)
f = APP / "ui/screens/jam/JamViewModel.kt"
s = f.read_text()
if "import com.punjabibeats.app.domain.repository.SongRepository" not in s:
    sub(str(f), "import com.punjabibeats.app.playback.MusicPlayerController",
        "import com.punjabibeats.app.domain.repository.SongRepository\n"
        "import com.punjabibeats.app.playback.MusicPlayerController",
        "JamViewModel: add SongRepository import")

# ---- 2. JioSaavnDtos: bare Song used without import -> toSong never exported
f = APP / "data/remote/dto/JioSaavnDtos.kt"
s = f.read_text()
if "import com.punjabibeats.app.domain.model.Song" not in s:
    sub(str(f), "import kotlinx.serialization.KSerializer",
        "import com.punjabibeats.app.domain.model.Song\nimport kotlinx.serialization.KSerializer",
        "JioSaavnDtos: add Song import")

# ---- 3. AudiusDtos: @SerialName used without import
f = APP / "data/remote/dto/AudiusDtos.kt"
s = f.read_text()
if "import kotlinx.serialization.SerialName" not in s:
    sub(str(f), "import kotlinx.serialization.Serializable",
        "import kotlinx.serialization.SerialName\nimport kotlinx.serialization.Serializable",
        "AudiusDtos: add SerialName import")

# ---- 4. PlayerConfigStore: PbJson used without import
f = APP / "data/cipher/PlayerConfigStore.kt"
s = f.read_text()
if "import com.punjabibeats.app.core.util.PbJson" not in s:
    sub(str(f), "import com.punjabibeats.app.core.util.runSuspendCatching",
        "import com.punjabibeats.app.core.util.PbJson\n"
        "import com.punjabibeats.app.core.util.runSuspendCatching",
        "PlayerConfigStore: add PbJson import")

# ---- 5. YtCipherManager: firstObject/firstLong used without import
f = APP / "data/cipher/YtCipherManager.kt"
s = f.read_text()
add = []
if "import com.punjabibeats.app.core.util.firstObject" not in s:
    add.append("import com.punjabibeats.app.core.util.firstObject")
if "import com.punjabibeats.app.core.util.firstLong" not in s:
    add.append("import com.punjabibeats.app.core.util.firstLong")
if add:
    sub(str(f), "import com.punjabibeats.app.core.util.firstString",
        "\n".join(add) + "\nimport com.punjabibeats.app.core.util.firstString",
        "YtCipherManager: add " + ", ".join(a.rsplit('.', 1)[-1] for a in add) + " imports")

# ---- 6. StreamValidator: runSuspendCatching (suspend) called in non-suspend probe
sub("app/src/main/java/com/punjabibeats/app/data/cipher/StreamValidator.kt",
    "    private fun probe(url: String, offset: Long): Probe {",
    "    private suspend fun probe(url: String, offset: Long): Probe {",
    "StreamValidator: probe made suspend")

# ---- 7. JsonExt: objectArray needs vararg keys (AiRepositoryImpl calls with 3 keys)
sub("app/src/main/java/com/punjabibeats/app/core/util/JsonExt.kt",
    '''fun JsonObject.objectArray(key: String): List<JsonObject> =
    firstArray(key).mapNotNull { it as? JsonObject }''',
    '''fun JsonObject.objectArray(vararg keys: String): List<JsonObject> {
    for (key in keys) {
        val list = (this[key] as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
        if (list.isNotEmpty()) return list
    }
    return emptyList()
}''',
    "JsonExt: objectArray now vararg (first non-empty key wins)")

# ---- 8. Vendored engine fixes
sub("zenmusic/src/main/kotlin/com/zenmusic/ZenMusic.kt",
    '''    private val EMULATOR_MARKERS = listOf(
        "generic", "sdk_gphone", "emulator", "vbox86p", "genymotion",
        "nox", "bluestacks", "x86_64",
    )''',
    '''    // Deliberately conservative: every marker here takes real playback away
    // from whatever device matches. `generic` and `x86_64` were removed —
    // they also match AOSP ROM fingerprints and ChromeOS/WSA/x86 hardware,
    // which served decoy streams to real users ("songs never play").
    private val EMULATOR_MARKERS = listOf(
        "sdk_gphone", "emulator", "vbox86p", "genymotion", "nox", "bluestacks",
    )''',
    "engine: narrowed tamper markers (decoy shower false positives)")

sub("zen-datasource/src/main/kotlin/com/zenmusic/datasource/ZenDataSource.kt",
    '''        val stream = kotlinx.coroutines.runBlocking {
            kotlinx.coroutines.withTimeoutOrNull(30_000) { resolver.resolve(uriString) }
        } ?: return null''',
    '''        val stream = kotlinx.coroutines.runBlocking {
            kotlinx.coroutines.withTimeoutOrNull(RE_RESOLVE_TIMEOUT_MS) { resolver.resolve(uriString) }
        } ?: run {
            ZenLog.w("re-resolve timed out after ${RE_RESOLVE_TIMEOUT_MS}ms for $uriString")
            return null
        }''',
    "engine: stale-handle re-resolve bound 30s -> 12s")

sub("zen-datasource/src/main/kotlin/com/zenmusic/datasource/ZenDataSource.kt",
    '''    /** Extracts `#zh=<handle>` fragment, if present. */
    private fun fragmentHandle(uri: String): String? {''',
    '''    private companion object {
        /** A stale handle must not stall the player loader thread for long. */
        const val RE_RESOLVE_TIMEOUT_MS = 12_000L
    }

    /** Extracts `#zh=<handle>` fragment, if present. */
    private fun fragmentHandle(uri: String): String? {''',
    "engine: re-resolve timeout const added")

# ---- 9. Build infra: mirrors + KSP resolutionStrategy in settings
sub("settings.gradle.kts",
    '''pluginManagement {
    repositories {''',
    '''pluginManagement {
    // Resolve KSP by real artifact coordinates — its plugin marker is only on
    // the (throttled) Gradle Portal; the artifact itself is on the central mirror.
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.google.devtools.ksp") {
                useModule("com.google.devtools.ksp:symbol-processing-gradle-plugin:${requested.version}")
            }
        }
    }
    repositories {
        // Fast mirrors first (sandbox IP is rate-limited on repo.maven.apache.org)
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
        google()''',
    "settings: pluginManagement mirrors + KSP strategy")

sub("settings.gradle.kts",
    '''    repositories {
        google()
        mavenCentral()
    }
}''',
    '''    repositories {
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
        google()
        mavenCentral()
    }
}''',
    "settings: dependencyResolutionManagement mirrors")

# ---- 10. gradle.properties: memory tuning for the 4GB sandbox
p = P / "gradle.properties"
s = p.read_text()
s = s.replace("org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8 -XX:+UseParallelGC",
              "org.gradle.jvmargs=-Xmx1792m -Dfile.encoding=UTF-8 -XX:+UseSerialGC\n"
              "org.gradle.workers.max=2\n"
              "kotlin.daemon.jvmargs=-Xmx2048m -XX:+UseSerialGC")
p.write_text(s)
print("OK    gradle.properties memory tuned")

print("SOURCE FIXES DONE")
