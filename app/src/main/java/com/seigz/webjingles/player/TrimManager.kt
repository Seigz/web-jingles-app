package com.seigz.webjingles.player

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.ByteBuffer
import java.nio.channels.Channels

class TrimManager(private val context: Context) {

    data class TrimResult(
        val success: Boolean,
        val outputPath: String? = null,
        val error: String? = null
    )

    suspend fun trimAudio(
        inputUri: Uri,
        startMs: Long,
        endMs: Long,
        outputFile: File
    ): TrimResult = withContext(Dispatchers.IO) {
        try {
            val extractor = MediaExtractor()

            val fd = context.contentResolver.openFileDescriptor(inputUri, "r")
            if (fd != null) {
                extractor.setDataSource(fd.fileDescriptor)
            } else {
                extractor.setDataSource(context, inputUri, null)
            }

            val audioTrackIndex = findAudioTrack(extractor)
            if (audioTrackIndex < 0) {
                fd?.close()
                return@withContext TrimResult(false, error = "No audio track found")
            }

            extractor.selectTrack(audioTrackIndex)
            val format = extractor.getTrackFormat(audioTrackIndex)

            val muxer = MediaMuxer(
                outputFile.absolutePath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )
            val muxerTrackIndex = muxer.addTrack(format)
            muxer.start()

            val startUs = startMs * 1000L
            val endUs = endMs * 1000L
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val bufferSize = format.getInteger(
                MediaFormat.KEY_MAX_INPUT_SIZE,
                1024 * 1024
            )
            val buffer = ByteBuffer.allocate(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                val sampleTime = extractor.sampleTime
                if (sampleTime > endUs) break

                if (sampleTime >= startUs) {
                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = sampleTime - startUs
                    bufferInfo.flags = extractor.sampleFlags
                    muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                }

                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            extractor.release()
            fd?.close()

            TrimResult(true, outputPath = outputFile.absolutePath)
        } catch (e: Exception) {
            TrimResult(false, error = e.message ?: "Trim failed")
        }
    }

    suspend fun trimFromCacheUrl(
        url: String,
        startMs: Long,
        endMs: Long,
        outputFile: File
    ): TrimResult = withContext(Dispatchers.IO) {
        try {
            // Download remote URL to a local temp file first
            // (streaming URLs don't support seeking for MediaExtractor)
            val dataSource = if (url.startsWith("http")) {
                val tempFile = File(context.cacheDir, "temp_audio_${System.currentTimeMillis()}.tmp")
                try {
                    downloadToFile(url, tempFile)
                    tempFile.absolutePath
                } catch (e: Exception) {
                    return@withContext TrimResult(false, error = "Failed to download audio: ${e.message}")
                }
            } else {
                url
            }

            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(dataSource)
            } catch (e: Exception) {
                if (url.startsWith("http")) File(dataSource).delete()
                return@withContext TrimResult(false, error = "Failed to load audio source: ${e.message}")
            }

            val audioTrackIndex = findAudioTrack(extractor)
            if (audioTrackIndex < 0) {
                extractor.release()
                if (url.startsWith("http")) File(dataSource).delete()
                return@withContext TrimResult(false, error = "No audio track found")
            }

            extractor.selectTrack(audioTrackIndex)
            val format = extractor.getTrackFormat(audioTrackIndex)

            val muxer = MediaMuxer(
                outputFile.absolutePath,
                MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            )
            val muxerTrackIndex = muxer.addTrack(format)
            muxer.start()

            val startUs = startMs * 1000L
            val endUs = endMs * 1000L
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val bufferSize = format.getInteger(
                MediaFormat.KEY_MAX_INPUT_SIZE,
                1024 * 1024
            )
            val buffer = ByteBuffer.allocate(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            var sampleCount = 0
            while (true) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) break

                val sampleTime = extractor.sampleTime
                if (sampleTime > endUs) break

                if (sampleTime >= startUs) {
                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = sampleTime - startUs
                    bufferInfo.flags = extractor.sampleFlags
                    muxer.writeSampleData(muxerTrackIndex, buffer, bufferInfo)
                    sampleCount++
                }

                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            extractor.release()

            // Clean up temp file
            if (url.startsWith("http")) File(dataSource).delete()

            if (sampleCount == 0) {
                outputFile.delete()
                return@withContext TrimResult(false, error = "No audio samples found in the specified range")
            }

            TrimResult(true, outputPath = outputFile.absolutePath)
        } catch (e: Exception) {
            if (outputFile.exists()) outputFile.delete()
            TrimResult(false, error = "Trim failed: ${e.message}")
        }
    }

    private fun downloadToFile(url: String, file: File) {
        val connection = URL(url).openConnection()
        connection.connect()
        connection.getInputStream().use { input ->
            FileOutputStream(file).use { output ->
                val inCh = Channels.newChannel(input)
                val outCh = output.channel
                outCh.transferFrom(inCh, 0, Long.MAX_VALUE)
                inCh.close()
                outCh.close()
            }
        }
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
