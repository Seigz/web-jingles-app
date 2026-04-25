package com.seigz.webjingles.data.model

data class SearchResult(
    val id: String,
    val title: String,
    val gameName: String,
    val duration: String,
    val durationSeconds: Int,
    val thumbnailUrl: String?,
    val streamUrl: String?,
    val sourceUrl: String,
    val source: String = "YouTube"
)

data class YouTubeSearchResponse(
    val items: List<YouTubeSearchItem>?,
    val nextPageToken: String?,
    val prevPageToken: String?
)

data class YouTubeSearchItem(
    val id: YouTubeVideoId?,
    val snippet: YouTubeSnippet?
)

data class YouTubeVideoId(
    val videoId: String?
)

data class YouTubeSnippet(
    val title: String?,
    val channelTitle: String?,
    val thumbnails: YouTubeThumbnails?,
    val description: String?
)

data class YouTubeThumbnails(
    val default: YouTubeThumbnail?,
    val medium: YouTubeThumbnail?,
    val high: YouTubeThumbnail?
)

data class YouTubeThumbnail(
    val url: String?,
    val width: Int?,
    val height: Int?
)

data class YouTubeVideoDetailsResponse(
    val items: List<YouTubeVideoDetailItem>?
)

data class YouTubeVideoDetailItem(
    val id: String?,
    val contentDetails: YouTubeContentDetails?
)

data class YouTubeContentDetails(
    val duration: String?
)
