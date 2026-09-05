package com.pratyaksh.iykyk.video

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.sqrt

class FaceEmbedder(
    context: Context
) {
    companion object {
        private const val TAG = "FaceEmbedder"
        private const val MODEL_FILE = "MobileFaceNet.tflite"
        private const val INPUT_SIZE = 112
        private const val EMBEDDING_SIZE = 192
        private const val BATCH_SIZE = 2
        private const val PIXEL_MEAN = 127.5f
        private const val PIXEL_STD = 128f
    }

    private val interpreter: Interpreter

    init {
        interpreter = Interpreter(
            loadModel(context),
            Interpreter.Options().apply {
                setNumThreads(4)
            }
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

        return try {
            embedFace(faceBitmap)
        } finally {
            if (faceBitmap !== bitmap && !faceBitmap.isRecycled) {
                faceBitmap.recycle()
            }
        }
    }

    fun embedPair(
        firstBitmap: Bitmap,
        firstBoundingBox: Rect,
        secondBitmap: Bitmap,
        secondBoundingBox: Rect
    ): Pair<FloatArray, FloatArray> {
        val firstFaceBitmap = cropFace(
            bitmap = firstBitmap,
            boundingBox = firstBoundingBox
        )

        val secondFaceBitmap = cropFace(
            bitmap = secondBitmap,
            boundingBox = secondBoundingBox
        )

        return try {
            embedPair(
                firstBitmap = firstFaceBitmap,
                secondBitmap = secondFaceBitmap
            )
        } finally {
            if (!firstFaceBitmap.isRecycled) {
                firstFaceBitmap.recycle()
            }

            if (!secondFaceBitmap.isRecycled) {
                secondFaceBitmap.recycle()
            }
        }
    }

    fun embedPair(
        firstBitmap: Bitmap,
        secondBitmap: Bitmap
    ): Pair<FloatArray, FloatArray> {
        require(firstBitmap.width > 0 && firstBitmap.height > 0) {
            "First bitmap must have positive dimensions"
        }

        require(secondBitmap.width > 0 && secondBitmap.height > 0) {
            "Second bitmap must have positive dimensions"
        }

        val firstResizedBitmap = Bitmap.createScaledBitmap(
            firstBitmap,
            INPUT_SIZE,
            INPUT_SIZE,
            true
        )

        val secondResizedBitmap = Bitmap.createScaledBitmap(
            secondBitmap,
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
                bitmap = firstResizedBitmap,
                input = input
            )

            addBitmapToInput(
                bitmap = secondResizedBitmap,
                input = input
            )

            input.rewind()

            val output = Array(BATCH_SIZE) {
                FloatArray(EMBEDDING_SIZE)
            }

            val startTime = System.nanoTime()

            interpreter.run(
                input,
                output
            )

            val inferenceTimeMs =
                (System.nanoTime() - startTime) / 1_000_000

            val firstEmbedding = normalizeEmbedding(output[0])
            val secondEmbedding = normalizeEmbedding(output[1])

            Log.d(
                TAG,
                "Pair inference time=${inferenceTimeMs}ms"
            )

            Log.d(
                TAG,
                "First embedding size=${firstEmbedding.size}"
            )

            Log.d(
                TAG,
                "Second embedding size=${secondEmbedding.size}"
            )

            Log.d(
                TAG,
                "First embedding magnitude=${"%.6f".format(
                    embeddingMagnitude(firstEmbedding)
                )}"
            )

            Log.d(
                TAG,
                "Second embedding magnitude=${"%.6f".format(
                    embeddingMagnitude(secondEmbedding)
                )}"
            )

            return Pair(
                firstEmbedding,
                secondEmbedding
            )
        } finally {
            if (!firstResizedBitmap.isRecycled) {
                firstResizedBitmap.recycle()
            }

            if (!secondResizedBitmap.isRecycled) {
                secondResizedBitmap.recycle()
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

            addBitmapToInput(
                bitmap = resizedBitmap,
                input = input
            )

            input.rewind()

            val output = Array(BATCH_SIZE) {
                FloatArray(EMBEDDING_SIZE)
            }

            val startTime = System.nanoTime()

            interpreter.run(
                input,
                output
            )

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

        return Bitmap.createBitmap(
            bitmap,
            left,
            top,
            right - left,
            bottom - top
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

        return FloatArray(embedding.size) { index ->
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
        interpreter.close()
    }
}