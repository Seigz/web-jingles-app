package com.seigz.webjingles.data.api

import retrofit2.http.GET
import retrofit2.http.Path

interface PipedApiService {

    @GET("streams/{videoId}")
    suspend fun getStreams(
        @Path("videoId") videoId: String
    ): PipedStreamResponse
}

data class PipedStreamResponse(
    val title: String?,
    val uploader: String?,
    val duration: Int?,
    val audioStreams: List<PipedAudioStream>?,
    val thumbnailUrl: String?
)

data class PipedAudioStream(
    val url: String?,
    val format: String?,
    val quality: String?,
    val mimeType: String?,
    val bitrate: Int?,
    val codec: String?
)
