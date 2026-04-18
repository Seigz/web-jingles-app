package com.seigz.webjingles.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.util.concurrent.TimeUnit

data class ResolvedStream(
    val audioUrl: String,
    val mimeType: String?,
    val quality: String?,
    val bitrate: Int?
)

object StreamResolver {

    private const val TAG = "StreamResolver"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    @Volatile
    private var initialized = false

    private fun ensureInitialized() {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    Log.d(TAG, "Initializing NewPipeExtractor...")
                    NewPipe.init(NewPipeDownloaderImpl(httpClient))
                    initialized = true
                    Log.d(TAG, "NewPipeExtractor initialized")
                }
            }
        }
    }

    suspend fun resolveAudioStream(videoId: String): Result<ResolvedStream> =
        withContext(Dispatchers.IO) {
            try {
                ensureInitialized()

                val url = "https://www.youtube.com/watch?v=$videoId"
                Log.d(TAG, "Extracting streams for: $url")

                val info = StreamInfo.getInfo(ServiceList.YouTube, url)
                val audioStreams: List<AudioStream> = info.audioStreams ?: emptyList()

                Log.d(TAG, "Found ${audioStreams.size} audio streams")

                if (audioStreams.isEmpty()) {
                    return@withContext Result.failure(
                        Exception("No audio streams found for video $videoId")
                    )
                }

                // Pick the best audio stream (highest bitrate)
                val best = audioStreams
                    .filter { it.content != null && it.content.isNotBlank() }
                    .maxByOrNull { it.averageBitrate }

                if (best == null || best.content.isNullOrBlank()) {
                    return@withContext Result.failure(
                        Exception("No playable audio stream URL found")
                    )
                }

                val mimeType = best.format?.mimeType
                Log.d(TAG, "Selected stream: bitrate=${best.averageBitrate}, mime=$mimeType, url=${best.content.take(80)}...")

                Result.success(
                    ResolvedStream(
                        audioUrl = best.content,
                        mimeType = mimeType,
                        quality = "${best.averageBitrate}kbps",
                        bitrate = best.averageBitrate
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "NewPipeExtractor failed: ${e.javaClass.simpleName}: ${e.message}")
                Result.failure(Exception("Stream extraction failed: ${e.message}"))
            }
        }
}
