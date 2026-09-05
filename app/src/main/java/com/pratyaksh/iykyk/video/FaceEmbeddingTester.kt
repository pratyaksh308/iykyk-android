package com.pratyaksh.iykyk.video

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import kotlin.math.sqrt

class FaceEmbeddingTester(
    private val videoProcessor: VideoProcessor,
    private val faceEmbedder: FaceEmbedder
) {
    companion object {
        private const val TAG = "FaceEmbeddingTester"
        private const val SAMPLE_INTERVAL_MS = 500L
        private const val MAX_TEST_FRAMES = 15
        private const val MAX_EVALUATED_PAIRS = 40
    }

    private data class FaceSample(
        val timestampMs: Long,
        val faceIndex: Int,
        val face: DetectedFace,
        val bitmap: Bitmap
    )

    private data class PairEvaluation(
        val sampleA: FaceSample,
        val sampleB: FaceSample,
        val similarity: Float
    )

    suspend fun run(uri: Uri) {
        val frameDetections = videoProcessor.processVideo(
            uri = uri,
            intervalMs = SAMPLE_INTERVAL_MS
        )

        val totalTimestampsSampled = frameDetections.size

        if (frameDetections.isEmpty()) {
            Log.e(
                TAG,
                "No frames sampled from video"
            )
            return
        }

        val detectionsWithFaces = frameDetections.filter { it.faces.isNotEmpty() }

        if (detectionsWithFaces.isEmpty()) {
            Log.e(
                TAG,
                "No faces detected across the entire video"
            )
            return
        }

        val selectedDetections = if (detectionsWithFaces.size <= MAX_TEST_FRAMES) {
            detectionsWithFaces
        } else {
            val step = (detectionsWithFaces.size - 1).toDouble() / (MAX_TEST_FRAMES - 1)
            (0 until MAX_TEST_FRAMES).map { i ->
                val index = (i * step).toInt().coerceIn(0, detectionsWithFaces.size - 1)
                detectionsWithFaces[index]
            }.distinctBy { it.timestampMs }
        }

        val samples = mutableListOf<FaceSample>()

        try {
            for (detection in selectedDetections) {
                val bitmap = videoProcessor.extractFrame(
                    uri = uri,
                    timestampMs = detection.timestampMs
                ) ?: continue

                detection.faces.forEachIndexed { index, face ->
                    samples.add(
                        FaceSample(
                            timestampMs = detection.timestampMs,
                            faceIndex = index,
                            face = face,
                            bitmap = bitmap
                        )
                    )
                }
            }

            if (samples.size < 2) {
                Log.e(
                    TAG,
                    "Insufficient face samples found for pair evaluation. Total faces=${samples.size}"
                )
                return
            }

            Log.d(
                TAG,
                "=================== FULL VIDEO FACE EMBEDDING TEST ==================="
            )

            val allPairs = mutableListOf<Pair<FaceSample, FaceSample>>()
            for (i in 0 until samples.size) {
                for (j in i + 1 until samples.size) {
                    allPairs.add(Pair(samples[i], samples[j]))
                }
            }

            val selectedPairs = if (allPairs.size <= MAX_EVALUATED_PAIRS) {
                allPairs
            } else {
                val step = (allPairs.size - 1).toDouble() / (MAX_EVALUATED_PAIRS - 1)
                (0 until MAX_EVALUATED_PAIRS).map { k ->
                    val idx = (k * step).toInt().coerceIn(0, allPairs.size - 1)
                    allPairs[idx]
                }.distinct()
            }

            val pairEvaluations = mutableListOf<PairEvaluation>()

            for (pair in selectedPairs) {
                val sampleA = pair.first
                val sampleB = pair.second

                val (firstEmbedding, secondEmbedding) = faceEmbedder.embedPair(
                    firstBitmap = sampleA.bitmap,
                    firstBoundingBox = sampleA.face.boundingBox,
                    secondBitmap = sampleB.bitmap,
                    secondBoundingBox = sampleB.face.boundingBox
                )

                val similarity = cosineSimilarity(
                    first = firstEmbedding,
                    second = secondEmbedding
                )

                val evaluation = PairEvaluation(
                    sampleA = sampleA,
                    sampleB = sampleB,
                    similarity = similarity
                )

                pairEvaluations.add(evaluation)
            }

            Log.d(
                TAG,
                "--- PAIRWISE SIMILARITIES (${pairEvaluations.size}) ---"
            )

            pairEvaluations.forEach { eval ->
                logEvaluation(eval)
            }

            val similarities = pairEvaluations.map { it.similarity }
            val minSim = similarities.minOrNull() ?: 0f
            val maxSim = similarities.maxOrNull() ?: 0f
            val avgSim = if (similarities.isNotEmpty()) {
                similarities.average().toFloat()
            } else 0f

            Log.d(
                TAG,
                "--- SUMMARY ---"
            )

            Log.d(
                TAG,
                "Total timestamps sampled: $totalTimestampsSampled"
            )

            Log.d(
                TAG,
                "Total face samples: ${samples.size}"
            )

            Log.d(
                TAG,
                "Total evaluated pairs: ${pairEvaluations.size}"
            )

            Log.d(
                TAG,
                "Minimum similarity: ${"%.4f".format(minSim)}"
            )

            Log.d(
                TAG,
                "Maximum similarity: ${"%.4f".format(maxSim)}"
            )

            Log.d(
                TAG,
                "Average similarity: ${"%.4f".format(avgSim)}"
            )

            Log.d(
                TAG,
                "====================================================================="
            )

        } finally {
            val recycledBitmaps = mutableSetOf<Bitmap>()

            samples.forEach { sample ->
                if (!recycledBitmaps.contains(sample.bitmap)) {
                    recycledBitmaps.add(sample.bitmap)

                    if (!sample.bitmap.isRecycled) {
                        sample.bitmap.recycle()
                    }
                }
            }
        }
    }

    private fun logEvaluation(eval: PairEvaluation) {
        val sampleA = eval.sampleA
        val sampleB = eval.sampleB

        Log.d(
            TAG,
            "Face [t=${sampleA.timestampMs}ms, #${sampleA.faceIndex}] vs " +
                    "Face [t=${sampleB.timestampMs}ms, #${sampleB.faceIndex}] -> " +
                    "Cosine Similarity: ${"%.4f".format(eval.similarity)}"
        )
    }

    private fun cosineSimilarity(
        first: FloatArray,
        second: FloatArray
    ): Float {
        require(first.size == second.size) {
            "Embeddings must have the same size"
        }

        var dotProduct = 0f
        var firstMagnitude = 0f
        var secondMagnitude = 0f

        for (index in first.indices) {
            dotProduct += first[index] * second[index]
            firstMagnitude += first[index] * first[index]
            secondMagnitude += second[index] * second[index]
        }

        val denominator = sqrt(firstMagnitude) * sqrt(secondMagnitude)

        if (denominator == 0f) {
            return 0f
        }

        return dotProduct / denominator
    }
}