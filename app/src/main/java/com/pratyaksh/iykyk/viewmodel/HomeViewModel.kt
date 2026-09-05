package com.pratyaksh.iykyk.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pratyaksh.iykyk.video.VideoProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel(
    private val videoProcessor: VideoProcessor
) : ViewModel() {

    var selectedVideoUri by mutableStateOf<Uri?>(null)
        private set

    var selectedVideoName by mutableStateOf<String?>(null)
        private set

    var extractedFrames by mutableStateOf<List<Bitmap>>(emptyList())
        private set

    fun onVideoSelected(uri: Uri?) {
        Log.d(
            "HomeViewModel",
            "CHECKPOINT 1: onVideoSelected() called. URI = $uri"
        )

        selectedVideoUri = uri

        if (uri == null) {
            selectedVideoName = null
            extractedFrames = emptyList()

            Log.d(
                "HomeViewModel",
                "CHECKPOINT: URI was null"
            )

            return
        }

        selectedVideoName = videoProcessor.getVideoName(uri)

        Log.d(
            "HomeViewModel",
            "CHECKPOINT 2: Starting frame extraction"
        )

        viewModelScope.launch(Dispatchers.IO) {

            try {
                val frames = videoProcessor.extractFrames(uri)

                Log.d(
                    "HomeViewModel",
                    "CHECKPOINT 3: Frame extraction completed. Frames = ${frames.size}"
                )

                extractedFrames = frames

                if (frames.isEmpty()) {
                    Log.d(
                        "HomeViewModel",
                        "CHECKPOINT: No frames available. Stopping."
                    )

                    return@launch
                }

                val firstFrame = frames.first()

                Log.d(
                    "HomeViewModel",
                    "CHECKPOINT 4: Starting face detection"
                )

                val faces = videoProcessor.detectFacesInFrame(firstFrame)

                Log.d(
                    "HomeViewModel",
                    "CHECKPOINT 5: Face detection completed. Faces = ${faces.size}"
                )

                faces.forEachIndexed { index, face ->

                    Log.d(
                        "HomeViewModel",
                        """
                        Face ${index + 1}
                        Bounding box: ${face.boundingBox}
                        Head rotation X: ${face.headEulerAngleX}
                        Head rotation Y: ${face.headEulerAngleY}
                        Head rotation Z: ${face.headEulerAngleZ}
                        Left eye open: ${face.leftEyeOpenProbability}
                        Right eye open: ${face.rightEyeOpenProbability}
                        Smiling: ${face.smilingProbability}
                        """.trimIndent()
                    )
                }

            } catch (exception: Exception) {

                Log.e(
                    "HomeViewModel",
                    "PROCESSING ERROR",
                    exception
                )
            }
        }
    }

    override fun onCleared() {
        videoProcessor.close()
        super.onCleared()
    }
}