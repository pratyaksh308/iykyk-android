package com.pratyaksh.iykyk.viewmodel

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pratyaksh.iykyk.ui.PersonResultItem
import com.pratyaksh.iykyk.video.AppearanceSegment
import com.pratyaksh.iykyk.video.AppearanceSegmenter
import com.pratyaksh.iykyk.video.CollageGenerator
import com.pratyaksh.iykyk.video.DetectedFace
import com.pratyaksh.iykyk.video.FaceEmbedder
import com.pratyaksh.iykyk.video.FrameDetection
import com.pratyaksh.iykyk.video.IdentityGrouper
import com.pratyaksh.iykyk.video.PersonIdentity
import com.pratyaksh.iykyk.video.RepresentativeFrame
import com.pratyaksh.iykyk.video.RepresentativeFrameLoader
import com.pratyaksh.iykyk.video.RepresentativeFrameSelector
import com.pratyaksh.iykyk.video.VideoProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class HomeViewModel(
    private val videoProcessor: VideoProcessor,
    private val faceEmbedder: FaceEmbedder,
    private val appearanceSegmenter: AppearanceSegmenter,
    private val identityGrouper: IdentityGrouper,
    private val representativeFrameSelector: RepresentativeFrameSelector,
    private val representativeFrameLoader: RepresentativeFrameLoader,
    private val collageGenerator: CollageGenerator,
    private val context: Context
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
    var personResults by mutableStateOf<List<PersonResultItem>>(emptyList())
        private set
    var representativeFrames by mutableStateOf<List<RepresentativeFrame>>(emptyList())
        private set
    var representativeBitmaps by mutableStateOf<List<Bitmap>>(emptyList())
        private set
    var collageBitmap by mutableStateOf<Bitmap?>(null)
        private set
    var collageStatus by mutableStateOf<String?>(null)
        private set
    var savedCollageUri by mutableStateOf<Uri?>(null)
        private set
    var videoThumbnailBitmap by mutableStateOf<Bitmap?>(null)
        private set
    var videoDurationText by mutableStateOf("0:30")
        private set
    var isProcessing by mutableStateOf(false)
        private set
    var processingProgress by mutableStateOf(0f)
        private set
    var processingStage by mutableStateOf("getting ready")
        private set

    private var processingJob: Job? = null

    fun cancelProcessing() {
        processingJob?.cancel()
        processingJob = null
        isProcessing = false
        processingCompleted = false
        processingProgress = 0f
        processingStage = "getting ready"
        processingEstimatedTimeLeft = ""
        processingFacesCount = 0
    }

    var processingEstimatedTimeLeft by mutableStateOf("")
        private set
    var processingFacesCount by mutableStateOf(0)
        private set
    var processingCompleted by mutableStateOf(false)
        private set
    var processingError by mutableStateOf<String?>(null)
        private set

    fun onVideoSelected(uri: Uri?) {
        processingJob?.cancel()
        processingJob = null
        selectedVideoUri = uri

        videoThumbnailBitmap?.let {
            if (!it.isRecycled) it.recycle()
        }
        videoThumbnailBitmap = null

        collageBitmap?.let {
            if (!it.isRecycled) it.recycle()
        }
        collageBitmap = null
        collageStatus = null
        savedCollageUri = null
        frameDetections = emptyList()
        appearanceSegments = emptyList()
        personIdentities = emptyList()
        representativeFrames = emptyList()
        representativeBitmaps = emptyList()
        videoProcessor.clearCache()
        processingCompleted = false
        processingError = null
        isProcessing = false
        processingProgress = 0f
        processingStage = "getting ready"
        processingFacesCount = 0
        processingEstimatedTimeLeft = ""

        if (uri == null) {
            selectedVideoName = null
            videoDurationText = "0:30"
            return
        }

        selectedVideoName = videoProcessor.getVideoName(uri)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val thumbnail = videoProcessor.extractFrame(uri = uri, timestampMs = 0L)
                val durationMs = videoProcessor.getVideoDuration(uri)
                val totalSeconds = (durationMs / 1000).toInt()
                val minutes = totalSeconds / 60
                val seconds = totalSeconds % 60
                val formattedDuration = "%d:%02d".format(minutes, seconds)

                videoThumbnailBitmap = thumbnail
                if (durationMs > 0) {
                    videoDurationText = formattedDuration
                }
            } catch (exception: Exception) {
                Log.e("HomeViewModel", "Thumbnail extraction error", exception)
            }
        }
    }

    fun runSegmentationTest() {
        val uri = selectedVideoUri ?: return
        processingJob?.cancel()
        processingJob = null

        if (isProcessing) return

        processingJob = viewModelScope.launch(Dispatchers.Default) {
            isProcessing = true
            processingCompleted = false
            processingError = null
            processingProgress = 0f
            processingFacesCount = 0
            processingStage = "finding faces"
            processingEstimatedTimeLeft = ""

            val startTimeMs = System.currentTimeMillis()

            fun updateProgress(rawProgress: Float) {
                processingProgress = rawProgress.coerceIn(0f, 1f)
                val elapsedMs = System.currentTimeMillis() - startTimeMs
                if (processingProgress > 0.05f && processingProgress < 1.0f) {
                    val estimatedTotalMs = elapsedMs / processingProgress.toDouble()
                    val remainingMs = (estimatedTotalMs - elapsedMs).toLong()
                    val remainingSec = (remainingMs / 1000).coerceAtLeast(1L)
                    processingEstimatedTimeLeft = "~${remainingSec}s left"
                } else {
                    processingEstimatedTimeLeft = ""
                }
            }

            var dimensionBitmap: Bitmap? = null

            try {
                if (videoThumbnailBitmap == null || videoThumbnailBitmap?.isRecycled == true) {
                    try {
                        videoThumbnailBitmap = videoProcessor.extractFrame(uri = uri, timestampMs = 0L)
                    } catch (e: Exception) {
                        Log.e("HomeViewModel", "Failed to extract thumbnail before processing", e)
                    }
                }

                val adaptiveIntervalMs = 100L

                val detections = videoProcessor.processVideo(
                    uri = uri,
                    intervalMs = adaptiveIntervalMs,
                    onProgress = { p -> updateProgress(p * 0.85f) }
                )

                if ((videoThumbnailBitmap == null || videoThumbnailBitmap?.isRecycled == true) && detections.isNotEmpty()) {
                    try {
                        val firstTs = detections.first().timestampMs
                        videoThumbnailBitmap = videoProcessor.extractFrame(uri = uri, timestampMs = firstTs)
                    } catch (e: Exception) {
                        Log.e("HomeViewModel", "Failed to extract thumbnail from first detection", e)
                    }
                }

                frameDetections = detections
                processingFacesCount = detections.sumOf { it.faces.size }
                updateProgress(0.85f)
                processingStage = "tracking appearances"

                val results = appearanceSegmenter.segment(
                    uri = uri,
                    frameDetections = detections,
                    onProgress = { p -> updateProgress(0.85f + p * 0.05f) }
                )

                appearanceSegments = results
                updateProgress(0.90f)
                processingStage = "figuring out who's who"

                val identities = identityGrouper.group(
                    uri = uri,
                    appearanceSegments = results,
                    onProgress = { p -> updateProgress(0.90f + p * 0.04f) }
                )

                personIdentities = identities
                appearanceSegments = identities.flatMap { it.appearances }
                updateProgress(0.94f)
                processingStage = "picking the best shots"

                dimensionBitmap = videoProcessor.extractFrame(uri = uri, timestampMs = 0L)
                    ?: detections.firstOrNull()?.timestampMs?.let { videoProcessor.extractFrame(uri = uri, timestampMs = it) }

                if (dimensionBitmap == null) throw IllegalStateException("Unable to extract frame for video dimensions")

                val frameWidth = dimensionBitmap.width
                val frameHeight = dimensionBitmap.height
                val totalSegments = results.size

                val selectedResults = results.mapIndexed { index, segment ->
                    val result = representativeFrameSelector.select(
                        uri = uri,
                        segment = segment,
                        frameWidth = frameWidth,
                        frameHeight = frameHeight,
                        frameLoader = representativeFrameLoader
                    )?.takeIf { it.second != null }

                    val progressStep = 0.94f + (((index + 1).toFloat() / kotlin.math.max(1, totalSegments).toFloat()) * 0.04f)
                    updateProgress(progressStep)
                    result
                }.filterNotNull()

                val selectedFrames = selectedResults.map { it.first }
                val selectedBitmaps = selectedResults.map { it.second!! }
                representativeFrames = selectedFrames
                representativeBitmaps = selectedBitmaps

                updateProgress(0.98f)
                processingStage = "making your collage"

                val generatedCollage = collageGenerator.generate(
                    identities = identities,
                    representativeFrames = selectedFrames,
                    representativeBitmaps = selectedBitmaps
                )

                collageBitmap?.let { if (!it.isRecycled) it.recycle() }
                collageBitmap = generatedCollage

                if (generatedCollage == null) throw IllegalStateException("Unable to generate collage")

                collageStatus = "Collage generated successfully"
                savedCollageUri = null

                val identityCandidates = identities.map { identity ->
                    val bestCandidate = findBestRepresentativeForIdentity(identity, selectedFrames, selectedBitmaps)
                    identity to bestCandidate
                }

                val bestOverallIdentity = identityCandidates.maxByOrNull { (_, candidate) -> candidate?.frame?.score ?: Float.MIN_VALUE }?.first

                val resultsList = identityCandidates.map { (identity, candidate) ->
                    val faceThumbnail = if (candidate != null) cropFaceThumbnail(candidate.bitmap, candidate.frame.face) ?: candidate.bitmap else null
                    PersonResultItem(
                        id = identity.id,
                        name = identity.name,
                        appearancesCount = identity.appearances.size,
                        isStarShot = identity == bestOverallIdentity,
                        thumbnailBitmap = faceThumbnail
                    )
                }
                personResults = resultsList

                updateProgress(1.0f)
                processingStage = "all done"
                processingEstimatedTimeLeft = ""
                processingCompleted = true
            } catch (exception: Exception) {
                processingError = exception.message ?: "Something went wrong"
                collageStatus = "Collage generation failed: ${exception.message}"
                Log.e("HomeViewModel", "PROCESSING ERROR", exception)
            } finally {
                dimensionBitmap?.let { if (!it.isRecycled) it.recycle() }
                videoProcessor.clearCache()
                isProcessing = false
            }
        }
    }

    private data class RepresentativeCandidate(val frame: RepresentativeFrame, val bitmap: Bitmap)

    private fun findBestRepresentativeForIdentity(
        identity: PersonIdentity,
        representativeFrames: List<RepresentativeFrame>,
        representativeBitmaps: List<Bitmap>
    ): RepresentativeCandidate? {
        val candidates = identity.appearances.mapNotNull { segment ->
            representativeFrames.indices.mapNotNull { index ->
                val frame = representativeFrames[index]
                val bitmap = representativeBitmaps.getOrNull(index)
                if (bitmap != null && frame.timestampMs >= segment.startTimestampMs && frame.timestampMs <= segment.endTimestampMs) {
                    RepresentativeCandidate(frame = frame, bitmap = bitmap)
                } else null
            }.maxByOrNull { it.frame.score }
        }
        return candidates.maxByOrNull { it.frame.score }
    }

    private fun cropFaceThumbnail(bitmap: Bitmap, face: DetectedFace): Bitmap? {
        return try {
            val imageWidth = bitmap.width.toFloat()
            val imageHeight = bitmap.height.toFloat()
            val padding = 1.6f
            val cropWidth = (face.width * padding).coerceAtMost(imageWidth)
            val cropHeight = (face.height * padding).coerceAtMost(imageHeight)
            val left = (face.centerX - cropWidth / 2f).coerceIn(0f, kotlin.math.max(0f, imageWidth - cropWidth))
            val top = (face.centerY - cropHeight * 0.45f).coerceIn(0f, kotlin.math.max(0f, imageHeight - cropHeight))

            Bitmap.createBitmap(
                bitmap,
                left.toInt(),
                top.toInt(),
                cropWidth.toInt().coerceAtMost((imageWidth - left).toInt().coerceAtLeast(1)),
                cropHeight.toInt().coerceAtMost((imageHeight - top).toInt().coerceAtLeast(1))
            )
        } catch (_: Exception) {
            null
        }
    }

    fun resetForNewVideo() {
        onVideoSelected(null)
    }

    fun saveCollage() {
        val bitmap = collageBitmap ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uri = saveBitmapToGallery(bitmap)
                savedCollageUri = uri
                collageStatus = "Collage saved to gallery"
            } catch (exception: Exception) {
                collageStatus = "Failed to save collage: ${exception.message}"
                Log.e("HomeViewModel", "COLLAGE SAVE ERROR", exception)
            }
        }
    }

    fun getCollageShareUri(): Uri? = savedCollageUri

    private fun saveBitmapToGallery(bitmap: Bitmap): Uri {
        val resolver = context.contentResolver
        val fileName = "iykyk_collage_${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/iykyk")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: throw IllegalStateException("Unable to create gallery item")

        try {
            resolver.openOutputStream(uri).use { outputStream ->
                if (outputStream == null) throw IllegalStateException("Unable to open gallery output")
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) throw IllegalStateException("Unable to encode collage")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val completedValues = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                resolver.update(uri, completedValues, null, null)
            }
            return uri
        } catch (exception: Exception) {
            resolver.delete(uri, null, null)
            throw exception
        }
    }

    override fun onCleared() {
        videoThumbnailBitmap?.let { if (!it.isRecycled) it.recycle() }
        collageBitmap?.let { if (!it.isRecycled) it.recycle() }
        representativeBitmaps.forEach { bitmap -> if (!bitmap.isRecycled) bitmap.recycle() }
        faceEmbedder.close()
        videoProcessor.close()
        super.onCleared()
    }
}