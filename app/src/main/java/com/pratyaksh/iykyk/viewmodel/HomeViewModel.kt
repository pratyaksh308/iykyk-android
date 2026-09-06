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
import com.pratyaksh.iykyk.video.CollageGenerator
import com.pratyaksh.iykyk.video.FaceEmbedder
import com.pratyaksh.iykyk.video.FaceEmbeddingTester
import com.pratyaksh.iykyk.video.FrameDetection
import com.pratyaksh.iykyk.video.IdentityGrouper
import com.pratyaksh.iykyk.video.PersonIdentity
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
    private val identityGrouper: IdentityGrouper,
    private val representativeFrameSelector: RepresentativeFrameSelector,
    private val representativeFrameLoader: RepresentativeFrameLoader,
    private val collageGenerator: CollageGenerator
) : ViewModel() {

    var selectedVideoUri by mutableStateOf<Uri?>(null)
        private set

    var selectedVideoName by mutableStateOf<String?>(null)
        private set

    var frameDetections by mutableStateOf<List<FrameDetection>>(emptyList())
        private set

    var appearanceSegments by mutableStateOf<List<AppearanceSegment>>(emptyList())
        private set

    var personIdentities by mutableStateOf<List<PersonIdentity>>(emptyList())
        private set

    var representativeFrames by mutableStateOf<List<RepresentativeFrame>>(emptyList())
        private set

    var representativeBitmaps by mutableStateOf<List<Bitmap>>(emptyList())
        private set

    var collageBitmap by mutableStateOf<Bitmap?>(null)
        private set

    var collageStatus by mutableStateOf<String?>(null)
        private set

    fun onVideoSelected(uri: Uri?) {
        selectedVideoUri = uri

        collageBitmap?.let {
            if (!it.isRecycled) {
                it.recycle()
            }
        }

        collageBitmap = null
        collageStatus = null

        if (uri == null) {
            selectedVideoName = null
            frameDetections = emptyList()
            appearanceSegments = emptyList()
            personIdentities = emptyList()
            representativeFrames = emptyList()
            representativeBitmaps = emptyList()
            return
        }

        selectedVideoName =
            videoProcessor.getVideoName(uri)

        frameDetections = emptyList()
        appearanceSegments = emptyList()
        personIdentities = emptyList()
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
        val uri =
            selectedVideoUri
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
        val detections =
            frameDetections

        val uri =
            selectedVideoUri

        if (
            detections.isEmpty() ||
            uri == null
        ) {
            Log.w(
                "HomeViewModel",
                "No frame detections or video URI available for segmentation test"
            )
            return
        }

        viewModelScope.launch(Dispatchers.Default) {
            var dimensionBitmap: Bitmap? = null

            try {
                collageStatus =
                    "Processing collage..."

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

                val identities =
                    identityGrouper.group(
                        uri = uri,
                        appearanceSegments = results
                    )

                personIdentities =
                    identities

                Log.d(
                    "HomeViewModel",
                    "Identity grouping completed. " +
                            "Identities=${identities.size}"
                )

                identities.forEach { identity ->
                    Log.d(
                        "HomeViewModel",
                        "Identity ${identity.id}: " +
                                "${identity.appearances.size} appearances"
                    )
                }

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

                val selectedResults =
                    results.mapNotNull { segment ->
                        representativeFrameSelector.select(
                            uri = uri,
                            segment = segment,
                            frameWidth = frameWidth,
                            frameHeight = frameHeight,
                            frameLoader = representativeFrameLoader
                        )?.takeIf {
                            it.second != null
                        }
                    }

                val selectedFrames =
                    selectedResults.map {
                        it.first
                    }

                val selectedBitmaps =
                    selectedResults.map {
                        it.second!!
                    }

                representativeFrames =
                    selectedFrames

                representativeBitmaps =
                    selectedBitmaps

                Log.d(
                    "HomeViewModel",
                    "Representative frame selection completed. " +
                            "Frames=${selectedFrames.size}, " +
                            "Bitmaps=${selectedBitmaps.size}"
                )

                selectedFrames.forEach { representative ->
                    Log.d(
                        "HomeViewModel",
                        "Representative timestamp=${representative.timestampMs}ms, " +
                                "score=${"%.3f".format(representative.score)}, " +
                                "rawSharpness=${"%.1f".format(representative.rawSharpness)}"
                    )
                }

                Log.d(
                    "HomeViewModel",
                    "Starting collage generation. " +
                            "Identities=${identities.size}, " +
                            "Representatives=${selectedFrames.size}"
                )

                val generatedCollage =
                    collageGenerator.generate(
                        identities = identities,
                        representativeFrames = selectedFrames,
                        representativeBitmaps = selectedBitmaps
                    )

                collageBitmap?.let {
                    if (!it.isRecycled) {
                        it.recycle()
                    }
                }

                collageBitmap =
                    generatedCollage

                if (generatedCollage != null) {
                    collageStatus =
                        "Collage generated successfully"

                    Log.d(
                        "HomeViewModel",
                        "Collage generation completed successfully. " +
                                "Size=${generatedCollage.width}x${generatedCollage.height}"
                    )
                } else {
                    collageStatus =
                        "Collage generation returned no image"

                    Log.e(
                        "HomeViewModel",
                        "Collage generation returned null"
                    )
                }
            } catch (exception: Exception) {
                collageStatus =
                    "Collage generation failed: ${exception.message}"

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
        collageBitmap?.let {
            if (!it.isRecycled) {
                it.recycle()
            }
        }

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