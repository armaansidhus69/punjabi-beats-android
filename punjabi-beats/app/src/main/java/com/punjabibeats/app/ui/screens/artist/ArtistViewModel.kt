package com.punjabibeats.app.ui.screens.artist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punjabibeats.app.core.util.runSuspendCatching
import com.punjabibeats.app.domain.model.AlbumCard
import com.punjabibeats.app.domain.model.Song
import com.punjabibeats.app.domain.repository.SongRepository
import com.punjabibeats.app.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.URLDecoder
import javax.inject.Inject

data class ArtistBioInfo(
    val bio: String,
    val origin: String,
    val monthlyListeners: String,
)

data class ArtistUiState(
    val isLoading: Boolean = true,
    val artistName: String = "",
    val songs: List<Song> = emptyList(),
    val albums: List<AlbumCard> = emptyList(),
    val artworkUrl: String? = null,
    val bioInfo: ArtistBioInfo = ArtistBioInfo(
        bio = "Celebrated Punjabi artist delivering authentic rhythms, folk heritage and urban beats.",
        origin = "Punjab, India",
        monthlyListeners = "12.4M monthly listeners",
    ),
)

@HiltViewModel
class ArtistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val songRepository: SongRepository,
) : ViewModel() {

    private val rawName = savedStateHandle.get<String>(Routes.ARG_ARTIST_NAME).orEmpty()
    val artistName: String = try {
        URLDecoder.decode(rawName, "UTF-8")
    } catch (_: Exception) {
        rawName
    }

    private val _uiState = MutableStateFlow(ArtistUiState(artistName = artistName, bioInfo = getBioForArtist(artistName)))
    val uiState: StateFlow<ArtistUiState> = _uiState.asStateFlow()

    init {
        loadArtistData()
    }

    private var loadArtistJob: kotlinx.coroutines.Job? = null

    fun loadArtistData() {
        loadArtistJob?.cancel()
        loadArtistJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            runSuspendCatching {
                val results = songRepository.searchAll(artistName)
                val songs = if (results.songs.isNotEmpty()) {
                    results.songs
                } else {
                    songRepository.searchSongs(artistName, limit = 25)
                }

                val albums = if (results.albums.isNotEmpty()) {
                    results.albums
                } else {
                    songs.mapNotNull { it.albumName.takeIf { a -> a.isNotBlank() } }
                        .distinct()
                        .take(8)
                        .mapIndexed { idx, albumName ->
                            AlbumCard(
                                id = "album_${artistName}_$idx",
                                name = albumName,
                                artist = artistName,
                                artworkUrl = songs.firstOrNull { it.albumName == albumName }?.artworkUrl,
                            )
                        }
                }

                val topArtwork = results.artists.firstOrNull { it.name.contains(artistName, ignoreCase = true) }?.artworkUrl
                    ?: songs.firstOrNull { !it.artworkUrl.isNullOrBlank() }?.artworkUrl

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    songs = songs,
                    albums = albums,
                    artworkUrl = topArtwork,
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    companion object {
        fun getBioForArtist(name: String): ArtistBioInfo {
            val lower = name.lowercase()
            return when {
                lower.contains("sidhu") || lower.contains("moose") -> ArtistBioInfo(
                    bio = "Pioneer of modern Punjabi hip-hop and gangster rap. His fierce lyricism and raw authenticity conquered global Billboard charts and immortalized his legacy.",
                    origin = "Moose, Mansa, Punjab",
                    monthlyListeners = "18.5M monthly listeners",
                )
                lower.contains("karan") || lower.contains("aujla") -> ArtistBioInfo(
                    bio = "Known as 'Geetan Di Machine', a generational lyricist, rapper and singer celebrated for chart-topping masterpieces Making Memories, Four You, and Street Dreams.",
                    origin = "Ghurala, Ludhiana, Punjab",
                    monthlyListeners = "14.2M monthly listeners",
                )
                lower.contains("diljit") || lower.contains("dosanjh") -> ArtistBioInfo(
                    bio = "Global Punjabi icon and historic Coachella headliner. Beloved worldwide for anthems like G.O.A.T., Born to Shine, Lover, and Lemonade.",
                    origin = "Dosanjh Kalan, Jalandhar, Punjab",
                    monthlyListeners = "21.8M monthly listeners",
                )
                lower.contains("dhillon") -> ArtistBioInfo(
                    bio = "Architect of the Run-Up Records movement and 'Brown Munde'. Blending Punjabi folk vocals with synthwave, trap, and contemporary RnB.",
                    origin = "Gurdaspur, Punjab",
                    monthlyListeners = "11.6M monthly listeners",
                )
                lower.contains("shubh") -> ArtistBioInfo(
                    bio = "Viral international sensation behind worldwide smashes Baller, Elevated, No Love, Cheques, and Still Rollin.",
                    origin = "Punjab, India",
                    monthlyListeners = "12.8M monthly listeners",
                )
                lower.contains("amrit") || lower.contains("maan") -> ArtistBioInfo(
                    bio = "The 'Goniana Aala' hitmaker and songwriter known for powerful vocals, sharp folk lyrics, and iconic Punjabi anthems.",
                    origin = "Goniana, Bathinda, Punjab",
                    monthlyListeners = "5.2M monthly listeners",
                )
                lower.contains("randhawa") -> ArtistBioInfo(
                    bio = "Global Punjabi pop superstar behind High Rated Gabru, Lahore, and international cross-genre collaborations.",
                    origin = "Gurdaspur, Punjab",
                    monthlyListeners = "9.8M monthly listeners",
                )
                else -> ArtistBioInfo(
                    bio = "Celebrated Punjabi artist delivering authentic rhythms, folk heritage and urban beats.",
                    origin = "Punjab, India",
                    monthlyListeners = "Top Punjabi Artist",
                )
            }
        }
    }
}
