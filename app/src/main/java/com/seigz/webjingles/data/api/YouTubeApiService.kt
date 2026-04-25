package com.seigz.webjingles.data.api

import com.seigz.webjingles.data.model.YouTubeSearchResponse
import com.seigz.webjingles.data.model.YouTubeVideoDetailsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApiService {

    @GET("search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("type") type: String = "video",
        @Query("maxResults") maxResults: Int = 25,
        @Query("pageToken") pageToken: String? = null,
        @Query("key") apiKey: String
    ): YouTubeSearchResponse

    @GET("videos")
    suspend fun getVideoDetails(
        @Query("part") part: String = "contentDetails,snippet",
        @Query("id") id: String,
        @Query("key") apiKey: String
    ): YouTubeVideoDetailsResponse
}
