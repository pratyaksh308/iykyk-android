package com.pratyaksh.iykyk.video.pipeline

import com.pratyaksh.iykyk.video.ml.FaceEmbedder
import com.pratyaksh.iykyk.video.model.AppearanceSegment
import com.pratyaksh.iykyk.video.model.DetectedFace
import com.pratyaksh.iykyk.video.model.FaceObservation
import com.pratyaksh.iykyk.video.model.FrameDetection

import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class AppearanceSegmenter(
    private val videoProcessor: VideoProcessor,
    private val faceEmbedder: FaceEmbedder
) {
    companion object {
        private const val TAG = "AppearanceSegmenter"
        private const val MAX_TRACK_GAP_MS = 800L
        private const val MAX_CENTER_DISTANCE_PX = 300f
        private const val MIN_CENTER_DISTANCE_PX = 50f
        private const val MAX_SIZE_CHANGE_RATIO = 2.5f
        private const val MIN_IOU_FOR_MATCH = 0.01f
        private const val EMBEDDING_SIM_THRESHOLD = 0.45f
        private const val VELOCITY_SMOOTHING = 0.65f
    }

    private data class Track(
        val id: Int,
        val observations: MutableList<FaceObservation>,
        var lastTimestampMs: Long,
        var velocityXPerMs: Float = 0f,
        var velocityYPerMs: Float = 0f,
        var referenceEmbedding: FloatArray? = null,
        var referenceQualityArea: Int = 0
    ) {
        val lastFace: DetectedFace
            get() = observations.last().face
    }

    private data class Candidate(
        val track: Track,
        val faceIndex: Int,
        val score: Float
    )

    fun segment(
        uri: Uri,
        frameDetections: List<FrameDetection>,
        onProgress: (Float) -> Unit = {}
    ): List<AppearanceSegment> {
        if (frameDetections.isEmpty()) return emptyList()

        val activeTracks = mutableListOf<Track>()
        val completedTracks = mutableListOf<Track>()
        var nextTrackId = 1
        var totalDetectedFaces = 0
        var maxSimultaneousTracks = 0

        for (i in frameDetections.indices) {
            val frame = frameDetections[i]
            onProgress(i.toFloat() / max(1, frameDetections.size).toFloat())
            val timestampMs = frame.timestampMs

            val faces = frame.faces.filter { face ->
                val aspectRatio = face.width.toFloat() / face.height.toFloat()
                aspectRatio > 0.70f
            }

            totalDetectedFaces += faces.size

            val expiredTracks = activeTracks.filter { timestampMs - it.lastTimestampMs > MAX_TRACK_GAP_MS }
            completedTracks += expiredTracks
            activeTracks.removeAll(expiredTracks)

            if (faces.isEmpty()) continue

            val candidates = mutableListOf<Candidate>()
            var frameBitmap: Bitmap? = null

            try {
                val faceEmbeddingsCache = mutableMapOf<Int, FloatArray?>()

                for (track in activeTracks) {
                    val gapMs = timestampMs - track.lastTimestampMs
                    if (gapMs <= 0L || gapMs > MAX_TRACK_GAP_MS) continue

                    val lastFace = track.lastFace
                    val predictedCenterX = lastFace.centerX + track.velocityXPerMs * gapMs.toFloat()
                    val predictedCenterY = lastFace.centerY + track.velocityYPerMs * gapMs.toFloat()

                    for (faceIndex in faces.indices) {
                        val face = faces[faceIndex]
                        val centerDistance = distance(predictedCenterX, predictedCenterY, face.centerX, face.centerY)
                        val iou = calculateIoU(lastFace.boundingBox, face.boundingBox)
                        val sizeRatio = calculateSizeRatio(lastFace, face)

                        if (sizeRatio > MAX_SIZE_CHANGE_RATIO) continue

                        val faceDiagonal = sqrt((face.width.toFloat() * face.width.toFloat() + face.height.toFloat() * face.height.toFloat()).toDouble()).toFloat()
                        val adaptiveDistance = max(MIN_CENTER_DISTANCE_PX, min(MAX_CENTER_DISTANCE_PX, faceDiagonal * 0.75f))

                        var spatialMatch = centerDistance <= adaptiveDistance || iou >= MIN_IOU_FOR_MATCH
                        var simPenalty = 0f

                        if (track.referenceEmbedding != null) {
                            val embedding = faceEmbeddingsCache.getOrPut(faceIndex) {
                                if (frameBitmap == null) frameBitmap = videoProcessor.extractFrame(uri, timestampMs)
                                val currentBitmap = frameBitmap
                                if (currentBitmap != null) {
                                    try { faceEmbedder.embed(currentBitmap, face.boundingBox) } catch (_: Exception) { null }
                                } else null
                            }

                            if (embedding != null) {
                                val sim = cosineSimilarity(track.referenceEmbedding!!, embedding)
                                if (sim < EMBEDDING_SIM_THRESHOLD) {
                                    continue
                                }
                                simPenalty = (1.0f - sim).coerceIn(0f, 1f)
                                spatialMatch = true
                            }
                        }

                        if (!spatialMatch) continue

                        val normalizedDistance = (centerDistance / adaptiveDistance).coerceIn(0f, 1.5f)
                        val score = normalizedDistance + (simPenalty * 2.0f)

                        candidates += Candidate(track = track, faceIndex = faceIndex, score = score)
                    }
                }
            } finally {
                frameBitmap?.let { if (!it.isRecycled) it.recycle() }
            }

            candidates.sortBy { it.score }
            val assignedTracks = mutableSetOf<Int>()
            val assignedFaces = mutableSetOf<Int>()

            for (candidate in candidates) {
                if (candidate.track.id in assignedTracks || candidate.faceIndex in assignedFaces) continue

                val track = candidate.track
                val face = faces[candidate.faceIndex]
                val previousFace = track.lastFace
                val previousTimestamp = track.lastTimestampMs

                updateVelocity(track, previousFace, previousTimestamp, face, timestampMs)
                updateReferenceEmbeddingIfNeeded(track, uri, timestampMs, face)

                track.observations += FaceObservation(timestampMs, face)
                track.lastTimestampMs = timestampMs

                assignedTracks += track.id
                assignedFaces += candidate.faceIndex
            }

            for (faceIndex in faces.indices) {
                if (faceIndex in assignedFaces) continue
                val face = faces[faceIndex]
                val track = Track(
                    id = nextTrackId++,
                    observations = mutableListOf(FaceObservation(timestampMs, face)),
                    lastTimestampMs = timestampMs
                )
                updateReferenceEmbeddingIfNeeded(track, uri, timestampMs, face)
                activeTracks += track
            }

            maxSimultaneousTracks = max(maxSimultaneousTracks, activeTracks.size)
        }

        completedTracks += activeTracks

        return completedTracks
            .filter { track -> track.observations.size >= 2 }
            .sortedBy { it.observations.first().timestampMs }
            .mapIndexed { index, track ->
                AppearanceSegment(
                    id = index + 1,
                    startTimestampMs = track.observations.first().timestampMs,
                    endTimestampMs = track.observations.last().timestampMs,
                    observations = track.observations.toList()
                )
            }
    }

    private fun updateVelocity(track: Track, previousFace: DetectedFace, previousTimestampMs: Long, currentFace: DetectedFace, currentTimestampMs: Long) {
        val elapsedMs = (currentTimestampMs - previousTimestampMs).coerceAtLeast(1L)
        val rawVelocityX = (currentFace.centerX - previousFace.centerX) / elapsedMs.toFloat()
        val rawVelocityY = (currentFace.centerY - previousFace.centerY) / elapsedMs.toFloat()
        track.velocityXPerMs = VELOCITY_SMOOTHING * track.velocityXPerMs + (1f - VELOCITY_SMOOTHING) * rawVelocityX
        track.velocityYPerMs = VELOCITY_SMOOTHING * track.velocityYPerMs + (1f - VELOCITY_SMOOTHING) * rawVelocityY
    }

    private fun updateReferenceEmbeddingIfNeeded(track: Track, uri: Uri, timestampMs: Long, face: DetectedFace) {
        val needsInitial = track.referenceEmbedding == null
        val isBetterQuality = face.isApproximatelyFrontal && face.area > track.referenceQualityArea
        if (needsInitial || isBetterQuality) {
            var bitmap: Bitmap? = null
            try {
                bitmap = videoProcessor.extractFrame(uri, timestampMs)
                if (bitmap != null) {
                    track.referenceEmbedding = faceEmbedder.embed(bitmap, face.boundingBox)
                    track.referenceQualityArea = face.area
                }
            } catch (e: Exception) {
                Log.e(TAG, "Reference embedding update failed for Track${track.id}", e)
            } finally {
                bitmap?.let { if (!it.isRecycled) it.recycle() }
            }
        }
    }

    private fun calculateSizeRatio(first: DetectedFace, second: DetectedFace): Float {
        val firstArea = first.area.toFloat()
        val secondArea = second.area.toFloat()
        if (firstArea <= 0f || secondArea <= 0f) return Float.MAX_VALUE
        return max(firstArea, secondArea) / min(firstArea, secondArea)
    }

    private fun calculateIoU(first: Rect, second: Rect): Float {
        val left = maxOf(first.left, second.left)
        val top = maxOf(first.top, second.top)
        val right = minOf(first.right, second.right)
        val bottom = minOf(first.bottom, second.bottom)
        val width = max(0, right - left)
        val height = max(0, bottom - top)
        val intersection = width * height
        if (intersection <= 0) return 0f
        val union = first.width() * first.height() + second.width() * second.height() - intersection
        if (union <= 0) return 0f
        return intersection.toFloat() / union.toFloat()
    }

    private fun distance(firstX: Float, firstY: Float, secondX: Float, secondY: Float): Float {
        val dx = secondX - firstX
        val dy = secondY - firstY
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    private fun cosineSimilarity(first: FloatArray, second: FloatArray): Float {
        if (first.size != second.size || first.isEmpty()) return -1f
        var dot = 0f
        var firstMag = 0f
        var secondMag = 0f
        for (i in first.indices) {
            dot += first[i] * second[i]
            firstMag += first[i] * first[i]
            secondMag += second[i] * second[i]
        }
        if (firstMag <= 0f || secondMag <= 0f) return -1f
        return dot / sqrt(firstMag * secondMag)
    }
}