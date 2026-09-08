package com.pratyaksh.iykyk.video

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.tasks.await

class FaceDetector {
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.02f)
            .build()
    )

    suspend fun detectFaces(bitmap: Bitmap): List<DetectedFace> {
        val image = InputImage.fromBitmap(bitmap, 0)
        val minDimension = (kotlin.math.min(bitmap.width, bitmap.height) * 0.02f).toInt()
        val minArea = (bitmap.width * bitmap.height * 0.0006f).toInt()

        return detector.process(image)
            .await()
            .filter { face ->
                val bounds = face.boundingBox
                val w = bounds.width()
                val h = bounds.height()
                val aspect = if (h > 0) w.toFloat() / h.toFloat() else 0f
                w >= minDimension && h >= minDimension && (w * h) >= minArea && aspect in 0.30f..2.5f
            }
            .map { face ->
                DetectedFace(
                    boundingBox = face.boundingBox,
                    headEulerAngleX = face.headEulerAngleX,
                    headEulerAngleY = face.headEulerAngleY,
                    headEulerAngleZ = face.headEulerAngleZ,
                    leftEyeOpenProbability = null,
                    rightEyeOpenProbability = null,
                    smilingProbability = null
                )
            }
    }

    fun close() {
        detector.close()
    }
}