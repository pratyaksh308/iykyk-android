package com.pratyaksh.iykyk.video.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.sqrt

class FaceEmbedder(
    context: Context
) {
    companion object {
        private const val TAG = "FaceEmbedder"
        private const val MODEL_FILE = "facenet_512.tflite"
        private const val INPUT_SIZE = 160
        private const val EMBEDDING_SIZE = 512
        private const val BATCH_SIZE = 1
        private const val PIXEL_MEAN = 127.5f
        private const val PIXEL_STD = 128f
        private const val FACE_PADDING = 0.10f
    }

    private val interpreter: Interpreter

    private val landmarkDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .build()
    )

    init {
        Log.i(TAG, "Initializing TFLite Interpreter on CPU with 4 threads (NEON SIMD accelerated)")
        val options = Interpreter.Options().apply {
            setNumThreads(4)
        }
        interpreter = Interpreter(
            loadModel(context),
            options
        )
        interpreter.allocateTensors()
        validateModel()
    }

    fun embed(
        bitmap: Bitmap,
        boundingBox: Rect
    ): FloatArray {
        val faceBitmap = cropFace(
            bitmap = bitmap,
            boundingBox = boundingBox
        )

        var alignedBitmap: Bitmap? = null
        try {
            alignedBitmap = alignFace(faceBitmap)
            return embedFace(alignedBitmap)
        } finally {
            if (alignedBitmap != null && alignedBitmap !== faceBitmap && !alignedBitmap.isRecycled) {
                alignedBitmap.recycle()
            }
            if (!faceBitmap.isRecycled) {
                faceBitmap.recycle()
            }
        }
    }

    fun embedFace(
        bitmap: Bitmap
    ): FloatArray {
        require(bitmap.width > 0 && bitmap.height > 0) {
            "Bitmap must have positive dimensions"
        }

        val resizedBitmap = Bitmap.createScaledBitmap(
            bitmap,
            INPUT_SIZE,
            INPUT_SIZE,
            true
        )

        try {
            val input = ByteBuffer.allocateDirect(
                BATCH_SIZE *
                        INPUT_SIZE *
                        INPUT_SIZE *
                        3 *
                        Float.SIZE_BYTES
            ).order(ByteOrder.nativeOrder())

            addBitmapToInput(
                bitmap = resizedBitmap,
                input = input
            )

            input.rewind()

            val output = Array(BATCH_SIZE) {
                FloatArray(EMBEDDING_SIZE)
            }

            val startTime = System.nanoTime()

            synchronized(interpreter) {
                interpreter.run(
                    input,
                    output
                )
            }

            val inferenceTimeMs =
                (System.nanoTime() - startTime) / 1_000_000

            val embedding = normalizeEmbedding(output[0])

            Log.d(
                TAG,
                "Inference time=${inferenceTimeMs}ms"
            )

            Log.d(
                TAG,
                "Embedding size=${embedding.size}"
            )

            Log.d(
                TAG,
                "Embedding magnitude=${"%.6f".format(
                    embeddingMagnitude(embedding)
                )}"
            )

            return embedding
        } finally {
            if (!resizedBitmap.isRecycled) {
                resizedBitmap.recycle()
            }
        }
    }

    private fun alignFace(
        faceBitmap: Bitmap
    ): Bitmap {
        if (faceBitmap.width < 20 || faceBitmap.height < 20) {
            throw IllegalArgumentException("Face crop too small")
        }

        val image = InputImage.fromBitmap(
            faceBitmap,
            0
        )

        val faces = try {
            Tasks.await(
                landmarkDetector.process(image)
            )
        } catch (_: Exception) {
            emptyList()
        }

        val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
            ?: throw IllegalArgumentException("Landmark verification failed: No face found")

        val leftEye = face.getLandmark(
            FaceLandmark.LEFT_EYE
        )?.position

        val rightEye = face.getLandmark(
            FaceLandmark.RIGHT_EYE
        )?.position

        if (leftEye == null || rightEye == null) {
            throw IllegalArgumentException("Missing eye landmarks. Rejecting non-face artifact.")
        }

        val dx = leftEye.x - rightEye.x
        val dy = leftEye.y - rightEye.y

        if (dx <= 0f) {
            return faceBitmap
        }

        val angle = Math.toDegrees(
            atan2(
                dy.toDouble(),
                dx.toDouble()
            )
        ).toFloat()

        if (kotlin.math.abs(angle) < 1f) {
            return faceBitmap
        }

        val matrix = Matrix().apply {
            postRotate(
                -angle,
                faceBitmap.width / 2f,
                faceBitmap.height / 2f
            )
        }

        val aligned = Bitmap.createBitmap(
            faceBitmap,
            0,
            0,
            faceBitmap.width,
            faceBitmap.height,
            matrix,
            true
        )

        Log.d(
            TAG,
            "Face alignment angle=${"%.2f".format(angle)}"
        )

        return aligned
    }

    private fun addBitmapToInput(
        bitmap: Bitmap,
        input: ByteBuffer
    ) {
        val pixels = IntArray(
            INPUT_SIZE * INPUT_SIZE
        )

        bitmap.getPixels(
            pixels,
            0,
            INPUT_SIZE,
            0,
            0,
            INPUT_SIZE,
            INPUT_SIZE
        )

        pixels.forEach { pixel ->
            val red = (pixel shr 16) and 0xFF
            val green = (pixel shr 8) and 0xFF
            val blue = pixel and 0xFF

            input.putFloat(
                (red - PIXEL_MEAN) / PIXEL_STD
            )

            input.putFloat(
                (green - PIXEL_MEAN) / PIXEL_STD
            )

            input.putFloat(
                (blue - PIXEL_MEAN) / PIXEL_STD
            )
        }
    }

    private fun cropFace(
        bitmap: Bitmap,
        boundingBox: Rect
    ): Bitmap {
        val left = boundingBox.left.coerceIn(
            0,
            bitmap.width - 1
        )

        val top = boundingBox.top.coerceIn(
            0,
            bitmap.height - 1
        )

        val right = boundingBox.right.coerceIn(
            left + 1,
            bitmap.width
        )

        val bottom = boundingBox.bottom.coerceIn(
            top + 1,
            bitmap.height
        )

        val width = right - left
        val height = bottom - top
        val squareSize = max(width, height)
        val paddedSize = (squareSize * (1f + FACE_PADDING)).toInt()

        val centerX = (left + right) / 2
        val centerY = (top + bottom) / 2

        var cropLeft = centerX - paddedSize / 2
        var cropTop = centerY - paddedSize / 2

        cropLeft = cropLeft.coerceIn(
            0,
            max(0, bitmap.width - paddedSize)
        )

        cropTop = cropTop.coerceIn(
            0,
            max(0, bitmap.height - paddedSize)
        )

        val cropRight = minOf(
            bitmap.width,
            cropLeft + paddedSize
        )

        val cropBottom = minOf(
            bitmap.height,
            cropTop + paddedSize
        )

        return Bitmap.createBitmap(
            bitmap,
            cropLeft,
            cropTop,
            cropRight - cropLeft,
            cropBottom - cropTop
        )
    }

    private fun normalizeEmbedding(
        embedding: FloatArray
    ): FloatArray {
        var sum = 0f

        embedding.forEach { value ->
            sum += value * value
        }

        val magnitude = sqrt(sum)

        if (magnitude == 0f) {
            return embedding
        }

        return FloatArray(
            embedding.size
        ) { index ->
            embedding[index] / magnitude
        }
    }

    private fun embeddingMagnitude(
        embedding: FloatArray
    ): Float {
        var sum = 0f

        embedding.forEach { value ->
            sum += value * value
        }

        return sqrt(sum)
    }

    private fun validateModel() {
        val inputTensor = interpreter.getInputTensor(0)
        val outputTensor = interpreter.getOutputTensor(0)
        val inputShape = inputTensor.shape()
        val outputShape = outputTensor.shape()

        Log.d(
            TAG,
            "Input shape=${inputShape.contentToString()}"
        )

        Log.d(
            TAG,
            "Input type=${inputTensor.dataType()}"
        )

        Log.d(
            TAG,
            "Output shape=${outputShape.contentToString()}"
        )

        Log.d(
            TAG,
            "Output type=${outputTensor.dataType()}"
        )

        require(
            inputShape.contentEquals(
                intArrayOf(
                    BATCH_SIZE,
                    INPUT_SIZE,
                    INPUT_SIZE,
                    3
                )
            )
        ) {
            "Unexpected input shape: ${inputShape.contentToString()}"
        }

        require(
            outputShape.contentEquals(
                intArrayOf(
                    BATCH_SIZE,
                    EMBEDDING_SIZE
                )
            )
        ) {
            "Unexpected output shape: ${outputShape.contentToString()}"
        }

        require(
            inputTensor.dataType() == DataType.FLOAT32
        ) {
            "Unexpected input type: ${inputTensor.dataType()}"
        }

        require(
            outputTensor.dataType() == DataType.FLOAT32
        ) {
            "Unexpected output type: ${outputTensor.dataType()}"
        }
    }

    private fun loadModel(
        context: Context
    ): ByteBuffer {
        val descriptor = context.assets.openFd(
            MODEL_FILE
        )

        descriptor.use {
            FileInputStream(
                it.fileDescriptor
            ).use { inputStream ->
                val channel = inputStream.channel

                return channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    it.startOffset,
                    it.declaredLength
                )
            }
        }
    }

    fun close() {
        landmarkDetector.close()
        interpreter.close()
    }
}