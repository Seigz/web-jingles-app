package com.seigz.webjingles.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.seigz.webjingles.data.model.SearchResult
import com.seigz.webjingles.data.preferences.AppPreferences
import com.seigz.webjingles.data.repository.SearchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val recentSearches: List<String> = emptyList(),
    val selectedIndex: Int = -1,
    val nextPageToken: String? = null,
    val prevPageToken: String? = null,
    val currentPage: Int = 1
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SearchRepository()
    private val preferences = AppPreferences(application)

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.recentSearches.collect { searches ->
                _uiState.value = _uiState.value.copy(
                    recentSearches = searches.toList().reversed()
                )
            }
        }
        viewModelScope.launch {
            preferences.youtubeApiKey.collect { key ->
                SearchRepository.API_KEY = key
            }
        }
        viewModelScope.launch {
            preferences.betterSearching.collect { enabled ->
                SearchRepository.BETTER_SEARCHING = enabled
            }
        }
    }

    fun updateQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun search(query: String? = null, pageToken: String? = null, page: Int = 1) {
        val searchQuery = query ?: _uiState.value.query
        if (searchQuery.isBlank()) return

        _uiState.value = _uiState.value.copy(
            query = searchQuery,
            isLoading = true,
            error = null,
            selectedIndex = -1
        )

        viewModelScope.launch {
            if (page == 1) {
                preferences.addRecentSearch(searchQuery)
            }

            val result = repository.search(searchQuery, pageToken)
            result.fold(
                onSuccess = { searchPage ->
                    _uiState.value = _uiState.value.copy(
                        results = searchPage.results,
                        isLoading = false,
                        selectedIndex = if (searchPage.results.isNotEmpty()) 0 else -1,
                        nextPageToken = searchPage.nextPageToken,
                        prevPageToken = searchPage.prevPageToken,
                        currentPage = page
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Search failed"
                    )
                }
            )
        }
    }

    fun nextPage() {
        val state = _uiState.value
        val token = state.nextPageToken ?: return
        search(state.query, pageToken = token, page = state.currentPage + 1)
    }

    fun prevPage() {
        val state = _uiState.value
        val token = state.prevPageToken ?: return
        search(state.query, pageToken = token, page = state.currentPage - 1)
    }

    fun clearSearch() {
        _uiState.value = SearchUiState(
            recentSearches = _uiState.value.recentSearches
        )
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            preferences.clearRecentSearches()
        }
    }

    fun moveSelection(delta: Int) {
        val current = _uiState.value
        val maxIndex = current.results.size - 1
        if (maxIndex < 0) return
        val newIndex = (current.selectedIndex + delta).coerceIn(0, maxIndex)
        _uiState.value = current.copy(selectedIndex = newIndex)
    }

    fun getSelectedResult(): SearchResult? {
        val state = _uiState.value
        return if (state.selectedIndex in state.results.indices) {
            state.results[state.selectedIndex]
        } else null
    }
}
