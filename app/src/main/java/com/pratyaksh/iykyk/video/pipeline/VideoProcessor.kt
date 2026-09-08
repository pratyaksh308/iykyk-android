package com.pratyaksh.iykyk.video.pipeline

import com.pratyaksh.iykyk.video.ml.FaceDetector
import com.pratyaksh.iykyk.video.model.FrameDetection

import android.content.ContentResolver
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.OpenableColumns
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

class VideoProcessor(
    private val contentResolver: ContentResolver,
    private val faceDetector: FaceDetector
) {
    companion object {
        private const val TAG = "VideoProcessor"
        private const val DEFAULT_INTERVAL_MS = 150L
        private const val FRAME_CHUNK_SIZE = 25
    }

    private val frameCache = java.util.concurrent.ConcurrentHashMap<Long, Bitmap>()

    fun clearCache() {
        val count = frameCache.size
        frameCache.values.forEach { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        frameCache.clear()
        Log.d(TAG, "Cleared $count cached frames from RAM")
    }

    fun getVideoName(uri: Uri): String? {
        contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    return cursor.getString(nameIndex)
                }
            }
        }
        return null
    }

    fun getVideoDuration(uri: Uri): Long {
        val retriever = MediaMetadataRetriever()

        return try {
            setDataSource(retriever, uri)

            retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_DURATION
            )?.toLong() ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "getVideoDuration failed", e)
            0L
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    suspend fun processVideo(
        uri: Uri,
        intervalMs: Long = DEFAULT_INTERVAL_MS,
        onProgress: (Float) -> Unit = {}
    ): List<FrameDetection> = withContext(Dispatchers.Default) {
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND)
        } catch (_: Exception) {}

        clearCache()

        require(intervalMs > 0) {
            "intervalMs must be greater than zero"
        }

        val totalStartTime = System.nanoTime()
        val retriever = MediaMetadataRetriever()

        try {
            setDataSource(retriever, uri)

            val durationMs =
                retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )?.toLong() ?: 0L

            val frameCount =
                retriever.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT
                )?.toIntOrNull() ?: 0

            val frameRate = getFrameRate(
                retriever = retriever,
                frameCount = frameCount,
                durationMs = durationMs
            )

            Log.d(
                TAG,
                "Video: duration=${durationMs}ms, frames=$frameCount, fps=$frameRate"
            )

            val results =
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                    frameCount > 0 &&
                    frameRate > 0f
                ) {
                    val consecutiveResults = try {
                        processUsingConsecutiveFrames(
                            retriever = retriever,
                            frameCount = frameCount,
                            frameRate = frameRate,
                            intervalMs = intervalMs,
                            onProgress = onProgress
                        )
                    } catch (e: Throwable) {
                        Log.w(TAG, "getFramesAtIndex failed, falling back to timestamp extraction", e)
                        null
                    }

                    if (!consecutiveResults.isNullOrEmpty()) {
                        consecutiveResults
                    } else {
                        Log.w(TAG, "Consecutive frame extraction produced no results, falling back to timestamp extraction")
                        processUsingTimestampFallback(
                            uri = uri,
                            durationMs = durationMs,
                            intervalMs = intervalMs,
                            onProgress = onProgress
                        )
                    }
                } else {
                    processUsingTimestampFallback(
                        uri = uri,
                        durationMs = durationMs,
                        intervalMs = intervalMs,
                        onProgress = onProgress
                    )
                }

            val totalTimeMs =
                (System.nanoTime() - totalStartTime) / 1_000_000

            Log.d(TAG, "========== PROCESSING TIMING ==========")
            Log.d(TAG, "Total processing time: ${totalTimeMs}ms")
            Log.d(TAG, "=======================================")

            return@withContext results
        } finally {
            retriever.release()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private suspend fun processUsingConsecutiveFrames(
        retriever: MediaMetadataRetriever,
        frameCount: Int,
        frameRate: Float,
        intervalMs: Long,
        onProgress: (Float) -> Unit
    ): List<FrameDetection> {
        val sampleEveryFrames =
            max(
                1,
                (frameRate * intervalMs / 1000f).roundToInt()
            )

        val detections = mutableListOf<FrameDetection>()

        var totalDecodeTimeNs = 0L
        var totalDetectionTimeNs = 0L
        var sampledFrameCount = 0

        var chunkStartFrame = 0
        val totalSampledEstimate = max(1, frameCount / sampleEveryFrames)

        while (chunkStartFrame < frameCount) {
            val framesRemaining =
                frameCount - chunkStartFrame

            val framesToRetrieve =
                minOf(
                    FRAME_CHUNK_SIZE,
                    framesRemaining
                )

            val decodeStartTime = System.nanoTime()

            val frames = try {
                retriever.getFramesAtIndex(
                    chunkStartFrame,
                    framesToRetrieve
                )
            } catch (e: Throwable) {
                Log.w(TAG, "getFramesAtIndex failed for chunk at $chunkStartFrame", e)
                emptyList()
            }

            if (frames.isEmpty()) {
                chunkStartFrame += framesToRetrieve
                continue
            }

            totalDecodeTimeNs +=
                System.nanoTime() - decodeStartTime

            try {
                data class SampledTask(val timestampMs: Long, val frame: Bitmap)
                val sampledTasks = mutableListOf<SampledTask>()

                frames.forEachIndexed { index, frame ->
                    val absoluteFrameIndex = chunkStartFrame + index
                    if (absoluteFrameIndex % sampleEveryFrames == 0) {
                        val timestampMs = (
                            absoluteFrameIndex.toDouble() *
                                1000.0 /
                                frameRate
                        ).roundToInt().toLong()
                        sampledTasks.add(SampledTask(timestampMs, frame))
                    }
                }

                if (sampledTasks.isNotEmpty()) {
                    val batchResults = coroutineScope {
                        sampledTasks.map { task ->
                            async(Dispatchers.Default) {
                                val detectionStartTime = System.nanoTime()
                                val processedFrame = scaleBitmapIfNeeded(task.frame)

                                val cacheCopy = if (processedFrame == task.frame) {
                                    task.frame.copy(task.frame.config ?: Bitmap.Config.ARGB_8888, false)
                                } else {
                                    processedFrame
                                }
                                frameCache[task.timestampMs] = cacheCopy

                                val faces = faceDetector.detectFaces(processedFrame)
                                val durationNs = System.nanoTime() - detectionStartTime
                                FrameDetection(task.timestampMs, faces) to durationNs
                            }
                        }.awaitAll()
                    }

                    for ((detection, durationNs) in batchResults) {
                        sampledFrameCount++
                        totalDetectionTimeNs += durationNs
                        detections.add(detection)
                        onProgress(sampledFrameCount.toFloat() / totalSampledEstimate.toFloat())
                    }
                }
            } finally {
                frames.forEach { bitmap ->
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                }
            }

            chunkStartFrame += framesToRetrieve
        }

        onProgress(1f)
        logTimingBreakdown(
            sampledFrameCount = sampledFrameCount,
            decodeTimeNs = totalDecodeTimeNs,
            detectionTimeNs = totalDetectionTimeNs
        )

        return detections
    }

    private suspend fun processUsingTimestampFallback(
        uri: Uri,
        durationMs: Long,
        intervalMs: Long,
        onProgress: (Float) -> Unit
    ): List<FrameDetection> {
        val retriever = MediaMetadataRetriever()
        return try {
            setDataSource(retriever, uri)

            val detections = mutableListOf<FrameDetection>()

            var totalDecodeTimeNs = 0L
            var totalDetectionTimeNs = 0L
            var sampledFrameCount = 0

            var timestampMs = 0L
            val totalSteps = max(1, (durationMs / intervalMs).toInt())

            val batchSize = 8
            while (timestampMs < durationMs) {
                val batchTimestamps = mutableListOf<Long>()
                while (batchTimestamps.size < batchSize && timestampMs < durationMs) {
                    batchTimestamps.add(timestampMs)
                    timestampMs += intervalMs
                }

                val decodeStartTime = System.nanoTime()
                val extractedFrames = mutableListOf<Pair<Long, Bitmap>>()
                for (ts in batchTimestamps) {
                    val frame = retriever.getFrameAtTime(
                        ts * 1000,
                        MediaMetadataRetriever.OPTION_CLOSEST
                    )
                    if (frame != null) {
                        extractedFrames.add(ts to frame)
                    }
                }
                totalDecodeTimeNs += System.nanoTime() - decodeStartTime

                if (extractedFrames.isNotEmpty()) {
                    val batchResults = coroutineScope {
                        extractedFrames.map { (ts, frame) ->
                            async(Dispatchers.Default) {
                                try {
                                    val detectionStartTime = System.nanoTime()
                                    val processedFrame = scaleBitmapIfNeeded(frame)

                                    val cacheCopy = if (processedFrame == frame) {
                                        frame.copy(frame.config ?: Bitmap.Config.ARGB_8888, false)
                                    } else {
                                        processedFrame
                                    }
                                    frameCache[ts] = cacheCopy

                                    val faces = faceDetector.detectFaces(processedFrame)
                                    val durationNs = System.nanoTime() - detectionStartTime
                                    FrameDetection(ts, faces) to durationNs
                                } finally {
                                    if (!frame.isRecycled) {
                                        frame.recycle()
                                    }
                                }
                            }
                        }.awaitAll()
                    }

                    for ((detection, durationNs) in batchResults) {
                        sampledFrameCount++
                        totalDetectionTimeNs += durationNs
                        detections.add(detection)
                        onProgress(sampledFrameCount.toFloat() / totalSteps.toFloat())
                    }
                }
            }

            onProgress(1f)
            logTimingBreakdown(
                sampledFrameCount = sampledFrameCount,
                decodeTimeNs = totalDecodeTimeNs,
                detectionTimeNs = totalDetectionTimeNs
            )

            detections
        } catch (e: Exception) {
            Log.e(TAG, "processUsingTimestampFallback failed", e)
            emptyList()
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    private fun logTimingBreakdown(
        sampledFrameCount: Int,
        decodeTimeNs: Long,
        detectionTimeNs: Long
    ) {
        val decodeTimeMs =
            decodeTimeNs / 1_000_000

        val detectionTimeMs =
            detectionTimeNs / 1_000_000

        val averageDecodeMs =
            if (sampledFrameCount > 0) {
                decodeTimeMs.toDouble() / sampledFrameCount
            } else {
                0.0
            }

        val averageDetectionMs =
            if (sampledFrameCount > 0) {
                detectionTimeMs.toDouble() / sampledFrameCount
            } else {
                0.0
            }

        Log.d(TAG, "========== TIMING BREAKDOWN ==========")
        Log.d(TAG, "Sampled frames: $sampledFrameCount")
        Log.d(TAG, "Frame retrieval time: ${decodeTimeMs}ms")
        Log.d(TAG, "ML Kit detection time: ${detectionTimeMs}ms")
        Log.d(
            TAG,
            "Average retrieval/sample: ${"%.2f".format(averageDecodeMs)}ms"
        )
        Log.d(
            TAG,
            "Average ML Kit/sample: ${"%.2f".format(averageDetectionMs)}ms"
        )
        Log.d(TAG, "======================================")
    }

    private fun getFrameRate(
        retriever: MediaMetadataRetriever,
        frameCount: Int,
        durationMs: Long
    ): Float {
        val captureFrameRate =
            retriever.extractMetadata(
                MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE
            )?.toFloatOrNull()

        if (
            captureFrameRate != null &&
            captureFrameRate > 0f
        ) {
            return captureFrameRate
        }

        if (
            frameCount > 0 &&
            durationMs > 0
        ) {
            return frameCount * 1000f / durationMs
        }

        return 0f
    }

    private fun setDataSource(
        retriever: MediaMetadataRetriever,
        uri: Uri
    ) {
        contentResolver
            .openFileDescriptor(uri, "r")
            ?.use { pfd ->
                retriever.setDataSource(
                    pfd.fileDescriptor
                )
            }
            ?: throw IllegalArgumentException(
                "Unable to open video URI: $uri"
            )
    }

    fun batchExtractFrames(
        uri: Uri,
        timestampsMs: List<Long>
    ): Map<Long, Bitmap> {
        if (timestampsMs.isEmpty()) return emptyMap()

        val result = mutableMapOf<Long, Bitmap>()
        val missingTimestamps = mutableListOf<Long>()

        for (ts in timestampsMs.distinct()) {
            val cached = extractFrame(uri, ts)
            if (cached != null) {
                result[ts] = cached
            } else {
                missingTimestamps.add(ts)
            }
        }

        if (missingTimestamps.isEmpty()) return result

        val retriever = MediaMetadataRetriever()
        try {
            setDataSource(retriever, uri)
            for (ts in missingTimestamps) {
                val frame = retriever.getFrameAtTime(
                    ts * 1000,
                    MediaMetadataRetriever.OPTION_CLOSEST
                )
                if (frame != null) {
                    val scaled = scaleBitmapIfNeeded(frame)
                    result[ts] = scaled
                    val cacheCopy = if (scaled == frame) {
                        frame.copy(frame.config ?: Bitmap.Config.ARGB_8888, false)
                    } else {
                        scaled
                    }
                    frameCache[ts] = cacheCopy
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "batchExtractFrames failed", e)
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }

        return result
    }

    fun extractFrame(
        uri: Uri,
        timestampMs: Long
    ): Bitmap? {
        val cached = frameCache[timestampMs]
        if (cached != null && !cached.isRecycled) {
            return cached.copy(cached.config ?: Bitmap.Config.ARGB_8888, false)
        }

        val nearest = frameCache.entries.minByOrNull { kotlin.math.abs(it.key - timestampMs) }
        if (nearest != null && kotlin.math.abs(nearest.key - timestampMs) <= 50L && !nearest.value.isRecycled) {
            return nearest.value.copy(nearest.value.config ?: Bitmap.Config.ARGB_8888, false)
        }

        val retriever = MediaMetadataRetriever()

        return try {
            setDataSource(
                retriever,
                uri
            )

            val frame = retriever.getFrameAtTime(
                timestampMs * 1000,
                MediaMetadataRetriever.OPTION_CLOSEST
            )
            val scaled = frame?.let { scaleBitmapIfNeeded(it) }
            if (scaled != null) {
                val cacheCopy = if (scaled == frame) {
                    frame.copy(frame.config ?: Bitmap.Config.ARGB_8888, false)
                } else {
                    scaled
                }
                frameCache[timestampMs] = cacheCopy
            }
            scaled
        } catch (e: Exception) {
            Log.e(TAG, "extractFrame failed for timestamp $timestampMs", e)
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    fun close() {
        clearCache()
        faceDetector.close()
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int = 640): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }
        val scale = maxDimension.toFloat() / max(width, height).toFloat()
        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()
        val scaled = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        if (scaled != bitmap && !bitmap.isRecycled) {
            bitmap.recycle()
        }
        return scaled
    }
}