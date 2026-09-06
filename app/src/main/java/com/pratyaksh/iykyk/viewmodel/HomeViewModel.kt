package com.pratyaksh.iykyk.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pratyaksh.iykyk.video.AppearanceSegment
import com.pratyaksh.iykyk.video.AppearanceSegmenter
import com.pratyaksh.iykyk.video.FaceEmbedder
import com.pratyaksh.iykyk.video.FaceEmbeddingTester
import com.pratyaksh.iykyk.video.FrameDetection
import com.pratyaksh.iykyk.video.RepresentativeFrame
import com.pratyaksh.iykyk.video.RepresentativeFrameLoader
import com.pratyaksh.iykyk.video.RepresentativeFrameSelector
import com.pratyaksh.iykyk.video.VideoProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel(
    private val videoProcessor: VideoProcessor,
    private val faceEmbeddingTester: FaceEmbeddingTester,
    private val faceEmbedder: FaceEmbedder,
    private val appearanceSegmenter: AppearanceSegmenter,
    private val representativeFrameSelector: RepresentativeFrameSelector,
    private val representativeFrameLoader: RepresentativeFrameLoader
) : ViewModel() {

    var selectedVideoUri by mutableStateOf<Uri?>(null)
        private set

    var selectedVideoName by mutableStateOf<String?>(null)
        private set

    var frameDetections by mutableStateOf<List<FrameDetection>>(emptyList())
        private set

    var appearanceSegments by mutableStateOf<List<AppearanceSegment>>(emptyList())
        private set

    var representativeFrames by mutableStateOf<List<RepresentativeFrame>>(emptyList())
        private set

    var representativeBitmaps by mutableStateOf<List<Bitmap>>(emptyList())
        private set

    fun onVideoSelected(uri: Uri?) {
        selectedVideoUri = uri

        if (uri == null) {
            selectedVideoName = null
            frameDetections = emptyList()
            appearanceSegments = emptyList()
            representativeFrames = emptyList()
            representativeBitmaps = emptyList()
            return
        }

        selectedVideoName =
            videoProcessor.getVideoName(uri)

        frameDetections = emptyList()
        appearanceSegments = emptyList()
        representativeFrames = emptyList()
        representativeBitmaps = emptyList()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(
                    "HomeViewModel",
                    "Starting sequential 200ms video processing"
                )

                val startTime =
                    System.currentTimeMillis()

                val results =
                    videoProcessor.processVideo(
                        uri = uri,
                        intervalMs = 200L
                    )

                val elapsedTime =
                    System.currentTimeMillis() -
                            startTime

                frameDetections = results

                Log.d(
                    "HomeViewModel",
                    "Sequential processing completed. " +
                            "Frames=${results.size}, " +
                            "Time=${elapsedTime}ms"
                )

                results.forEach { result ->
                    Log.d(
                        "HomeViewModel",
                        "Timestamp=${result.timestampMs}ms, " +
                                "faces=${result.faces.size}"
                    )
                }
            } catch (exception: Exception) {
                Log.e(
                    "HomeViewModel",
                    "VIDEO PROCESSING ERROR",
                    exception
                )
            }
        }
    }

    fun runEmbeddingTest() {
        val uri = selectedVideoUri
            ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(
                    "HomeViewModel",
                    "Starting face embedding test"
                )

                faceEmbeddingTester.run(uri)

                Log.d(
                    "HomeViewModel",
                    "Face embedding test completed"
                )
            } catch (exception: Exception) {
                Log.e(
                    "HomeViewModel",
                    "FACE EMBEDDING TEST ERROR",
                    exception
                )
            }
        }
    }

    fun runSegmentationTest() {
        val detections = frameDetections
        val uri = selectedVideoUri

        if (detections.isEmpty() || uri == null) {
            Log.w(
                "HomeViewModel",
                "No frame detections or video URI available for segmentation test"
            )
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            var dimensionBitmap: Bitmap? = null

            try {
                Log.d(
                    "HomeViewModel",
                    "Starting temporal appearance segmentation"
                )

                val results =
                    appearanceSegmenter.segment(
                        uri = uri,
                        frameDetections = detections
                    )

                appearanceSegments = results

                Log.d(
                    "HomeViewModel",
                    "Temporal appearance segmentation completed. " +
                            "Segments=${results.size}"
                )

                dimensionBitmap =
                    videoProcessor.extractFrame(
                        uri = uri,
                        timestampMs = 0L
                    )

                if (dimensionBitmap == null) {
                    throw IllegalStateException(
                        "Unable to extract frame for video dimensions"
                    )
                }

                val frameWidth =
                    dimensionBitmap.width

                val frameHeight =
                    dimensionBitmap.height

                Log.d(
                    "HomeViewModel",
                    "Representative frame dimensions: " +
                            "${frameWidth}x${frameHeight}"
                )

                val selectedFrames =
                    results.mapNotNull { segment ->
                        representativeFrameSelector.select(
                            segment = segment,
                            frameWidth = frameWidth,
                            frameHeight = frameHeight
                        )
                    }

                representativeFrames = selectedFrames

                Log.d(
                    "HomeViewModel",
                    "Representative frame selection completed. " +
                            "Frames=${selectedFrames.size}"
                )

                selectedFrames.forEach { representative ->
                    Log.d(
                        "HomeViewModel",
                        "Representative timestamp=${representative.timestampMs}ms, " +
                                "score=${"%.3f".format(representative.score)}"
                    )
                }

                val bitmaps =
                    representativeFrameLoader.load(
                        uri = uri,
                        representativeFrames = selectedFrames
                    )

                representativeBitmaps = bitmaps

                Log.d(
                    "HomeViewModel",
                    "Representative frame loading completed. " +
                            "Bitmaps=${bitmaps.size}"
                )
            } catch (exception: Exception) {
                Log.e(
                    "HomeViewModel",
                    "SEGMENTATION TEST ERROR",
                    exception
                )
            } finally {
                dimensionBitmap?.let {
                    if (!it.isRecycled) {
                        it.recycle()
                    }
                }
            }
        }
    }

    override fun onCleared() {
        representativeBitmaps.forEach { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }

        faceEmbedder.close()
        videoProcessor.close()
        super.onCleared()
    }
}