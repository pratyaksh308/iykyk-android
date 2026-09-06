package com.pratyaksh.iykyk.video

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
        private const val MAX_TRACK_GAP_MS = 1200L
        private const val MAX_CENTER_DISTANCE_PX = 250f
        private const val MIN_CENTER_DISTANCE_PX = 80f
        private const val MAX_SIZE_CHANGE_RATIO = 2.5f
        private const val MIN_IOU_FOR_MATCH = 0.05f
        private const val STRONG_IOU = 0.20f
        private const val EMBEDDING_SIM_THRESHOLD = 0.40f
        private const val MIN_FACE_AREA = 2000
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
        if (frameDetections.isEmpty()) {
            return emptyList()
        }

        val activeTracks = mutableListOf<Track>()
        val completedTracks = mutableListOf<Track>()
        var nextTrackId = 1
        var totalDetectedFaces = 0
        var maxSimultaneousTracks = 0

        for (i in frameDetections.indices) {
            val frame = frameDetections[i]
            onProgress(i.toFloat() / max(1, frameDetections.size).toFloat())
            val timestampMs = frame.timestampMs
            val faces = frame.faces
            totalDetectedFaces += faces.size

            val expiredTracks = activeTracks.filter {
                timestampMs - it.lastTimestampMs > MAX_TRACK_GAP_MS
            }

            completedTracks += expiredTracks
            activeTracks.removeAll(expiredTracks)

            if (faces.isEmpty()) {
                continue
            }

            val candidates = mutableListOf<Candidate>()
            var frameBitmap: Bitmap? = null

            try {
                val faceEmbeddingsCache = mutableMapOf<Int, FloatArray?>()

                for (track in activeTracks) {
                    val gapMs = timestampMs - track.lastTimestampMs

                    if (gapMs <= 0L || gapMs > MAX_TRACK_GAP_MS) {
                        continue
                    }

                    val lastFace = track.lastFace

                    val predictedCenterX =
                        lastFace.centerX + track.velocityXPerMs * gapMs.toFloat()

                    val predictedCenterY =
                        lastFace.centerY + track.velocityYPerMs * gapMs.toFloat()

                    for (faceIndex in faces.indices) {
                        val face = faces[faceIndex]

                        val centerDistance = distance(
                            predictedCenterX,
                            predictedCenterY,
                            face.centerX,
                            face.centerY
                        )

                        val iou = calculateIoU(
                            lastFace.boundingBox,
                            face.boundingBox
                        )

                        val sizeRatio = calculateSizeRatio(
                            lastFace,
                            face
                        )

                        if (sizeRatio > MAX_SIZE_CHANGE_RATIO) {
                            continue
                        }

                        val faceDiagonal = sqrt(
                            (
                                    face.width.toFloat() * face.width.toFloat() +
                                            face.height.toFloat() * face.height.toFloat()
                                    ).toDouble()
                        ).toFloat()

                        val adaptiveDistance = max(
                            MIN_CENTER_DISTANCE_PX,
                            min(
                                MAX_CENTER_DISTANCE_PX,
                                faceDiagonal * 0.75f
                            )
                        )

                        val spatialMatch =
                            centerDistance <= adaptiveDistance || iou >= MIN_IOU_FOR_MATCH

                        if (!spatialMatch) {
                            continue
                        }

                        var simPenalty = 0f

                        if (gapMs >= 400L || (centerDistance > 100f && iou < STRONG_IOU)) {
                            if (track.referenceEmbedding != null) {
                                val embedding = faceEmbeddingsCache.getOrPut(faceIndex) {
                                    if (frameBitmap == null) {
                                        frameBitmap = videoProcessor.extractFrame(uri, timestampMs)
                                    }
                                    if (frameBitmap != null) {
                                        try {
                                            faceEmbedder.embed(frameBitmap, face.boundingBox)
                                        } catch (_: Exception) {
                                            null
                                        }
                                    } else {
                                        null
                                    }
                                }

                                if (embedding != null) {
                                    val sim = cosineSimilarity(track.referenceEmbedding!!, embedding)

                                    if (sim < EMBEDDING_SIM_THRESHOLD) {
                                        continue
                                    }

                                    simPenalty = (1.0f - sim).coerceIn(0f, 1f)
                                }
                            }
                        }

                        val normalizedDistance =
                            (centerDistance / adaptiveDistance).coerceIn(0f, 1.5f)

                        val iouScore = if (iou >= STRONG_IOU) 1f - iou else 1f

                        val score =
                            0.5f * iouScore + 0.3f * normalizedDistance + 0.2f * simPenalty

                        candidates += Candidate(
                            track = track,
                            faceIndex = faceIndex,
                            score = score
                        )
                    }
                }
            } finally {
                frameBitmap?.let {
                    if (!it.isRecycled) {
                        it.recycle()
                    }
                }
            }

            candidates.sortBy { it.score }

            val assignedTracks = mutableSetOf<Int>()
            val assignedFaces = mutableSetOf<Int>()

            for (candidate in candidates) {
                if (candidate.track.id in assignedTracks) {
                    continue
                }

                if (candidate.faceIndex in assignedFaces) {
                    continue
                }

                val track = candidate.track
                val face = faces[candidate.faceIndex]
                val previousFace = track.lastFace
                val previousTimestamp = track.lastTimestampMs
                val elapsedMs = timestampMs - previousTimestamp

                updateVelocity(
                    track = track,
                    previousFace = previousFace,
                    previousTimestampMs = previousTimestamp,
                    currentFace = face,
                    currentTimestampMs = timestampMs
                )

                updateReferenceEmbeddingIfNeeded(
                    track = track,
                    uri = uri,
                    timestampMs = timestampMs,
                    face = face
                )

                track.observations += FaceObservation(
                    timestampMs = timestampMs,
                    face = face
                )

                track.lastTimestampMs = timestampMs

                assignedTracks += track.id
                assignedFaces += candidate.faceIndex

                Log.d(
                    TAG,
                    "[MATCH] Face${candidate.faceIndex} -> Track${track.id} " +
                            "t=${timestampMs}ms gap=${elapsedMs}ms " +
                            "score=${"%.3f".format(candidate.score)}"
                )
            }

            for (faceIndex in faces.indices) {
                if (faceIndex in assignedFaces) {
                    continue
                }

                val face = faces[faceIndex]

                val track = Track(
                    id = nextTrackId++,
                    observations = mutableListOf(
                        FaceObservation(
                            timestampMs = timestampMs,
                            face = face
                        )
                    ),
                    lastTimestampMs = timestampMs
                )

                updateReferenceEmbeddingIfNeeded(
                    track = track,
                    uri = uri,
                    timestampMs = timestampMs,
                    face = face
                )

                activeTracks += track

                Log.d(
                    TAG,
                    "[NEW] Face${faceIndex} -> Track${track.id} " +
                            "t=${timestampMs}ms"
                )
            }

            maxSimultaneousTracks = max(
                maxSimultaneousTracks,
                activeTracks.size
            )
        }

        completedTracks += activeTracks

        val validSegments = completedTracks
            .filter { track ->
                track.observations.size >= 2 ||
                        (track.observations.size == 1 &&
                                track.referenceEmbedding != null &&
                                track.observations.first().face.area >= 5000)
            }
            .sortedBy {
                it.observations.first().timestampMs
            }
            .mapIndexed { index, track ->
                AppearanceSegment(
                    id = index + 1,
                    startTimestampMs =
                        track.observations.first().timestampMs,
                    endTimestampMs =
                        track.observations.last().timestampMs,
                    observations = track.observations.toList()
                )
            }

        Log.d(
            TAG,
            "=================== APPEARANCE SEGMENTATION RESULTS ==================="
        )
        Log.d(
            TAG,
            "Total input frames: ${frameDetections.size}"
        )
        Log.d(
            TAG,
            "Total detected faces: $totalDetectedFaces"
        )
        Log.d(
            TAG,
            "Number of appearance segments: ${validSegments.size}"
        )
        Log.d(
            TAG,
            "Maximum simultaneous active appearances: $maxSimultaneousTracks"
        )

        validSegments.forEach { segment ->
            Log.d(
                TAG,
                "Segment #${segment.id}: " +
                        "start=${segment.startTimestampMs}ms, " +
                        "end=${segment.endTimestampMs}ms, " +
                        "duration=${segment.durationMs}ms, " +
                        "observations=${segment.observationCount}"
            )
        }

        Log.d(
            TAG,
            "======================================================================="
        )

        onProgress(1f)
        return validSegments
    }

    private fun updateVelocity(
        track: Track,
        previousFace: DetectedFace,
        previousTimestampMs: Long,
        currentFace: DetectedFace,
        currentTimestampMs: Long
    ) {
        val elapsedMs = (currentTimestampMs - previousTimestampMs).coerceAtLeast(1L)

        val rawVelocityX = (currentFace.centerX - previousFace.centerX) / elapsedMs.toFloat()
        val rawVelocityY = (currentFace.centerY - previousFace.centerY) / elapsedMs.toFloat()

        track.velocityXPerMs = VELOCITY_SMOOTHING * track.velocityXPerMs + (1f - VELOCITY_SMOOTHING) * rawVelocityX
        track.velocityYPerMs = VELOCITY_SMOOTHING * track.velocityYPerMs + (1f - VELOCITY_SMOOTHING) * rawVelocityY
    }

    private fun updateReferenceEmbeddingIfNeeded(
        track: Track,
        uri: Uri,
        timestampMs: Long,
        face: DetectedFace
    ) {
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
            } catch (exception: Exception) {
                Log.e(TAG, "Reference embedding update failed for Track${track.id}", exception)
            } finally {
                bitmap?.let {
                    if (!it.isRecycled) {
                        it.recycle()
                    }
                }
            }
        }
    }

    private fun calculateSizeRatio(
        first: DetectedFace,
        second: DetectedFace
    ): Float {
        val firstArea = first.area.toFloat()
        val secondArea = second.area.toFloat()

        if (firstArea <= 0f || secondArea <= 0f) {
            return Float.MAX_VALUE
        }

        return max(firstArea, secondArea) / min(firstArea, secondArea)
    }

    private fun calculateIoU(
        first: Rect,
        second: Rect
    ): Float {
        val left = maxOf(first.left, second.left)
        val top = maxOf(first.top, second.top)
        val right = minOf(first.right, second.right)
        val bottom = minOf(first.bottom, second.bottom)

        val width = max(0, right - left)
        val height = max(0, bottom - top)

        val intersection = width * height

        if (intersection <= 0) {
            return 0f
        }

        val firstArea = first.width() * first.height()
        val secondArea = second.width() * second.height()

        val union = firstArea + secondArea - intersection

        if (union <= 0) {
            return 0f
        }

        return intersection.toFloat() / union.toFloat()
    }

    private fun distance(
        firstX: Float,
        firstY: Float,
        secondX: Float,
        secondY: Float
    ): Float {
        val dx = secondX - firstX
        val dy = secondY - firstY
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    private fun cosineSimilarity(
        first: FloatArray,
        second: FloatArray
    ): Float {
        if (first.size != second.size || first.isEmpty()) {
            return -1f
        }

        var dot = 0f
        var firstMagnitude = 0f
        var secondMagnitude = 0f

        for (index in first.indices) {
            dot += first[index] * second[index]
            firstMagnitude += first[index] * first[index]
            secondMagnitude += second[index] * second[index]
        }

        if (firstMagnitude <= 0f || secondMagnitude <= 0f) {
            return -1f
        }

        return dot / sqrt(firstMagnitude * secondMagnitude)
    }
}