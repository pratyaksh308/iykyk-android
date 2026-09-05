package com.pratyaksh.iykyk.viewmodel

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pratyaksh.iykyk.video.FaceEmbedder
import com.pratyaksh.iykyk.video.FaceEmbeddingTester
import com.pratyaksh.iykyk.video.FrameDetection
import com.pratyaksh.iykyk.video.VideoProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeViewModel(
    private val videoProcessor: VideoProcessor,
    private val faceEmbeddingTester: FaceEmbeddingTester,
    private val faceEmbedder: FaceEmbedder
) : ViewModel() {

    var selectedVideoUri by mutableStateOf<Uri?>(null)
        private set

    var selectedVideoName by mutableStateOf<String?>(null)
        private set

    var frameDetections by mutableStateOf<List<FrameDetection>>(emptyList())
        private set

    fun onVideoSelected(uri: Uri?) {
        selectedVideoUri = uri

        if (uri == null) {
            selectedVideoName = null
            frameDetections = emptyList()
            return
        }

        selectedVideoName =
            videoProcessor.getVideoName(uri)

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

    override fun onCleared() {
        faceEmbedder.close()
        videoProcessor.close()
        super.onCleared()
    }
}