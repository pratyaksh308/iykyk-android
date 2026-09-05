package com.pratyaksh.iykyk.video

import android.graphics.Rect
import kotlin.math.abs

data class DetectedFace(
    val boundingBox: Rect,
    val headEulerAngleX: Float?,
    val headEulerAngleY: Float?,
    val headEulerAngleZ: Float?,
    val leftEyeOpenProbability: Float?,
    val rightEyeOpenProbability: Float?,
    val smilingProbability: Float?
) {
    val centerX: Float
        get() = boundingBox.exactCenterX()

    val centerY: Float
        get() = boundingBox.exactCenterY()

    val width: Int
        get() = boundingBox.width()

    val height: Int
        get() = boundingBox.height()

    val area: Int
        get() = width * height

    val aspectRatio: Float
        get() = if (height > 0) {
            width.toFloat() / height.toFloat()
        } else {
            0f
        }

    val isApproximatelyFrontal: Boolean
        get() {
            val yaw = headEulerAngleY ?: return false
            val roll = headEulerAngleZ ?: return false

            return abs(yaw) <= 25f && abs(roll) <= 20f
        }
}