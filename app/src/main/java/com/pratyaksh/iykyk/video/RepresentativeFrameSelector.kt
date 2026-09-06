package com.pratyaksh.iykyk.video

import android.util.Log
import kotlin.math.abs

data class RepresentativeFrame(
    val timestampMs: Long,
    val face: DetectedFace,
    val score: Float,
    val passedFramingGate: Boolean = true,
    val minEdgeMargin: Float = 0f
)

class RepresentativeFrameSelector {

    companion object {
        private const val TAG = "RepresentativeFrameSelector"
        private const val MIN_NORMALIZED_MARGIN = 0.02f
    }

    fun select(
        segment: AppearanceSegment,
        frameWidth: Int,
        frameHeight: Int
    ): RepresentativeFrame? {
        if (segment.observations.isEmpty()) {
            return null
        }

        val candidates = segment.observations.map { observation ->
            val minMargin = calculateMinEdgeMargin(
                face = observation.face,
                frameWidth = frameWidth,
                frameHeight = frameHeight
            )

            val passedGate = minMargin >= MIN_NORMALIZED_MARGIN

            val score = calculateScore(
                face = observation.face,
                frameWidth = frameWidth,
                frameHeight = frameHeight,
                minEdgeMargin = minMargin
            )

            RepresentativeFrame(
                timestampMs = observation.timestampMs,
                face = observation.face,
                score = score,
                passedFramingGate = passedGate,
                minEdgeMargin = minMargin
            )
        }

        val validCandidates = candidates.filter { it.passedFramingGate }

        val selected = if (validCandidates.isNotEmpty()) {
            validCandidates.maxByOrNull { it.score }
        } else {
            candidates.maxByOrNull { it.score }
        }

        selected?.let { frame ->
            Log.d(
                TAG,
                "Segment #${segment.id}: Selected t=${frame.timestampMs}ms | Score=${"%.3f".format(frame.score)} | PassedGate=${frame.passedFramingGate} | MinMargin=${"%.3f".format(frame.minEdgeMargin)}"
            )
        }

        return selected
    }

    private fun calculateScore(
        face: DetectedFace,
        frameWidth: Int,
        frameHeight: Int,
        minEdgeMargin: Float
    ): Float {
        val frontalityScore = calculateFrontalityScore(face)
        val eyeScore = calculateEyeScore(face)
        val smileScore = calculateSmileScore(face)
        val sizeScore = calculateSizeScore(face, frameWidth, frameHeight)
        val clippingScore = (minEdgeMargin / 0.08f).coerceIn(0f, 1f)

        return (
                frontalityScore * 0.30f +
                        eyeScore * 0.25f +
                        smileScore * 0.10f +
                        sizeScore * 0.20f +
                        clippingScore * 0.15f
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