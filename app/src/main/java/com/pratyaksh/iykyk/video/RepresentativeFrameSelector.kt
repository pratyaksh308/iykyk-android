package com.pratyaksh.iykyk.video

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import kotlin.math.abs

data class RepresentativeFrame(
    val timestampMs: Long,
    val face: DetectedFace,
    val score: Float,
    val passedFramingGate: Boolean = true,
    val minEdgeMargin: Float = 0f,
    val rawSharpness: Float = 0f,
    val normalizedSharpness: Float = 0.5f
)

class RepresentativeFrameSelector {

    companion object {
        private const val TAG = "RepresentativeFrameSelector"
        private const val MIN_NORMALIZED_MARGIN = 0.02f
    }

    private data class UnscoredCandidate(
        val timestampMs: Long,
        val face: DetectedFace,
        val bitmap: Bitmap?,
        val minMargin: Float,
        val passedGate: Boolean,
        val rawSharpness: Float
    )

    private data class ScoredCandidate(
        val frame: RepresentativeFrame,
        val bitmap: Bitmap?
    )

    fun select(
        uri: Uri,
        segment: AppearanceSegment,
        frameWidth: Int,
        frameHeight: Int,
        frameLoader: RepresentativeFrameLoader
    ): Pair<RepresentativeFrame, Bitmap?>? {
        if (segment.observations.isEmpty()) {
            return null
        }

        val timestamps = segment.observations.map { it.timestampMs }
        val loadedBitmaps = frameLoader.loadFrames(uri, timestamps)

        val initialCandidates = segment.observations.map { observation ->
            val bitmap = loadedBitmaps[observation.timestampMs]
            var rawSharpness = 0f

            if (bitmap != null) {
                try {
                    rawSharpness = calculateSharpness(bitmap, observation.face)
                } catch (_: Exception) {
                    rawSharpness = 0f
                }
            }

            val minMargin = calculateMinEdgeMargin(
                face = observation.face,
                frameWidth = frameWidth,
                frameHeight = frameHeight
            )

            val passedGate = minMargin >= MIN_NORMALIZED_MARGIN

            UnscoredCandidate(
                timestampMs = observation.timestampMs,
                face = observation.face,
                bitmap = bitmap,
                minMargin = minMargin,
                passedGate = passedGate,
                rawSharpness = rawSharpness
            )
        }

        val validInitial = initialCandidates.filter { it.passedGate }

        val normGroup = if (validInitial.isNotEmpty()) validInitial else initialCandidates
        val sortedGroup = normGroup.sortedBy { it.rawSharpness }
        val groupSize = sortedGroup.size

        val normSharpnessMap = mutableMapOf<Long, Float>()

        if (groupSize == 1) {
            normSharpnessMap[sortedGroup[0].timestampMs] = 0.5f
        } else if (groupSize > 1) {
            sortedGroup.forEachIndexed { index, candidate ->
                val normSharpness = index.toFloat() / (groupSize - 1).toFloat()
                normSharpnessMap[candidate.timestampMs] = normSharpness
            }
        }

        val candidates = initialCandidates.map { candidate ->
            val normSharpness = normSharpnessMap[candidate.timestampMs] ?: 0.0f

            val score = calculateScore(
                face = candidate.face,
                frameWidth = frameWidth,
                frameHeight = frameHeight,
                minEdgeMargin = candidate.minMargin,
                normalizedSharpness = normSharpness
            )

            val repFrame = RepresentativeFrame(
                timestampMs = candidate.timestampMs,
                face = candidate.face,
                score = score,
                passedFramingGate = candidate.passedGate,
                minEdgeMargin = candidate.minMargin,
                rawSharpness = candidate.rawSharpness,
                normalizedSharpness = normSharpness
            )

            ScoredCandidate(repFrame, candidate.bitmap)
        }

        val validScoredCandidates = candidates.filter { it.frame.passedFramingGate }

        val selectedCandidate = if (validScoredCandidates.isNotEmpty()) {
            validScoredCandidates.maxByOrNull { it.frame.score }
        } else {
            candidates.maxByOrNull { it.frame.score }
        }

        candidates.forEach { candidate ->
            if (candidate !== selectedCandidate && candidate.bitmap != null && !candidate.bitmap.isRecycled) {
                candidate.bitmap.recycle()
            }
        }

        selectedCandidate?.let { sc ->
            Log.d(
                TAG,
                "Segment #${segment.id}: Selected t=${sc.frame.timestampMs}ms | Score=${"%.3f".format(sc.frame.score)} | PassedGate=${sc.frame.passedFramingGate} | MinMargin=${"%.3f".format(sc.frame.minEdgeMargin)} | RawSharpness=${"%.1f".format(sc.frame.rawSharpness)} | NormSharpness=${"%.3f".format(sc.frame.normalizedSharpness)}"
            )

            segment.observations.forEach { obs ->
                val cand = candidates.firstOrNull { it.frame.timestampMs == obs.timestampMs }
                cand?.let {
                    Log.d(
                        TAG,
                        "  Candidate t=${obs.timestampMs}ms | Score=${"%.3f".format(it.frame.score)} | Gate=${it.frame.passedFramingGate} | Margin=${"%.3f".format(it.frame.minEdgeMargin)} | RawSharpness=${"%.1f".format(it.frame.rawSharpness)} | NormSharpness=${"%.3f".format(it.frame.normalizedSharpness)}"
                    )
                }
            }

            return Pair(sc.frame, sc.bitmap)
        }

        return null
    }

    private fun calculateSharpness(bitmap: Bitmap, face: DetectedFace): Float {
        val box = face.boundingBox
        val left = box.left.coerceIn(0, bitmap.width - 1)
        val top = box.top.coerceIn(0, bitmap.height - 1)
        val right = box.right.coerceIn(left + 1, bitmap.width)
        val bottom = box.bottom.coerceIn(top + 1, bitmap.height)

        val width = right - left
        val height = bottom - top

        if (width <= 2 || height <= 2) {
            return 0f
        }

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, left, top, width, height)

        val gray = FloatArray(width * height)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            gray[i] = 0.299f * r + 0.587f * g + 0.114f * b
        }

        var laplacianSum = 0.0
        var laplacianSqSum = 0.0
        var count = 0

        for (y in 1 until height - 1) {
            val rowOffset = y * width
            val prevRowOffset = (y - 1) * width
            val nextRowOffset = (y + 1) * width

            for (x in 1 until width - 1) {
                val center = gray[rowOffset + x]
                val leftVal = gray[rowOffset + x - 1]
                val rightVal = gray[rowOffset + x + 1]
                val topVal = gray[prevRowOffset + x]
                val bottomVal = gray[nextRowOffset + x]

                val lap = leftVal + rightVal + topVal + bottomVal - 4f * center

                laplacianSum += lap
                laplacianSqSum += (lap * lap)
                count++
            }
        }

        if (count == 0) {
            return 0f
        }

        val mean = laplacianSum / count
        val variance = (laplacianSqSum / count) - (mean * mean)

        return variance.toFloat().coerceAtLeast(0f)
    }

    private fun calculateScore(
        face: DetectedFace,
        frameWidth: Int,
        frameHeight: Int,
        minEdgeMargin: Float,
        normalizedSharpness: Float
    ): Float {
        val frontalityScore = calculateFrontalityScore(face)
        val eyeScore = calculateEyeScore(face)
        val smileScore = calculateSmileScore(face)
        val sizeScore = calculateSizeScore(face, frameWidth, frameHeight)
        val clippingScore = (minEdgeMargin / 0.08f).coerceIn(0f, 1f)

        return (
                frontalityScore * 0.285f +
                        eyeScore * 0.2375f +
                        smileScore * 0.095f +
                        sizeScore * 0.19f +
                        clippingScore * 0.1425f +
                        normalizedSharpness * 0.05f
                )
    }

    private fun calculateFrontalityScore(face: DetectedFace): Float {
        val yaw = face.headEulerAngleY ?: return 0.5f
        val roll = face.headEulerAngleZ ?: return 0.5f

        val yawScore = 1f - (abs(yaw) / 45f).coerceIn(0f, 1f)
        val rollScore = 1f - (abs(roll) / 30f).coerceIn(0f, 1f)

        return (
                yawScore * 0.7f +
                        rollScore * 0.3f
                ).coerceIn(0f, 1f)
    }

    private fun calculateEyeScore(face: DetectedFace): Float {
        val left = face.leftEyeOpenProbability ?: 0.5f
        val right = face.rightEyeOpenProbability ?: 0.5f

        return minOf(left, right).coerceIn(0f, 1f)
    }

    private fun calculateSmileScore(face: DetectedFace): Float {
        return (face.smilingProbability ?: 0.5f).coerceIn(0f, 1f)
    }

    private fun calculateSizeScore(
        face: DetectedFace,
        frameWidth: Int,
        frameHeight: Int
    ): Float {
        if (frameWidth <= 0 || frameHeight <= 0) {
            return 0f
        }

        val frameArea = (frameWidth * frameHeight).toFloat()
        val faceArea = face.area.toFloat()

        return (faceArea / (frameArea * 0.15f)).coerceIn(0f, 1f)
    }

    private fun calculateMinEdgeMargin(
        face: DetectedFace,
        frameWidth: Int,
        frameHeight: Int
    ): Float {
        if (frameWidth <= 0 || frameHeight <= 0) {
            return 0f
        }

        val box = face.boundingBox

        val leftMargin = (box.left.toFloat() / frameWidth.toFloat()).coerceIn(0f, 1f)
        val topMargin = (box.top.toFloat() / frameHeight.toFloat()).coerceIn(0f, 1f)
        val rightMargin = ((frameWidth - box.right).toFloat() / frameWidth.toFloat()).coerceIn(0f, 1f)
        val bottomMargin = ((frameHeight - box.bottom).toFloat() / frameHeight.toFloat()).coerceIn(0f, 1f)

        return minOf(minOf(leftMargin, rightMargin), minOf(topMargin, bottomMargin))
    }
}