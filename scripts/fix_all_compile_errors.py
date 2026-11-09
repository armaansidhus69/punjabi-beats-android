#!/usr/bin/env python3
"""Fixes the real source bugs found by javap ground-truthing the resolved
media3 1.4.1 API + reading every error region. ~35 patches."""
import pathlib

P = pathlib.Path(__file__).resolve().parent.parent / "punjabi-beats"
if not P.exists():
    P = pathlib.Path("/home/z/my-project/download/punjabi-beats")
A = P / "app/src/main/java/com/punjabibeats/app"

def sub(rel, old, new, label, count=1):
    f = A / rel if not rel.startswith("/") else pathlib.Path(rel)
    s = f.read_text(encoding="utf-8", errors="ignore")
    if new in s and old not in s:
        print(f"skip  {label}")
        return
    c = s.count(old)
    if c != count:
        raise SystemExit(f"ANCHOR {label}: found {c}, expected {count}")
    f.write_text(s.replace(old, new), encoding="utf-8")
    print(f"OK    {label}")

def add_import(rel, imp, anchor, label):
    f = A / rel
    s = f.read_text(encoding="utf-8", errors="ignore")
    if imp in s:
        print(f"skip  {label}")
        return
    sub(rel, anchor, imp + "\n" + anchor, label)

# ================= media3 1.4.1 API fixes =================
sub("playback/PlayerControllerImpl.kt",
    "        controller.playbackParameters = controller.playbackParameters.withPitch(pitch)",
    "        controller.playbackParameters = androidx.media3.common.PlaybackParameters(\n"
    "            controller.playbackParameters.speed,\n            pitch,\n        )",
    "PlayerController: setPlaybackPitch via ctor (no withPitch in media3)")

sub("playback/PlayerControllerImpl.kt",
    "        controller.skipSilenceEnabled = enabled",
    "        if (controller is androidx.media3.exoplayer.ExoPlayer) {\n"
    "            controller.setSkipSilenceEnabled(enabled)\n        }",
    "PlayerController: setSkipSilence (ExoPlayer-only API)")

sub("playback/PlayerControllerImpl.kt",
    "        _queue.value = controller.currentMediaItems.toSongs()",
    "        _queue.value = (0 until controller.mediaItemCount)\n"
    "            .map { controller.getMediaItemAt(it) }.toSongs()",
    "PlayerController: timeline walk instead of currentMediaItems")

sub("playback/PlayerControllerImpl.kt",
    "                controller.skipSilenceEnabled = _skipSilenceEnabled.value",
    "                if (controller is androidx.media3.exoplayer.ExoPlayer) {\n"
    "                    controller.setSkipSilenceEnabled(_skipSilenceEnabled.value)\n                }",
    "PlayerController: restore skipSilence via ExoPlayer cast")

sub("playback/PlayerControllerImpl.kt",
    "                controller.playbackParameters = controller.playbackParameters\n"
    "                    .withSpeed(_playbackSpeed.value)\n"
    "                    .withPitch(_playbackPitch.value)",
    "                controller.playbackParameters = androidx.media3.common.PlaybackParameters(\n"
    "                    _playbackSpeed.value,\n                    _playbackPitch.value,\n                )",
    "PlayerController: restore speed+pitch via ctor")

sub("playback/MediaPlaybackService.kt",
    "                enableAudioTrackPlaybackParams: Boolean,\n"
    "                enableOffload: Boolean,\n"
    "            ): AudioSink = DefaultAudioSink.Builder(context)",
    "                enableAudioTrackPlaybackParams: Boolean,\n"
    "            ): AudioSink = DefaultAudioSink.Builder(context)",
    "Service: buildAudioSink 3-param override (1.4.1 signature)")

sub("playback/audio/CrossfadeAudioProcessor.kt",
    "    override fun queueInput(input: ByteBuffer): ByteBuffer {\n"
    "        val remaining = input.remaining()\n"
    "        if (remaining == 0) return replaceOutputBuffer(0)\n"
    "        val output = replaceOutputBuffer(remaining)",
    "    override fun queueInput(input: ByteBuffer) {\n"
    "        val remaining = input.remaining()\n"
    "        if (remaining == 0) {\n"
    "            replaceOutputBuffer(0)\n"
    "            return\n"
    "        }\n"
    "        val output = replaceOutputBuffer(remaining)",
    "Crossfade: queueInput void signature (1.4.1)")

sub("playback/audio/CrossfadeAudioProcessor.kt",
    "        output.flip()\n"
    "        val framesAdvanced = remaining.toLong() / maxOf(1, bytesPerFrame())\n"
    "        frameCount += framesAdvanced\n"
    "        return output\n"
    "    }",
    "        val framesAdvanced = remaining.toLong() / maxOf(1, bytesPerFrame())\n"
    "        frameCount += framesAdvanced\n"
    "    }",
    "Crossfade: no flip/return — getOutput() flips lazily")

sub("playback/audio/CrossfadeAudioProcessor.kt",
    "        val isPcm16 = inputAudioFormat.pcmEncoding == C.ENCODING_PCM_16BIT",
    "        val isPcm16 = inputAudioFormat.encoding == C.ENCODING_PCM_16BIT",
    "Crossfade: AudioFormat.encoding field name")

sub("playback/audio/CrossfadeAudioProcessor.kt",
    "    private fun bytesPerFrame(): Int = when (inputAudioFormat.pcmEncoding) {\n"
    "        C.ENCODING_PCM_16BIT -> 2 * inputAudioFormat.channelCount\n"
    "        C.ENCODING_PCM_8BIT -> inputAudioFormat.channelCount\n"
    "        C.ENCODING_PCM_24BIT -> 3 * inputAudioFormat.channelCount\n"
    "        C.ENCODING_PCM_32BIT, C.ENCODING_PCM_FLOAT -> 4 * inputAudioFormat.channelCount\n"
    "        else -> 2 * inputAudioFormat.channelCount\n"
    "    }",
    "    private fun bytesPerFrame(): Int =\n"
    "        if (inputAudioFormat.bytesPerFrame > 0) inputAudioFormat.bytesPerFrame\n"
    "        else 2 * inputAudioFormat.channelCount",
    "Crossfade: bytesPerFrame from AudioFormat.bytesPerFrame")

sub("playback/audio/AudioEffectsManager.kt",
    "                bandLevelsMilliBel.take(eq.numberOfBands).forEachIndexed { band, level ->",
    "                bandLevelsMilliBel.take(eq.numberOfBands.toInt()).forEachIndexed { band, level ->",
    "AudioEffects: numberOfBands Short -> Int for take()")

# ================= repository fixes =================
sub("data/repository/SongRepositoryImpl.kt",
    "import com.punjabibeats.app.data.remote.dto.toSong\n", "",
    "SongRepo: drop unresolvable top-level toSong import (all toSong are members)")

sub("data/repository/SongRepositoryImpl.kt",
    "                            year = entry.year?.toIntOrNull(),\n"
    "                        )\n"
    "                    }\n"
    "                }\n"
    "            }.getOrDefault(emptyList())",
    "                            year = entry.year?.toIntOrNull(),\n"
    "                        )\n"
    "                    }\n"
    "                } ?: emptyList()\n"
    "            }.getOrDefault(emptyList())",
    "SongRepo: albums async null-leak (withTimeoutOrNull)")

sub("data/repository/SongRepositoryImpl.kt",
    "                            artworkUrl = entry.image?.best()?.url,\n"
    "                        )\n"
    "                    }\n"
    "                }\n"
    "            }.getOrDefault(emptyList())",
    "                            artworkUrl = entry.image?.best()?.url,\n"
    "                        )\n"
    "                    }\n"
    "                } ?: emptyList()\n"
    "            }.getOrDefault(emptyList())",
    "SongRepo: artists async null-leak")

sub("data/repository/SongRepositoryImpl.kt",
    "                    .mapNotNull { it.toSong() }\n"
    "            }\n"
    "        }.getOrDefault(emptyList())",
    "                    .mapNotNull { it.toSong() }\n"
    "            } ?: emptyList()\n"
    "        }.getOrDefault(emptyList())",
    "SongRepo: searchSongs null-leak (return type fix)")

sub("data/repository/SongRepositoryImpl.kt",
    "            withTimeoutOrNull(2_000L) { historyDao.observeTopArtists(3).first() }\n"
    "        }.getOrDefault(emptyList())",
    "            withTimeoutOrNull(2_000L) {\n"
    "                historyDao.observeTopArtists(3).first().map { it.artist }\n"
    "            } ?: emptyList()\n"
    "        }.getOrDefault(emptyList())",
    "SongRepo: quick picks map ArtistCount -> artist name, non-null")

sub("ui/screens/library/LibraryViewModel.kt",
    "    fun removeFromPlaylist(playlistId: String, songId: String) {",
    "    fun observePlaylistSongs(playlistId: String): Flow<List<Song>> =\n"
    "        libraryRepository.observePlaylistSongs(playlistId)\n\n"
    "    fun removeFromPlaylist(playlistId: String, songId: String) {",
    "LibraryVM: observePlaylistSongs passthrough (UI calls it)")

add_import("ui/screens/library/LibraryViewModel.kt",
           "import kotlinx.coroutines.flow.Flow", "import kotlinx.coroutines.launch",
           "LibraryVM: Flow import if missing")

# ================= UI import/annotation fixes =================
sub("ui/screens/player/FullPlayerScreen.kt",
    "import coil.imageLoader\n"
    "import coil.request.ImageRequest\n"
    "import androidx.media3.common.Player\n"
    "import coil.imageLoader\n"
    "import coil.request.ImageRequest",
    "import coil.imageLoader\n"
    "import coil.request.ImageRequest\n"
    "import androidx.media3.common.Player",
    "FullPlayer: remove duplicate coil imports")

sub("ui/PunjabiBeatsAppRoot.kt",
    "import androidx.compose.material.icons.automirrored.rounded.LibraryMusic",
    "import androidx.compose.material.icons.rounded.LibraryMusic",
    "AppRoot: LibraryMusic non-mirrored import")

sub("ui/PunjabiBeatsAppRoot.kt",
    "Icons.AutoMirrored.Rounded.LibraryMusic",
    "Icons.Rounded.LibraryMusic",
    "AppRoot: LibraryMusic usage")

sub("ui/theme/Theme.kt",
    "androidx.compose.foundation.shape.Shapes(",
    "androidx.compose.material3.Shapes(",
    "Theme: Shapes lives in material3, not foundation.shape")

for f, n in [("ui/screens/jam/JamScreen.kt", 2),
             ("ui/screens/ai/AiStudioScreen.kt", 2),
             ("ui/screens/search/SearchScreen.kt", 1)]:
    sub(f, "androidx.compose.material3.BasicTextField(",
        "androidx.compose.foundation.text.BasicTextField(",
        f"{f.split('/')[-1]}: BasicTextField -> foundation.text", count=n)

add_import("ui/screens/ai/AiStudioScreen.kt",
           "import androidx.compose.runtime.setValue", "import androidx.compose.runtime.getValue",
           "AiStudio: setValue import for mutableIntStateOf delegate")

add_import("ui/components/CommonUi.kt",
           "import androidx.compose.foundation.layout.fillMaxSize",
           "import androidx.compose.foundation.layout.fillMaxWidth",
           "CommonUi: fillMaxSize import")

add_import("ui/navigation/AppNavHost.kt",
           "import androidx.compose.ui.unit.dp", "import androidx.compose.foundation.layout.PaddingValues",
           "AppNavHost: dp import")

# file-level experimental opt-in for combinedClickable
f = A / "ui/screens/lyrics/LyricsScreen.kt"
s = f.read_text()
if not s.startswith("@file:OptIn"):
    s = "@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)\n\n" + s
    f.write_text(s)
    print("OK    LyricsScreen: file-level ExperimentalFoundationApi opt-in")

f = A / "ui/screens/lyrics/LyricsScreen.kt"
s = f.read_text()
if "lines.drop(currentLineIndex).take(3).map { it.text }" in s:
    f.write_text(s.replace("lines.drop(currentLineIndex).take(3).map { it.text }",
                           "lines.drop(currentLineIndex).take(3)"))
    print("OK    LyricsScreen: selectedLines branches both List<LyricLine>")

sub("ui/screens/podcast/PodcastScreen.kt",
    "private fun TranscriptSheet(",
    "@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)\nprivate fun TranscriptSheet(",
    "PodcastScreen: ModalBottomSheet opt-in")

# ================= jam / innertube / misc =================
sub("jam/JamClockSync.kt",
    "val body = response.body?.string().orEmpty()",
    "val body = response.body()?.string().orEmpty()",
    "JamClockSync: Response.body() call (K2 no synthetic field access)")

sub("jam/JamTransport.kt",
    "val body = response.body?.string().orEmpty()",
    "val body = response.body()?.string().orEmpty()",
    "JamTransport: Response.body() call")

sub("jam/JamEngine.kt",
    "JamRoomUi(code = code, myId = me.userId, phase = JamPhase.IDLE)",
    "JamRoomUi(myId = me.userId)",
    "JamEngine: JamRoomUi has no code/phase-named params")

sub("jam/JamEngine.kt",
    "    fun close() {\n"
    "        runCatching { transport?.send(JamMessage.Leave(code, me.userId)) }\n"
    "        transport?.close()",
    "    fun close() {\n"
    "        val currentTransport = transport\n"
    "        scope.launch { runCatching { currentTransport?.send(JamMessage.Leave(code, me.userId)) } }\n"
    "        currentTransport?.close()",
    "JamEngine: suspend send moved into scope.launch")

add_import("jam/JamEngine.kt",
           "import kotlinx.coroutines.launch", "import kotlinx.coroutines.CoroutineScope",
           "JamEngine: launch import if missing")

sub("domain/model/JamModels.kt",
    "@Serializable\n@Serializable\ndata class JamQueueEntry(",
    "@Serializable\ndata class JamQueueEntry(",
    "JamModels: remove duplicate @Serializable")

sub("innertube/InnerTubeClient.kt",
    "configStore.config().client(WEB_REMIX)",
    'configStore.config().client("WEB_REMIX")',
    "InnerTubeClient: WEB_REMIX is a string literal")

add_import("innertube/InnerTubeClient.kt",
           "import com.punjabibeats.app.data.cipher.PlayerConfigParser",
           "import com.punjabibeats.app.data.cipher.PlayerConfigStore",
           "InnerTubeClient: PlayerConfigParser import")

add_import("innertube/InnerTubeResolver.kt",
           "import com.punjabibeats.app.core.util.firstObject",
           "import com.punjabibeats.app.data.cipher.YtCipherManager",
           "InnerTubeResolver: firstObject import")

add_import("innertube/InnerTubeResolver.kt",
           "import com.punjabibeats.app.core.util.firstString",
           "import com.punjabibeats.app.data.cipher.YtCipherManager",
           "InnerTubeResolver: firstString import")

sub("data/resolver/Resolvers.kt",
    "val body = backend.ytStream(videoId).string()",
    "val body = backend.ytStream(videoId).body()?.string().orEmpty()",
    "Resolvers: Response<ResponseBody>.body()?.string()")

sub("data/security/EncryptedFileDataSource.kt",
    "class EncryptedFileDataSource(private val context: Context) : BaseDataSource() {",
    "class EncryptedFileDataSource(private val context: Context) : BaseDataSource(false) {",
    "EncryptedFileDataSource: BaseDataSource(isNetwork=false)")

print("MEGA FIX DONE")
