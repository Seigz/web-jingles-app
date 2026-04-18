package com.seigz.webjingles.data.model

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Downloading(val progress: Float, val fileName: String) : DownloadState()
    data class Completed(val filePath: String, val fileName: String) : DownloadState()
    data class Failed(val error: String) : DownloadState()
}

data class FavoriteTrack(
    val id: String,
    val title: String,
    val gameName: String,
    val duration: String,
    val thumbnailUrl: String?,
    val sourceUrl: String,
    val timestamp: Long = System.currentTimeMillis()
)
