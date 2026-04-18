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
    val selectedIndex: Int = -1
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
    }

    fun updateQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun search(query: String? = null) {
        val searchQuery = query ?: _uiState.value.query
        if (searchQuery.isBlank()) return

        _uiState.value = _uiState.value.copy(
            query = searchQuery,
            isLoading = true,
            error = null,
            selectedIndex = -1
        )

        viewModelScope.launch {
            preferences.addRecentSearch(searchQuery)

            val result = repository.search(searchQuery)
            result.fold(
                onSuccess = { results ->
                    _uiState.value = _uiState.value.copy(
                        results = results,
                        isLoading = false,
                        selectedIndex = if (results.isNotEmpty()) 0 else -1
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
