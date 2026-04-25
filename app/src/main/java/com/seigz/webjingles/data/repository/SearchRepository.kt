package com.seigz.webjingles.data.repository

import com.seigz.webjingles.data.api.RetrofitClient
import com.seigz.webjingles.data.model.SearchResult
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SearchPage(
    val results: List<SearchResult>,
    val nextPageToken: String?,
    val prevPageToken: String?
)

class SearchRepository {

    companion object {
        // Users must supply their own YouTube Data API v3 key.
        // Set it in the app Settings screen or replace the default here.
        var API_KEY: String = ""
        var BETTER_SEARCHING: Boolean = true
    }

    suspend fun search(
        query: String,
        pageToken: String? = null
    ): Result<SearchPage> = withContext(Dispatchers.IO) {
        try {
            if (API_KEY.isBlank()) {
                return@withContext Result.failure(
                    IllegalStateException("YouTube API key not set. Please add your API key in Settings.")
                )
            }

            val effectiveQuery = if (BETTER_SEARCHING) query else "$query game music OST"

            val searchResponse = RetrofitClient.youTubeApi.searchVideos(
                query = effectiveQuery,
                pageToken = pageToken,
                apiKey = API_KEY
            )

            val videoIds = searchResponse.items
                ?.mapNotNull { it.id?.videoId }
                ?.joinToString(",") ?: ""

            if (videoIds.isBlank()) {
                return@withContext Result.success(SearchPage(emptyList(), null, null))
            }

            val detailsResponse = RetrofitClient.youTubeApi.getVideoDetails(
                id = videoIds,
                apiKey = API_KEY
            )

            val durationMap = detailsResponse.items?.associate {
                (it.id ?: "") to (it.contentDetails?.duration ?: "PT0S")
            } ?: emptyMap()

            val results = searchResponse.items?.mapNotNull { item ->
                val videoId = item.id?.videoId ?: return@mapNotNull null
                val snippet = item.snippet ?: return@mapNotNull null
                val isoDuration = durationMap[videoId] ?: "PT0S"
                val (formatted, seconds) = parseIsoDuration(isoDuration)

                SearchResult(
                    id = videoId,
                    title = snippet.title ?: "Unknown",
                    gameName = snippet.channelTitle ?: "Unknown",
                    duration = formatted,
                    durationSeconds = seconds,
                    thumbnailUrl = snippet.thumbnails?.medium?.url
                        ?: snippet.thumbnails?.default?.url,
                    streamUrl = null,
                    sourceUrl = "https://www.youtube.com/watch?v=$videoId"
                )
            } ?: emptyList()

            Result.success(
                SearchPage(
                    results = results,
                    nextPageToken = searchResponse.nextPageToken,
                    prevPageToken = searchResponse.prevPageToken
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseIsoDuration(iso: String): Pair<String, Int> {
        val regex = Regex("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?")
        val match = regex.find(iso)
        val hours = match?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val minutes = match?.groupValues?.get(2)?.toIntOrNull() ?: 0
        val seconds = match?.groupValues?.get(3)?.toIntOrNull() ?: 0
        val totalSeconds = hours * 3600 + minutes * 60 + seconds

        val formatted = if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
        return formatted to totalSeconds
    }
}
