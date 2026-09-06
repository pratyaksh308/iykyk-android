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
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.12f)
            .build()
    )

    suspend fun detectFaces(bitmap: Bitmap): List<DetectedFace> {
        val image = InputImage.fromBitmap(bitmap, 0)
        val minDimension = (kotlin.math.min(bitmap.width, bitmap.height) * 0.08f).toInt()
        val minArea = (bitmap.width * bitmap.height * 0.005f).toInt()

        return detector.process(image)
            .await()
            .filter { face ->
                val bounds = face.boundingBox
                val w = bounds.width()
                val h = bounds.height()
                val aspect = if (h > 0) w.toFloat() / h.toFloat() else 0f
                w >= minDimension && h >= minDimension && (w * h) >= minArea && aspect in 0.45f..1.8f
            }
            .map { face ->
                DetectedFace(
                    boundingBox = face.boundingBox,
                    headEulerAngleX = face.headEulerAngleX,
                    headEulerAngleY = face.headEulerAngleY,
                    headEulerAngleZ = face.headEulerAngleZ,
                    leftEyeOpenProbability = face.leftEyeOpenProbability,
                    rightEyeOpenProbability = face.rightEyeOpenProbability,
                    smilingProbability = face.smilingProbability
                )
            }
    }

    fun close() {
        detector.close()
    }
}