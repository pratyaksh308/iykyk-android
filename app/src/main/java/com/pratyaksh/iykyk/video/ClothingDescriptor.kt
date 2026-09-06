package com.pratyaksh.iykyk.video

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import kotlin.math.max
import kotlin.math.min

data class ClothingDescriptor(
    val values: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ClothingDescriptor

        return values.contentEquals(other.values)
    }

    override fun hashCode(): Int {
        return values.contentHashCode()
    }
}

class ClothingDescriptorExtractor {

    companion object {
        private const val HUE_BINS = 12
        private const val SAT_BINS = 4
        private const val VAL_BINS = 4
        private const val TOTAL_BINS = HUE_BINS * SAT_BINS * VAL_BINS
    }

    fun extract(
        bitmap: Bitmap,
        faceBounds: Rect
    ): ClothingDescriptor {
        val faceWidth = faceBounds.width()
        val faceHeight = faceBounds.height()

        if (faceWidth <= 0 || faceHeight <= 0) {
            throw IllegalArgumentException("Invalid face bounding box")
        }

        val torsoTop = (faceBounds.bottom + faceHeight * 0.15f).toInt()
        val torsoBottom = (torsoTop + faceHeight * 1.8f).toInt()
        val torsoLeft = (faceBounds.left - faceWidth * 0.4f).toInt()
        val torsoRight = (faceBounds.right + faceWidth * 0.4f).toInt()

        val left = max(0, torsoLeft)
        val top = max(0, torsoTop)
        val right = min(bitmap.width, torsoRight)
        val bottom = min(bitmap.height, torsoBottom)

        val cropWidth = right - left
        val cropHeight = bottom - top

        if (cropWidth <= 4 || cropHeight <= 4) {
            throw IllegalArgumentException("Torso crop area is too small")
        }

        val pixels = IntArray(cropWidth * cropHeight)
        bitmap.getPixels(pixels, 0, cropWidth, left, top, cropWidth, cropHeight)

        val histogram = FloatArray(TOTAL_BINS)
        val hsv = FloatArray(3)

        var validPixelCount = 0

        for (pixel in pixels) {
            val alpha = Color.alpha(pixel)
            if (alpha < 128) continue

            Color.colorToHSV(pixel, hsv)

            val hue = hsv[0]
            val sat = hsv[1]
            val valIndex = hsv[2]

            val hBin = ((hue / 360f) * HUE_BINS).toInt().coerceIn(0, HUE_BINS - 1)
            val sBin = (sat * SAT_BINS).toInt().coerceIn(0, SAT_BINS - 1)
            val vBin = (valIndex * VAL_BINS).toInt().coerceIn(0, VAL_BINS - 1)

            val binIndex = hBin * (SAT_BINS * VAL_BINS) + sBin * VAL_BINS + vBin
            histogram[binIndex] += 1.0f
            validPixelCount++
        }

        if (validPixelCount == 0) {
            throw IllegalStateException("No valid pixels in torso region")
        }

        var sumSquares = 0.0f
        for (value in histogram) {
            sumSquares += value * value
        }

        val norm = kotlin.math.sqrt(sumSquares)
        if (norm > 0f) {
            for (i in histogram.indices) {
                histogram[i] /= norm
            }
        }

        return ClothingDescriptor(values = histogram)
    }
}
