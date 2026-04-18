package com.seigz.webjingles.player

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Decodes audio files, applies effects (trim, fade in/out, volume),
 * and writes output as WAV.
 */
class AudioProcessor {

    companion object {
        private const val TAG = "AudioProcessor"
    }

    data class Effects(
        val trimStartMs: Long = 0L,
        val trimEndMs: Long = 0L,
        val fadeIn: Boolean = false,
        val fadeOut: Boolean = false,
        val fadeInDurationSec: Float = 2f,
        val fadeOutDurationSec: Float = 2f,
        val volume: Float = 1.0f
    )

    data class ProcessResult(
        val success: Boolean,
        val outputPath: String? = null,
        val error: String? = null
    )

    suspend fun downloadAndProcess(
        url: String,
        outputFile: File,
        effects: Effects,
        cacheDir: File,
        client: OkHttpClient? = null,
        onProgress: (Float) -> Unit = {}
    ): ProcessResult = withContext(Dispatchers.IO) {
        var tempFile: File? = null
        try {
            // Step 1: Download to temp file (0% – 50%)
            onProgress(0.01f)
            tempFile = File(cacheDir, "audio_proc_${System.currentTimeMillis()}.tmp")
            Log.d(TAG, "Downloading audio to temp file...")
            downloadToFile(url, tempFile, client) { dlProgress ->
                onProgress(dlProgress * 0.5f)  // 0 – 0.5
            }
            Log.d(TAG, "Downloaded ${tempFile.length()} bytes")

            if (tempFile.length() == 0L) {
                return@withContext ProcessResult(false, error = "Downloaded file is empty")
            }

            // Step 2: Decode to PCM and apply effects (50% – 100%)
            onProgress(0.5f)
            Log.d(TAG, "Decoding and processing audio...")
            val result = decodeAndProcess(tempFile, outputFile, effects) { procProgress ->
                onProgress(0.5f + procProgress * 0.5f)  // 0.5 – 1.0
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "Processing failed: ${e.message}", e)
            if (outputFile.exists()) outputFile.delete()
            ProcessResult(false, error = "Processing failed: ${e.message}")
        } finally {
            tempFile?.delete()
        }
    }

    private fun downloadToFile(url: String, file: File, client: OkHttpClient?, onProgress: (Float) -> Unit) {
        val httpClient = client ?: OkHttpClient()

        // First, get content length with a HEAD-like range request
        val headRequest = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            .header("Range", "bytes=0-0")
            .build()
        val headResponse = httpClient.newCall(headRequest).execute()
        val contentRange = headResponse.header("Content-Range")
        headResponse.close()

        val totalBytes = contentRange?.substringAfter("/")?.toLongOrNull() ?: -1L

        if (totalBytes > 0) {
            // Chunked range download to bypass YouTube throttling
            val chunkSize = 2 * 1024 * 1024L // 2MB chunks
            var downloaded = 0L

            FileOutputStream(file).use { output ->
                while (downloaded < totalBytes) {
                    val rangeEnd = minOf(downloaded + chunkSize - 1, totalBytes - 1)
                    val chunkRequest = Request.Builder()
                        .url(url)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                        .header("Range", "bytes=$downloaded-$rangeEnd")
                        .build()
                    val chunkResponse = httpClient.newCall(chunkRequest).execute()
                    if (!chunkResponse.isSuccessful && chunkResponse.code != 206) {
                        chunkResponse.close()
                        throw java.io.IOException("Chunk download failed: ${chunkResponse.code}")
                    }
                    val chunkBody = chunkResponse.body
                    if (chunkBody != null) {
                        chunkBody.byteStream().use { input ->
                            val buffer = ByteArray(131072)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                downloaded += bytesRead
                                onProgress((downloaded.toFloat() / totalBytes).coerceIn(0f, 1f))
                            }
                        }
                    }
                    chunkResponse.close()
                }
                output.flush()
            }
            Log.d(TAG, "Chunked download complete: $downloaded bytes")
        } else {
            // Fallback: single request download
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .build()
            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) throw java.io.IOException("Download failed: ${response.code}")
            val body = response.body ?: throw java.io.IOException("Empty response body")
            val fallbackTotal = body.contentLength()
            body.byteStream().use { input ->
                FileOutputStream(file).use { output ->
                    val buffer = ByteArray(131072)
                    var bytesRead: Int
                    var totalRead = 0L
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (fallbackTotal > 0) {
                            onProgress((totalRead.toFloat() / fallbackTotal).coerceIn(0f, 1f))
                        }
                    }
                    output.flush()
                }
            }
        }
    }

    private fun decodeAndProcess(
        inputFile: File,
        outputFile: File,
        effects: Effects,
        onProgress: (Float) -> Unit
    ): ProcessResult {
        val extractor = MediaExtractor()
        extractor.setDataSource(inputFile.absolutePath)

        val audioTrackIndex = findAudioTrack(extractor)
        if (audioTrackIndex < 0) {
            extractor.release()
            return ProcessResult(false, error = "No audio track found in source")
        }

        extractor.selectTrack(audioTrackIndex)
        val format = extractor.getTrackFormat(audioTrackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: "audio/opus"
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
            format.getLong(MediaFormat.KEY_DURATION)
        } else 0L

        Log.d(TAG, "Audio format: mime=$mime, sampleRate=$sampleRate, channels=$channelCount, durationUs=$durationUs")

        // Setup decoder
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()

        val trimStartUs = effects.trimStartMs * 1000L
        val trimEndUs = if (effects.trimEndMs > 0) effects.trimEndMs * 1000L else durationUs

        // Seek to trim start
        if (trimStartUs > 0) {
            extractor.seekTo(trimStartUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
        }

        // Collect all PCM samples
        val pcmChunks = mutableListOf<ByteArray>()
        val bufferInfo = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        val timeoutUs = 10000L

        while (!outputDone) {
            // Feed input
            if (!inputDone) {
                val inputIndex = codec.dequeueInputBuffer(timeoutUs)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex) ?: continue
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        val sampleTime = extractor.sampleTime
                        // Stop feeding if past trim end
                        if (trimEndUs > 0 && sampleTime > trimEndUs) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(inputIndex, 0, sampleSize, sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
            }

            // Read output
            val outputIndex = codec.dequeueOutputBuffer(bufferInfo, timeoutUs)
            if (outputIndex >= 0) {
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    outputDone = true
                }

                val outputBuffer = codec.getOutputBuffer(outputIndex)
                if (outputBuffer != null && bufferInfo.size > 0) {
                    // Only keep samples within trim range
                    val presentationTimeUs = bufferInfo.presentationTimeUs
                    if (presentationTimeUs >= trimStartUs && (trimEndUs <= 0 || presentationTimeUs <= trimEndUs)) {
                        val chunk = ByteArray(bufferInfo.size)
                        outputBuffer.get(chunk)
                        pcmChunks.add(chunk)
                    }
                }

                codec.releaseOutputBuffer(outputIndex, false)
            } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // Format changed, could update sampleRate/channelCount if needed
                Log.d(TAG, "Output format changed: ${codec.outputFormat}")
            }
        }

        codec.stop()
        codec.release()
        extractor.release()

        onProgress(0.5f)

        if (pcmChunks.isEmpty()) {
            return ProcessResult(false, error = "No audio data decoded")
        }

        // Merge chunks into single PCM buffer
        val totalSize = pcmChunks.sumOf { it.size }
        val pcmData = ByteArray(totalSize)
        var offset = 0
        for (chunk in pcmChunks) {
            System.arraycopy(chunk, 0, pcmData, offset, chunk.size)
            offset += chunk.size
        }
        pcmChunks.clear()

        Log.d(TAG, "Decoded ${pcmData.size} bytes of PCM data")

        // Apply effects to PCM (16-bit signed little-endian)
        onProgress(0.7f)
        applyEffects(pcmData, sampleRate, channelCount, effects)

        onProgress(0.85f)

        // Write WAV file
        writeWav(outputFile, pcmData, sampleRate, channelCount, 16)

        onProgress(1.0f)
        Log.d(TAG, "Written WAV file: ${outputFile.absolutePath} (${outputFile.length()} bytes)")

        return ProcessResult(true, outputPath = outputFile.absolutePath)
    }

    private fun applyEffects(pcmData: ByteArray, sampleRate: Int, channelCount: Int, effects: Effects) {
        val bytesPerSample = 2 // 16-bit
        val frameSize = bytesPerSample * channelCount
        val totalFrames = pcmData.size / frameSize
        val totalDurationSec = totalFrames.toFloat() / sampleRate

        val buffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until totalFrames) {
            val timeSec = i.toFloat() / sampleRate
            var gain = effects.volume

            // Fade in
            if (effects.fadeIn && timeSec < effects.fadeInDurationSec) {
                gain *= (timeSec / effects.fadeInDurationSec).coerceIn(0f, 1f)
            }

            // Fade out
            if (effects.fadeOut) {
                val fadeOutStart = totalDurationSec - effects.fadeOutDurationSec
                if (timeSec > fadeOutStart) {
                    val fadeProgress = ((totalDurationSec - timeSec) / effects.fadeOutDurationSec).coerceIn(0f, 1f)
                    gain *= fadeProgress
                }
            }

            // Apply gain to each channel in this frame
            for (ch in 0 until channelCount) {
                val byteIndex = (i * frameSize) + (ch * bytesPerSample)
                val sample = buffer.getShort(byteIndex).toFloat()
                val processed = (sample * gain).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                buffer.putShort(byteIndex, processed.toShort())
            }
        }
    }

    private fun writeWav(file: File, pcmData: ByteArray, sampleRate: Int, channelCount: Int, bitsPerSample: Int) {
        val byteRate = sampleRate * channelCount * (bitsPerSample / 8)
        val blockAlign = channelCount * (bitsPerSample / 8)
        val dataSize = pcmData.size
        val chunkSize = 36 + dataSize

        RandomAccessFile(file, "rw").use { raf ->
            // RIFF header
            raf.writeBytes("RIFF")
            raf.writeIntLE(chunkSize)
            raf.writeBytes("WAVE")

            // fmt sub-chunk
            raf.writeBytes("fmt ")
            raf.writeIntLE(16) // sub-chunk size
            raf.writeShortLE(1) // PCM format
            raf.writeShortLE(channelCount)
            raf.writeIntLE(sampleRate)
            raf.writeIntLE(byteRate)
            raf.writeShortLE(blockAlign)
            raf.writeShortLE(bitsPerSample)

            // data sub-chunk
            raf.writeBytes("data")
            raf.writeIntLE(dataSize)
            raf.write(pcmData)
        }
    }

    private fun RandomAccessFile.writeIntLE(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
        write((value shr 16) and 0xFF)
        write((value shr 24) and 0xFF)
    }

    private fun RandomAccessFile.writeShortLE(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
    }

    private fun findAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) return i
        }
        return -1
    }
}
