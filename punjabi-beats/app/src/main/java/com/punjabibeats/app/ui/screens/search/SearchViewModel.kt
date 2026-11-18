package com.punjabibeats.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punjabibeats.app.core.util.runSuspendCatching
import com.punjabibeats.app.core.util.userMessage
import com.punjabibeats.app.domain.model.PodcastChannel
import com.punjabibeats.app.domain.model.SearchResults
import com.punjabibeats.app.domain.model.TRENDING_SEARCHES
import com.punjabibeats.app.domain.repository.LibraryRepository
import com.punjabibeats.app.domain.repository.PodcastRepository
import com.punjabibeats.app.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SearchTab { ALL, SONGS, ARTISTS, ALBUMS, PODCASTS }

data class SearchUiState(
    val query: String = "",
    val results: SearchResults? = null,
    val isSearching: Boolean = false,
    val error: String? = null,
    val recentSearches: List<String> = emptyList(),
    val trendingSearches: List<String> = emptyList(),
    val tab: SearchTab = SearchTab.ALL,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val podcastRepository: PodcastRepository,
    private val libraryRepository: LibraryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SearchUiState(trendingSearches = songRepository.trendingSearches())
    )
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            libraryRepository.observeRecentSearches().collect { recent ->
                _uiState.update { it.copy(recentSearches = recent) }
            }
        }
        viewModelScope.launch {
            queryFlow
                .debounce(150)
                .collectLatest { query ->
                    executeSearch(query)
                }
        }
    }

    private suspend fun executeSearch(query: String) {
        if (query.isBlank()) {
            _uiState.update { it.copy(results = null, isSearching = false, error = null) }
            return
        }
        _uiState.update { it.copy(isSearching = true, error = null) }
        coroutineScope {
            val songsDeferred = async { runSuspendCatching { songRepository.searchAll(query) } }
            val podcastsDeferred = async {
                runSuspendCatching { podcastRepository.searchLocalChannels(query) }
                    .getOrDefault(emptyList<PodcastChannel>())
            }
            val searchResult = songsDeferred.await()
            val podcasts = podcastsDeferred.await()
            searchResult.fold(
                onSuccess = { results ->
                    _uiState.update {
                        it.copy(
                            results = results.copy(podcasts = podcasts),
                            isSearching = false,
                            error = null,
                        )
                    }
                },
                onFailure = { failure ->
                    _uiState.update {
                        it.copy(isSearching = false, error = failure.userMessage())
                    }
                },
            )
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        queryFlow.value = query
    }

    fun submitSearch() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return
        viewModelScope.launch {
            runSuspendCatching { libraryRepository.addRecentSearch(query) }
        }
    }

    fun onSuggestionClick(query: String) {
        _uiState.update { it.copy(query = query) }
        queryFlow.value = query
        submitSearch()
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            executeSearch(query)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            runSuspendCatching { libraryRepository.clearRecentSearches() }
        }
    }

    fun setTab(tab: SearchTab) {
        _uiState.update { it.copy(tab = tab) }
    }
}
