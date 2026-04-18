package com.seigz.webjingles.download

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import com.seigz.webjingles.data.model.DownloadState
import com.seigz.webjingles.data.preferences.AppPreferences
import com.seigz.webjingles.player.AudioProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class DownloadManager(
    private val context: Context,
    private val preferences: AppPreferences
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(5, 30, TimeUnit.SECONDS))
        .build()

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    fun getDownloadState(trackId: String): DownloadState {
        return _downloadStates.value[trackId] ?: DownloadState.Idle
    }

    companion object {
        private const val TAG = "DownloadManager"
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        private const val CHUNK_SIZE = 2L * 1024 * 1024 // 2MB
    }

    suspend fun downloadAudio(
        trackId: String,
        url: String,
        fileName: String,
        format: String? = null
    ) = withContext(Dispatchers.IO) {
        try {
            val preferredFormat = format ?: preferences.preferredFormat.first()
            val ext = if (preferredFormat.equals("WAV", ignoreCase = true)) "wav" else "mp3"
            val sanitizedName = sanitizeFileName(fileName)
            val fullFileName = "$sanitizedName.$ext"

            updateState(trackId, DownloadState.Downloading(0f, fullFileName))

            val folderUri = preferences.downloadFolderUri.first()

            if (folderUri != null) {
                val outputStream = prepareSafOutputStream(folderUri, fullFileName)
                if (outputStream == null) {
                    updateState(trackId, DownloadState.Failed("Could not create file in selected folder"))
                    return@withContext
                }
                chunkedDownloadToStream(trackId, url, fullFileName, outputStream)
            } else {
                val outputStream = prepareDefaultOutputStream(fullFileName)
                if (outputStream == null) {
                    updateState(trackId, DownloadState.Failed("File already exists: $fullFileName"))
                    return@withContext
                }
                chunkedDownloadToStream(trackId, url, fullFileName, outputStream)
            }
        } catch (e: Exception) {
            updateState(trackId, DownloadState.Failed(e.message ?: "Unknown error"))
        }
    }

    private fun prepareSafOutputStream(folderUriStr: String, fileName: String): java.io.OutputStream? {
        val folderUri = Uri.parse(folderUriStr)
        val folder = DocumentFile.fromTreeUri(context, folderUri)
        val existing = folder?.findFile(fileName)
        if (existing != null && existing.exists()) return null
        val mimeType = if (fileName.endsWith(".wav")) "audio/wav" else "audio/mpeg"
        val newFile = folder?.createFile(mimeType, fileName) ?: return null
        return context.contentResolver.openOutputStream(newFile.uri)
    }

    private fun prepareDefaultOutputStream(fileName: String): java.io.OutputStream? {
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val dir = File(musicDir, "WebJingles")
        if (!dir.exists()) dir.mkdirs()
        val targetFile = File(dir, fileName)
        if (targetFile.exists()) return null
        return FileOutputStream(targetFile)
    }

    private suspend fun chunkedDownloadToStream(
        trackId: String,
        url: String,
        fileName: String,
        outputStream: java.io.OutputStream
    ) {
        outputStream.use { out ->
            // Get content length via range probe
            val headRequest = Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Range", "bytes=0-0")
                .build()
            val headResponse = client.newCall(headRequest).execute()
            val contentRange = headResponse.header("Content-Range")
            headResponse.close()
            val totalBytes = contentRange?.substringAfter("/")?.toLongOrNull() ?: -1L

            if (totalBytes > 0) {
                var downloaded = 0L
                while (downloaded < totalBytes) {
                    val rangeEnd = minOf(downloaded + CHUNK_SIZE - 1, totalBytes - 1)
                    val chunkRequest = Request.Builder()
                        .url(url)
                        .header("User-Agent", USER_AGENT)
                        .header("Range", "bytes=$downloaded-$rangeEnd")
                        .build()
                    val chunkResponse = client.newCall(chunkRequest).execute()
                    if (!chunkResponse.isSuccessful && chunkResponse.code != 206) {
                        chunkResponse.close()
                        updateState(trackId, DownloadState.Failed("Download failed: ${chunkResponse.code}"))
                        return
                    }
                    chunkResponse.body?.byteStream()?.use { input ->
                        val buffer = ByteArray(131072)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            out.write(buffer, 0, bytesRead)
                            downloaded += bytesRead
                            updateState(trackId, DownloadState.Downloading(
                                (downloaded.toFloat() / totalBytes).coerceIn(0f, 1f), fileName
                            ))
                        }
                    }
                    chunkResponse.close()
                }
            } else {
                // Fallback: single request
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    updateState(trackId, DownloadState.Failed("Download failed: ${response.code}"))
                    return
                }
                val body = response.body ?: run {
                    updateState(trackId, DownloadState.Failed("Empty response body"))
                    return
                }
                val fallbackTotal = body.contentLength()
                body.byteStream().use { input ->
                    val buffer = ByteArray(131072)
                    var bytesRead: Int
                    var totalRead = 0L
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        out.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        val progress = if (fallbackTotal > 0) {
                            (totalRead.toFloat() / fallbackTotal).coerceIn(0f, 1f)
                        } else -1f
                        updateState(trackId, DownloadState.Downloading(progress, fileName))
                    }
                }
            }
        }
        updateState(trackId, DownloadState.Completed(fileName, fileName))
    }

    suspend fun downloadAudioWithEffects(
        trackId: String,
        url: String,
        fileName: String,
        effects: AudioProcessor.Effects
    ) = withContext(Dispatchers.IO) {
        try {
            val sanitizedName = sanitizeFileName(fileName)
            val fullFileName = "$sanitizedName.wav"

            updateState(trackId, DownloadState.Downloading(0.05f, fullFileName))

            // Determine output file location
            val folderUri = preferences.downloadFolderUri.first()
            val outputFile: File
            val useSaf = folderUri != null

            if (useSaf) {
                // For SAF, write to cache first then copy
                outputFile = File(context.cacheDir, "proc_$fullFileName")
            } else {
                val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                val webJinglesDir = File(musicDir, "WebJingles")
                if (!webJinglesDir.exists()) webJinglesDir.mkdirs()
                outputFile = File(webJinglesDir, fullFileName)

                if (outputFile.exists()) {
                    updateState(trackId, DownloadState.Failed("File already exists: $fullFileName"))
                    return@withContext
                }
            }

            val processor = AudioProcessor()
            val result = processor.downloadAndProcess(
                url = url,
                outputFile = outputFile,
                effects = effects,
                cacheDir = context.cacheDir,
                client = client,
                onProgress = { progress ->
                    updateState(trackId, DownloadState.Downloading(progress * 0.9f + 0.05f, fullFileName))
                }
            )

            if (!result.success) {
                updateState(trackId, DownloadState.Failed(result.error ?: "Processing failed"))
                return@withContext
            }

            // If SAF, copy processed file to SAF location
            if (useSaf && folderUri != null) {
                val uri = Uri.parse(folderUri)
                val folder = DocumentFile.fromTreeUri(context, uri)
                val existing = folder?.findFile(fullFileName)
                if (existing != null && existing.exists()) {
                    outputFile.delete()
                    updateState(trackId, DownloadState.Failed("File already exists: $fullFileName"))
                    return@withContext
                }
                val newFile = folder?.createFile("audio/wav", fullFileName)
                if (newFile == null) {
                    outputFile.delete()
                    updateState(trackId, DownloadState.Failed("Could not create file in selected folder"))
                    return@withContext
                }
                context.contentResolver.openOutputStream(newFile.uri)?.use { out ->
                    outputFile.inputStream().use { inp -> inp.copyTo(out) }
                }
                outputFile.delete()
            }

            updateState(trackId, DownloadState.Completed(fullFileName, fullFileName))
        } catch (e: Exception) {
            updateState(trackId, DownloadState.Failed(e.message ?: "Unknown error"))
        }
    }

    fun updateDownloadState(trackId: String, state: DownloadState) {
        updateState(trackId, state)
    }

    private fun updateState(trackId: String, state: DownloadState) {
        _downloadStates.value = _downloadStates.value.toMutableMap().apply {
            put(trackId, state)
        }
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9._\\- ]"), "")
            .replace(Regex("\\s+"), "_")
            .take(100)
    }
}
