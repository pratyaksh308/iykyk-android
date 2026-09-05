package com.pratyaksh.iykyk.video

import android.content.ContentResolver
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import androidx.annotation.RequiresApi
import kotlin.math.max
import kotlin.math.roundToInt

class VideoProcessor(
    private val contentResolver: ContentResolver,
    private val faceDetector: FaceDetector
) {
    companion object {
        private const val TAG = "VideoProcessor"
        private const val DEFAULT_INTERVAL_MS = 200L
        private const val FRAME_CHUNK_SIZE = 25
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
        } finally {
            retriever.release()
        }
    }

    suspend fun processVideo(
        uri: Uri,
        intervalMs: Long = DEFAULT_INTERVAL_MS
    ): List<FrameDetection> {
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
                    processUsingConsecutiveFrames(
                        retriever = retriever,
                        frameCount = frameCount,
                        frameRate = frameRate,
                        intervalMs = intervalMs
                    )
                } else {
                    processUsingTimestampFallback(
                        retriever = retriever,
                        durationMs = durationMs,
                        intervalMs = intervalMs
                    )
                }

            val totalTimeMs =
                (System.nanoTime() - totalStartTime) / 1_000_000

            Log.d(TAG, "========== PROCESSING TIMING ==========")
            Log.d(TAG, "Total processing time: ${totalTimeMs}ms")
            Log.d(TAG, "=======================================")

            return results
        } finally {
            retriever.release()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private suspend fun processUsingConsecutiveFrames(
        retriever: MediaMetadataRetriever,
        frameCount: Int,
        frameRate: Float,
        intervalMs: Long
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

        while (chunkStartFrame < frameCount) {
            val framesRemaining =
                frameCount - chunkStartFrame

            val framesToRetrieve =
                minOf(
                    FRAME_CHUNK_SIZE,
                    framesRemaining
                )

            val decodeStartTime = System.nanoTime()

            val frames =
                retriever.getFramesAtIndex(
                    chunkStartFrame,
                    framesToRetrieve
                )

            totalDecodeTimeNs +=
                System.nanoTime() - decodeStartTime

            try {
                frames.forEachIndexed { index, frame ->
                    val absoluteFrameIndex =
                        chunkStartFrame + index

                    if (
                        absoluteFrameIndex % sampleEveryFrames != 0
                    ) {
                        return@forEachIndexed
                    }

                    sampledFrameCount++

                    val timestampMs =
                        (
                                absoluteFrameIndex.toDouble() *
                                        1000.0 /
                                        frameRate
                                ).roundToInt().toLong()

                    val detectionStartTime =
                        System.nanoTime()

                    val faces =
                        faceDetector.detectFaces(frame)

                    totalDetectionTimeNs +=
                        System.nanoTime() - detectionStartTime

                    detections.add(
                        FrameDetection(
                            timestampMs = timestampMs,
                            faces = faces
                        )
                    )
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

        logTimingBreakdown(
            sampledFrameCount = sampledFrameCount,
            decodeTimeNs = totalDecodeTimeNs,
            detectionTimeNs = totalDetectionTimeNs
        )

        return detections
    }

    private suspend fun processUsingTimestampFallback(
        retriever: MediaMetadataRetriever,
        durationMs: Long,
        intervalMs: Long
    ): List<FrameDetection> {
        val detections = mutableListOf<FrameDetection>()

        var totalDecodeTimeNs = 0L
        var totalDetectionTimeNs = 0L
        var sampledFrameCount = 0

        var timestampMs = 0L

        while (timestampMs < durationMs) {
            val decodeStartTime =
                System.nanoTime()

            val frame =
                retriever.getFrameAtTime(
                    timestampMs * 1000,
                    MediaMetadataRetriever.OPTION_CLOSEST
                )

            totalDecodeTimeNs +=
                System.nanoTime() - decodeStartTime

            if (frame != null) {
                try {
                    sampledFrameCount++

                    val detectionStartTime =
                        System.nanoTime()

                    val faces =
                        faceDetector.detectFaces(frame)

                    totalDetectionTimeNs +=
                        System.nanoTime() - detectionStartTime

                    detections.add(
                        FrameDetection(
                            timestampMs = timestampMs,
                            faces = faces
                        )
                    )
                } finally {
                    if (!frame.isRecycled) {
                        frame.recycle()
                    }
                }
            }

            timestampMs += intervalMs
        }

        logTimingBreakdown(
            sampledFrameCount = sampledFrameCount,
            decodeTimeNs = totalDecodeTimeNs,
            detectionTimeNs = totalDetectionTimeNs
        )

        return detections
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

    fun extractFrame(
        uri: Uri,
        timestampMs: Long
    ): Bitmap? {
        val retriever =
            MediaMetadataRetriever()

        return try {
            setDataSource(
                retriever,
                uri
            )

            retriever.getFrameAtTime(
                timestampMs * 1000,
                MediaMetadataRetriever.OPTION_CLOSEST
            )
        } finally {
            retriever.release()
        }
    }

    fun close() {
        faceDetector.close()
    }
}